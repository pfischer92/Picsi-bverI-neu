package utils;

import org.eclipse.swt.graphics.RGB;

public class RGB2
{
    public final RGB rgb;
    public final float h;
    public final float s;
    public final float b;

    public RGB2(RGB rgb){
        this.rgb = rgb;
        float[] hsb = rgb.getHSB();
        this.h = hsb[0];
        this.s = hsb[1];
        this.b = hsb[2];
    }
}
