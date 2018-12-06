package imageprocessing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.MessageBox;

import gui.OptionPane;
import main.Picsi;
import utils.Parallel;

/**
 * RGB to YUV color transform
 * @author Christoph Stamm
 *
 */
public class RGBtoYUV implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_RGB;
    }

    @Override
    public ImageData run(ImageData inRGB, int imageType) {
        ImageData outYUV = (ImageData)inRGB.clone();
        ImageData outRGB = (ImageData)inRGB.clone();

        Object[] outputTypes = { "YUV", "RGB" };
        int ch = OptionPane.showOptionDialog("Choose Output", SWT.ICON_QUESTION, outputTypes, 0);
        if (ch < 0) return null;

        // RGB to YUV
        Parallel.For(0, inRGB.height, v -> {
            RGB yuv = new RGB(0, 0 , 0);

            for (int u=0; u < inRGB.width; u++) {
                RGB rgb = inRGB.palette.getRGB(inRGB.getPixel(u,v));
                double Y = Math.round((0.299*rgb.red) + (0.587*rgb.green) +(0.114*rgb.blue));
                double U = Math.round((-0.147*rgb.red) + (-0.289*rgb.green) +(0.436*rgb.blue));
                double V = Math.round((0.615*rgb.red) + (-0.515*rgb.green) +(-0.1*rgb.blue));
                //double U = Math.round(0.492*(rgb.blue - Y));
                //double V = Math.round(0.877*(rgb.red - Y));

                yuv.red = ImageProcessing.clamp8(Y); 			// red is misused as Y
                yuv.green = ImageProcessing.signedClamp8(U/2);	// green is misused as U
                yuv.blue = ImageProcessing.signedClamp8(V/2);	// blue is misused as V

                outYUV.setPixel(u, v, outYUV.palette.getPixel(yuv));
            }
        });

        // YUV to RGB
        Parallel.For(0, outYUV.height, v -> {
            RGB rgb = new RGB(0, 0 , 0);

            for (int u=0; u < outYUV.width; u++) {
                RGB yuv = outYUV.palette.getRGB(outYUV.getPixel(u,v));
                int Y = yuv.red;
                int U = 2*(byte)yuv.green;
                int V = 2*(byte)yuv.blue;
                double R = Math.round(Y - 3.9457e-005*U + 1.1398*V);
                double G = Math.round(Y - 0.39461*U - 0.5805*V);
                double B = Math.round(Y + 2.032*U - 0.00048138*V);
                //double G = Math.round(Y - 0.00003947313749*U - 0.580809209*V);
                //double R = Math.round((1.140250855*V) + Y);
                //double B = Math.round((2.032520325*U) + Y);

                rgb.red = ImageProcessing.clamp8(R);
                rgb.green = ImageProcessing.clamp8(G);
                rgb.blue = ImageProcessing.clamp8(B);

                outRGB.setPixel(u, v, outRGB.palette.getPixel(rgb));
            }
        });

        // compute PSNR
        final int size = inRGB.width*inRGB.height;
        double[] PSNR = new double[3];

        Parallel.For(0, inRGB.height, PSNR,
                // creator
                () -> new double[3],
                // loop body
                (v, psnr) -> {
                    for (int u=0; u < inRGB.width; u++) {
                        RGB in = inRGB.palette.getRGB(inRGB.getPixel(u, v));
                        RGB out = outRGB.palette.getRGB(outRGB.getPixel(u, v));
                        psnr[0] += (out.red - in.red)*(out.red - in.red);
                        psnr[1] += (out.green - in.green)*(out.green - in.green);
                        psnr[2] += (out.blue - in.blue)*(out.blue - in.blue);
                    }
                },
                // reducer
                psnr -> {
                    for(int i=0; i < psnr.length; i++) PSNR[i] += psnr[i];
                }
        );
        for(int i=0; i < PSNR.length; i++) PSNR[i] = 20*Math.log10(255/Math.sqrt(PSNR[i]/size));

        // show PSNR in message box
        MessageBox box = new MessageBox(Picsi.s_shell, SWT.OK);
        box.setText("PSNR");
        box.setMessage(Picsi.createMsg("Red: {0}, Green: {1}, Blue: {2}", new Object[] {PSNR[0], PSNR[1], PSNR[2]}));
        box.open();

        // return image
        return (ch == 0) ? outYUV : outRGB;
    }

}
