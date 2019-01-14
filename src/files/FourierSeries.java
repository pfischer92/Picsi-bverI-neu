package files;

import java.io.IOException;
import java.io.PrintWriter;

public class FourierSeries implements IImageCreator {
    @Override
    public void create(PrintWriter pw, int imageType, int width, int height, int maxValue) throws IOException {
        final double maxY = 2*maxValue/Math.PI;
        final double dx = 2*Math.PI/width;
        double [] line = new double[width];

        // init line with A0/2
        for(int u = 0; u < width; u++) line[u] = maxValue/2;

        for (int v = 0; v < height; v++){
            final int k = 1 + 2*v;
            final double ak = (v %2 == 0) ? maxY/k : -maxY/k;
            for (int u = 0; u < width; u++){
                line[u] += ak*Math.cos(k*u*dx);
                pw.print((int)Math.round(line[u]));
                pw.print(' ');
            }
            pw.println();
        }
    }
}
