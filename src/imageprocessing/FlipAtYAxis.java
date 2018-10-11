package imageprocessing;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import utils.Matrix;
import utils.Parallel;

public class FlipAtYAxis implements IImageProcessor
{
    @Override
    public boolean isEnabled(int imageType)
    {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType)
    {

        double[][] A1 = new double[][]{  {1,0, -inData.width/2},
                {0,1,0},
                {0,0,1},
        };

        double[][] A2 = new double[][]{  {-1,0,0},
                {0,1,0},
                {0,0,1},
        };

        double[][] A3 = new double[][]{  {1,0, inData.width/2},
                {0,1,0},
                {0,0,1},
        };

        Matrix matA1 = new Matrix(A1);
        Matrix matA2 = new Matrix(A2);
        Matrix matA3 = new Matrix(A3);

        Matrix abbild = (matA3.multiply(matA2)).multiply(matA1);

        ImageData outData = (ImageData) inData.clone();


        //Pixel updaten
        Parallel.For(0, outData.width, u -> {
            for (int v=0; v < outData.height; v++) {
                //Neue Pixelkoordinate
                int[] gridCoords = Interpolation.nearestNeighbor(abbild.multiply(Matrix.getVector3(u, v)));
                //Update Pixel
                if(gridCoords[0] >= 0 && gridCoords[0] < outData.width && gridCoords[1] >= 0 && gridCoords[1] < outData.height)
                    outData.setPixel(u, v, inData.getPixel(gridCoords[0], gridCoords[1]));
                else
                    outData.setPixel(u, v, 0);
            }
        });




        return outData;

    }
}
