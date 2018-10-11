package files;
import javax.swing.JTextArea;

import main.Picsi;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

/**
 * Windows bitmaps (Windows image file type)
 * @author Christoph Stamm
 *
 */
public class BMP implements IImageFile {
	@Override
	public ImageData read(String fileName) {
		// Read the new image from the chosen file.
		return new ImageData(fileName);
	}

	@Override
	public void save(String fileName, int fileType, ImageData imageData, int imageType) {
		// Save the current image to the specified file.
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { imageData };
		loader.save(fileName, fileType);
	}

	@Override
	public boolean isBinaryFormat() {
		return true;
	}

	@Override
	public void displayTextOfBinaryImage(ImageData imageData, JTextArea text) {
		int imageType = Picsi.determineImageType(imageData);
		
		switch(imageType) {
		case Picsi.IMAGE_TYPE_BINARY:
			text.append("P1");
			text.append("\n" + imageData.width + " " + imageData.height);
			text.append("\n");
			PNM.writePBM(imageData, text);
			break;
		case Picsi.IMAGE_TYPE_GRAY:
			text.append("P2");
			text.append("\n" + imageData.width + " " + imageData.height);
			text.append("\n255\n");
			PNM.writePGM(imageData, text, 255);
			break;
		case Picsi.IMAGE_TYPE_RGB:
			text.append("P3");
			text.append("\n" + imageData.width + " " + imageData.height);
			text.append("\n255\n");
			PNM.writePPM(imageData, text, 255);
			break;
		}
	}
}
