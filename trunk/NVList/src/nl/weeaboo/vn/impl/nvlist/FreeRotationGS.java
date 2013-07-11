package nl.weeaboo.vn.impl.nvlist;

/*
@LuaSerializable
public class FreeRotationGS extends BaseShader implements IGeometryShader {

	private static final long serialVersionUID = NVListImpl.serialVersionUID;

	private double rotX, rotY, rotZ;
	
	public FreeRotationGS() {
		super(true);
	}

	//Functions	
	@Override
	public void draw(IDrawBuffer d, IImageDrawable image, ITexture tex,
			double alignX, double alignY, IPixelShader ps)
	{
		BaseDrawBuffer dd = BaseDrawBuffer.cast(d);
		
		short z = image.getZ();
		boolean clip = image.isClipEnabled();
		BlendMode blend = image.getBlendMode();
		int argb = image.getColorARGB();
		Matrix trans = image.getTransform();
		double w = image.getUnscaledWidth();
		double h = image.getUnscaledHeight();
		
		Vec2 offset = LayoutUtil.getImageOffset(tex, alignX, alignY);
		dd.draw(new RotatedQuadCommand(z, clip, blend, argb, tex,
					trans, offset.x, offset.y, w, h, ps, rotX, rotY, rotZ));
	}
	
	//Getters
	
	//Setters
	public void setRotation(double rx, double ry, double rz) {
		if (rotX != rx || rotY != ry || rotZ != rz) {
			rotX = rx;
			rotY = ry;
			rotZ = rz;
			markChanged();
		}
	}
	
}
*/