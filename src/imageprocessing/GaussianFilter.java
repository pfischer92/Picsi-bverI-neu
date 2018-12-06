package imageprocessing;

import gui.OptionPane;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;

public class GaussianFilter implements IImageProcessor{

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY || imageType == Picsi.IMAGE_TYPE_RGB;
    }

    @Override
    public ImageData run(ImageData input, int imageType) {
        String s = OptionPane.showInputDialog("Enter sigma");
        if (s == null) return null;
        try {
            double sigma = Double.parseDouble(s);
            if (sigma <= 0) return null;

            final double sigma2 = 2*sigma*sigma;
            final int factor = 100;
            final int len = (int)Math.round(5*sigma);
            final int len2 = 2*(len/2) + 1;

            int den, x;

            // create filter matrix
            int[] filter = new int[len];
            den = 0;
            for (int i=0; i < len; i++) {
                x = i - len2;
                filter[i] = (int)(factor*Math.exp(-x*x/sigma2));
                den += filter[i];
            }

            return ImageProcessing.convolve(input, imageType, filter, den, 0, filter, den, 0);
        } catch(NumberFormatException ex) {
            return null;
        }
    }
}
