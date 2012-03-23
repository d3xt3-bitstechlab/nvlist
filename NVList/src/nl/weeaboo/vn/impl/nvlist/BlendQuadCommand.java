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

import javax.media.opengl.GL2ES1;

import nl.weeaboo.common.Rect2D;
import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.texture.GLTexture;
import nl.weeaboo.vn.BlendMode;
import nl.weeaboo.vn.IPixelShader;
import nl.weeaboo.vn.IRenderer;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.CustomRenderCommand;
import nl.weeaboo.vn.impl.base.LayoutUtil;
import nl.weeaboo.vn.impl.base.TriangleGrid;
import nl.weeaboo.vn.impl.base.TriangleGrid.TextureWrap;
import nl.weeaboo.vn.math.Matrix;

public class BlendQuadCommand extends CustomRenderCommand {

	public static String[] REQUIRED_EXTENSIONS = new String[] {
		"GL_ARB_texture_env_crossbar",
		"GL_ARB_texture_env_combine"
	};
	
	private final ITexture itex0;
	private final double alignX0, alignY0;
	private final ITexture itex1;
	private final double alignX1, alignY1;
	private final double frac;
	private final Matrix transform;
	
	public BlendQuadCommand(short z, boolean clipEnabled, BlendMode blendMode, int argb,
		ITexture tex0, double alignX0, double alignY0,
		ITexture tex1, double alignX1, double alignY1,
		double frac, Matrix transform, IPixelShader ps)
	{
		super(z, clipEnabled, blendMode, argb, ps, tex0 != null ? (byte)tex0.hashCode() : 0);
		
		this.itex0 = tex0;
		this.alignX0 = alignX0;
		this.alignY0 = alignY0;
		this.itex1 = tex1;
		this.alignX1 = alignX1;
		this.alignY1 = alignY1;
		this.frac = frac;
		this.transform = transform;
		
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
		
		TextureAdapter ta0 = (TextureAdapter)itex0;
		ta0.forceLoad(glm);
		GLTexture tex0 = ta0.getTexture();
		Rect2D bounds0 = LayoutUtil.getBounds(itex0, alignX0, alignY0);
		Rect2D texBounds0 = ta0.getUV();
		
		TextureAdapter ta1 = (TextureAdapter)itex1;
		ta1.forceLoad(glm);
		GLTexture tex1 = ta1.getTexture();
		Rect2D bounds1 = LayoutUtil.getBounds(itex1, alignX1, alignY1);
		Rect2D texBounds1 = ta1.getUV();
		
		//Check if all extensions are available
		boolean extensionsAvailable = true;
		for (String ext : REQUIRED_EXTENSIONS) {
			if (!gl.isExtensionAvailable(ext)) {
				extensionsAvailable = false;
				break;
			}
		}
		
		gl.glPushMatrix();
		gl.glMultMatrixf(transform.toGLMatrix(), 0);		
		if (f <= 0 || f >= 1 || tex0 == null || tex1 == null || !extensionsAvailable) {
			//Fallback for OpenGL < 1.4 or when some textures can't/needn't be drawn
			if (tex0 != null && f > 0) {
				glm.setTexture(tex0);
				glm.pushColor();
				glm.mixColor(1, 1, 1, f);
				glm.fillRect(bounds0.x, bounds0.y, bounds0.w, bounds0.h,
						texBounds0.x, texBounds0.y, texBounds0.w, texBounds0.h);
				glm.popColor();
			}

			if (tex1 != null && f < 1) {
				glm.setTexture(tex1);
				glm.pushColor();
				glm.mixColor(1, 1, 1, 1-f);
				glm.fillRect(bounds1.x, bounds1.y, bounds1.w, bounds1.h,
						texBounds1.x, texBounds1.y, texBounds1.w, texBounds1.h);
				glm.popColor();
			}
		} else {		
			//Set texture 0		
			gl.glActiveTexture(GL_TEXTURE0);
			gl.glEnable(GL_TEXTURE_2D);
			gl.glBindTexture(GL_TEXTURE_2D, tex0.getTexId());
			
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
			
			//Render triangle grid
			TriangleGrid grid = TriangleGrid.layout2(
					bounds0, texBounds0, TextureWrap.CLAMP,
					bounds1, texBounds1, TextureWrap.CLAMP);			
			rr.renderTriangleGrid(grid);
			
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
	    gl.glPopMatrix();
	}
	
	//Getters
	
	//Setters
	
}
