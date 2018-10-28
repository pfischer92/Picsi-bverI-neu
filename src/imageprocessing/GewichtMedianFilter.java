package imageprocessing;

import main.Picsi;
import org.eclipse.swt.graphics.ImageData;

import java.util.Arrays;

public class GewichtMedianFilter implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return Picsi.IMAGE_TYPE_GRAY == imageType;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        ImageData outData = (ImageData) inData.clone();

        // Gewichtismatrix
        int[][] gewichte = {
                {1, 2, 1},
                {2, 4, 2},
                {1, 2, 1}
        };

        applyWeigthMedianFilter(outData, gewichte, 1, 1);

        return outData;
    }

    public static void applyWeigthMedianFilter(ImageData imData, int[][]weights, int hotSpotX, int hotSpotY ){
        int gewSum = 0, val, pos = 0, x, y;

        // Summe der Gewichte ermittlen
        for (int i = 0; i < weights.length; i++){
            for (int j = 0; j < weights[0].length; j++){
                gewSum += weights[i][j];
            }
        }

        int[] values = new int[gewSum];

        // Filter anwenden
        for(int u = 0; u < imData.height; u++){
            for (int v = 0; v < imData.width; v++){
                for (int n = 0; n < weights.length; n++){
                    for (int m = 0; m < weights[0].length; m++){

                        // Randbehandlung
                        x = u + n -hotSpotX;
                        y = v + m - hotSpotY;
                        if (x < 0) x = -x;
                        if (x >= imData.width) x = 2*imData.width -1 -x;
                        if(y < 0) y = -y;
                        if(y >= imData.height) y = 2*imData.height - 1 -y;

                        // Intensität zwischenspeichern
                        for (int k = 0; k < weights[n][m]; k++){
                            values[pos] = imData.getPixel(x,y);
                            pos++;
                        }
                    }
                }

                // Neue Intensität bestimmen
                Arrays.sort(values);
                pos = values.length / 2;
                if (values.length % 2 == 0){
                    val = values[pos-1] + values[pos]/2;    // gerade Anzahl: Median als arithmetischer Durchschnitt
                }
                else {
                    val = values[pos];
                }

                try{
                    imData.setPixel(u,v, ImageProcessing.clamp8(val));
                }
                catch (Exception e){
                    System.out.println("Set pixel: u=" + u + " v=" +v);
                }
            }
        }

    }
}
