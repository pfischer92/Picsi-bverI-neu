package imageprocessing;

import gui.OptionPane;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.MessageBox;
import utils.Parallel;

public class RGBtoHSV implements IImageProcessor {
    public static class HSV {
        private RGB hsv;

        public HSV() {
            hsv = new RGB(0, 0, 0);
        }

        public HSV(int h, int s, int v) {
            hsv = new RGB(h, s, v);
        }

        public HSV(RGB in) {
            hsv = in;
        }

        public void setH(int h) { hsv.red = h; }
        public void setS(int s) { hsv.green = s; }
        public void setV(int v) { hsv.blue = v; }
        public void fromRGB(RGB in) { hsv = in; }

        public int getH() { return hsv.red; }
        public int getS() { return hsv.green; }
        public int getV() { return hsv.blue; }
        public RGB asRGB() { return hsv; }
        public RGB asNewRGB() { return new RGB(hsv.red, hsv.green, hsv.blue); }
    }

    private static final int hFactor = 255/6;		// 42
    public static final int MaxH = hFactor*6;		// 252
    public static final int UndefinedH = MaxH + 1;	// 253

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_INDEXED;
    }

    @Override
    public ImageData run(ImageData inRGB, int imageType) {
        Object[] outputTypes = { "HSV", "RGB" };
        int ch = OptionPane.showOptionDialog("Choose Output", SWT.ICON_INFORMATION, outputTypes, 1);
        if (ch < 0) return null;

        // RGB to HSV
        ImageData outHSV = rgb2hsv(inRGB, imageType);

        // HSV to RGB
        ImageData outRGB = hsv2rgb(outHSV, imageType);

        // compute PSNR
        double[] PSNR = ImageProcessing.psnr(inRGB, outRGB, imageType);

        // show PSNR in message box
        MessageBox box = new MessageBox(Picsi.s_shell, SWT.OK);
        box.setText("PSNR");
        box.setMessage(Picsi.createMsg("Red: {0}, Green: {1}, Blue: {2}", new Object[] {PSNR[0], PSNR[1], PSNR[2]}));
        box.open();

        // return image
        return (ch == 0) ? outHSV : outRGB;
    }

    /**
     * RGB to HSV color transform
     * @param inRGB color image
     * @param imageType
     * @return HSV image with H = [0,252], S = [0,255], V = [0,255]. if H > 252 then H is undefined
     */
    public static ImageData rgb2hsv(ImageData inRGB, int imageType) {
        assert imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_INDEXED;

        ImageData outHSV = (ImageData)inRGB.clone();

        if (imageType == Picsi.IMAGE_TYPE_INDEXED) {
            RGB[] colors = new RGB[inRGB.palette.colors.length];
            HSV hsv = new HSV();

            for(int i = 0; i < inRGB.palette.colors.length; i++) {
                rgb2hsv(inRGB.palette.colors[i], hsv);
                colors[i] = hsv.asNewRGB();
            }
            outHSV.palette = new PaletteData(colors);
        } else {
            Parallel.For(0, inRGB.height, v -> {
                HSV hsv = new HSV();

                for (int u=0; u < inRGB.width; u++) {
                    rgb2hsv(inRGB.palette.getRGB(inRGB.getPixel(u, v)), hsv);
                    outHSV.setPixel(u, v, outHSV.palette.getPixel(hsv.asRGB()));
                }
            });
        }
        return outHSV;
    }

    /**
     * RGB to HSV color transform
     * @param rgb
     * @param hsv with H = [0,252], S = [0,255], V = [0,255]. if H > 252 then H is undefined
     */
    public static void rgb2hsv(RGB rgb, HSV hsv) {
        final int min = Math.min(Math.min(rgb.blue, rgb.green), rgb.red);
        final int V = Math.max(Math.max(rgb.blue, rgb.green), rgb.red);

        if (min == V) {
            // undefined hue
            hsv.setH(UndefinedH);
            hsv.setS(0);
            hsv.setV(V);
        } else {
            final int f = (rgb.red == min) ? rgb.green - rgb.blue : ((rgb.blue == min) ? rgb.red - rgb.green : rgb.blue - rgb.red);
            final int i = (rgb.red == min) ? 3 : ((rgb.blue == min) ? 1 : 5);
            hsv.setH(hFactor*i - hFactor*f/(V - min));
            hsv.setS(255*(V - min)/V);
            hsv.setV(V);
        }
    }

    /**
     * HSV to RGB color transform (H = [0,252], S = [0,255], V = [0,255]) with H > 252 is interpreted as undefined
     * @param inHSV
     * @param imageType
     * @return RGB image
     */
    public static ImageData hsv2rgb(ImageData inHSV, int imageType) {
        assert imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_INDEXED;

        ImageData outRGB = (ImageData)inHSV.clone();

        if (imageType == Picsi.IMAGE_TYPE_INDEXED) {
            RGB[] colors = new RGB[inHSV.palette.colors.length];
            HSV hsv = new HSV();

            for(int i = 0; i < inHSV.palette.colors.length; i++) {
                hsv.fromRGB(inHSV.palette.colors[i]);
                colors[i] = new RGB(0, 0, 0);
                hsv2rgb(hsv, colors[i]);
            }
            outRGB.palette = new PaletteData(colors);
        } else {
            Parallel.For(0, inHSV.height, v -> {
                RGB rgb = new RGB(0, 0 , 0);
                HSV hsv = new HSV();

                for (int u=0; u < inHSV.width; u++) {
                    hsv.fromRGB(inHSV.palette.getRGB(inHSV.getPixel(u, v)));
                    hsv2rgb(hsv, rgb);
                    outRGB.setPixel(u, v, outRGB.palette.getPixel(rgb));
                }
            });
        }
        return outRGB;
    }

    /**¨
     * HSV to RGB color transform
     * @param hsv H = [0,252], S = [0,255], V = [0,255] with H > 252 is interpreted as undefined
     * @param rgb
     */
    public static void hsv2rgb(HSV hsv, RGB rgb) {
        final int H = hsv.getH();
        final int S = hsv.getS();
        final int V = hsv.getV();

        if (H >= UndefinedH) {
            rgb.blue = rgb.green = rgb.red = V;
        } else {
            final int i = H/hFactor; 	// [0..6]
            int f = H - i*hFactor;		// [0..hFactor - 1]
            if ((i & 1) == 0) f = hFactor - f;
            final int m = V - V*S/255;
            final int n = V - V*S*f/255/hFactor;
            switch(i) {
                case 1:  rgb.red = n; rgb.green = V; rgb.blue = m; break;
                case 2:  rgb.red = m; rgb.green = V; rgb.blue = n; break;
                case 3:  rgb.red = m; rgb.green = n; rgb.blue = V; break;
                case 4:  rgb.red = n; rgb.green = m; rgb.blue = V; break;
                case 5:  rgb.red = V; rgb.green = m; rgb.blue = n; break;
                default: rgb.red = V; rgb.green = n; rgb.blue = m; break;
            }
        }
    }
}
