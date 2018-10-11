package imageprocessing;

import javax.swing.JOptionPane;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import org.eclipse.swt.graphics.RGB;
import utils.Parallel;

public class Filter implements IImageProcessor
{
    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        Object[] outputTypes = {"Box", "Gauss", "Laplace"};
        int ch = JOptionPane.showOptionDialog(null, "Choose the output", "Convert to Gray:",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, outputTypes, outputTypes[0]);
        int[][] filter = {};
        switch(ch){
            case 0:
                filter = new int[][]{  {1,1,1,1,1},
                        {1,1,1,1,1},
                        {1,1,1,1,1},
                        {1,1,1,1,1},
                        {1,1,1,1,1},
                };
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
                /*filter = new int[][]{  {0,0,-1,0,0},
                                        {0,-1,-2,-1,0},
                                        {-1,-2,16,-2,-1},
                                        {0,-1,-2,-1,0},
                                        {0,0,-1,0,0},
                };*/
                filter = new int[][]{  {0,1,0},
                        {1,-4,1},
                        {0,1,0}
                };
                break;
        }



        int norm = 0;
        for(int[] j : filter) {
            for (int i : j) {
                norm += i;
            }
        }
        if (norm == 0) norm = 1;

        ImageData outData = convolve(inData, imageType, filter, norm, 0);
        return outData;
    }

    public static ImageData convolve (ImageData inData, int imageType, int[][] filter, int norm, int offset) {
        ImageData outData = new ImageData(inData.width, inData.height, inData.depth, inData.palette);
        final int middlePointX;
        final int middlePointY;
        if(filter.length%2 != 1 || filter[0].length%2 != 1) {
            throw new IllegalArgumentException("Filter has to be a middle");
        } else {
            middlePointX = filter.length/2; //5/2 = 2 -> in Arraylocation correct
            middlePointY = filter[0].length/2;
        }

        Parallel.For(0, inData.height, v -> {
            for (int u = 0; u < inData.width; u++) {
                RGB newPixel = new RGB(0,0,0);

                int uu = u - middlePointX;
                int vv = v - middlePointY;

                for(int x = 0; x < filter.length; x++ ) {
                    for( int y = 0; y < filter.length; y++) {
                        newPixel.red += getPixel(uu,vv,inData).red * filter[x][y];
                        newPixel.green += getPixel(uu,vv,inData).green * filter[x][y];
                        newPixel.blue += getPixel(uu,vv,inData).blue * filter[x][y];
                        vv++;
                    }
                    uu++;
                    vv = v - middlePointY;
                }

                newPixel.red += offset;
                newPixel.red /= norm;
                newPixel.green += offset;
                newPixel.green /= norm;
                newPixel.blue += offset;
                newPixel.blue /= norm;

                newPixel.red = ImageProcessing.clamp8(newPixel.red);
                newPixel.green = ImageProcessing.clamp8(newPixel.green);
                newPixel.blue = ImageProcessing.clamp8(newPixel.blue);

                outData.setPixel(u, v, outData.palette.getPixel(newPixel));
            }
        });

        return outData;
    }

    private static RGB getPixel(int u, int v, ImageData data) {
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
