package nl.weeaboo.vn.image.impl;

import nl.weeaboo.common.Area2D;
import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.tex.GLTexRect;
import nl.weeaboo.gl.tex.GLTexture;
import nl.weeaboo.gl.tex.TextureException;
import nl.weeaboo.vn.image.ITexture;

public class TextureAdapter implements ITexture {

	private static final long serialVersionUID = ImageImpl.serialVersionUID;

	protected GLTexRect tr;

	private double scaleX, scaleY;

	public void glLoad(GLManager glm) throws TextureException {
		if (tr != null) {
			setTexRect(tr.glLoad(glm), scaleX, scaleY);
		}
	}

	public void glTryLoad(GLManager glm) {
		if (tr != null) {
			setTexRect(tr.glTryLoad(glm), scaleX, scaleY);
		}
	}

	@Override
	public int hashCode() {
		return (tr != null ? tr.hashCode() : 0);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
		    return false;
		}
		TextureAdapter ta = (TextureAdapter)obj;
		return (tr == ta.tr || (tr != null && tr.equals(ta.tr)))
			&& scaleX == ta.scaleX
			&& scaleY == ta.scaleY;
	}

	@Override
	public String toString() {
		return "TextureAdapter(" + tr + ")";
	}

	public int glId() {
		return (tr != null ? tr.glId() : 0);
	}

	public GLTexture getTexture() {
		if (tr == null) {
		    return null;
		}
		return tr.getTexture();
	}

	public GLTexRect getTexRect() {
		return tr;
	}

	@Override
	public Area2D getUV() {
		if (tr == null) {
		    return ITexture.DEFAULT_UV;
		}
		return tr.getUV();
	}

	@Override
	public double getWidth() {
		if (tr == null) {
		    return 0;
		}
		return tr.getWidth() * scaleX;
	}

	@Override
	public double getHeight() {
		if (tr == null) {
		    return 0;
		}
		return tr.getHeight() * scaleY;
	}

	@Override
	public double getScaleX() {
		return scaleX;
	}

	@Override
	public double getScaleY() {
		return scaleY;
	}

	public void setTexRect(GLTexRect tr, double scale) {
        setTexRect(tr, scale, scale);
    }
	public void setTexRect(GLTexRect tr, double sx, double sy) {
		this.tr = tr;
		this.scaleX = sx;
		this.scaleY = sy;
	}

}