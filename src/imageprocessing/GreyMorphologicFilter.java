package imageprocessing;

import gui.OptionPane;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

public class GreyMorphologicFilter implements IImageProcessor {

    // Strukturmatrix H
    public static int[][] struct = {
            {-1, 1, -1},
            { 1, 2,  1},
            {-1, 1, -1}
    };


    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData input, int imageType) {
        ImageData clone = (ImageData) input.clone();
        Object[] operations = { "Erosion", "Dilation" };
        int ch = OptionPane.showOptionDialog("Morphological Operation", SWT.ICON_INFORMATION, operations, 0);
        if (ch < 0) return null;


        if (ch == 0) {
            clone = erosion(input, struct, 1, 1);
        } else {
            clone = dilation(input, struct, 1, 1);
        }

        return clone;
    }

    private ImageData erosion(ImageData input, int[][] struct, int hotSpotX, int hotSpotY) {
        final int w = input.width;
        final int h = input.height;
        final int sh = struct.length;
        final int sw = struct[0].length;

        ImageData copy = (ImageData) input.clone();
        int tmp, min, x, y;

        for (int v = 0; v < h; v++) {
            for (int u = 0; u < w; u++) {

                // Anwendung des Filters an der Position u,v
                min = Integer.MAX_VALUE;
                for (int j = 0; j < sh; j++) {
                    for (int i = 0; i < sw; i++) {
                        if (struct[j][i] >= 0) {
                            // Randbehandlung
                            x = u + i - hotSpotX;
                            y = v + j - hotSpotY;
                            if (x < 0) x = -x;
                            if (x >= w) x = 2*w - 1 - x;
                            if (y < 0) y = -y;
                            if (y >= h) y = 2*h - 1 - y;

                            // neue Intensität berechnen
                            tmp = copy.getPixel(x, y) - struct[j][i];
                            if (tmp < min) min = tmp;
                        }
                    }
                }
                // clamping
                if (min < 0)   min = 0;
                if (min > 255) min = 255;

                // neue Intensität setzen
                copy.setPixel(u, v, min);
            }
        }
        return copy;
    }

    private ImageData dilation(ImageData input, int[][] struct, int hotSpotX, int hotSpotY) {
        final int w = input.width;
        final int h = input.height;
        final int sh = struct.length;
        final int sw = struct[0].length;

        ImageData copy = (ImageData) input.clone();
        int tmp, max, x, y;

        for (int v = 0; v < h; v++) {
            for (int u = 0; u < w; u++) {

                // Anwendung des Filters an der Position u,v
                max = -1;
                for (int j = 0; j < sh; j++) {
                    for (int i = 0; i < sw; i++) {
                        if (struct[j][i] >= 0) {
                            // Randbehandlung
                            x = u + i - hotSpotX;
                            y = v + j - hotSpotY;
                            if (x < 0) x = -x;
                            if (x >= w) x = 2*w - 1 - x;
                            if (y < 0) y = -y;
                            if (y >= h) y = 2*h - 1 - y;

                            // neue Intensität berechnen
                            tmp = struct[j][i] + copy.getPixel(x, y);
                            if (tmp > max) max = tmp;
                        }
                    }
                }
                // clamping
                if (max < 0)   max = 0;
                if (max > 255) max = 255;

                // neue Intensität setzen
                copy.setPixel(u, v, max);
            }
        }
        return copy;
    }
}
