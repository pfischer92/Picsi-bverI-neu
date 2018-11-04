package imageprocessing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import gui.OptionPane;
import main.Picsi;
import utils.Parallel;

/**
 * Morphologic filter and demo
 * @author Christoph Stamm
 *
 */
public class MorphologicFilter implements IImageProcessor {
	public static int s_background = 0; // white
	public static int s_foreground = 1; // black
	public static boolean[][] s_circle3 = new boolean[][] {{ false, true, false},{true, true, true},{false, true, false}};
	public static boolean[][] s_circle5 = new boolean[][] {
		{ false, true,  true, true,  false},
		{ true,  true,  true, true,  true},
		{ true,  true,  true, true,  true},
		{ true,  true,  true, true,  true},
		{ false, true,  true, true,  false}};
	public static boolean[][] s_circle7 = new boolean[][] {
		{ false, false, true, true, true, false, false},
		{ false, true,  true, true, true, true,  false},
		{ true,  true,  true, true, true, true,  true},
		{ true,  true,  true, true, true, true,  true},
		{ true,  true,  true, true, true, true,  true},
		{ false, true,  true, true, true, true,  false},
		{ false, false, true, true, true, false, false}};
	public static boolean[][] s_diamond5 = new boolean[][] {
		{ false, false, true, false, false},
		{ false, true , true, true , false},
		{ true,  true,  true, true,  true},
		{ false, true,  true, true,  false},
		{ false, false, true, false, false}};
	public static boolean[][] s_diamond7 = new boolean[][] {
		{ false, false, false, true, false, false, false},
		{ false, false, true,  true, true,  false, false},
		{ false, true,  true,  true, true,  true,  false},
		{ true,  true,  true,  true, true,  true,  true},
		{ false, true,  true,  true, true,  true,  false},
		{ false, false, true,  true, true,  false, false},
		{ false, false, false, true, false, false, false}};
	public static boolean[][] s_square2 = new boolean[][] {{ true, true},{true, true}};
	public static boolean[][] s_square3 = new boolean[][] {{ true, true, true},{true, true, true},{true, true, true}};
	public static boolean[][] s_square4 = new boolean[][] {{ true, true, true, true},{true, true, true, true},{true, true, true, true},{true, true, true, true}};
	public static boolean[][] s_square5 = new boolean[][] {{ true, true, true, true, true},{true, true, true, true, true},{true, true, true, true, true},{true, true, true, true, true},{true, true, true, true, true}};

	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_BINARY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		Object[] operations = { "Erosion", "Dilation" };
		int ch = OptionPane.showOptionDialog("Morphological Operation", SWT.ICON_INFORMATION, operations, 0);
		if (ch < 0) return null;
		
		Object[] structure = { "Circle-3", "Circle-5", "Circle-7", "Diamond-5", "Diamond-7", "Square-2", "Square-3", "Square-4", "Square-5" };
		int s = OptionPane.showOptionDialog("Structure", SWT.ICON_INFORMATION, structure, 0);
		if (s < 0) return null;
		boolean[][] struct;
		int cx, cy;
		switch(s) {
		default:
		case 0: struct = s_circle3; cx = cy = 1; break;
		case 1: struct = s_circle5; cx = cy = 2; break;
		case 2: struct = s_circle7; cx = cy = 3; break;
		case 3: struct = s_diamond5; cx = cy = 2; break;
		case 4: struct = s_diamond7; cx = cy = 3; break;
		case 5: struct = s_square2; cx = cy = 0; break;
		case 6: struct = s_square3; cx = cy = 1; break;
		case 7: struct = s_square4; cx = cy = 1; break;
		case 8: struct = s_square5; cx = cy = 2; break;
		}
		
		if (ch == 0) {
			return erosion(inData, struct, cx, cy);
		} else {
			return dilation(inData, struct, cx, cy);
		}
	}

	/**
	 * Erosion
	 * @param inData binary image or binarized grayscale image
	 * @param struct all true elements belong to the structure
	 * @param cx origin of the structure (hotspot)
	 * @param cy origin of the structure (hotspot)
	 * @return new eroded binary image
	 */
	public static ImageData erosion(ImageData inData, boolean[][] struct, int cx, int cy) {
		assert Picsi.determineImageType(inData) == Picsi.IMAGE_TYPE_BINARY || Picsi.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;
		
		ImageData outData = (ImageData)inData.clone();
		
		Parallel.For(0, outData.height, v -> {
			for (int u=0; u < outData.width; u++) {
				boolean set = true;
				
				for (int j=0; set && j < struct.length; j++) {
					final int v0 = v + j - cy;
					
					if (v0 >= 0 && v0 < inData.height) {
						for (int i=0; set && i < struct[j].length; i++) {
							final int u0 = u + i - cx;
							
							if (u0 < 0 || u0 >= inData.width || (struct[j][i] && inData.getPixel(u0, v0) != s_foreground)) {
								set = false;
							}
						}
					} else {
						set = false;
					}
				}
				if (set) outData.setPixel(u, v, s_foreground); // foreground
				else outData.setPixel(u, v, s_background); // background
			}
		});
		return outData;
	}

	/**
	 * Dilation
	 * @param inData binary image or binarized grayscale image
	 * @param struct all true elements belong to the structure
	 * @param cx origin of the structure (hotspot)
	 * @param cy origin of the structure (hotspot)
	 * @return new dilated binary image
	 */
	public static ImageData dilation(ImageData inData, boolean[][] struct, int cx, int cy) {
		assert Picsi.determineImageType(inData) == Picsi.IMAGE_TYPE_BINARY || Picsi.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;
		
		ImageData outData = new ImageData(inData.width, inData.height, inData.depth, inData.palette); // new Data is initialized with 0

        Parallel.For(0, outData.height, v -> {
            for (int u=0; u < outData.width; u++) {
                boolean set = false;

                for (int j=0; set && j < struct.length; j++) {
                    final int v0 = v + j - cy;

                    if (v0 >= 0 && v0 < inData.height) {
                        for (int i=0; set && i < struct[j].length; i++) {
                            final int u0 = u + i - cx;

                            if (u0 < 0 || u0 >= inData.width || (struct[j][i] && inData.getPixel(u0, v0) != s_foreground)) {
                                set = true;
                            }
                        }
                    } else {
                        set = true;
                    }
                }
                if (set) outData.setPixel(u, v, s_foreground); // foreground
                else outData.setPixel(u, v, s_background); // background
            }
        });
        return outData;
	}
	
	/**
	 * Opening
	 * @param inData binary image or binarized grayscale image
	 * @param struct all true elements belong to the structure
	 * @param cx origin of the structure (hotspot)
	 * @param cy origin of the structure (hotspot)
	 * @return new opened binary image
	 */
	public static ImageData opening(ImageData inData, boolean[][] struct, int cx, int cy) {
		return dilation(erosion(inData, struct, cx, cy), struct, cx, cy);
	}

	/**
	 * Closing
	 * @param inData binary image or binarized grayscale image
	 * @param struct all true elements belong to the structure
	 * @param cx origin of the structure (hotspot)
	 * @param cy origin of the structure (hotspot)
	 * @return new closed binary image
	 */
	public static ImageData closing(ImageData inData, boolean[][] struct, int cx, int cy) {
		return erosion(dilation(inData, struct, cx, cy), struct, cx, cy);
	}
}
