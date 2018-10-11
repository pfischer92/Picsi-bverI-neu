package imageprocessing;

import main.Picsi;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import utils.Parallel;

/**
 * Image inverter
 * @author Christoph Stamm
 *
 */
public class Inverter implements IImageProcessor {

	@Override
	public boolean isEnabled(int imageType) {
		return true;
	}

	@Override
	public ImageData run(ImageData input, int imageType) {
		ImageData outData = (ImageData)input.clone();
		invert(outData, imageType);
		return outData;
	}

	/**
	 * Invert image data
	 * @param imageData will be modified
	 * @param imageType
	 */
	public static void invert(ImageData imageData, int imageType) {
		if (imageType == Picsi.IMAGE_TYPE_RGB) {
			// change pixel colors
			/* sequential image loop 
			for(int v=0; v < imageData.height; v++) {
				for (int u=0; u < imageData.width; u++) {
					int pixel = imageData.getPixel(u,v);
					RGB rgb = imageData.palette.getRGB(pixel);
					rgb.red   = 255 - rgb.red;
					rgb.green = 255 - rgb.green;
					rgb.blue  = 255 - rgb.blue;
					imageData.setPixel(u, v, imageData.palette.getPixel(rgb));
				}
			}
			*/
			// parallel image loop
			Parallel.For(0, imageData.height, v -> {
				for (int u=0; u < imageData.width; u++) {
					RGB rgb = imageData.palette.getRGB(imageData.getPixel(u,v));
					rgb.red   = 255 - rgb.red;
					rgb.green = 255 - rgb.green;
					rgb.blue  = 255 - rgb.blue;
					imageData.setPixel(u, v, imageData.palette.getPixel(rgb));
				}
			});
		} else {
			// change palette
			RGB[] paletteIn = imageData.getRGBs();
			RGB[] paletteOut = new RGB[paletteIn.length];
			
			for (int i=0; i < paletteIn.length; i++) {
				RGB rgbIn = paletteIn[i];
				paletteOut[i] = new RGB(255 - rgbIn.red, 255 - rgbIn.green, 255 - rgbIn.blue);
			}
			imageData.palette = new PaletteData(paletteOut);
		}
	}
}
