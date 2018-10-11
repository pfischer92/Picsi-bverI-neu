package imageprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import utils.Parallel;
import utils.RGB2;

public class AllRGB implements IImageProcessor {

    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

//	private void setPixel(ImageData data, int x, int y, int pixel){
//		if(x >= 0 && x < data.width && y >= 0 && y < data.height){
//			data.setPixel(x, y, 0xffffff);
//		}
//	}

    static class Triangle {

        private float detv0v1;
        private float detv0v2;
        private float detv1v2;
        private float x0;
        private float x1;
        private float x2;
        private float y0;
        private float y1;
        private float y2;
        public Triangle(float x0, float y0, float x1, float y1, float x2, float y2){
            this.x0 = x0;
            this.x1 = x1-x0;
            this.x2 = x2-x0;
            this.y0 = y0;
            this.y1 = y1-y0;
            this.y2 = y2-y0;
            //barycentric coordinates
            //http://mathworld.wolfram.com/TriangleInterior.html
            //http://stackoverflow.com/questions/13300904/determine-whether-point-lies-inside-triangle
//			alpha = (det(v v2) - det(v0 v2)) / det(v1 v2)
//			beta = - ( (det(v v1) - det(v0 v1)) / det(v1 v2) )
            detv0v1 = this.x0*this.y1 - this.y0*this.x1;
            detv0v2 = this.x0*this.y2 - this.y0*this.x2;
            detv1v2 = this.x1*this.y2 - this.y1*this.x2;
        }

        public boolean inside(float x, float y){
            float detvv2 = x*y2 - y*x2;
            float detvv1 = x*y1 - y*x1;
            float alpha = (detvv2 - detv0v2) / detv1v2;
            float beta = - ( (detvv1 - detv0v1) / detv1v2 );
            return alpha > 0 && beta > 0 && alpha + beta < 1;
        }

        public int fill(Iterator<RGB2> it, ImageData outData){
            int count = 0;
            float x1 = this.x1 + this.x0;
            float x2 = this.x2 + this.x0;
            float y1 = this.y1 + this.x0;
            float y2 = this.y2 + this.x0;
            int maxx = Math.min(outData.width,  (int) Math.ceil(Math.max(x0, Math.max(x2, x1))));
            int maxy = Math.min(outData.height, (int) Math.ceil(Math.max(y0, Math.max(y2, y1))));
            for(int u = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x2, x1)))); u < maxx; ++u){
                for(int v = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y2, y1)))); v < maxy; ++v){
                    if(inside(u, v) && outData.getPixel(u, v) == 0){
                        if(it.hasNext()){ ++count; }
                        outData.setPixel(u, v, it.hasNext() ? outData.palette.getPixel(it.next().rgb) : 0);
                    }
                }
            }
            return count;
        }
    }

    @Override
    public ImageData run(ImageData imageData, int imageType) {
        final ImageData outData = new ImageData(4096, 4096, 24, imageData.palette);
        String[] options = new String[]{ "boring", "linear", "logarithmic spiral", "green", "pacman" };
        int option = JOptionPane.showOptionDialog(null, "Choose the ImageType", "AllRGB", 0, JOptionPane.QUESTION_MESSAGE, null, options, "");

        switch (option) {
            //Iterative
            case 0:
                Parallel.For(0, outData.height, y -> {
                    for(int x = 0; x < outData.width; ++x){
                        outData.setPixel(x, y, x + (outData.width * y));
                    }
                });
                break;
            case 1:
            case 2:
            case 3:
            case 4:
                List<RGB2> rgbs = new ArrayList<RGB2>(4096*4096);
                for(int x = 0; x < 0x1000000; ++x){
                    rgbs.add(new RGB2(outData.palette.getRGB(x)));
                }
                switch (option) {
                    case 3:
                    case 4:
                        rgbs.sort(new CompGreen());
                        break;
                    default:
                        rgbs.sort(new Comp15());
                        break;
                }
                Iterator<RGB2> it = rgbs.iterator();

                switch(option){
                    case 1:
                        drawLinear(it, outData);
                        break;
                    case 2:
                        drawSpiral(it, outData);
                        break;
                    case 3:
                    case 4:
                        drawCircle(rgbs, outData, 1000, option == 4);
                }

                break;
            default:
                break;
        }

        return outData;
    }

    private void drawLinear(Iterator<RGB2> it, ImageData outData){
        for(int y = 0; y < outData.height; ++y){
            for(int x = 0; x < outData.width; ++x){
                if(outData.getPixel(x, y) == 0){
                    outData.setPixel(x, y, it.hasNext() ? outData.palette.getPixel(it.next().rgb) : 0);
                }
            }
        }
    }

    private void drawSpiral(Iterator<RGB2> it, ImageData outData){
        float m = 4096/2;
        boolean first = true;
        float x0 = 0, y0 = 0;
        for(double i = 0.01; i < 50.0 && it.hasNext(); i+=0.01){
            double factor = Math.pow(Math.E, 0.3*i);
            double x = factor * Math.cos(i) + m;
            double y = factor * Math.sin(i) + m;
            if(first){
                first = false;
            } else {
                Triangle t = new Triangle(m, m, x0, y0, (float)x, (float)y);
                t.fill(it, outData);
            }
            x0 = (float) x;
            y0 = (float) y;
//			setPixel(outData, (int)x, (int)y, 0xffffff);
        }
    }

    private void drawCircle(List<RGB2> rgbs, ImageData outData, int r, boolean pac){
        float m = 4096/2;
        boolean first = true;
        float x0 = 0, y0 = 0;
        //randomize circle colors
        Iterator<RGB2> it = rgbs.iterator();
        if(r == 1000){
            List<RGB2> rgbs2 = rgbs.stream().limit(pac ? 2489836 : 3141380).collect(Collectors.toList());
            Collections.shuffle(rgbs2);
            it = rgbs2.iterator();
        }
        int count = 0;
        for(double i = 0.0 + (pac ? 0.7 : 0); i + (pac ? 0.7 : 0) < 2*Math.PI+0.1 && it.hasNext(); i+=0.01){
            double x = r*Math.cos(i) + m;
            double y = r*Math.sin(i) + m;
            if(first){
                first = false;
            } else {
                Triangle t = new Triangle(m, m, x0, y0, (float)x, (float)y);
                count += t.fill(it, outData);
            }
            x0 = (float) x;
            y0 = (float) y;
        }
        System.out.println(count);

        List<RGB2> remaining = rgbs.stream().skip(count).collect(Collectors.toList());
        Collections.shuffle(remaining);
        drawLinear(remaining.iterator(), outData);
    }

    static class CompH implements Comparator<RGB2> {
        @Override
        public int compare(RGB2 o1, RGB2 o2) {
//			return Float.compare(Math.round(o1.h), Math.round(o2.h));
            return Float.compare(o1.h, o2.h);
        }
    }
    static class CompS implements Comparator<RGB2> {
        @Override
        public int compare(RGB2 o1, RGB2 o2) {
//			return Float.compare(Math.round(o1.s*100)/100, Math.round(o2.s*100)/100);
            return Float.compare(o1.s, o2.s);
        }
    }
    static class CompB implements Comparator<RGB2> {
        @Override
        public int compare(RGB2 o1, RGB2 o2) {
//			return Float.compare(Math.round(o1.b*100)/100, Math.round(o2.b*100)/100);
            return Float.compare(o1.b, o2.b);
        }
    }
    static class Comp3 implements Comparator<RGB2> {
        @Override
        public int compare(RGB2 o1, RGB2 o2) {
            return Float.compare(o1.b+o1.s+(o1.h/180f), o2.b+o2.s+(o2.h/180f));
        }
    }
    static class Comp3Inv implements Comparator<RGB2> {
        @Override
        public int compare(RGB2 o2, RGB2 o1) {
            return Float.compare(o1.b+o1.s+(o1.h/180f), o2.b+o2.s+(o2.h/180f));
        }
    }
    static class Comp15 implements Comparator<RGB2> {
        @Override
        public int compare(RGB2 o1, RGB2 o2) {
            return Float.compare(o1.b+o1.s+(o1.h/36f), o2.b+o2.s+(o2.h/36f));
        }
    }
    static class Comp15Inv implements Comparator<RGB2> {
        @Override
        public int compare(RGB2 o2, RGB2 o1) {
            return Float.compare(o1.b+o1.s+(o1.h/36f), o2.b+o2.s+(o2.h/36f));
        }
    }
    static class CompGreen implements Comparator<RGB2> {
        @Override
        public int compare(RGB2 o2, RGB2 o1) {
            return Integer.compare(o1.rgb.green*2 - o1.rgb.blue - o1.rgb.red, o2.rgb.green*2 - o2.rgb.blue - o2.rgb.red);
        }
    }
}