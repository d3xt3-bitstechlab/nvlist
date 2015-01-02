package nl.weeaboo.vn;

import nl.weeaboo.common.Area2D;

public class TextureStub implements ITexture {

    private static final long serialVersionUID = 1L;

    private final int width, height;
    private final double scaleX, scaleY;
    private final Area2D uv;

    public TextureStub(int w, int h) {
        width = w;
        height = h;

        scaleX = scaleY = 1.0;

        uv = ITexture.DEFAULT_UV;
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public double getScaleX() {
        return scaleX;
    }

    @Override
    public double getScaleY() {
        return scaleY;
    }

    @Override
    public Area2D getUV() {
        return uv;
    }

}
