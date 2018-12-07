package imageprocessing;

import org.eclipse.swt.graphics.ImageData;
import utils.Complex;
import utils.Parallel;

public class DFT1D {
    private double[] m_cosTable;	// table of cos values, used to increase performance
    private double[] m_sinTable;	// "        sin

    /**
     * Creates a Fourier Transform object and initializes tables values of size M
     * @param M
     */
    public DFT1D(int M) {
        final double PIT2DM = 2*Math.PI/M;

        m_cosTable = new double[M];
        m_sinTable = new double[M];

        for (int i=0; i < M; i++) {
            final double m = PIT2DM*i;
            m_cosTable[i] = Math.cos(m);
            m_sinTable[i] = Math.sin(m);
        }
    }

    /**
     * Computes the DFT or the inverse DFT of g
     * @param g input values
     * @param forward is true for forward transform
     * @return complex Fourier spectrum
     */
    public Complex[] dft(Complex[] g, boolean forward) {
        int M = g.length;
        double s = 1/Math.sqrt(M);
        Complex[] G = new Complex[M];

        Parallel.For(0, M, m -> {
            //for (int m=0; m < M; m++) {
            double sumRe = 0;
            double sumIm = 0;
            int k = 0;

            for (int u=0; u < M; u++) {
                double gRe = g[u].m_re;
                double gIm = g[u].m_im;
                double cosw = m_cosTable[k];
                double sinw = m_sinTable[k];
                if (!forward) sinw = -sinw;

                sumRe += gRe*cosw - gIm*sinw;
                sumIm += gRe*sinw + gIm*cosw;

                k += m;
                if (k >= M) k -= M;
            }
            G[m] = new Complex(s*sumRe, s*sumIm);
        });
        return G;
    }
}
