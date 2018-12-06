package imageprocessing;

import gui.OptionPane;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import utils.Parallel;

import java.util.Arrays;

public class MedianFilter implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        Object[] operations = { "Normal", "Weighted" };
        int ch = OptionPane.showOptionDialog("Median Filter", SWT.ICON_INFORMATION, operations, 0);
        if (ch < 0) return null;

        ImageData outData = (ImageData)inData.clone();
        int[][] w = {
                {1, 2, 1},
                {2, 4, 2},
                {1, 2, 1}
        };

        if (ch == 0) {
            String s = OptionPane.showInputDialog("Enter filter radius");
            if (s == null) return null;
            try {
                int r = Integer.parseInt(s);
                int d = 2*r + 1;
                w = new int[d][d];
                for(int j=0; j < d; j++) {
                    for(int i=0; i < d; i++) {
                        w[j][i] = 1;
                    }
                }
            } catch(NumberFormatException ex) {
                return null;
            }
        }

        return medianFilter(outData, w);

    }

    /**
     * Weighted median filtering
     * @param inData grayscale image
     * @param weights
     * @return outData grayscale image
     */
    public static ImageData medianFilter(ImageData inData, int[][] weights) {
        assert Picsi.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;
        ImageData outData = (ImageData)inData.clone();

        // compute sum of weights
        final int fhD2 = weights.length/2;
        final int fwD2 = weights[0].length/2;
        int wSum = 0;
        for (int j = 0; j < weights.length; j++) {
            for (int i = 0; i < weights[0].length; i++) {
                wSum += weights[j][i];
            }
        }
        final int size = wSum;

        Parallel.For(0, inData.height, v -> {
            int[] values = new int[size]; // has to be inside the parallel for-loop

            for (int u=0; u < inData.width; u++) {
                // apply filter at position u,v
                int pos = 0;
                for (int j = -fhD2; j <= fhD2; j++) {
                    int y = v + j;
                    if (y < 0) y = -y;
                    if (y >= inData.height) y = 2*inData.height - 2 - y;

                    for (int i = -fwD2; i <= fwD2; i++) {
                        // border handling
                        int x = u + i;
                        if (x < 0) x = -x;
                        if (x >= inData.width) x = 2*inData.width - 2 - x;

                        // collect pixel intensities
                        for(int k = 0; k < weights[j + fhD2][i + fwD2]; k++) {
                            values[pos++] = inData.getPixel(x, y);
                        }
                    }
                }

                // compute new pixel value
                Arrays.sort(values);
                pos = values.length/2;
                int val;
                if (values.length%2 == 0) {
                    // even number of values: median is arithmetic mean of the two values in the middle
                    val = (values[pos - 1] + values[pos])/2;
                } else {
                    val = values[pos];
                }

                outData.setPixel(u, v, val);
            }
        });

        return outData;
    }
}
