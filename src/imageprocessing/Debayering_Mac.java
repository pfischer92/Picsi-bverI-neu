package imageprocessing;

import main.Picsi;
import utils.Parallel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import gui.OptionPane;

/**
 * Debayering
 * @author Christoph Stamm
 *
 */
public class Debayering_Mac implements IImageProcessor {
	static final int Bypp = 3;

	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_RGB;	// MacOS Case
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		// create outData
		PaletteData pd = new PaletteData(0xFF0000, 0xFF00, 0xFF); // R G B
		pd.redShift = -16;
		pd.greenShift = -8;
		pd.blueShift = 0;
		ImageData outData = new ImageData(inData.width, inData.height, Bypp*8, pd);
		
		// Debayering of raw input image
		debayering(inData, outData);
		
		return outData;
	}

	/**
	 * ToDo: Debayering
	 */
	private void debayering(ImageData inData, ImageData outData) {
		// Parallel.For(0, outData.height, v -> {
		RGB rgb = new RGB(0, 0, 0);
		for(int v = 0; v < outData.height; v++) {		
			
			for (int u=0; u < outData.width; u++) {
				
				int value = 0xFF & inData.getPixel(u, v);	// MacOS Case
				
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
						int rightNeighbour = 0xFF & inData.getPixel(u+1, v);	// MacOS Case
						int bottomNeighbour = 0xFF & inData.getPixel(u, v+1);	// MacOS Case
						
						int interpolatedGreen = (rightNeighbour + bottomNeighbour) >> 1;
						
						// Red val from Pixel at u+1 v+1
						int redNeighbour = 0xFF & inData.getPixel(u+1, v+1);	// MacOS Case
						
						// Set new RGB to Pixel						
						rgb.red = redNeighbour;
						rgb.green = interpolatedGreen;							
					}
					
					// Case Red
					else if(u%2 == 1 && v%2 == 1) {
						// red stays
						
						// Green val from Pixel at u+1 and v+1 divided by 2 (right shift)
						int rightNeighbour = 0xFF & inData.getPixel(u+1, v);		// MacOS Case
						int bottomNeighbour = 0xFF & inData.getPixel(u, v+1);		// MacOS Case
						
						int interpolatedGreen = (rightNeighbour + bottomNeighbour) >> 1;
						
						// Blue val from Pixel at u+1 v+1
						int blueNeighbour = 0xFF & inData.getPixel(u+1, v+1);		// MacOS Case
						
						// Set new RGB to Pixel		
						rgb.green = interpolatedGreen;	
						rgb.blue = blueNeighbour;	
					}
					
					// Case Green
					else {	
						// Green val from actual Pixel and from Pixel at u+1 v+1 divided by 2 (right shift)
						int greenNeighbour = 0xFF & inData.getPixel(u+1, v+1); 		// MacOS Case
						int interpolatedGreen = (rgb.green + greenNeighbour) >> 1;
						
						// Red val from Pixel at v+1
						int redNeighbour = 0xFF & inData.getPixel(u, v+1);	// MacOS Case
						
						// Blue val from Pixel at v+1
						int blueNeighbour = 0xFF & inData.getPixel(u+1, v);		// MacOS Case
						
						// Set new RGB to Pixel						
						rgb.red = redNeighbour;
						rgb.green = interpolatedGreen;	
						rgb.blue = blueNeighbour;
					}
				}				

				// write rgb to output pixel
				outData.setPixel(u, v, outData.palette.getPixel(rgb));	// MacOS Case
			}
		//});
		}
	}
}
