package nl.weeaboo.vn.impl.nvlist;

import java.util.Arrays;

import javax.media.opengl.GL2ES1;

import nl.weeaboo.common.Rect;
import nl.weeaboo.common.Rect2D;
import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.text.ParagraphRenderer;
import nl.weeaboo.gl.texture.GLTexRect;
import nl.weeaboo.gl.texture.GLTexture;
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

public class Renderer extends BaseRenderer {

	private final GLManager glm;
	private final ParagraphRenderer pr;
	
	private transient BaseRenderCommand[] tempArray;
	
	public Renderer(GLManager glm, ParagraphRenderer pr, int w, int h,
			int rx, int ry, int rw, int rh, int sw, int sh)
	{
		super(w, h, rx, ry, rw, rh, sw, sh);
		
		this.glm = glm;
		this.pr = pr;
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
			ITexture tex, double x, double y, double w, double h, IPixelShader ps,
			int dir, boolean fadeIn, double span, double time)
	{
		draw(new FadeQuadCommand(z, clipEnabled, blendMode, argb, tex, x, y, w, h, ps,
				dir, fadeIn, span, time));
	}
	
	@Override
	public void render(Rect2D bounds) {
		if (commands.isEmpty()) {
			return;
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
		Arrays.sort(tempArray, 0, len);
				
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
			cx = rx + Math.max(0, Math.min(rw, (int)Math.round(bounds.x * getScale())));
			cy = ry + Math.max(0, Math.min(rh, (int)Math.round(bounds.y * getScale())));
			cw = Math.max(0, Math.min(rw, (int)Math.round(bounds.w * getScale())));
			ch = Math.max(0, Math.min(rh, (int)Math.round(bounds.h * getScale())));
		}
		gl.glScissor(cx, sh-cy-ch, cw, ch);
				
		//Setup blend mode
		BlendMode blendMode = BlendMode.DEFAULT;
		gl.glBlendFunc(GL2ES1.GL_SRC_ALPHA, GL2ES1.GL_ONE_MINUS_SRC_ALPHA);
		
		//Setup color
		int foreground = 0xFFFFFFFF;
		glm.pushColor();
		glm.setColor(foreground);
		
		//Render buffered commands
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
				case DEFAULT: gl.glBlendFunc(GL2ES1.GL_SRC_ALPHA, GL2ES1.GL_ONE_MINUS_SRC_ALPHA); break;
				case ADD: gl.glBlendFunc(GL2ES1.GL_SRC_ALPHA, GL2ES1.GL_ONE); break;
				}
			}
			
			//Foreground color changed
			if (cmd.argb != foreground) {
				foreground = cmd.argb;
				glm.setColor(foreground);
			}
			
			//Don't render fully transparent objects
			if (((glm.getColor()>>24)&0xFF) == 0) {
				continue;
			}
			
			if (cmd.id == QuadRenderCommand.id) {
				QuadRenderCommand qrc = (QuadRenderCommand)cmd;
				renderQuad(glm, qrc.tex, qrc.x, qrc.y, qrc.width, qrc.height, qrc.ps,
						qrc.u, qrc.v, qrc.uw, qrc.vh);
			} else if (cmd.id == ScreenshotRenderCommand.id) {
				ScreenshotRenderCommand src = (ScreenshotRenderCommand)cmd;
				Screenshot ss = (Screenshot)src.ss;
				
				nl.weeaboo.gl.capture.Screenshot gss = new nl.weeaboo.gl.capture.Screenshot();
				gss.set(gl, new Rect(rx, ry, rw, rh));
				
				ss.set(BufferUtil.toArray(gss.getARGB()), gss.getWidth(), gss.getHeight(), rw, rh);
			} else if (cmd.id == RenderTextCommand.id) {
				RenderTextCommand rtc = (RenderTextCommand)cmd;

				IPixelShader ps = rtc.ps;				
				if (ps != null) ps.preDraw(this);
				
				gl.glPushMatrix();
				glm.translate(rtc.x, rtc.y);
				glm.translate(0, -rtc.textLayout.getLineTop(rtc.lineStart));
				
				pr.setVisibleChars(rtc.visibleChars);
				pr.setLineOffset(rtc.lineStart);
				pr.setVisibleLines(rtc.lineEnd - rtc.lineStart);
				pr.drawLayout(glm, rtc.textLayout);
				gl.glPopMatrix();
				
				if (ps != null) ps.postDraw(this);
			} else if (cmd.id == CustomRenderCommand.id) {
				CustomRenderCommand crc = (CustomRenderCommand)cmd;
				crc.render(this);
			} else {
				throw new RuntimeException("Unsupported command type: " + cmd.id);
			}
		}
		
		glm.popColor();
		gl.glBlendFunc(GL2ES1.GL_SRC_ALPHA, GL2ES1.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDisable(GL2ES1.GL_SCISSOR_TEST);
		gl.glPopMatrix();
		
		Arrays.fill(tempArray, 0, len, null);
	}
	
	void renderQuad(GLManager glm, ITexture itex, double x, double y, double w, double h,
			IPixelShader ps, double u, double v, double uw, double vh)
	{
		if (ps != null) ps.preDraw(this);
		
		if (itex == null) {
			glm.setTexture(null);
			glm.fillRect(x, y, w, h, 0, 0, 1, 1);
		} else {
			GLTexRect tr = ((TextureAdapter)itex).getTexRect();
			GLTexture tex = tr.getTexture();
			tex.forceLoad(glm);
			glm.setTexture(tex);
			
			int tw = tex.getTexWidth();
			int th = tex.getTexHeight();
			u  = (tr.getX() + u * tr.getWidth()) / tw;
			v  = (tr.getY() + v * tr.getHeight()) / th;
			uw = (uw * tr.getWidth()) / tw;
			vh = (vh * tr.getHeight()) / th;

			//System.out.printf("%.2f, %.2f -> %.2f, %.2f\n", u, v, uw, vh);
			glm.fillRect(x, y, w, h, u, v, uw, vh);
		}
		
		if (ps != null) ps.postDraw(this);		
	}
	
	//Getters
	@Override
	public double getScale() {
		return getScale(getWidth(), getHeight(), getRealWidth(), getRealHeight());
	}
	
	public GLManager getGLManager() {
		return glm;
	}
	
	protected static double getScale(int vwidth, int vheight, int rwidth, int rheight) {
		return Math.min(rwidth/(double)vwidth, rheight/(double)vheight);		
	}
		
	//Setters
	
}
