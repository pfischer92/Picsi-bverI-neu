package imageprocessing;

import utils.Matrix;

public class Interpolation {
	
	static int nearestNeighbor(double p) {
		return (int) (p+.5);
	}

	static int[] nearestNeighbor(Matrix m) {
		return new int[] {nearestNeighbor(m.el(0, 0)), nearestNeighbor(m.el(1, 0))};
	}
}
