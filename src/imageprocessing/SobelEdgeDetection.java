package imageprocessing;

import main.Picsi;
import org.eclipse.swt.graphics.ImageData;

import javax.swing.*;

import static imageprocessing.Filter.convolve;


public class SobelEdgeDetection implements IImageProcessor{
    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData input, int imageType) {
        String[] options = new String[]{ "Partial-X", "Partial-Y" };
        int option = JOptionPane.showOptionDialog(null, "Choose the Filter", "Filter", 0, JOptionPane.QUESTION_MESSAGE, null, options, "");

        if(imageType == Picsi.IMAGE_TYPE_RGB){
            input = Grauwertbild.createGreyScale(input, imageType, 0);
        }
        ImageData outData;
        switch (option) {
            case 0:
                outData = convolve(input, imageType, new int[][]{ { -3,  0,  3 }, { -10, 0, 10 }, { -3, 0, 3 } }, 32, 128);
                break;
            case 1:
                outData = convolve(input, imageType, new int[][]{ { -3, -10, -3 }, {  0, 0, 0 }, {  3, 10, 3 } }, 32, 128);
                break;
            default:
                outData = null;
                throw new IllegalArgumentException();
        }

        return outData;
    }
}
