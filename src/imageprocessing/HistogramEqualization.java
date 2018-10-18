package imageprocessing;

import org.eclipse.swt.graphics.ImageData;

import main.Picsi;

/**
 * Histogram equalization
 * @author Christoph Stamm
 *
 */
public class HistogramEqualization implements IImageProcessor {

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData input, int imageType) {
        ImageData outData = (ImageData)input.clone();

        equalization(outData);
        return outData;
    }

    /**
     * Histogram equalization
     * @param imageData grayscale image
     */
    public static void equalization(ImageData imageData) {
        assert Picsi.determineImageType(imageData) == Picsi.IMAGE_TYPE_GRAY;

        final int nClasses = 256;
        final int max = nClasses - 1;
        final int size = imageData.width*imageData.height;
        byte[] lut = new byte[nClasses];
        int cumHist = 0; // cumulative histogram

        // compute histogram
        int[] hist = ImageProcessing.histogram(imageData, nClasses);

        // compute LUT
        for(int i=0; i < lut.length; i++) {
            // compute cumulative histogram
            cumHist += hist[i];

            // histogram equalization
            lut[i] = (byte)(cumHist*max/size);
        }

        // apply LUT
        ImageProcessing.applyLUT(imageData, lut);
    }
}
