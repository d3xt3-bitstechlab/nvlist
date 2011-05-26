package nl.weeaboo.vn.impl.nvlist;

import static javax.media.opengl.GL.GL_SRC_ALPHA;
import static javax.media.opengl.GL.GL_SRC_COLOR;
import static javax.media.opengl.GL.GL_TEXTURE0;
import static javax.media.opengl.GL.GL_TEXTURE1;
import static javax.media.opengl.GL.GL_TEXTURE_2D;
import static javax.media.opengl.GL2ES1.GL_COMBINE;
import static javax.media.opengl.GL2ES1.GL_COMBINE_ALPHA;
import static javax.media.opengl.GL2ES1.GL_COMBINE_RGB;
import static javax.media.opengl.GL2ES1.GL_CONSTANT;
import static javax.media.opengl.GL2ES1.GL_INTERPOLATE;
import static javax.media.opengl.GL2ES1.GL_MODULATE;
import static javax.media.opengl.GL2ES1.GL_OPERAND0_ALPHA;
import static javax.media.opengl.GL2ES1.GL_OPERAND0_RGB;
import static javax.media.opengl.GL2ES1.GL_OPERAND1_ALPHA;
import static javax.media.opengl.GL2ES1.GL_OPERAND1_RGB;
import static javax.media.opengl.GL2ES1.GL_OPERAND2_ALPHA;
import static javax.media.opengl.GL2ES1.GL_OPERAND2_RGB;
import static javax.media.opengl.GL2ES1.GL_PREVIOUS;
import static javax.media.opengl.GL2ES1.GL_PRIMARY_COLOR;
import static javax.media.opengl.GL2ES1.GL_SRC0_ALPHA;
import static javax.media.opengl.GL2ES1.GL_SRC0_RGB;
import static javax.media.opengl.GL2ES1.GL_SRC1_ALPHA;
import static javax.media.opengl.GL2ES1.GL_SRC1_RGB;
import static javax.media.opengl.GL2ES1.GL_SRC2_ALPHA;
import static javax.media.opengl.GL2ES1.GL_SRC2_RGB;
import static javax.media.opengl.GL2ES1.GL_TEXTURE_ENV;
import static javax.media.opengl.GL2ES1.GL_TEXTURE_ENV_COLOR;
import static javax.media.opengl.GL2ES1.GL_TEXTURE_ENV_MODE;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;

import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.GLUtil;
import nl.weeaboo.gl.texture.GLTexRect;
import nl.weeaboo.gl.texture.GLTexture;
import nl.weeaboo.vn.BlendMode;
import nl.weeaboo.vn.IPixelShader;
import nl.weeaboo.vn.IRenderer;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.CustomRenderCommand;
import nl.weeaboo.vn.math.Matrix;

import com.sun.opengl.util.BufferUtil;

public class BlendQuadCommand extends CustomRenderCommand {

	private final ITexture tex0, tex1;
	private final double frac;
	private final Matrix transform;
	private final double x, y, w, h;
	
	public BlendQuadCommand(short z, boolean clipEnabled, BlendMode blendMode, int argb,
		ITexture tex0, ITexture tex1, double frac, Matrix transform,
		double x, double y, double w, double h, IPixelShader ps)
	{
		super(z, clipEnabled, blendMode, argb, ps, (byte)tex0.hashCode());
		
		this.tex0 = tex0;
		this.tex1 = tex1;
		this.frac = frac;
		this.transform = transform;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		
		if (tex0 == null || tex1 == null) {
			throw new IllegalArgumentException("Crossfade texture args may not be null");
		}
	}

	//Functions
	@Override
	protected void renderGeometry(IRenderer renderer) {
		Renderer rr = (Renderer)renderer;
		GLManager glm = rr.getGLManager();
		GL2ES1 gl = glm.getGL();
		float f = 1f - (float)frac;
		
		//Init textures
		GLTexture oldtex = glm.getTexture();
		
		GLTexRect tr0 = ((TextureAdapter)tex0).getTexRect();
		GLTexture tex0 = tr0.getTexture();
		tex0.forceLoad(glm);
				
		GLTexRect tr1 = ((TextureAdapter)tex1).getTexRect();
		GLTexture tex1 = tr1.getTexture();
		tex1.forceLoad(glm);
		
		float[][] uv = new float[][] {
			GLUtil.getUV(tex0.getTexWidth(), tex0.getTexHeight(), tr0.getRect()),		
			GLUtil.getUV(tex1.getTexWidth(), tex1.getTexHeight(), tr1.getRect())		
		};

		if (!gl.isExtensionAvailable("GL_ARB_texture_env_crossbar")
			|| !gl.isExtensionAvailable("GL_ARB_texture_env_combine"))
		{
			//Fallback for OpenGL < 1.4
			
			gl.glPushMatrix();
			gl.glMultMatrixf(transform.toGLMatrix(), 0);
			
			glm.setTexture(tex0);
			glm.pushColor();
			glm.mixColor(1, 1, 1, f);
			glm.fillRect(x, y, w, h, uv[0][0], uv[0][2], uv[0][1]-uv[0][0], uv[0][3]-uv[0][2]);
			glm.popColor();

			glm.setTexture(tex1);
			glm.pushColor();
			glm.mixColor(1, 1, 1, 1-f);
			glm.fillRect(x, y, w, h, uv[1][0], uv[1][2], uv[1][1]-uv[1][0], uv[1][3]-uv[1][2]);
			glm.popColor();

			glm.setTexture(tex0);
			gl.glPopMatrix();
			
			return;
		}
		
		//Set texture 0		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glEnable(GL_TEXTURE_2D);
		gl.glBindTexture(GL.GL_TEXTURE_2D, tex0.getTexId());
		
		gl.glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, new float[] {f, f, f, f}, 0);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);
		
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_INTERPOLATE);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_RGB, GL_TEXTURE0);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_SRC1_RGB, GL_TEXTURE1);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_SRC2_RGB, GL_CONSTANT);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, GL_SRC_COLOR);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND2_RGB, GL_SRC_COLOR);

		gl.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_INTERPOLATE);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_ALPHA, GL_TEXTURE0);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_SRC1_ALPHA, GL_TEXTURE1);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_SRC2_ALPHA, GL_CONSTANT);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, GL_SRC_ALPHA);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND2_ALPHA, GL_SRC_ALPHA);
		
		// Set texture 1
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glEnable(GL_TEXTURE_2D);
		gl.glBindTexture(GL_TEXTURE_2D, tex1.getTexId());
		
		gl.glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, new float[] {1, 1, 1, 1}, 0);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);
		
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_MODULATE);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_RGB, GL_PREVIOUS);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_SRC1_RGB, GL_PRIMARY_COLOR);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, GL_SRC_COLOR);

		gl.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_INTERPOLATE);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_ALPHA, GL_PREVIOUS);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_SRC1_ALPHA, GL_PRIMARY_COLOR);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, GL_SRC_ALPHA);
		
		gl.glPushMatrix();
		gl.glMultMatrixf(transform.toGLMatrix(), 0);
		
		
		
		gl.glEnableClientState(GL2ES1.GL_VERTEX_ARRAY);
	    gl.glVertexPointer(2, GL2ES1.GL_FLOAT, 0, BufferUtil.newFloatBuffer(new float[] {
	    		(float)x, (float)y, (float)(x+w), (float)y,
	    		(float)x, (float)(y+h), (float)(x+w), (float)(y+h)}));
	    
		for (int n = 0; n <= 1; n++) {			
			gl.glClientActiveTexture(GL2ES1.GL_TEXTURE0+n);
		    gl.glEnableClientState(GL2ES1.GL_TEXTURE_COORD_ARRAY);
		    gl.glTexCoordPointer(2, GL2ES1.GL_FLOAT, 0, BufferUtil.newFloatBuffer(new float[] {
		    	uv[n][0], uv[n][2], uv[n][1], uv[n][2],
		    	uv[n][0], uv[n][3], uv[n][1], uv[n][3]
		    }));
		}
		
	    gl.glDrawArrays(GL2ES1.GL_TRIANGLE_STRIP, 0, 4);
	    
		for (int n = 1; n >= 0; n--) {
			gl.glClientActiveTexture(GL2ES1.GL_TEXTURE0+n);
			gl.glDisableClientState(GL2ES1.GL_TEXTURE_COORD_ARRAY);
		}
	    gl.glDisableClientState(GL2ES1.GL_VERTEX_ARRAY);
	    
	    gl.glPopMatrix();
		
		//Reset texture
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		gl.glDisable(GL_TEXTURE_2D);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		if (oldtex != null) {
			gl.glBindTexture(GL_TEXTURE_2D, oldtex.getTexId());
		} else {
			gl.glDisable(GL_TEXTURE_2D);
		}
	}
	
	//Getters
	
	//Setters
	
}
