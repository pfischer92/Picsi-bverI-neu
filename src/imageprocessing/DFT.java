package imageprocessing;

import gui.OptionPane;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import utils.Complex;
import utils.FrequencyDomain;
import utils.Parallel;

public class DFT implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        // let the user choose the output
        Object[] output = new Object[]{ "Power", "Phase", "Transformed Image" };
        int ch = OptionPane.showOptionDialog("Fourier Transform Output", SWT.ICON_INFORMATION, output, 0);
        if (ch < 0) return null;

        ImageData outData = null;

        FrequencyDomain fd = dft2D(inData);
        switch(ch) {
            case 0:
                outData = FFT.getPowerSpectrum(fd);
                FFT.swapQuadrants(outData);
                break;
            case 1:
                outData = FFT.getPhaseSpectrum(fd);
                FFT.swapQuadrants(outData);
                break;
            case 2:
                outData = idft2D(fd);
                break;
        }

        return outData;
    }

    /**
     * 2D Discrete Fourier Transform (forward transform)
     * @param inData input data
     * @return frequency domain object
     */
    public static FrequencyDomain dft2D(ImageData inData) {
        Complex[][] G = new Complex[inData.height][];

        // create arrays
        Complex[] row = new Complex[inData.width];
        for (int i=0; i < row.length; i++) {
            row[i] = new Complex();
        }

        // forward transform rows
        DFT1D dft1Drows = new DFT1D(inData.width);
        for (int v=0; v < inData.height; v++) {
            int rowPos = v*inData.bytesPerLine;

            Parallel.For(0, inData.width, u -> {
                row[u].m_re = 0xFF & inData.data[rowPos + u];
            });
            G[v] = dft1Drows.dft(row, true);
        }

        // forward transform columns
        DFT1D dft1Dcols = new DFT1D(inData.height);
        Complex[] col = new Complex[inData.height];
        for (int u=0; u < inData.width; u++) {
            final int u_ = u;
            Parallel.For(0, inData.height, v -> {
                col[v] = G[v][u_];
            });
            Complex[] Gcol = dft1Dcols.dft(col, true);
            Parallel.For(0, inData.height, v -> {
                G[v][u_] = Gcol[v];
            });
        }

        return new FrequencyDomain(inData, G);
    }

    /**
     * 2D Inverse Discrete Fourier Transform
     * @param fd frequency domain object
     * @return output grayscale image
     */
    public static ImageData idft2D(FrequencyDomain fd) {
        // create output image
        ImageData outData = new ImageData(fd.m_width, fd.m_height, fd.m_depth, fd.m_palette);

        // inverse transform rows
        DFT1D dft1Dr = new DFT1D(fd.getSpectrumWidth());

        for (int v=0; v < fd.getSpectrumHeight(); v++) {
            fd.m_g[v] = dft1Dr.dft(fd.m_g[v], false);
        }

        // inverse transform columns
        DFT1D dft1Dc = new DFT1D(fd.getSpectrumHeight());
        Complex[] col = new Complex[fd.getSpectrumHeight()];

        for (int u=0; u < outData.width; u++) {
            final int u_ = u;
            Parallel.For(0, fd.getSpectrumHeight(), v -> {
                col[v] = fd.m_g[v][u_];
            });
            Complex[] Gcol = dft1Dc.dft(col, false);
            Parallel.For(0, fd.getSpectrumHeight(), v -> {
                outData.data[u_ + v*outData.bytesPerLine] = (byte)ImageProcessing.clamp8(Gcol[v].m_re);
            });
        }
        return outData;
    }

}
