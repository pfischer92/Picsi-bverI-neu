package imageprocessing;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import main.Picsi;

public class LinearHistogramCompensation implements IImageProcessor {

	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_GRAY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		ImageData outData = linearHistogrammCompensation(inData);
		return outData;
	}
	
	/**
	 * Invert image data
	 * @param imageData will be modified
	 */
	public static ImageData linearHistogrammCompensation(ImageData imageData) {
		ImageData outData = (ImageData)imageData.clone();
		int[] hist = ImageProcessing.histogram(outData, 256);		// Histogramm erstellen
		int K = hist.length; 										// Anzahl Intensitäts-Stufen
		int k1 = K-1; 												// maximale Intensität
		int n = outData.width*outData.height;					// Anzahl Pixel
		byte[] LUT = new byte[K]; 									// lookup table
		int hKum = 0;												// kumuliertes Histogramm

		for (int i = 0; i < K; i++) {
			hKum += hist[i];										// kumuliertes Histogramm berechnen
			LUT[i] = (byte) (hKum*k1/n);							// Histogrammausgleich auf LUT anwenden
		}
			
		ImageProcessing.applyLUT(outData, LUT);					// LUT aufs Bild anwenden
		return outData;
	}
}

