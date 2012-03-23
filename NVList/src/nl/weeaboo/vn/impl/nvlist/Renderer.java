package nl.weeaboo.vn.impl.nvlist;

import static javax.media.opengl.GL.GL_FLOAT;
import static javax.media.opengl.GL.GL_TEXTURE0;
import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;
import static javax.media.opengl.fixedfunc.GLPointerFunc.GL_TEXTURE_COORD_ARRAY;
import static javax.media.opengl.fixedfunc.GLPointerFunc.GL_VERTEX_ARRAY;

import java.util.Arrays;

import javax.media.opengl.GL2ES1;

import nl.weeaboo.collections.MergeSort;
import nl.weeaboo.common.Rect;
import nl.weeaboo.common.Rect2D;
import nl.weeaboo.gl.GLBlendMode;
import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.capture.GLScreenshot;
import nl.weeaboo.gl.text.ParagraphRenderer;
import nl.weeaboo.io.BufferUtil;
import nl.weeaboo.textlayout.TextLayout;
import nl.weeaboo.vn.BlendMode;
import nl.weeaboo.vn.IPixelShader;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.BaseRenderCommand;
import nl.weeaboo.vn.impl.base.BaseRenderer;
import nl.weeaboo.vn.impl.base.CustomRenderCommand;
import nl.weeaboo.vn.impl.base.QuadRenderCommand;
import nl.weeaboo.vn.impl.base.ScreenshotRenderCommand;
import nl.weeaboo.vn.impl.base.TriangleGrid;
import nl.weeaboo.vn.math.Matrix;

import com.jogamp.common.nio.Buffers;

public class Renderer extends BaseRenderer {

	private final GLManager glm;
	private final ParagraphRenderer pr;
	private final RenderStats renderStats;
	
	private transient BaseRenderCommand[] tempArray;
	
	public Renderer(GLManager glm, ParagraphRenderer pr, int w, int h,
			int rx, int ry, int rw, int rh, int sw, int sh)
	{
		super(w, h, rx, ry, rw, rh, sw, sh);
		
		this.glm = glm;
		this.pr = pr;
		this.renderStats = null; //new RenderStats();
	}
	
	//Functions
	public void drawText(short z, boolean clipEnabled, BlendMode blendMode, int argb,
			TextLayout textLayout, int lineStart, int lineEnd, double visibleChars,
			double x, double y, IPixelShader ps)
	{		
		draw(new RenderTextCommand(z, clipEnabled, blendMode, argb, textLayout,
				lineStart, lineEnd, visibleChars, x, y, ps));
	}
	
	@Override
	public void drawFadeQuad(short z, boolean clipEnabled, BlendMode blendMode, int argb,
			ITexture tex, Matrix trans, double x, double y, double w, double h, IPixelShader ps,
			int dir, boolean fadeIn, double span, double time)
	{
		draw(new FadeQuadCommand(z, clipEnabled, blendMode, argb, tex,
				trans, x, y, w, h, ps, dir, fadeIn, span, time));
	}
	
	@Override
	public void drawBlendQuad(short z, boolean clipEnabled, BlendMode blendMode, int argb,
			ITexture tex0, double alignX0, double alignY0,
			ITexture tex1, double alignX1, double alignY1,
			double frac, Matrix trans, IPixelShader ps)
	{
		draw(new BlendQuadCommand(z, clipEnabled, blendMode, argb,
				tex0, alignX0, alignY0,
				tex1, alignX1, alignY1,
				frac, trans, ps));
	}
	
	public void onFrameRenderDone() {
		if (renderStats != null) {
			renderStats.onFrameRenderDone();
		}
	}		
	
	@Override
	public void render(Rect2D bounds) {
		if (commands.isEmpty()) {
			return;
		}
		
		if (renderStats != null) {
			renderStats.startRender();
		}
		
		final int rx = getRealX();
		final int ry = getRealY();
		final int rw = getRealWidth();
		final int rh = getRealHeight();
		//final int sw = getScreenWidth();
		final int sh = getScreenHeight();
						
		if (tempArray == null) {
			tempArray = new BaseRenderCommand[commands.size()];
		}
		tempArray = commands.toArray(tempArray);
		final int len = commands.size();
		
		// Merge sort is only faster than Arrays.sort() for small (up to ~1000
		// elements) arrays or when the input is nearly sorted. Since both of
		// these are typically the case...
		MergeSort.sort(tempArray, 0, len);

		GL2ES1 gl = glm.getGL();
		gl.glPushMatrix();
		if (bounds != null) {
			glm.translate(bounds.x, bounds.y);
		}

		//Setup clipping
		boolean clipping = true;
		gl.glEnable(GL2ES1.GL_SCISSOR_TEST);

		final int cx, cy, cw, ch; //Clip rect in screen coords
		if (bounds == null) {
			cx = rx; cy = ry; cw = rw; ch = rh;
		} else {
			cw = Math.max(0, Math.min(rw, (int)Math.floor(bounds.w * getScale())));
			ch = Math.max(0, Math.min(rh, (int)Math.floor(bounds.h * getScale())));			
			cx = rx + Math.max(0, Math.min(rw, (int)Math.ceil(bounds.x * getScale())));
			int ucy = ry + Math.max(0, Math.min(rh, (int)Math.ceil(bounds.y * getScale())));
			cy = sh - ucy - ch;
		}
		gl.glScissor(cx, cy, cw, ch);
		
		//Setup blend mode
		BlendMode blendMode = BlendMode.DEFAULT;
		glm.setBlendMode(GLBlendMode.DEFAULT);

		//Setup color
		int foreground = 0xFFFFFFFF;
		glm.pushColor();
		glm.setColor(foreground);
		
		//Render buffered commands
		long renderStatsTimestamp = 0;
		for (int n = 0; n < len; n++) {
			BaseRenderCommand cmd = tempArray[n];			
			
			//Clipping changed
			if (cmd.clipEnabled != clipping) {
				if (cmd.clipEnabled) {
					gl.glEnable(GL2ES1.GL_SCISSOR_TEST);
				} else {
					gl.glDisable(GL2ES1.GL_SCISSOR_TEST);
				}
				clipping = cmd.clipEnabled;
			}
			
			//Blend mode changed
			if (cmd.blendMode != blendMode) {
				blendMode = cmd.blendMode;
				
				switch (blendMode) {
				case DEFAULT: glm.setBlendMode(GLBlendMode.DEFAULT); break;
				case ADD:     glm.setBlendMode(GLBlendMode.ADD); break;
				case OPAQUE:  glm.setBlendMode(null); break;
				}
			}
			
			//Foreground color changed
			if (cmd.argb != foreground) {
				foreground = cmd.argb;
				glm.setColor(foreground);
			}
			
			//Don't render fully transparent objects
			if (((foreground>>24)&0xFF) == 0) {
				continue;
			}
			
			//Set pre-command execute timestamp
			if (renderStats != null) {
				renderStatsTimestamp = System.nanoTime();
			}
			
			//Handle commands
			if (cmd.id == QuadRenderCommand.id) {
				QuadRenderCommand qrc = (QuadRenderCommand)cmd;
				renderQuad(glm, qrc.tex, qrc.transform,
						qrc.x, qrc.y, qrc.width, qrc.height, qrc.ps,
						qrc.u, qrc.v, qrc.uw, qrc.vh);
			} else if (cmd.id == ScreenshotRenderCommand.id) {
				ScreenshotRenderCommand src = (ScreenshotRenderCommand)cmd;
				Screenshot ss = (Screenshot)src.ss;
				
				GLScreenshot gss = new GLScreenshot();
				gss.set(glm, new Rect(cx, cy, cw, ch));
				
				ss.set(BufferUtil.toArray(gss.getARGB()), gss.getWidth(), gss.getHeight(), rw, rh);
			} else if (cmd.id == RenderTextCommand.id) {
				RenderTextCommand rtc = (RenderTextCommand)cmd;
				renderText(glm, rtc.textLayout, rtc.x, rtc.y - rtc.textLayout.getLineTop(rtc.lineStart),
						rtc.lineStart, rtc.lineEnd, rtc.visibleChars, rtc.ps);
			} else if (cmd.id == CustomRenderCommand.id) {
				CustomRenderCommand crc = (CustomRenderCommand)cmd;
				crc.render(this);
			} else {
				throw new RuntimeException("Unsupported command type: " + cmd.id);
			}
			
			if (renderStats != null) {
				renderStats.log(cmd, System.nanoTime()-renderStatsTimestamp);
			}
		}
		
		glm.popColor();
		glm.setBlendMode(GLBlendMode.DEFAULT);
		gl.glDisable(GL2ES1.GL_SCISSOR_TEST);
		gl.glPopMatrix();
		
		Arrays.fill(tempArray, 0, len, null); //Null array to allow garbage collection
		
		if (renderStats != null) {
			renderStats.stopRender();
		}		
	}
	
	void renderQuad(GLManager glm, ITexture itex, Matrix t,
			double x, double y, double w, double h,
			IPixelShader ps, double u, double v, double uw, double vh)
	{
		if (itex == null) {
			glm.setTexture(null);
		} else {
			TextureAdapter ta = (TextureAdapter)itex;
			ta.forceLoad(glm);
			if (ta.getTexId() != 0) {
				glm.setTexture(ta.getTexture());
				Rect2D uv = ta.getUV();
				u  = uv.x + u * uv.w;
				v  = uv.y + v * uv.h;
				uw = uv.w * uw;
				vh = uv.h * vh;
			} else {
				glm.setTexture(null);
			}
		}

		renderQuad(glm, t, x, y, w, h, ps, u, v, uw, vh);
	}
	
	void renderQuad(GLManager glm, Matrix t, double x, double y, double w, double h,
			IPixelShader ps, double u, double v, double uw, double vh)
	{
		if (ps != null) ps.preDraw(this);
				
		GL2ES1 gl = glm.getGL();		
		if (t.hasShear()) {
			gl.glPushMatrix();		
			gl.glMultMatrixf(t.toGLMatrix(), 0);
			glm.fillRect(x, y, w, h, u, v, uw, vh);
			gl.glPopMatrix();
		} else {
			double sx = t.getScaleX();
			double sy = t.getScaleY();
			x = x * sx + t.getTranslationX();
			y = y * sy + t.getTranslationY();
			w = w * sx;
			h = h * sy;
			glm.fillRect(x, y, w, h, u, v, uw, vh);
			//System.out.printf("%.2f, %.2f, %.2f, %.2f\n", x, y, w, h);
		}		
		
		if (ps != null) ps.postDraw(this);		
	}
	
	void renderText(GLManager glm, TextLayout layout, double x, double y,
			int lineStart, int lineEnd, double visibleChars, IPixelShader ps)
	{
		if (ps != null) ps.preDraw(this);
		
		//GL2ES1 gl = glm.getGL();		
		//gl.glPushMatrix();
		glm.translate(x, y);
		
		pr.setLineOffset(lineStart);
		pr.setVisibleLines(lineEnd - lineStart);
		pr.setVisibleChars(visibleChars);
		pr.drawLayout(glm, layout);

		glm.translate(-x, -y);
		//gl.glPopMatrix();
		
		if (ps != null) ps.postDraw(this);		
	}
	
	@Override
	public void renderTriangleGrid(TriangleGrid grid) {
		GL2ES1 gl = glm.getGL();
		gl.glEnableClientState(GL_VERTEX_ARRAY);
		for (int n = 0; n < grid.getTextures(); n++) {
			gl.glClientActiveTexture(GL_TEXTURE0 + n);
			gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		}
		for (int row = 0; row < grid.getRows(); row++) {
			gl.glVertexPointer(2, GL_FLOAT, 0, Buffers.copyFloatBuffer(grid.getPos(row)));
			for (int n = 0; n < grid.getTextures(); n++) {
				gl.glClientActiveTexture(GL_TEXTURE0 + n);
			    gl.glTexCoordPointer(2, GL_FLOAT, 0, Buffers.copyFloatBuffer(grid.getTex(n, row)));
			}
		    gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, grid.getVertexCount(row));
		}
		for (int n = grid.getTextures()-1; n >= 0; n--) {
			gl.glClientActiveTexture(GL_TEXTURE0 + n);
			gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		}
	    gl.glDisableClientState(GL2ES1.GL_VERTEX_ARRAY);		
	}
	
	//Getters	
	public GLManager getGLManager() {
		return glm;
	}
		
	//Setters
	
}
