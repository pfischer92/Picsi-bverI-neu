package imageprocessing;

import main.Picsi;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;

import gui.OptionPane;
import utils.Complex;
import utils.FrequencyDomain;
import utils.Parallel;

/**
 * 2D Discrete Fourier Transform (FFT, FHT)
 * 
 * @author Christoph Stamm
 *
 */
public class FFT implements IImageProcessor {
	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_GRAY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		// let the user choose the operation
		Object[] operations = { "FFT", "FHT", "Inverse Filtering" };
		int f1 = OptionPane.showOptionDialog("Fourier Transform Operation", 
				SWT.ICON_INFORMATION, operations, 0);
		if (f1 < 0) return null;
		
		Object[] output = null;
		int f2;
		
		if (f1 < 2) {
			output = new Object[]{ "Power", "Phase", "Transformed Image" };
			f2 = OptionPane.showOptionDialog("Fourier Transform Output", 
					SWT.ICON_INFORMATION, output, 0);
		} else {
			output = new Object[]{ "Blurred Image", "Inverse Filtered Image" };
			f2 = OptionPane.showOptionDialog("Inverse Filtering Output", 
					SWT.ICON_INFORMATION, output, 0);
		}
		if (f2 < 0) return null;
		
		FrequencyDomain fd = null;
		ImageData outData = null;
		
		switch(f1) {
		case 0:
			fd = fft2D(inData);
			switch(f2) {
			case 0:
				outData = getPowerSpectrum(fd);
				swapQuadrants(outData);
				break;
			case 1:
				outData = getPhaseSpectrum(fd);
				swapQuadrants(outData);
				break;
			case 2:
				outData = ifft2D(fd);
				break;
			}
			break;
		case 1:
			fd = fht2D(inData);
			switch(f2) {
			case 0:
				outData = getPowerSpectrum(fd);
				swapQuadrants(outData);
				break;
			case 1:
				outData = getPhaseSpectrum(fd);
				swapQuadrants(outData);
				break;
			case 2:
				outData = ifht2D(fd);
				break;
			}
			break;
		case 2:
			// Inverse Filtering
			outData = fht2DInverseFiltering(inData, f2 == 0);
			break;
		default:
			return null;
		}
		
		return outData;
	}

	/**
	 * 2D Fast Fourier Transform (forward transform)
	 * @param inData input data
	 * @return frequency domain object
	 */
	public static FrequencyDomain fft2D(ImageData inData) {
		return fft2D(inData, 1);
	}
	
	/**
	 * 2D Fast Fourier Transform (forward transform)
	 * @param inData input data
	 * @param norm
	 * @return frequency domain object
	 */
	public static FrequencyDomain fft2D(ImageData inData, int norm) {
		int l = inData.width - 1;
		int w = 1;
		while(l > 0) {
			l >>= 1;
			w <<= 1;
		}
		l = inData.height - 1;
		int h = 1;
		while(l > 0) {
			l >>= 1;
			h <<= 1;
		}
		
		Complex[] row = new Complex[w];
		Complex[] col = new Complex[h];
		Complex[][] G = new Complex[h][];
		
		// create arrays
		for (int i=0; i < row.length; i++) {
			row[i] = new Complex();
		}
		
		// forward transform rows
		int rowPos = 0;
		for (int v=0; v < h; v++) {
			if (v < inData.height) {
				for (int u=0; u < inData.width; u++) {
					row[u].m_re = (0xFF & inData.data[rowPos + u])/(double)norm;
				}
				rowPos += inData.bytesPerLine;
			} else if (v == inData.height) {
				for (int u=0; u < inData.width; u++) {
					row[u].m_re = 0;
				}
			}
			G[v] = FFT1D.fft(row);
		}
		
		// forward transform columns
		for (int u=0; u < w; u++) {
			for (int v=0; v < h; v++) {
				col[v] = G[v][u];
			}
			Complex[] Gcol = FFT1D.fft(col);
			for (int v=0; v < h; v++) {
				G[v][u] = Gcol[v];
			}	
		}
		return new FrequencyDomain(inData, G);
	}
	
	/**
	 * 2D Inverse Fast Fourier Transform
	 * @param fd frequency domain object
	 * @return output image
	 */
	public static ImageData ifft2D(FrequencyDomain fd) {
		ImageData outData = new ImageData(fd.m_width, fd.m_height, fd.m_depth, fd.m_palette);
		Complex[] col = new Complex[fd.m_g.length];
	
		// inverse transform rows
		for (int v=0; v < fd.m_g.length; v++) {
			fd.m_g[v] = FFT1D.ifft(fd.m_g[v]);
		}
		
		// inverse transform columns
		int rowPos = 0;
		for (int u=0; u < outData.width; u++) {
			for (int v=0; v < fd.m_g.length; v++) {
				col[v] = fd.m_g[v][u];
			}
			Complex[] Gcol = FFT1D.ifft(col);
			rowPos = 0;
			for (int v=0; v < outData.height; v++) {
				outData.data[u + rowPos] = (byte)ImageProcessing.clamp8(Gcol[v].m_re);
				rowPos += outData.bytesPerLine;
			}	
		}
		return outData;
	}

	public static void multiply(FrequencyDomain fd1, FrequencyDomain fd2) {
		assert fd1.m_g.length == fd2.m_g.length;
		
		for(int v = 0; v < fd1.m_g.length; v++) {
			assert fd1.m_g[v].length == fd2.m_g[v].length;
			for(int u = 0; u < fd1.m_g[v].length; u++) {
				fd1.m_g[v][u].mul(fd2.m_g[v][u]);
			}
		}
	}

	public static void divide(FrequencyDomain fd1, FrequencyDomain fd2) {
		assert fd1.m_g.length == fd2.m_g.length;
		
		for(int v = 0; v < fd1.m_g.length; v++) {
			assert fd1.m_g[v].length == fd2.m_g[v].length;
			for(int u = 0; u < fd1.m_g[v].length; u++) {
				fd1.m_g[v][u].div(fd2.m_g[v][u]);
			}
		}
	}
	
	/**
	 * 2D Fast Hartley Transform (forward transform)
	 * @param inData input data
	 * @return frequency domain object
	 */
	public static FrequencyDomain fht2D(ImageData inData) {
		FHT fht2D = new FHT(inData);
		
		fht2D.transform();
		return new FrequencyDomain(inData, fht2D.getSpectrum());
	}
		
	/**
	 * 2D Inverse Fast Hartley Transform
	 * @param fd frequency domain object
	 * @return output image
	 */
	public static ImageData ifht2D(FrequencyDomain fd) {
		FHT fht2D = new FHT(fd.m_g, fd.m_width, fd.m_height, fd.m_depth, fd.m_palette);
		
		fht2D.inverseTransform();
		return fht2D.getImage();
	}

	/**
	 * Experiment: inverse image filtering
	 * @param inData
	 * @return
	 */
	public static ImageData fft2DInverseFiltering(ImageData inData, boolean blurredImage) {
		final int fsize = (int)(Math.min(inData.width, inData.height)/3.8);
		final int hstart = (inData.height - fsize)/2;
		final int wstart = (inData.width - fsize)/2;
		
		ImageData filter = new ImageData(inData.width, inData.height, inData.depth, inData.palette);

		for (int v=hstart; v < hstart + fsize; v++) {
			for (int u=wstart; u < wstart + fsize; u++) {
				filter.setPixel(u, v, 1);
			}
		}
		
		FrequencyDomain fdi = fft2D(inData);				// 0-frequency top-left
		FrequencyDomain fdf = fft2D(filter, fsize*fsize);	// 0-frequency top-left
		multiply(fdi, fdf);									// 0-frequency in center

		if (blurredImage) {
			// show blurred image
			ImageData outData = ifft2D(fdi);				// swapped quadrants
			swapQuadrants(outData);
			return outData;									// normal quadrants
		} else {
			// inverse filtering
			divide(fdi, fdf);								// 0-frequency top-left
			ImageData outData = ifft2D(fdi);				// normal quadrants
			return outData;
		}
	}
	
	/**
	 * Experiment: inverse image filtering
	 * @param inData
	 * @return
	 */
	public static ImageData fht2DInverseFiltering(ImageData inData, boolean blurredImage) {
		final int fsize = (int)(Math.min(inData.width, inData.height)/3.8);
		final int hstart = (inData.height - fsize)/2;
		final int wstart = (inData.width - fsize)/2;
		
		ImageData filter = new ImageData(inData.width, inData.height, inData.depth, inData.palette);

		for (int v=hstart; v < hstart + fsize; v++) {
			for (int u=wstart; u < wstart + fsize; u++) {
				filter.setPixel(u, v, 1);
			}
		}
		
		FHT fht2Df = new FHT(filter, fsize*fsize); // filter coefficients: 1/(fsize*fsize)
		FHT fht2Di = new FHT(inData);
		
		fht2Df.transform();							// 0-frequency top-left
		fht2Di.transform(); 						// 0-frequency top-left
		
		FHT blurred = fht2Di.multiply(fht2Df); 		// 0-frequency in center
		blurred.inverseTransform(); 				// swapped quadrants
				
		if (blurredImage) {
			// show blurred image
			ImageData outData = blurred.getImage();	// swapped quadrants
			swapQuadrants(outData);					// normal quadrants
			return outData;
		} else {
			// inverse filtering
			blurred.transform(); 					// 0-frequency in center
			FHT div = blurred.divide(fht2Df); 		// 0-frequency top-left
			div.inverseTransform(); 				// normal quadrants
			return div.getImage();
		}
	}

	/**
	 * 2D power spectrum image: log(re^2 + im^2)
	 * @param fd frequency domain object
	 * @return output image of the power spectrum
	 */
	public static ImageData getPowerSpectrum(FrequencyDomain fd) {
		final int height = fd.getSpectrumHeight();
		final int width = fd.getSpectrumWidth();

		if (fd.m_powerScale == 0) {
			final double delta = 50;
			
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
	
	  		for (int row=0; row < height; row++) {
				for (int col=0; col < width; col++) {
					final double power = fd.m_g[row][col].abs2();
					if (power < min) min = power;
					if (power > max) max = power;
				}
			}
	
	  		//System.out.println("min = " + min + ", max = " + max);
			max = Math.log(max)/2;
			min = Math.log(min)/2;
			if (Double.isNaN(min) || max - min > delta)
				min = max - delta; //display range not more than approx. e^delta
			fd.m_powerScale = 253.999/(max - min);
			fd.m_min = min;
		}

 		byte[] ps = new byte[height*width];
 		
		Parallel.For(0, height, row -> {
			final int offset = row*width;
			
			for (int col=0; col < width; col++) {
				double power = fd.m_g[row][col].abs2();
				power = (Math.log(power)/2 - fd.m_min)*fd.m_powerScale;
				if (Double.isNaN(power) || power < 0) power = 0;
				ps[offset + col] = (byte)(power + 1); // 1 is min value
			}
  		});
		
		ImageData outData = new ImageData(width, height, fd.m_depth, fd.m_palette, 1, ps);
		return outData;		
	}
	
	/**
	 * 2D phase spectrum image
	 * @param fd frequency domain object
	 * @return output image of the phase spectrum
	 */
	public static ImageData getPhaseSpectrum(FrequencyDomain fd) {
		final double PID2 = Math.PI/2;
		final int height = fd.getSpectrumHeight();
		final int width = fd.getSpectrumWidth();
		final double scale = 255/Math.PI;
		
 		byte[] ps = new byte[height*width];
 		
		Parallel.For(0, height, row -> {
			final int offset = row*width;

			for (int col=0; col < width; col++) {
				double phi = fd.m_g[row][col].arg();
				ps[offset + col] = (byte)((phi + PID2)*scale);

			}
		});
		
		ImageData outData = new ImageData(width, height, fd.m_depth, fd.m_palette, 1, ps);
		return outData;
	}
	
	/**	
	 * Swap quadrants B and D and A and C of the specified image data 
	 * so the power spectrum origin is at the center of the image.
	<pre>
	    B A
	    C D
	</pre>
	 */
	public static void swapQuadrants(ImageData inData) {
		final int w2 = inData.width/2,  w1 = inData.width - w2;
		final int h2 = inData.height/2, h1 = inData.height - h2;

		ImageData tA = ImageProcessing.crop(inData, w1, 0, w2, h1);
		ImageData tB = ImageProcessing.crop(inData, 0, 0, w1, h1);
		ImageData tC = ImageProcessing.crop(inData, 0, h1, w1, h2);
		ImageData tD = ImageProcessing.crop(inData, w1, h1, w2, h2);
		
		ImageProcessing.insert(inData, tA, 0, h2);
		ImageProcessing.insert(inData, tB, w2, h2);
		ImageProcessing.insert(inData, tC, w2, 0);
		ImageProcessing.insert(inData, tD, 0, 0);
	}

}
