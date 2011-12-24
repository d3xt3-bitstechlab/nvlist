package nl.weeaboo.vn.impl.nvlist;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL2ES1;

import nl.weeaboo.awt.ImageUtil;
import nl.weeaboo.common.Rect2D;
import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.texture.GLTexture;
import nl.weeaboo.vn.BlendMode;
import nl.weeaboo.vn.IPixelShader;
import nl.weeaboo.vn.IRenderer;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.CustomRenderCommand;
import nl.weeaboo.vn.math.Matrix;

public class FadeQuadCommand extends CustomRenderCommand {

	private static Geometry geometry;

	private Matrix transform;
	private double x, y, w, h;
	private ITexture itex;
	private int dir;
	private boolean fadeIn;
	private double span;
	private double frac;
	
	protected FadeQuadCommand(short z, boolean clipEnabled, BlendMode blendMode,
		int argb, ITexture tex, Matrix trans, double x, double y, double w, double h,
		IPixelShader ps, int dir, boolean fadeIn, double span, double frac)
	{
		super(z, clipEnabled, blendMode, argb, ps, (byte)tex.hashCode());
		
		this.transform = trans;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.itex = tex;
		this.dir = dir;
		this.fadeIn = fadeIn;
		this.span = span;
		this.frac = frac;
	}

	//Functions
	@Override
	protected void renderGeometry(IRenderer r) {
		Renderer rr = (Renderer)r;
		GLManager glm = rr.getGLManager();
		GL2ES1 gl = glm.getGL();
		
		//Set texture
		GLTexture oldtex = glm.getTexture();
		
		TextureAdapter ta = (TextureAdapter)itex;
		ta.forceLoad(glm);		
		Rect2D uv = ta.getUV();
		glm.setTexture(ta.getTexture());
		
		//Draw geometry		
		double a, b;
		frac = frac * (1.0 + span) - span; //Stretch frac to (-span, 1)
		if (dir == 2 || dir == 6) {
			a = frac;
		} else {
			a = 1.0 - frac - span;
		}
		b = a + span;
		
		int c0 = argb;
		int c1 = argb & 0xFFFFFF;
		if (!fadeIn ^ (dir == 8 || dir == 4)) {
			int temp = c0;
			c0 = c1;
			c1 = temp;
		}
		
		gl.glPushMatrix();
		gl.glMultMatrixf(transform.toGLMatrix(), 0);
		if (geometry == null) {
			geometry = new Geometry();
		}		
		geometry.set(uv, dir == 4 || dir == 6, (float)a, (float)b,
				(float)x, (float)y, (float)w, (float)h, c0, c1);
		geometry.draw(gl);
		gl.glPopMatrix();
		
		//Reset state
		glm.setTexture(oldtex);
	}
	
	//Getters
	
	//Setters
	
	//Inner Classes
	private static class Geometry {
		
	    private FloatBuffer vs;
	    private FloatBuffer ts;
	    private IntBuffer cs;
	    private ByteBuffer is;
		
	    public Geometry() {
	    	ByteBuffer vertsB = ByteBuffer.allocateDirect(8 * 2 * 4);
	    	vertsB.order(ByteOrder.nativeOrder());
	        vs = vertsB.asFloatBuffer();

	    	ByteBuffer texCoordsB = ByteBuffer.allocateDirect(8 * 2 * 4);
	    	texCoordsB.order(ByteOrder.nativeOrder());
	        ts = texCoordsB.asFloatBuffer();

	    	ByteBuffer colorsB = ByteBuffer.allocateDirect(8 * 1 * 4);
	    	colorsB.order(ByteOrder.nativeOrder());
	        cs = colorsB.asIntBuffer();

	        ByteBuffer indexBufB = ByteBuffer.allocateDirect(8 * 2 * 1);
	        indexBufB.order(ByteOrder.nativeOrder());
	        is = indexBufB;
			for (int n = 0; n < 8; n++) {
				is.put((byte)n);
			}
	        is.rewind();
	    }
	    
	    public void draw(GL2ES1 gl) {
	        gl.glEnableClientState(GL2ES1.GL_VERTEX_ARRAY);
	        gl.glEnableClientState(GL2ES1.GL_TEXTURE_COORD_ARRAY);		
	        gl.glEnableClientState(GL2ES1.GL_COLOR_ARRAY);
	        gl.glVertexPointer(2, GL2ES1.GL_FLOAT, 0, vs);
	        gl.glTexCoordPointer(2, GL2ES1.GL_FLOAT, 0, ts);
	        gl.glColorPointer(4, GL2ES1.GL_UNSIGNED_BYTE, 0, cs);
	        gl.glDrawElements(GL2ES1.GL_TRIANGLE_STRIP, 8, GL2ES1.GL_UNSIGNED_BYTE, is);
	        gl.glDisableClientState(GL2ES1.GL_VERTEX_ARRAY);	        
	        gl.glDisableClientState(GL2ES1.GL_TEXTURE_COORD_ARRAY);	        
	        gl.glDisableClientState(GL2ES1.GL_COLOR_ARRAY);	        
	    }
	    
	    public void set(Rect2D uv, boolean horizontal, float start, float end,
	    		float x, float y, float w, float h, int c0, int c1)
	    {
	    	start = Math.max(0f, Math.min(1f, start));
	    	end = Math.max(0f, Math.min(1f, end));
	    	
    		float x0 = x;
    		float x1 = x + w;	    		
    		float y0 = y;
    		float y1 = y + h;

	    	float u0 = (float)(uv.x);
	    	float v0 = (float)(uv.y);
	    	float u1 = (float)(uv.x+uv.w);
	    	float v1 = (float)(uv.y+uv.h);
    		
	    	float uva, uvb, posa, posb;
	    	if (horizontal) {
	    		uva = (float)(uv.x + start * uv.w);
	    		uvb = (float)(uv.x + end * uv.w);
	    		posa = x + start * w;
	    		posb = x + end * w;
	    	} else {
	    		uva = (float)(uv.y + start * uv.h);
	    		uvb = (float)(uv.y + end * uv.h);
	    		posa = y + start * h;
	    		posb = y + end * h;
	    	}
	    	
    		//System.out.printf("(%d, %d, %d, %d) (%.1f, %.1f, %.1f, %.1f)\n", ix0, iposa, iposb, ix1, u0, uva, uvb, u1);
	    	
	    	//Colors must be in ABGR for OpenGL
	    	c0 = ImageUtil.premultiplyAlpha(c0);
	    	c0 = (c0&0xFF000000) | ((c0<<16)&0xFF0000) | (c0&0xFF00) | ((c0>>16)&0xFF);
	    	c1 = ImageUtil.premultiplyAlpha(c1);
	    	c1 = (c1&0xFF000000) | ((c1<<16)&0xFF0000) | (c1&0xFF00) | ((c1>>16)&0xFF);
	    	
	    	if (horizontal) {
	    		vs.put(x0);   vs.put(y0);   ts.put(u0);   ts.put(v0);   cs.put(c0);
	    		vs.put(x0);   vs.put(y1);   ts.put(u0);   ts.put(v1);   cs.put(c0);
	    		vs.put(posa); vs.put(y0);   ts.put(uva);  ts.put(v0);   cs.put(c0);
	    		vs.put(posa); vs.put(y1);   ts.put(uva);  ts.put(v1);   cs.put(c0);	    		
	    		vs.put(posb); vs.put(y0);   ts.put(uvb);  ts.put(v0);   cs.put(c1);
	    		vs.put(posb); vs.put(y1);   ts.put(uvb);  ts.put(v1);   cs.put(c1);
	    		vs.put(x1);   vs.put(y0);   ts.put(u1);   ts.put(v0);   cs.put(c1);
	    		vs.put(x1);   vs.put(y1);   ts.put(u1);   ts.put(v1);   cs.put(c1);
	    	} else {
	    		//System.out.printf("(%d, %d, %d, %d) (%.1f, %.1f, %.1f, %.1f)\n", iy0, iposa, iposb, iy1, v0, uva, uvb, v1);
	    		
	    		vs.put(x0);   vs.put(y0);   ts.put(u0);   ts.put(v0);   cs.put(c0);
	    		vs.put(x1);   vs.put(y0);   ts.put(u1);   ts.put(v0);   cs.put(c0);
	    		vs.put(x0);   vs.put(posa); ts.put(u0);   ts.put(uva);  cs.put(c0);
	    		vs.put(x1);   vs.put(posa); ts.put(u1);   ts.put(uva);  cs.put(c0);
	    		vs.put(x0);   vs.put(posb); ts.put(u0);   ts.put(uvb);  cs.put(c1);
	    		vs.put(x1);   vs.put(posb); ts.put(u1);   ts.put(uvb);  cs.put(c1);
	    		vs.put(x0);   vs.put(y1);   ts.put(u0);   ts.put(v1);   cs.put(c1);
	    		vs.put(x1);   vs.put(y1);   ts.put(u1);   ts.put(v1);   cs.put(c1);
	    	}
	    	vs.rewind();
	    	ts.rewind();
	    	cs.rewind();
	    }	    
	}
	
}
