package imageprocessing;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import utils.Matrix;
import utils.Parallel;

public class Affine implements IImageProcessor
{
    @Override
    public boolean isEnabled(int imageType){ return true; }

    @Override
    public ImageData run(ImageData input, int imageType)
    {
        ImageData original = (ImageData) input.clone();
        ImageData outData   = input;
        int alpha = 45;
        int scaleFactor = 2;

        //Rotationsmatrix
        Matrix M1 = Matrix.translation(-outData.width/2, -outData.height/2);    // Translate to middle point
        Matrix M2 = Matrix.rotation((Math.PI/180.)*alpha).inverse();                    // Rotate against clock
        Matrix M3 = Matrix.scaling(scaleFactor, scaleFactor).inverse();                 // Scale in both direction
        Matrix M4 = Matrix.translation(outData.width/2, outData.height/2);      // Translate back to zero point
        Matrix rotationMatrix = M4.multiply(M3).multiply(M2).multiply(M1);              //Target-To-Source-Mappin

        //Pixel updaten
        Parallel.For(0, outData.width, u -> {
            for (int v=0; v < outData.height; v++) {
                //Neue Pixelkoordinate
                int[] gridCoords = Interpolation.nearestNeighbor(rotationMatrix.multiply(Matrix.getVector3(u, v)));
                //Update Pixel
                if(gridCoords[0] >= 0 && gridCoords[0] < outData.width && gridCoords[1] >= 0 && gridCoords[1] < outData.height)
                    outData.setPixel(u, v, original.getPixel(gridCoords[0], gridCoords[1]));
                else
                    outData.setPixel(u, v, 0);
            }
        });
        return outData;
    }
}
