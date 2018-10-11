package imageprocessing;

import main.Picsi;
import org.eclipse.swt.graphics.ImageData;

import static imageprocessing.ImageProcessing.clamp8;
import static imageprocessing.ModifiedGammaCorr.modGammaCorr;

public class sRGBtoYUV implements IImageProcessor {
	private static double diffOldNewRed = 0, diffOldNewGreen = 0, diffOldNewBlue = 0;
	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_RGB;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		ImageData outData = sRGBtoYUV(inData);
		int n = inData.width*inData.height;
		double rmse_red = Math.sqrt(diffOldNewRed /n);
		double rmse_green = Math.sqrt(diffOldNewGreen /n);
		double rmse_blue = Math.sqrt(diffOldNewGreen /n);

		double psnrRed = 20*Math.log10(255/rmse_red);
		double psnrGreen = 20*Math.log10(255/rmse_green);
		double psnrBlue = 20*Math.log10(255/rmse_blue);

		System.out.println("PSNR for RED channel: " + psnrRed);
		System.out.println("PSNR for GREEN channel: " + psnrGreen);
		System.out.println("PSNR for BLUE channel: " + psnrBlue);
		return outData;
	}
	
	/**
	 * Invert image data
	 * @param imageData will be modified
	 */
	public static ImageData sRGBtoYUV(ImageData imageData) {
		ImageData outData = (ImageData)imageData.clone();
		int color, oldR, oldG, oldB, Y, U, V, R, G, B;

		for(int u = 0; u < outData.height; u++){
			for (int v = 0; v < outData.width; v++){
				color  = outData.getPixel(u, v);
				oldR = (0x00FF0000 & color) >> 16;
				oldG = (0x0000FF00 & color) >> 8;
				oldB = (0x000000FF & color);

				Y = (int)Math.round((0.299*oldR) + (0.587*oldG) +(0.114*oldB));
				U= (int)Math.round(0.492*(oldB-Y));
				V =(int)Math.round(0.877*(oldR-Y));

				Y = clamp8(Y);

				R = (int)Math.round(Y - 3.9457e-005*U + 1.1398*V);
				G = (int)Math.round(Y - 0.39461*U - 0.5805*V);
				B = (int)Math.round(Y + 2.032*U - 0.00048138*V);

				R = clamp8(R);
				G = clamp8(G);
				B = clamp8(B);

				diffOldNewRed += (oldR - R)*(oldR - R);
				diffOldNewGreen += (oldG - G)*(oldG - G);
				diffOldNewBlue += (oldB - B)*(oldB - B);

				outData.setPixel(u, v, (R << 16) | (G << 8) |  B);
		}
	}

		return outData;
	}

	private static int clampAndGammaCorrect(int color)
	{
		int gamma_corrected = (int)Math.pow(color, 1f / 2.2);

		if (gamma_corrected > 255) return 255;
		if (gamma_corrected < 0) return 0;
		return (byte)gamma_corrected;
	}
}

