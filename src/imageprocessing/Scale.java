package imageprocessing;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import utils.Parallel;

import javax.swing.*;

public class Scale implements IImageProcessor
{
    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        Object[] operations = { "Nearest Neighbor", "Bilinear" };
        float scaleFactor = Float.parseFloat(JOptionPane.showInputDialog("Choose scalefactor: "));
        int selection = JOptionPane.showOptionDialog(null, "Choose Interpolation methode", "Interpolation", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, operations, operations[0]);
        if(selection < 0) return null;

        ImageData original = (ImageData) inData.clone();
        ImageData outData  = new ImageData((int) (original.width*scaleFactor), (int) (original.height*scaleFactor), original.depth, original.palette);
        double ratio = 1./scaleFactor;

        switch(selection) {
            case 0 :
                //Nearest Neighbor Interpolation
                Parallel.For(0, outData.width, u -> {
                    for (int v=0; v < outData.height; v++) {
                        int x = (int) (u*ratio);
                        int y = (int) (v*ratio);
                        outData.setPixel(u, v, original.getPixel(x, y));
                    }
                });
                break;
            case 1 :
                //Bilineare Interpolation
                Parallel.For(0, outData.width, u -> {
                    for (int v=0; v < outData.height; v++) {
                        int x = (int) (u*ratio);
                        int y = (int) (v*ratio);
                        double x_diff = (u*ratio) - x;
                        double y_diff = (v*ratio) - y;

                        //Nicht am Rand
                        if(x > 0 && x < original.width-1 && y > 0 && y < original.height-1) {
                            int A = original.getPixel(x, y);
                            int B = original.getPixel(x+1, y);
                            int C = original.getPixel(x, y+1);
                            int D = original.getPixel(x+1,y+1);
                            //Bilineare Funktion zur Intensitätsbestimmung
                            int intensity = (int) (A*(1-x_diff)*(1-y_diff)
                                    + B*(x_diff)*(1-y_diff)
                                    + C*(y_diff)*(1-x_diff)
                                    + D*(x_diff*y_diff));
                            outData.setPixel(u, v, intensity);
                        } else { //am Rand Pixel ohne Interpolation übernehmen
                            outData.setPixel(u, v, original.getPixel(x, y));
                        }
                    }
                });
        }
        return outData;
    }
}
