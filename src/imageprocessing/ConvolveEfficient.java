package imageprocessing;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import utils.Parallel;

import javax.swing.*;

public class ConvolveEfficient implements IImageProcessor
{
    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        Object[] outputTypes = {"Box", "Gauss", "Pewitt"};
        int ch = JOptionPane.showOptionDialog(null, "Choose the output", "Convert to Gray:",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, outputTypes, outputTypes[0]);
        int[][] filter = {};
        float[] filterX = {};
        float[] filterY = {};
        float normX = 0;
        float normY = 0;
        switch(ch){
            case 0:
                filterX = new float[] { 1, 1, 1, 1, 1};
                filterY = new float[] { 1, 1, 1, 1, 1};
                normX = 5;
                normY = 5;
                break;
            case 1:
                filter = new int[][]{  {0,1,2,1,0},
                        {1,3,5,3,1},
                        {2,5,9,5,2},
                        {1,3,5,3,1},
                        {0,1,2,1,0},
                };
                break;
            case 2:
                filterX = new float[] { 1,1,1};
                filterY = new float[] { 1, 0, -1};
                normX = 3;
                normY = 1;
                break;
        }

        Object[] algo = {"x", "y", "xy"};
        int ch2 = JOptionPane.showOptionDialog(null, "Choose the output", "Convert to Gray:",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, algo, outputTypes[0]);

        ImageData outData = null;
        switch (ch2){
            case 0:
                outData = convolveX( inData, filterX, normX);
                break;
            case 1:
                outData = convolveY( inData, filterY, normY );
                break;
            case 2:
                outData = convolveX( inData, filterX, normX);
                outData = convolveY( outData, filterY, normY);
                break;
        }
        return outData;
    }

    public static ImageData convolveX(ImageData inData, float[] filterX, float norm) {
        final int middle = filterX.length/2;
        ImageData outData = new ImageData(inData.width, inData.height, inData.depth, inData.palette);

        Parallel.For(0, inData.height, v -> {
            for (int u = 0; u < inData.width; u++) {

                RGB newPixel = new RGB(0, 0, 0);

                int uu = u - middle;

                for (int x = 0; x < filterX.length; x++) {
                    newPixel.red += getPixel(uu, v, inData).red * filterX[x];
                    newPixel.green += getPixel(uu, v, inData).green * filterX[x];
                    newPixel.blue += getPixel(uu, v, inData).blue * filterX[x];
                    uu++;
                }

                newPixel.red = ImageProcessing.clamp8(newPixel.red/norm);
                newPixel.green = ImageProcessing.clamp8(newPixel.green/norm);
                newPixel.blue = ImageProcessing.clamp8(newPixel.blue/norm);

                outData.setPixel(u, v, outData.palette.getPixel(newPixel));
            }
        });

        return outData;
    }

    public static ImageData convolveY(ImageData inData, float[] filterY, float norm) {
        final int middle = filterY.length/2;
        ImageData outData = new ImageData(inData.width, inData.height, inData.depth, inData.palette);

        Parallel.For(0, inData.height, v -> {
            for (int u = 0; u < inData.width; u++) {
                RGB newPixel = new RGB(0, 0, 0);

                int vv = v - middle;

                for (int y = 0; y < filterY.length; y++) {
                    newPixel.red += getPixel(u, vv, inData).red * filterY[y];
                    newPixel.green += getPixel(u, vv, inData).green * filterY[y];
                    newPixel.blue += getPixel(u, vv, inData).blue * filterY[y];
                    vv++;
                }

                newPixel.red = ImageProcessing.clamp8(newPixel.red/norm);
                newPixel.green = ImageProcessing.clamp8(newPixel.green/norm);
                newPixel.blue = ImageProcessing.clamp8(newPixel.blue/norm);

                outData.setPixel(u, v, outData.palette.getPixel(newPixel));
            }
        });
        return outData;
    }

    ImageData convolveXY(float[][] data, float[] filterX, float[] filterY) {
        return null;
    }

    private static RGB getPixel(int u, int v, ImageData data){
        if(u < 0) {
            u = u*(-1) - 1;
        } else if ( u >= data.width) {
            u = u - (u - data.width) - 1;
        }

        if(v < 0) {
            v = v*(-1) - 1;
        } else if ( v >= data.height) {
            v = v - (v - data.height) - 1;
        }
        return data.palette.getRGB(data.getPixel(u,v));
    }
}
