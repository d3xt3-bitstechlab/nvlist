package nl.weeaboo.vn.impl.nvlist;

import nl.weeaboo.common.Rect2D;
import nl.weeaboo.gl.texture.GLTexRect;
import nl.weeaboo.gl.texture.GLTexture;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.vn.ITexture;

@LuaSerializable
public class TextureAdapter implements ITexture {

	private static final long serialVersionUID = NVListImpl.serialVersionUID;
	
	private final GLTexRect tr;
	private final double scaleX, scaleY;
	
	public TextureAdapter(GLTexRect tr, double sx, double sy) {
		this.tr = tr;
		this.scaleX = sx;
		this.scaleY = sy;
	}
	
	@Override
	public String toString() {
		return String.format("TextureAdapter(%s)", (tr.getPath() != null ? tr.getPath() : tr.getWidth()+"x"+tr.getHeight()));
	}
	
	public int getTexId() {
		GLTexture tex = (tr != null ? tr.getTexture() : null);
		return (tex != null ? tex.getTexId() : 0);
	}
	
	public GLTexRect getTexRect() {
		return tr;
	}
	
	@Override
	public Rect2D getUV() {
		if (tr == null) return new Rect2D(0, 0, 1, 1);
		return tr.getUV();
	}
	
	@Override
	public double getWidth() {
		return tr.getWidth() * scaleX;
	}
			
	@Override
	public double getHeight() {
		return tr.getHeight() * scaleY;
	}
	
	public double getScaleX() {
		return scaleX;
	}
	
	public double getScaleY() {
		return scaleY;
	}
	
}