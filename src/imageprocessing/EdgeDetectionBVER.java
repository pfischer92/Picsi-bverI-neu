package imageprocessing;

import gui.OptionPane;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import utils.Parallel;

public class EdgeDetectionBVER implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY || imageType == Picsi.IMAGE_TYPE_RGB;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        final int edgeThreshold = 15;

        // filters
        final int offset = 128;
        final int[] edge = { -1, 0, 1 };
        final int edgeDen = 2;
        int[] blur;
        int blurDen;

        // let the user choose the filter type
        Object[] filterTypes = { "Prewitt", "Sobel", "Improved Sobel" };
        int f = OptionPane.showOptionDialog("Filter Type", SWT.ICON_INFORMATION, filterTypes, 0);
        if (f < 0) return null;
        switch(f) {
            case 0: blur = new int[]  { 1,  1, 1 }; blurDen = 3; break;
            case 1: blur = new int[]  { 1,  3, 1 }; blurDen = 5; break;
            default: blur = new int[] { 3, 10, 3 }; blurDen = 16; break;
        }

        // let the user choose the filter output
        Object[] outputTypes = { "Vertical", "Horizontal", "Edges", "Phases" };
        int ch = OptionPane.showOptionDialog("Edge Detection Output", SWT.ICON_INFORMATION, outputTypes, 2);

        ImageData outData = null;

        switch(ch) {
            case 0:
                if (imageType == Picsi.IMAGE_TYPE_RGB) inData = Grauwertbild.createGreyScale(inData, imageType, 0);
                outData = ImageProcessing.convolve(inData, imageType, edge, edgeDen, offset, blur, blurDen, 0);
                break;
            case 1:
                if (imageType == Picsi.IMAGE_TYPE_RGB) inData = Grauwertbild.createGreyScale(inData, imageType, 0);
                outData = ImageProcessing.convolve(inData, imageType, blur, blurDen, 0, edge, edgeDen, offset);
                break;
            case 2:
            case 3:
                ImageData out = (ImageData)inData.clone();
                if (imageType == Picsi.IMAGE_TYPE_RGB) inData = Grauwertbild.createGreyScale(inData, imageType, 0);
                ImageData outDataX = ImageProcessing.convolve(inData, imageType, edge, edgeDen, offset, blur, blurDen, 0);
                ImageData outDataY = ImageProcessing.convolve(inData, imageType, blur, blurDen, 0, edge, edgeDen, offset);

                Parallel.For(0, outDataX.height, v -> {
                    RGB rgb = new RGB(255, 255, 255);
                    RGBtoHSV.HSV hsv = new RGBtoHSV.HSV();

                    for (int u=0; u < outDataX.width; u++) {
                        final int dx = outDataX.getPixel(u, v) - offset;
                        final int dy = outDataY.getPixel(u, v) - offset;
                        final int val = (int)Math.sqrt(dx*dx + dy*dy);

                        if (val > edgeThreshold) {
                            if (ch == 2) {
                                // edges
                                if (imageType == Picsi.IMAGE_TYPE_GRAY) {
                                    out.setPixel(u, v, 255);
                                } else {
                                    out.setPixel(u, v, out.palette.getPixel(rgb));
                                }
                            } else {
                                // edge direction
                                final double phi = Math.atan2(dy, dx)/Math.PI + 1; // [0..2]

                                if (imageType == Picsi.IMAGE_TYPE_GRAY) {
                                    out.setPixel(u, v, ImageProcessing.clamp8(255*phi/2));
                                } else {
                                    hsv.setH(ImageProcessing.clamp8(RGBtoHSV.MaxH*phi/2));
                                    hsv.setS(128);
                                    hsv.setV(255);
                                    RGBtoHSV.hsv2rgb(hsv, rgb);
                                    out.setPixel(u, v, out.palette.getPixel(rgb));
                                }
                            }
                        } else {
                            out.setPixel(u, v, 0);
                        }
                    }
                });
                outData = out;
        }
        if (outData == null) {
            return null;
        } else {
            if (outData.getTransparencyType() == SWT.TRANSPARENCY_ALPHA) {
                outData.alpha = 255;
            }
            return outData;
        }
    }
}
