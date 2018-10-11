package imageprocessing;

import main.Picsi;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import javax.swing.*;

public class EdgeDetection implements IImageProcessor
{
    @Override
    public boolean isEnabled(int imageType)
    {
        return imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType)
    {
        Object[] outputTypes = {"x direction", "y direction", "combined"};
        int ch = JOptionPane.showOptionDialog(null, "Choose the edge detection direction ", "Apply Prewitt-filter:",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, outputTypes, outputTypes[0]);
        int[][] filter = {};
        int norm = 0;
        switch(ch){
            case 0:
                filter = new int[][]{  {-1,0,1},
                        {-1,0,1},
                        {-1,0,1},
                };
                break;
            case 1:
                filter = new int[][]{  {-1,-1,-1},
                        {0,0,0},
                        {1,1,1},
                };
                break;
            case 2:
                filter = new int[][]{  {-1,0,1},
                        {-1,0,1},
                        {-1,0,1},
                };


                norm = getNorm(filter);

                inData = Filter.convolve(inData, imageType, filter, norm, 0);

                filter = new int[][]{  {-1,-1,-1},
                        {0,0,0},
                        {1,1,1},
                };
                break;
        }
        norm = getNorm(filter);

        ImageData outData = Filter.convolve(inData, imageType, filter, norm, 0);
        return outData;
    }

    public static int getNorm(int[][] filter){
        int norm = 0;
        for(int[] j : filter) {
            for (int i : j) {
                norm += i;
            }
        }
        if (norm == 0) norm = 1;

        return norm;
    }
}
