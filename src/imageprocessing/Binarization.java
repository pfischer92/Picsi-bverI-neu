package imageprocessing;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import main.Picsi;
import utils.Parallel;

/**
 * Image segmentation (binarization) using Otsu's method
 * Image foreground = black
 * Palette: background, foreground
 * @author Christoph Stamm
 *
 */
public class Binarization implements IImageProcessor {
    public static int s_background = 0; // white
    public static int s_foreground = 1; // black

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        int threshold = otsuThreshold(inData);
        System.out.println(threshold);

        return binarization(inData, threshold);
    }

    /**
     * Binarization of grayscale image
     * @param inData grayscale image
     * @param threshold Image foreground <= threshold
     * @return binary image
     */
    public static ImageData binarization(ImageData inData, int threshold) {
        assert Picsi.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;

        ImageData outData = new ImageData(inData.width, inData.height, 1, new PaletteData(new RGB[]{ new RGB(255, 255, 255), new RGB(0, 0, 0) }));

        Parallel.For(0, inData.height, v -> {
            for (int u=0; u < inData.width; u++) {
                outData.setPixel(u, v, (inData.getPixel(u,v) <= threshold) ? s_foreground : s_background);
            }
        });
        return outData;
    }

    /**
     * Computes a global threshold for binarization using Otsu's method
     * @param inData grayscale image
     * @return threshold
     */
    public static int otsuThreshold(ImageData inData) {
        assert Picsi.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;

        final int size = inData.width*inData.height;
        final int[] hist = ImageProcessing.histogram(inData, 256);

        // compute mean value
        long all = 0;
        for(int i=0; i < 256; i++) {
            all += hist[i]*i;
        }
        double mean = (double)all/size;

        // try all possible thresholds t and choose the best
        double maxInter = 0;
        int bestT = 0;
        int w0 = 0;
        long sum0 = 0;

        // group 0: all pixel values <= t
        // group 1: all pixel values > t
        for(int t = 0; t < 255; t++) {
            w0 += hist[t];
            sum0 += hist[t]*t;

            if (w0 != 0 && w0 != size) {
                int w1 = size - w0;
                long sum1 = all - sum0;
                double mean0 = (double)sum0/w0;
                double mean1 = (double)sum1/w1;
                double p0 = (double)w0/size;
                double p1 = 1 - p0;

                // compute inter-variance
                double inter = p0*(mean0 - mean)*(mean0 - mean) + p1*(mean1 - mean)*(mean1 - mean);

                // find max Q(t)
                if (inter >= maxInter) {
                    maxInter = inter;
                    bestT = t;
                    //System.out.println(t + ", " + inter);
                }
            }
        }
        return bestT;
    }
}
