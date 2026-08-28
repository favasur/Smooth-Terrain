package io.github.favasur.smoothterrain.client.render.struct;

public final class Color {
    public static final Color WHITE = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    public float red;
    public float green;
    public float blue;
    public float alpha;

    public Color(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public Color multiplyUNSAFENEEDSVALHALLA(float shading) {
        red *= shading;
        green *= shading;
        blue *= shading;
        return this;
    }

    public void unpackFromARGB(int color) {
        red = (color >> 16 & 0xFF) / 255.0F;
        green = (color >> 8 & 0xFF) / 255.0F;
        blue = (color & 0xFF) / 255.0F;
    }

    public int packToARGB() {
        return ((int) (alpha * 255.0F) & 255) << 24
                | ((int) (red * 255.0F) & 255) << 16
                | ((int) (green * 255.0F) & 255) << 8
                | ((int) (blue * 255.0F) & 255);
    }
}
