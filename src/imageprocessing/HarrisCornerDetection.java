//package imageprocessing;
//
//import org.eclipse.swt.graphics.Image;
//import org.eclipse.swt.graphics.ImageData;
//import utils.Matrix;
//
//public class HarrisCornerDetection implements IImageProcessor
//{
//    @Override
//    public boolean isEnabled(int imageType)
//    {
//        return true;
//    }
//
//    @Override
//    public Image run(Image input, int imageType)
//    {
//        float[] hp = {2f/9, 5f/9, 2f/9};
//        float[] hd = {0.5f, 0, -0.5f};
//        float[] hb = {1f/64,6f/64,15f/64,20f/64,15f/64,6f/64,1f/64};
//
//        float normHp = getNorm(hp);
//        float normHd = getNorm(hd);
//        float normHb = getNorm(hb);
//
//        ImageData Ix = (ImageData) input.getImageData().clone();
//        ImageData Iy = (ImageData) input.getImageData().clone();
//
//        Ix = ConvolveEfficient.convolveX(Ix, hp, normHp);
//        Ix = ConvolveEfficient.convolveX(Ix, hd, normHd);
//
//        Iy = ConvolveEfficient.convolveY(Iy, hp, normHp);
//        Iy = ConvolveEfficient.convolveY(Iy, hd, normHd);
//
//        // Todo: How to sqaure and multiply ImageData objects?
//        ImageData A = null;
//        ImageData B = null;
//        ImageData C = null;
//
//        A = ConvolveEfficient.convolveX(A, hb, normHb);
//        A = ConvolveEfficient.convolveY(A, hb, normHb);
//
//        B = ConvolveEfficient.convolveX(B, hb, normHb);
//        B = ConvolveEfficient.convolveY(B, hb, normHb);
//
//        C = ConvolveEfficient.convolveX(C, hb, normHb);
//        C = ConvolveEfficient.convolveY(C, hb, normHb);
//
//
//    }
//
//    public static float getNorm(float[] filter){
//        int norm = 0;
//            for (float i : filter) {
//                norm += i;
//        }
//        if (norm == 0) norm = 1;
//
//        return norm;
//    }
//
//    private void makeCrf(float alpha, ImageData A) {
//        Q = new ImageData()
//        final float[] pA = (float[]) A.getPixels();
//        final float[] pB = (float[]) B.getPixels();
//        final float[] pC = (float[]) C.getPixels();
//        final float[] pQ = (float[]) Q.getPixels();
//        for (int i = 0; i < M * N; i++) {
//           float a = pA[i], b = pB[i], c = pC[i];
//           float det = a * b - c * c; // det( ¯M )
//           float trace = a + b; // trace( ¯M )
//           pQ[i] = det - alpha * (trace * trace);
//           }
//    }
//}
//
//
