package imageprocessing;

import javax.swing.JOptionPane;

import main.Picsi;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import utils.Parallel;

public class FilterMaster implements IImageProcessor
{
    @Override
    public boolean isEnabled(int imageType) {
        return imageType != Picsi.IMAGE_TYPE_INDEXED;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {

        String[] options = new String[]{ "Box", "Gauss", "Laplace", "Edge", "Partial-X", "Partial-Y" };
        int option = JOptionPane.showOptionDialog(null, "Choose the Filter", "Filter", 0, JOptionPane.QUESTION_MESSAGE, null, options, "");

        ImageData outData;
        switch (option) {
            case 0:
                outData = convolve(inData, new double[][]{ { 1, 1, 1 }, { 1, 1, 1 }, { 1, 1, 1 } }, 9);
                break;
            case 1:
                outData = convolve(inData,
                        new double[][]{ { 0, 1, 2, 1, 0 },
                                { 1, 3, 5, 3, 1 },
                                { 2, 5, 9, 5, 2 },
                                { 1, 3, 5, 3, 1 },
                                { 0, 1, 2, 1, 0 } }, 57);
                break;
            case 2:
                outData = convolve(inData,
                        new double[][]{ {  0,  0, -1,  0,  0 },
                                {  0, -1, -2, -1,  0 },
                                { -1, -2, 16, -2, -1 },
                                {  0, -1, -2, -1,  0 },
                                {  0,  0, -1,  0,  0 } });
                break;
            case 3:
                final ImageData dx = convolve(inData, new double[][]{ { -1,  0,  1 }, { -1, 0, 1 }, { -1, 0, 1 } });
                final ImageData dy = convolve(inData, new double[][]{ { -1, -1, -1 }, {  0, 0, 0 }, {  1, 1, 1 } });
                final ImageData o = (ImageData)inData.clone();
                Parallel.For(0, o.height, v -> {
                    for(int u = 0; u < o.width; ++u){
                        RGB cx = dx.palette.getRGB(dx.getPixel(u, v));
                        RGB cy = dy.palette.getRGB(dy.getPixel(u, v));
                        o.setPixel(u, v, o.palette.getPixel(new RGB(
                                clamp((int) Math.sqrt(cx.red*cx.red + cy.red*cy.red)),
                                clamp((int) Math.sqrt(cx.green*cx.green + cy.green*cy.green)),
                                clamp((int) Math.sqrt(cx.blue*cx.blue + cy.blue*cy.blue))
                        )));
                    }
                });
                outData = o;
                break;
            case 4:
                outData = convolve(inData, new double[][]{ { -1,  0,  1 }, { -1, 0, 1 }, { -1, 0, 1 } });
                break;
            case 5:
                outData = convolve(inData, new double[][]{ { -1, -1, -1 }, {  0, 0, 0 }, {  1, 1, 1 } });
                break;
            default:
                outData = null;
                throw new IllegalArgumentException();
        }

        return outData;
    }

    public static int clamp(int c){ return c < 0 ? 0 : (c > 255 ? 255 : c); }
    public static RGB clampRGB(RGB c){
        c.red = clamp(c.red);
        c.green = clamp(c.green);
        c.blue = clamp(c.blue);
        return c;
    }
    public static ImageData convolve(ImageData inData, double[][] filter){ //standard
        return convolve(inData, filter, (int)Math.round((double)filter.length / 2.0), (int)Math.round((double)filter[0].length / 2.0), 1, 0);
    }
    public static ImageData convolve(ImageData inData, double[][] filter, int norm){ //standard
        return convolve(inData, filter, (int)Math.round((double)filter.length / 2.0), (int)Math.round((double)filter[0].length / 2.0), norm, 0);
    }
    public static ImageData convolve(ImageData inData, double[][] filter, int middleX, int middleY, int norm, int offset){
        ImageData outData = (ImageData)inData.clone();
        //Norm filter
        double[][] H = filter.clone();
        if(norm != 1){
            for(int y = 0; y < H.length; ++y){
                for(int x = 0; x < H[y].length; ++x){
                    H[y][x] /= (double)norm;
                }
            }
        }
        //Apply filter
        Parallel.For(0, outData.height, v -> {
            for(int u = 0; u < outData.width; ++u){
                //Per pixel: Calculate filter:
                double r = 0, g = 0, b = 0;
                for(int y = -middleY; y < H.length - middleY; ++y){
                    for(int x = -middleX; x < H[y+middleY].length - middleX; ++x){
                        RGB c = getRGB(inData, u+x, v+y);
                        r += c.red * H[y+middleY][x+middleX] + offset;
                        g += c.green * H[y+middleY][x+middleX] + offset;
                        b += c.blue * H[y+middleY][x+middleX] + offset;
                    }
                }

                outData.setPixel(u, v, outData.palette.getPixel(new RGB(clamp((int)(Math.round(r))), clamp((int)(Math.round(g))), clamp((int)(Math.round(b))))));
            }
        });
        return outData;
    }
    public static RGB getRGB(ImageData inData, int u, int v){
        //Upper bound
        u = u < inData.width ? u : inData.width - u - 1 + inData.width;
        v = v < inData.height ? v : inData.height - v - 1 + inData.height;
        //Lower bound
        u = Math.abs(u) % inData.width;
        v = Math.abs(v) % inData.height;
        return inData.palette.getRGB(inData.getPixel(u, v));
    }
}
