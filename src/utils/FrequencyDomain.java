package utils;

import java.util.Arrays;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

/**
 * Frequency domain object used to store the result in Fourier Transforms
 * 
 * @author Christoph Stamm
 *
 */
public class FrequencyDomain implements Cloneable {
	public int m_width, m_height;	// image size
	public int m_depth;				// image bit depth
	public double m_powerScale;		// scale factor used in power spectrum, 0 = undefined scale
	public double m_min;			// log of min transformed value
	public PaletteData m_palette;	// image palette
	public Complex[][] m_g;			// transformed image
	
	/**
	 * @param inData input image
	 * @param g Fourier coefficients
	 */
	public FrequencyDomain(ImageData inData, Complex[][] g) {
		m_width = inData.width;
		m_height = inData.height;
		m_depth = inData.depth;
		m_palette = inData.palette;
		m_g = g;
	}
	
	/**
	 * Copy constructor
	 * @param fd
	 */
	public FrequencyDomain(FrequencyDomain fd) {
		m_width = fd.m_width;
		m_height = fd.m_height;
		m_depth = fd.m_depth;
		m_palette = fd.m_palette;
		m_powerScale = fd.m_powerScale;
		m_min = fd.m_min;
		m_g = new Complex[fd.m_g.length][];
		for(int i=0; i < m_g.length; i++) {
			m_g[i] = Arrays.copyOf(fd.m_g[i], fd.m_g[i].length);
		}
	}
	
	/**
	 * Returns amplitude at given position
	 * @param u x-coordinate
	 * @param v y-coordinate
	 * @return amplitude
	 */
	public double getAmplitude(int u, int v) {
		return m_g[v][u].abs();
	}
	
	/**
	 * Returns phase at given position
	 * @param u x-coordinate
	 * @param v y-coordinate
	 * @return phase
	 */
	public double getPhase(int u, int v) {
		return m_g[v][u].arg();
	}
	
	public int getSpectrumWidth() { return m_g[0].length; }
	public int getSpectrumHeight() { return m_g.length; }
	
	/**
	 * Sets amplitude and phase at given position
	 * @param u x-coordinate
	 * @param v y-coordinate
	 * @param amp amplitude
	 * @param phi phase
	 */
	public void setValue(int u, int v, double amp, double phi) { 
		m_g[v][u] = new Complex(amp*Math.cos(phi), amp*Math.sin(phi));
	}
	
	@Override
	public FrequencyDomain clone() {
		return new FrequencyDomain(this);
	}
}
