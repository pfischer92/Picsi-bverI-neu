package imageprocessing;

import main.Picsi;
import utils.Parallel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import gui.OptionPane;

/**
 * Debayering
 * @author Christoph Stamm
 *
 */
public class Debayering implements IImageProcessor {
	static final int Bypp = 3;

	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_GRAY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		Object[] outputTypes = { "Own", "Simple", "Good" };
		int ch = OptionPane.showOptionDialog("Debayering algorithms", SWT.ICON_QUESTION, outputTypes, 0);
		if (ch < 0) return null;

		// create outData
		PaletteData pd = new PaletteData(0xFF0000, 0xFF00, 0xFF); // R G B
		pd.redShift = -16;
		pd.greenShift = -8;
		pd.blueShift = 0;
		ImageData outData = new ImageData(inData.width, inData.height, Bypp*8, pd);
		
		// Debayering of raw input image
		if (ch == 0) debayeringOwn(inData, outData);
		else if(ch == 1)debayering1(inData, outData);
		else debayering2(inData, outData);

		
		return outData;
	}

	private void debayeringOwn(ImageData inData, ImageData outData) {
		Parallel.For(0, outData.height, v -> {
		RGB rgb = new RGB(0, 0, 0);
			for (int u=0; u < outData.width; u++) {

				int value = inData.getPixel(u, v);

				// Filter edge cases
				if(u == outData.width - 1 || v == outData.height - 1) {
					rgb = inData.palette.getRGB(value);
				}
				else {
					// Apply 2x2 interpolation to pixel
					rgb = inData.palette.getRGB(value); // rgb val is equal for each channel

					// Check for actual bayering mask color

					// Case Blue
					if(u%2 == 0 && v%2 ==0) {
						// blue stays

						// Green val from Pixel at u+1 and v+1 divied by 2 (right shift)
						int rightNeighbour = inData.getPixel(u+1, v);
						int bottomNeighbour = inData.getPixel(u, v+1);

						int interpolatedGreen = (rightNeighbour + bottomNeighbour) >> 1;

						// Red val from Pixel at u+1 v+1
						int redNeighbour = inData.getPixel(u+1, v+1);

						// Set new RGB to Pixel
						rgb.red = redNeighbour;
						rgb.green = interpolatedGreen;
					}

					// Case Red
					else if(u%2 == 1 && v%2 == 1) {
						// red stays

						// Green val from Pixel at u+1 and v+1 divided by 2 (right shift)
						int rightNeighbour = inData.getPixel(u+1, v);
						int bottomNeighbour = inData.getPixel(u, v+1);

						int interpolatedGreen = (rightNeighbour + bottomNeighbour) >> 1;

						// Blue val from Pixel at u+1 v+1
						int blueNeighbour = inData.getPixel(u+1, v+1);

						// Set new RGB to Pixel
						rgb.green = interpolatedGreen;
						rgb.blue = blueNeighbour;
					}

					// Case Green
					else {
						// Green val from actual Pixel and from Pixel at u+1 v+1 divided by 2 (right shift)
						int greenNeighbour = inData.getPixel(u+1, v+1);
						int interpolatedGreen = (rgb.green + greenNeighbour) >> 1;

						// Red val from Pixel at v+1
						int redNeighbour = inData.getPixel(u, v+1);

						// Blue val from Pixel at v+1
						int blueNeighbour = inData.getPixel(u+1, v);

						// Set new RGB to Pixel
						rgb.red = redNeighbour;
						rgb.green = interpolatedGreen;
						rgb.blue = blueNeighbour;
					}
				}

				// write rgb to output pixel
				outData.setPixel(u, v, outData.palette.getPixel(rgb));
			}
			});
	}

	private void debayering1(ImageData inData, ImageData outData) {
		Parallel.For(0, outData.height, v -> {
			final int vm1 = Math.abs(v - 1);
			final int vp1 = (v + 1 < outData.height) ? v + 1 : 2*outData.height - v - 3;
			
			RGB rgb = new RGB(0, 0, 0);
			
			for (int u=0; u < outData.width; u++) {
				// do interpolation
				final int um1 = Math.abs(u - 1);
				final int up1 = (u + 1 < outData.width) ? u + 1 : 2*outData.width - u - 3;

				if (v%2 == 0) {
					if (u%2 == 0) {
						// b
						rgb.blue = inData.getPixel(u, v);
						int g1 = inData.getPixel(um1, v);
						int g2 = inData.getPixel(up1, v);
						int g3 = inData.getPixel(u, vm1);
						int g4 = inData.getPixel(u, vp1);
						rgb.green = (g1 + g2 + g3 + g4) >> 2;
						int r1 = inData.getPixel(um1, vm1);
						int r2 = inData.getPixel(up1, vm1);
						int r3 = inData.getPixel(um1, vp1);
						int r4 = inData.getPixel(up1, vp1);
						rgb.red = (r1 + r2 + r3 + r4) >> 2;
					} else {
						// g
						rgb.green = inData.getPixel(u, v);
						int r1 = inData.getPixel(u, vm1);
						int r2 = inData.getPixel(u, vp1);
						rgb.red = (r1 + r2) >> 1;
						int b1 = inData.getPixel(um1, v);
						int b2 = inData.getPixel(up1, v);
						rgb.blue = (b1 + b2) >> 1;
					}
				} else {
					if (u%2 == 0) {
						// g
						rgb.green = inData.getPixel(u, v);
						int r1 = inData.getPixel(um1, v);
						int r2 = inData.getPixel(up1, v);
						rgb.red = (r1 + r2) >> 1;
						int b1 = inData.getPixel(u, vm1);
						int b2 = inData.getPixel(u, vp1);
						rgb.blue = (b1 + b2) >> 1;
					} else {
						// r
						rgb.red = inData.getPixel(u, v);
						int g1 = inData.getPixel(um1, v);
						int g2 = inData.getPixel(up1, v);
						int g3 = inData.getPixel(u, vm1);
						int g4 = inData.getPixel(u, vp1);
						rgb.green = (g1 + g2 + g3 + g4) >> 2;
						int b1 = inData.getPixel(um1, vm1);
						int b2 = inData.getPixel(up1, vm1);
						int b3 = inData.getPixel(um1, vp1);
						int b4 = inData.getPixel(up1, vp1);
						rgb.blue = (b1 + b2 + b3 + b4) >> 2;
					}
				}
				outData.setPixel(u, v, outData.palette.getPixel(rgb));
			}
		});
	}

	private void debayering2(ImageData inData, ImageData outData) {
		// interpolation of green channel
		Parallel.For(0, outData.height, v -> {
			RGB rgb = new RGB(0, 0, 0);

			for (int u=0; u < outData.width; u++) {
				final int um1 = Math.abs(u - 1);
				final int um2 = Math.abs(u - 2);
				final int up1 = (u + 1 < outData.width) ? u + 1 : 2*outData.width - u - 3;
				final int up2 = (u + 2 < outData.width) ? u + 2 : 2*outData.width - u - 4;

				if (v%2 == 0) {
					if (u%2 == 0) {
						// b
						rgb.blue = inData.getPixel(u, v);
						int g1 = inData.getPixel(um1, v);
						int g2 = inData.getPixel(up1, v);
						int b1 = inData.getPixel(um2, v);
						int b2 = inData.getPixel(up2, v);
						rgb.green = ImageProcessing.clamp8((g1 + g2)/2 + (2*rgb.blue - b1 - b2)/4);
					} else {
						// g
						rgb.green = inData.getPixel(u, v);
					}
				} else {
					if (u%2 == 0) {
						// g
						rgb.green = inData.getPixel(u, v);
					} else {
						// r
						rgb.red = inData.getPixel(u, v);
						int g1 = inData.getPixel(um1, v);
						int g2 = inData.getPixel(up1, v);
						int r1 = inData.getPixel(um2, v);
						int r2 = inData.getPixel(up2, v);
						rgb.green = ImageProcessing.clamp8((g1 + g2)/2 + (2*rgb.red - r1 - r2)/4);
					}
				}
				outData.setPixel(u, v, outData.palette.getPixel(rgb));
			}
		});
		
		// interpolation of blue and red channels
		Parallel.For(0, outData.height, v -> {
			final int vm1 = Math.abs(v - 1);
			final int vp1 = (v + 1 < outData.height) ? v + 1 : 2*outData.height - v - 3;
			
			for (int u=0; u < outData.width; u++) {
				final int um1 = Math.abs(u - 1);
				final int up1 = (u + 1 < outData.width) ? u + 1 : 2*outData.width - u - 3;

				RGB rgb = outData.palette.getRGB(outData.getPixel(u, v));

				if (v%2 == 0) {
					if (u%2 == 0) {
						// b
						rgb.blue = inData.getPixel(u, v);
						
						int g1 = getGreen(um1, vm1, outData);
						int g2 = getGreen(up1, vm1, outData);
						int g3 = getGreen(um1, vp1, outData);
						int g4 = getGreen(up1, vp1, outData);
						int r1 = inData.getPixel(um1, vm1);
						int r2 = inData.getPixel(up1, vm1);
						int r3 = inData.getPixel(um1, vp1);
						int r4 = inData.getPixel(up1, vp1);
						rgb.red = ImageProcessing.clamp8(rgb.green + (r1 - g1 + r2 - g2 + r3 - g3 + r4 - g4)/4);
					} else {
						// g
						int g1 = getGreen(um1, v, outData);
						int g2 = getGreen(up1, v, outData);
						int b1 = inData.getPixel(um1, v);
						int b2 = inData.getPixel(up1, v);
						rgb.blue = ImageProcessing.clamp8(rgb.green + (b1 - g1 + b2 - g2)/2);

						g1 = getGreen(u, vm1, outData);
						g2 = getGreen(u, vp1, outData);
						int r1 = inData.getPixel(u, vm1);
						int r2 = inData.getPixel(u, vp1);
						rgb.red = ImageProcessing.clamp8(rgb.green + (r1 - g1 + r2 - g2)/2);						
					}
				} else {
					if (u%2 == 0) {
						// g
						int g1 = getGreen(um1, v, outData);
						int g2 = getGreen(up1, v, outData);			
						int r1 = inData.getPixel(um1, v);
						int r2 = inData.getPixel(up1, v);
						rgb.red = ImageProcessing.clamp8(rgb.green + (r1 - g1 + r2 - g2)/2);
						
						g1 = getGreen(u, vm1, outData);
						g2 = getGreen(u, vp1, outData);			
						int b1 = inData.getPixel(u, vm1);
						int b2 = inData.getPixel(u, vp1);
						rgb.blue = ImageProcessing.clamp8(rgb.green + (b1 - g1 + b2 - g2)/2);
					} else {
						// r
						rgb.red = inData.getPixel(u, v);
						
						int g1 = getGreen(um1, vm1, outData);
						int g2 = getGreen(up1, vm1, outData);
						int g3 = getGreen(um1, vp1, outData);
						int g4 = getGreen(up1, vp1, outData);
						int b1 = inData.getPixel(um1, vm1);
						int b2 = inData.getPixel(up1, vm1);
						int b3 = inData.getPixel(um1, vp1);
						int b4 = inData.getPixel(up1, vp1);
						rgb.blue = ImageProcessing.clamp8(rgb.green + (b1 - g1 + b2 - g2 + b3 - g3 + b4 - g4)/4);
					}
				}	

				outData.setPixel(u, v, outData.palette.getPixel(rgb));
			}
		});
	}
	
	private int getGreen(int u, int v, ImageData outData) {
		return outData.palette.getRGB(outData.getPixel(u, v)).green;
	}
}
