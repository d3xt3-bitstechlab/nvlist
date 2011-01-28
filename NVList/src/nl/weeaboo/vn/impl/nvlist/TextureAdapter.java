package nl.weeaboo.vn.impl.nvlist;

import nl.weeaboo.gl.texture.GLTexRect;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.vn.IRenderer;
import nl.weeaboo.vn.ITexture;

@LuaSerializable
public class TextureAdapter implements ITexture {

	private static final long serialVersionUID = NVListImpl.serialVersionUID;
	
	private final GLTexRect tr;
	private final double scaleX, scaleY;
	
	public TextureAdapter(GLTexRect tr, IRenderer r) {
		this(tr, r.getWidth(), r.getHeight(), r.getRealWidth(), r.getRealHeight());
	}
	public TextureAdapter(GLTexRect tr, int w, int h, int iw, int ih) {
		this.tr = tr;		
		this.scaleX = w / (double)iw;
		this.scaleY = h / (double)ih;
	}
	
	@Override
	public String toString() {
		return String.format("TextureAdapter(%s)", tr.getPath());
	}
	
	public GLTexRect getTexRect() {
		return tr;
	}

	@Override
	public double getWidth() {
		return tr.getWidth() * scaleX;
	}
			
	@Override
	public double getHeight() {
		return tr.getHeight() * scaleY;
	}
	
}