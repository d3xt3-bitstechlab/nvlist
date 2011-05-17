package nl.weeaboo.vn.impl.nvlist;

import javax.media.opengl.GL2ES1;

import nl.weeaboo.gl.GLManager;
import nl.weeaboo.vn.BlendMode;
import nl.weeaboo.vn.IPixelShader;
import nl.weeaboo.vn.IRenderer;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.CustomRenderCommand;

public class RotatedQuadCommand extends CustomRenderCommand {

	private ITexture itex;
	private double x, y, w, h;
	private double rotX, rotY, rotZ;
	
	public RotatedQuadCommand(short z, boolean clipEnabled, BlendMode blendMode,
			int argb, ITexture tex, double x, double y, double w, double h,
			IPixelShader ps, double rotX, double rotY, double rotZ)
	{
		super(z, clipEnabled, blendMode, argb, ps, (byte)0);
		
		this.itex = tex;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.rotX = rotX;
		this.rotY = rotY;
		this.rotZ = rotZ;
	}

	//Functions
	@Override
	protected void renderGeometry(IRenderer r) {
		Renderer rr = (Renderer)r;		
		GLManager glm = rr.getGLManager();
		GL2ES1 gl = glm.getGL();
		
		boolean lightingWasEnabled = gl.glIsEnabled(GL2ES1.GL_LIGHTING);
		//boolean depthWasEnabled = gl.glIsEnabled(GL2ES1.GL_DEPTH_TEST);
		
		gl.glEnable(GL2ES1.GL_LIGHTING);
		//gl.glEnable(GL2ES1.GL_DEPTH_TEST);		
		
		gl.glPushMatrix();
		glm.translate(x+w/2, y+h/2);
		gl.glRotatef((float)rotZ, 0, 0, 1);
		gl.glRotatef((float)rotY, 0, 1, 0);
		gl.glRotatef((float)rotX, 1, 0, 0);
		rr.renderQuad(glm, itex, -w/2, -h/2, w, h, null, 0, 0, 1, 1);
		gl.glPopMatrix();
				
		if (!lightingWasEnabled) gl.glDisable(GL2ES1.GL_LIGHTING);
		//if (!depthWasEnabled) gl.glDisable(GL2ES1.GL_DEPTH_TEST);
	}
	
	//Getters
	
	//Setters
	
}
