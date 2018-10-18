package imageprocessing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;

import gui.OptionPane;
import main.Picsi;

/**
 * Modified Gamma Correction
 * @author Christoph Stamm
 *
 */
public class GammaCorrection implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        final double gamma = 1/2.4;	// sRGB
        final double x0 = 0.00304;	// sRGB
        //final double gamma = 1/2.222;	// ITU
        //final double x0 = 0.018;	// ITU

        Object[] outputTypes = { "Modified", "Inverse Modified" };
        int ch = OptionPane.showOptionDialog("Gamma Correction", SWT.ICON_QUESTION, outputTypes, 0);
        if (ch < 0) return null;

        ImageData outData = (ImageData)inData.clone();
        gammaCorrection(outData, gamma, x0, ch == 1);
        return outData;
    }

    /**
     * (Inverse) modified gamma correction
     * @param imageData grayscale image
     * @param gamma
     * @param x0
     * @param inverse
     */
    public static void gammaCorrection(ImageData imageData, double gamma, double x0, boolean inverse) {
        assert Picsi.determineImageType(imageData) == Picsi.IMAGE_TYPE_GRAY;

        final int K = 256; // intensities
        final int iMax = K - 1;
        final double s = gamma/(x0*(gamma - 1) + Math.pow(x0, 1 - gamma));
        final double d = 1/(Math.pow(x0, gamma)*(gamma - 1) + 1) - 1;

        byte[] lut = new byte[K];

        // fill in LUT
        if (inverse) {
            // inverse modified gamma correction
            for(int i = 0; i < K; i++){
                double y = (double)i/iMax;
                if (y <= s*x0) {
                    lut[i] = (byte)Math.round(y/s*iMax);
                } else {
                    lut[i] = (byte)Math.round(Math.pow((y + d)/(1 + d), 1/gamma)*iMax);
                }
            }
        } else {
            // modified gamma correction
            for(int i = 0; i < K; i++){
                double x = (double)i/iMax;
                if (x <= x0) {
                    lut[i] = (byte)Math.round(s*x*iMax);
                } else {
                    lut[i] = (byte)Math.round(((1 + d)*Math.pow(x, gamma) - d)*iMax);
                }
            }
        }

        ImageProcessing.applyLUT(imageData, lut);
    }
}
