package nl.weeaboo.vn.impl.nvlist;

import static nl.weeaboo.vn.impl.base.Interpolators.BUTTERWORTH;
import static nl.weeaboo.vn.impl.base.Interpolators.getInterpolator;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import nl.weeaboo.common.Dim;
import nl.weeaboo.common.Rect;
import nl.weeaboo.common.ScaleUtil;
import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.GLUtil;
import nl.weeaboo.gl.shader.GLShader;
import nl.weeaboo.gl.texture.GLGeneratedTexture;
import nl.weeaboo.gl.texture.GLTexRect;
import nl.weeaboo.gl.texture.GLTexture;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.lua.platform.LuajavaLib;
import nl.weeaboo.vn.IImageDrawable;
import nl.weeaboo.vn.IInterpolator;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.IRenderer;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.BaseBitmapTween;
import nl.weeaboo.vn.impl.base.BaseRenderer;
import nl.weeaboo.vn.impl.base.CustomRenderCommand;
import nl.weeaboo.vn.math.Matrix;

import org.luaj.vm.LFunction;
import org.luaj.vm.LTable;
import org.luaj.vm.LuaErrorException;
import org.luaj.vm.LuaState;

@LuaSerializable
public class BitmapTween extends BaseBitmapTween {

	private static final long serialVersionUID = NVListImpl.serialVersionUID;
	
	private static final String requiredGlslVersion = "1.1";
	
	private final ImageFactory fac;
	
	//--- Initialized in prepare() ---
	private GLShader shader;
	private GLTexRect[] texs;
	private GLGeneratedTexture fadeTex;
	private GLGeneratedTexture remapTex;
	
	public BitmapTween(ImageFactory ifac, INotifier ntf, String fadeFilename, double duration,
			double range, IInterpolator i, boolean fadeTexLerp, boolean fadeTexTile)
	{	
		super(ntf, fadeFilename, duration, range, i, fadeTexLerp, fadeTexTile);
		
		this.fac = ifac;
	}
	
	//Functions
	public static void install(LTable globals, final ImageFactory ifac, final INotifier ntf) {
		LTable table = new LTable();
		BitmapTweenLib.install(table, ifac, ntf);
		globals.put("BitmapTween", table);
	}
				
	@Override
	protected void prepareShader() {
		shader = fac.getGLShader("bitmap-tween");
	}

	@Override
	protected void prepareTextures(ITexture[] itexs) {
		texs = new GLTexRect[itexs.length];
		for (int n = 0; n < itexs.length; n++) {
			if (itexs[n] instanceof TextureAdapter) {
				TextureAdapter ta = (TextureAdapter)itexs[n];
				texs[n] = ta.getTexRect();
			}
		}		
	}

	@Override
	protected void prepareFadeTexture(String filename, boolean scaleSmooth, Dim targetSize)
			throws IOException
	{
		BufferedImage src = fac.getBufferedImage(filename);			
		if (targetSize != null) {
			//Take the region of the image we want to use
			src = fadeImageSubRect(src, targetSize.w, targetSize.h);
		}
		
		//Get 16-bit grayscale pixels from image
		int[] argb = toGrayScale(src);

		fadeTex = fac.createGLTexture(argb, src.getWidth(), src.getHeight(),
				(scaleSmooth ? GL.GL_LINEAR : GL.GL_NEAREST),
				(scaleSmooth ? GL.GL_LINEAR : GL.GL_NEAREST),
				(targetSize == null ? GL.GL_REPEAT : GL.GL_CLAMP_TO_EDGE));		
	}

	protected BufferedImage fadeImageSubRect(BufferedImage img, int targetW, int targetH) {
		final int iw = img.getWidth();
		final int ih = img.getHeight();
		Dim d = ScaleUtil.scaleProp(targetW, targetH, iw, ih);
		return img.getSubimage((iw-d.w)/2, (ih-d.h)/2, d.w, d.h);
	}
	
	protected int[] toGrayScale(BufferedImage img) {
		int iw = img.getWidth();
		int ih = img.getHeight();
		BufferedImage temp = new BufferedImage(iw, ih, BufferedImage.TYPE_USHORT_GRAY);
		Graphics2D g = (Graphics2D)temp.getGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(img, 0, 0, null);
		g.dispose();

		DataBuffer dataBuffer = temp.getRaster().getDataBuffer();
		
		int data[] = new int[iw * ih];
		for (int n = 0; n < data.length; n++) {
			data[n] = dataBuffer.getElem(n);
		}
		return data;
	}
	
	@Override
	protected void prepareDefaultFadeTexture(int colorARGB) {
		int w = 16, h = 16;
		int[] argb = new int[w * h];
		Arrays.fill(argb, colorARGB);
		
		fadeTex = fac.createGLTexture(argb, w, h, GL.GL_NEAREST, GL.GL_NEAREST, GL.GL_CLAMP_TO_EDGE);		
	}

	@Override
	protected void prepareRemapTexture(int w, int h) {
		remapTex = fac.createGLTexture(null, w, h,
				GL.GL_NEAREST, GL.GL_NEAREST, GL.GL_CLAMP_TO_EDGE);
	}

	@Override
	protected float[] getOrigUV(int texIndex) {
		GLTexture tex;
		Rect crop;
		if (texIndex >= 0 && texIndex < texs.length) {
			GLTexRect tr = texs[texIndex];
			tex = tr.getTexture();
			crop = tr.getRect();
		} else if (texIndex == texs.length) {
			tex = fadeTex;
			crop = tex.getCrop();
		} else {
			throw new ArrayIndexOutOfBoundsException(texIndex);
		}
		return GLUtil.getUV(tex.getTexWidth(), tex.getTexHeight(), crop);
	}

	@Override
	protected Dim getSize(int texIndex) {
		if (texIndex >= 0 && texIndex < texs.length) {
			return new Dim(texs[texIndex].getWidth(), texs[texIndex].getHeight());
		} else if (texIndex == texs.length) {
			return new Dim(fadeTex.getCropWidth(), fadeTex.getCropHeight());
		} else {
			throw new ArrayIndexOutOfBoundsException(texIndex);
		}
	}

	@Override
	protected void updateRemapTex(int[] argb) {
		remapTex.setARGB(argb);
	}

	@Override
	protected void draw(BaseRenderer rr, IImageDrawable img, float[][] origUV, float[][] uv) {
		rr.draw(new RenderCommand(img, texs, origUV, uv, fadeTex, remapTex, shader));
	}
	
	//Getters
	
	//Setters
	
	//Inner Classes
	protected static final class RenderCommand extends CustomRenderCommand {

		private final Matrix transform;
		private final float x, y, w, h;
		private final GLTexRect[] texs;
		private final float[][] origUV;
		private final float[][] uv;
		private final GLTexture fadeTex;
		private final GLTexture remapTex;
		private final GLShader shader;
		
		public RenderCommand(IImageDrawable id, GLTexRect[] texs,
				float[][] origUV, float[][] uv,
				GLTexture fadeTex, GLTexture remapTex, GLShader shader)
		{
			super(id.getZ(), id.isClipEnabled(), id.getBlendMode(), id.getColor(),
					id.getPixelShader(), (byte)0);
			
			this.transform = id.getTransform();
			this.x = 0f;
			this.y = 0f;
			this.w = (float)id.getUnscaledWidth();
			this.h = (float)id.getUnscaledHeight();			
			this.texs = texs;
			this.origUV = origUV;
			this.uv = uv;
			this.fadeTex = fadeTex;
			this.remapTex = remapTex;
			this.shader = shader;
		}

		@Override
		protected void renderGeometry(IRenderer r) {
			Renderer rr = (Renderer)r;			
			GLManager glm = rr.getGLManager();
			GL2 gl2 = GLManager.getGL2(glm.getGL());
			
			gl2.glPushMatrix();
			gl2.glMultMatrixf(transform.toGLMatrix(), 0);
			
			GLTexture oldTexture = glm.getTexture();
			glm.setTexture(null);
			
			//Force load textures
			for (int n = 0; n < texs.length; n++) {
				GLTexture t = null;
				if (texs[n] != null) {
					t = texs[n].getTexture();
				}
				if (t != null) {
					t.forceLoad(glm);
				}
			}
			fadeTex.forceLoad(glm);
			remapTex.forceLoad(glm);
			
			//Force load shader
			shader.forceLoad(glm);
			GLShader oldShader = glm.getShader();
			glm.setShader(shader);
			
			//Initialize shader
			shader.setVec4Param(gl2, "crop0", origUV[0], 0);
			shader.setVec4Param(gl2, "crop1", origUV[1], 0);
			shader.setTextureParam(gl2, 0, "src0", texId(texs[0]));
			shader.setTextureParam(gl2, 1, "src1", texId(texs[1]));
			shader.setTextureParam(gl2, 2, "fade", texId(fadeTex));
			shader.setTextureParam(gl2, 3, "remap", texId(remapTex));
			
			//Render a quad
			gl2.glBegin(GL2.GL_QUADS);
			for (int n = 0; n <= 2; n++) {
				gl2.glMultiTexCoord2f(GL2.GL_TEXTURE0 + n, uv[n][0], uv[n][2]);
			}
			gl2.glVertex2f(x, y);
			for (int n = 0; n <= 2; n++) {
				gl2.glMultiTexCoord2f(GL2.GL_TEXTURE0 + n, uv[n][1], uv[n][2]);
			}
			gl2.glVertex2f(x+w, y);
			for (int n = 0; n <= 2; n++) {
				gl2.glMultiTexCoord2f(GL2.GL_TEXTURE0 + n, uv[n][1], uv[n][3]);
			}
			gl2.glVertex2f(x+w, y+h);
			for (int n = 0; n <= 2; n++) {
				gl2.glMultiTexCoord2f(GL2.GL_TEXTURE0 + n, uv[n][0], uv[n][3]);
			}
			gl2.glVertex2f(x, y+h);
			gl2.glEnd();

			//Disable shader
			glm.setShader(oldShader);
						
			//Restore previous texture
			glm.setTexture(oldTexture);
			
			//Pop matrix
			gl2.glPopMatrix();
		}
		
		private static final int texId(GLTexRect tr) {
			return texId(tr != null ? tr.getTexture() : null);
		}
		private static final int texId(GLTexture tex) {
			return (tex != null ? tex.getTexId() : 0);
		}
		
	}
	
	@LuaSerializable
	private static class BitmapTweenLib extends LFunction implements Serializable {
		
		private static final long serialVersionUID = NVListImpl.serialVersionUID;

		private static final String[] NAMES = {
			"new",
			"isAvailable"
		};

		private static final int NEW = 0;
		private static final int IS_AVAILABLE = 1;
		
		private final int id;
		private final ImageFactory fac;
		private final INotifier ntf;
		
		private BitmapTweenLib(int id, ImageFactory fac, INotifier ntf) {
			this.id = id;
			this.fac = fac;
			this.ntf = ntf;
		}
		
		public static void install(LTable table, ImageFactory fac, INotifier ntf) {
			for (int n = 0; n < NAMES.length; n++) {
				table.put(NAMES[n], new BitmapTweenLib(n, fac, ntf));
			}
		}

		@Override
		public int invoke(LuaState vm) {
			switch (id) {
			case NEW: return newBitmapTween(vm);
			case IS_AVAILABLE: return isAvailable(vm);
			default:
				throw new LuaErrorException("Invalid function id: " + id);
			}
		}
		
		protected int newBitmapTween(LuaState vm) {
			String fadeFilename = (vm.isstring(1) ? vm.tostring(1) : null);
			double duration = vm.tonumber(2);
			double range = vm.tonumber(3);
			IInterpolator i = getInterpolator(vm, vm.topointer(4), BUTTERWORTH);
			vm.resettop();
			BitmapTween tween = new BitmapTween(fac, ntf, fadeFilename, duration, range,
					i, true, false);
			vm.pushlvalue(LuajavaLib.toUserdata(tween, tween.getClass()));
			return 1;
		}
		
		protected int isAvailable(LuaState vm) {
			vm.resettop();
			vm.pushboolean(fac.getGlslVersion().compareTo(requiredGlslVersion) >= 0);
			return 1;
		}
		
	}
}
