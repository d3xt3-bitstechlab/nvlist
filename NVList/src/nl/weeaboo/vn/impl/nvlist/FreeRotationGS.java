package nl.weeaboo.vn.impl.nvlist;

import nl.weeaboo.vn.BlendMode;
import nl.weeaboo.vn.IGeometryShader;
import nl.weeaboo.vn.IImageDrawable;
import nl.weeaboo.vn.IPixelShader;
import nl.weeaboo.vn.IRenderer;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.BaseRenderer;

public class FreeRotationGS implements IGeometryShader {

	private static final long serialVersionUID = NVListImpl.serialVersionUID;

	private double rotX, rotY, rotZ;
	private boolean changed;
	
	public FreeRotationGS() {
	}

	//Functions
	@Override
	public boolean update() {
		//Nothing to do
		
		boolean result = changed;
		changed = false;
		return result;
	}
	
	@Override
	public void draw(IRenderer r, IImageDrawable image, ITexture tex, IPixelShader ps) {
		BaseRenderer rr = (BaseRenderer)r;
		
		short z = image.getZ();
		boolean clip = image.isClipEnabled();
		BlendMode blend = image.getBlendMode();
		int argb = image.getColor();
		double x = image.getX();
		double y = image.getY();
		double w = image.getWidth();
		double h = image.getHeight();
		
		rr.draw(new RotatedQuadCommand(z, clip, blend, argb, tex,
					x, y, w, h, ps, rotX, rotY, rotZ));
	}
	
	//Getters
	
	//Setters
	public void setRotation(double rx, double ry, double rz) {
		if (rotX != rx || rotY != ry || rotZ != rz) {
			rotX = rx;
			rotY = ry;
			rotZ = rz;
			changed = true;
		}
	}
	
}
