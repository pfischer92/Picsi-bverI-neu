package imageprocessing;

import main.Picsi;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import gui.OptionPane;

/*
 * @author Christoph Stamm
 * @version 9.5.2018
 */

public class Dithering implements IImageProcessor {
	
	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_GRAY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		// let the user choose the dithering method
		Object[] operations = { "Floyd-Steinberg", "Floyd-Steinberg 2", "Stucki" };
		int f = OptionPane.showOptionDialog("Dithering Method", 
				SWT.ICON_INFORMATION, operations, 2);
		if (f < 0) return null;
		
		// create palette and output image data
		ImageData outData = new ImageData(inData.width, inData.height, 1, new PaletteData(new RGB[]{ new RGB(255, 255, 255), new RGB(0, 0, 0) }));

		// convert to binary
		final int inPadding = inData.bytesPerLine - inData.width;
		int inPos = 0;
		int outPos = 0;
		
		for (int v=0; v < outData.height; v++) {
			int u = 0;
			for (int l=0; l < outData.bytesPerLine; l++) {
				int val = 0;
				for (int k=0; k < 8; k++) {
					val <<= 1;
					if (u < inData.width) {
						switch(f) {
						case 0: val += floyd(inData, u++, v, inPos++); 
							break;
						case 1: val += floyd2(inData, u++, v, inPos++); 
							break;
						case 2: val += stucki(inData, u++, v, inPos++); 
							break;
						}
					}
				}
				outData.data[outPos++] = (byte)~val;
			}
			inPos += inPadding;
		}
		
		return outData;
	}

	/*
	 * clamping is necessary
	 * used only in floyd
	 */
	private static void set(ImageData inData, int u, int v, int delta) {
		inData.setPixel(u, v, ImageProcessing.clamp8(inData.getPixel(u, v) + delta));
	}
	
	private static int floyd(ImageData inData, int u, int v, int inPos) {
		// example of a simple and inefficient implementation
		final int gray = inData.getPixel(u, v);
		final int bw = (gray < 128) ? 0 : 1;
		final int diff = gray - 255*bw;
		boolean one_on_right = (u + 1) < inData.width;
		
		if (one_on_right) {
			set(inData, u + 1, v, 7*diff/16);
		}
		if (v + 1 < inData.height) {
			if (u >= 1) {
				set(inData, u - 1, v + 1, 3*diff/16);
			}
			set(inData, u, v + 1, 5*diff/16);
			if (one_on_right) {
				set(inData, u + 1, v + 1, diff/16);
			}
		}
		
		return bw;
	}
	
	/*
	 * clamping is necessary
	 * 0xFF & x is necessary because x a signed byte value
	 * used in floyd2 and stucki
	 */
	private static void set(ImageData inData, int inPos, int delta) {
		inData.data[inPos] = (byte)ImageProcessing.clamp8((0xFF & inData.data[inPos]) + delta);
	}
	
	private static int floyd2(ImageData inData, int u, int v, int inPos) {
		// example of an efficient implementation
		final int gray = 0xFF & inData.data[inPos]; // 0xFF & is necessary, because byte is signed
		final int bw = (gray < 128) ? 0 : 1;
		final int diff = gray - 255*bw; 
		assert -127 <= diff && diff <= 127 : "wrong diff";
		final int d7 = 7*diff >> 4;
		final int d3 = 3*diff >> 4;
		final int d5 = 5*diff >> 4;
		final int d1 = diff - d7 - d5 - d3;
		final boolean one_on_right = (u + 1) < inData.width;
		
		if (one_on_right) {
			set(inData, inPos + 1, d7);
		}
		
		if (v + 1 < inData.height) {
			inPos += inData.bytesPerLine - 1;
			if (u >= 1) {
				set(inData, inPos, d3);
			}
			inPos++;
			set(inData, inPos, d5);
			if (one_on_right) {
				inPos++;
				set(inData, inPos, d1);
			}
		}
		
		return bw;
	}
	
	private static int stucki(ImageData inData, int u, int v, int inPos) {
		// example of an efficient implementation
		final int gray = 0xFF & inData.data[inPos];
		final int bw = (gray < 128) ? 0 : 1;
		final int d8 = ((gray - 255*bw) << 3)/42;
		final int d4 = d8 >> 1;
		final int d2 = d8 >> 2;
		final int d1 = d8 >> 3;
		final boolean one_on_right = (u + 1) < inData.width;
		final boolean two_on_right = (u + 2) < inData.width;
		
		// first diffusion ( (x+1,y) -> 7)
		if (one_on_right) {
			set(inData, inPos + 1, d8);
		}
		
		// second diffusion ( (x+2,y) -> 5)
		if (two_on_right) {
			set(inData, inPos + 2, d4);
		}
		if (v + 1 < inData.height) {
			inPos += inData.bytesPerLine - 2;
			// third diffusion ( (x-2,y+1) -> 3)
			if (u >= 2) {
				set(inData, inPos, d2);
			}
			// fourth diffusion ( (x-1,y+1) -> 5)
			if (u >= 1) {
				set(inData, inPos + 1, d4);
			}
			// fifth diffusion ( (x,y+1) -> 7)
			set(inData, inPos + 2, d8);

			// sixth diffusion ( (x+1,y+1) -> 5)
			if (one_on_right) {
				set(inData, inPos + 3, d4);

				// seventh diffusion ( (x+2,y+1) -> 3)
				if (two_on_right) {
					set(inData, inPos + 4, d2);
				} else {
					set(inData, inPos + 3, d2);
				}
			} else {
				set(inData, inPos + 2, d4 + d2);
			}
		}

		if (v + 2 < inData.height) {
			inPos += inData.bytesPerLine;
			// eighth diffusion ( (x-2,y+2) -> 1)
			if (u >= 2) {
				set(inData, inPos, d1);
			}
			inPos++;

			// ninth diffusion ( (x-1,y+2) -> 3)
			if (u >= 1) {
				set(inData, inPos, d2);
			}
			inPos++;

			// tenth diffusion ( (x,y+2) -> 5)
			set(inData, inPos, d4);

			// eleventh diffusion ( (x+1,y+2) -> 3)
			if (one_on_right) {
				set(inData, inPos + 1, d2);
				
				// twelfth diffusion ( (x+2,y+2) -> 1)
				if (two_on_right) {
					set(inData, inPos + 2, d1);
				} else {
					set(inData, inPos + 1, d1);
				}
			} else {
				set(inData, inPos, d2 + d1);
			}
		}

		return bw;
	}
	
}
