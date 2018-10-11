package imageprocessing;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import utils.Parallel;

import javax.swing.*;

public class RotationNative implements IImageProcessor
{
    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {

        int verschiebungX = (inData.width + 1)/2;
        int verschiebungY = (inData.height + 1)/2;

        int winkel = Integer.parseInt(JOptionPane.showInputDialog("Winkel:"));

        final double cos = Math.cos(Math.toRadians(winkel));
        final double sin = Math.sin(Math.toRadians(winkel));

        /*Computation of the new image size*/
        int x1 = (int)(cos * -verschiebungX - sin * -verschiebungY + 1);
        int y1 = (int)(sin * -verschiebungX + cos * -verschiebungY + 1);
        int x2 = (int)(cos * (inData.width - verschiebungX) - sin * (inData.height- verschiebungY) + 1);
        int y2 = (int)(sin * (inData.width - verschiebungX) + cos * (inData.height- verschiebungY) + 1);
        int sx1 = Math.abs(x1-x2);
        int sy1 = Math.abs(y1-y2);

        int x3 = (int)(cos * (inData.width - verschiebungX) - sin * -verschiebungY + 1);
        int y3 = (int)(sin * (inData.width - verschiebungX) + cos * -verschiebungY + 1);
        int x4 = (int)(cos * (-verschiebungX) - sin * (inData.height- verschiebungY) + 1);
        int y4 = (int)(sin * (-verschiebungX) + cos * (inData.height- verschiebungY) + 1);
        int sx2 = Math.abs(x3-x4);
        int sy2 = Math.abs(y3-y4);

        /*new image Size*/
        int sx = sx1 > sx2 ? sx1:sx2;
        int sy = sy1 > sy2 ? sy1:sy2;

        ImageData outData = new ImageData(sx, sy, inData.depth, inData.palette);

        int newScaleFactorX = sx/2;
        int newScaleFactorY = sy/2;

        /* Source to Target */
        /*Parallel.For(0, inData.height, v -> {
            for (int u = 0; u < inData.width; u++) {
                int u2 = u - verschiebungX;
                int v2 = v - verschiebungY;
                double uu = (cos * u2 + sin * v2) + newScaleFactor;
                double vv = (-sin * u2 + cos * v2) + newScaleFactor;
                outData.setPixel((int)uu, (int)vv, inData.getPixel(u,v));
            }
        });*/

        /* Target to Source */
        Parallel.For(0, outData.height, v -> {
            for (int u = 0; u < outData.width; u++) {
                int u2 = u - newScaleFactorX;
                int v2 = v - newScaleFactorY;
                int uu = (int)(cos * u2 - sin * v2) + verschiebungX;
                int vv = (int)(sin * u2 + cos * v2) + verschiebungY;
                if(!(uu >= inData.width || uu < 0 || vv >= inData.height || vv < 0)) {
                    outData.setPixel(u, v, inData.getPixel(uu, vv));
                }
            }
        });
        return outData;
    }
}
