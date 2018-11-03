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

        int[] hist = ImageProcessing.histogram(inData, 256);

        int total = inData.width * inData.height;

        float sum = 0;
        for (int t = 0; t < 256; t++) sum += t * hist[t];

        float sumB = 0;
        int wB = 0;
        int wF = 0;

        float varMax = 0;
        int threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += hist[t];                  // Weight Background
            if (wB == 0) continue;

            wF = total - wB;                 // Weight Foreground
            if (wF == 0) break;

            sumB += (float) (t * hist[t]);

            float mB = sumB / wB;            // Mean Background
            float mF = (sum - sumB) / wF;    // Mean Foreground

            // Calculate Between Class Variance
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            // Check if new maximum found
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }
        System.out.println("Threshold: " + threshold);
        return threshold;
    }

}
