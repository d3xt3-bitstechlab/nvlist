package nl.weeaboo.vn.impl.nvlist;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import nl.weeaboo.common.Dim;
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
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.IRenderer;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.BaseImageTween;
import nl.weeaboo.vn.impl.base.BaseRenderer;
import nl.weeaboo.vn.impl.base.CustomRenderCommand;

import org.luaj.vm.LFunction;
import org.luaj.vm.LTable;
import org.luaj.vm.LuaErrorException;
import org.luaj.vm.LuaState;

public class BitmapTween extends BaseImageTween {

	private static final long serialVersionUID = NVListImpl.serialVersionUID;
	
	private static final String requiredGlslVersion = "1.1";
	private static final int interpMax = 65535;
	
	private final ImageFactory fac;
	private final INotifier notifier;
	private final String fadeFilename;
	private final double range;
	private final boolean fadeTexLerp; //Linear interpolation
	private final boolean fadeTexTile; //Texture tiling
	
	//--- Initialized in prepare() ---
	private int[] interpolation;
	private GLShader shader;
	private GLTexRect[] texs;
	private float[][] origUV;
	private float[][] uv;
	private float anchorX, anchorY; //Relative positioning of different-sized images (0.0-1.0)
	private float fadeTexAnchorX, fadeTexAnchorY;
	private GLGeneratedTexture fadeTex;
	private GLGeneratedTexture remapTex;
	private transient int[] remapTemp;
	
	public BitmapTween(ImageFactory ifac, INotifier ntf, String fadeFilename,
			int duration, double range)
	{
		this(ifac, ntf, fadeFilename, duration, range, true, false);
	}
	public BitmapTween(ImageFactory ifac, INotifier ntf, String fadeFilename,
			int duration, double range, boolean fadeTexLerp, boolean fadeTexTile)
	{	
		super(duration);
		
		this.fac = ifac;
		this.notifier = ntf;
		this.fadeFilename = fadeFilename;
		this.range = range;
		this.fadeTexLerp = fadeTexLerp;
		this.fadeTexTile = fadeTexTile;
	}
	
	//Functions
	public static void install(LTable globals, final ImageFactory ifac, final INotifier ntf) {
		LTable table = new LTable();
		BitmapTweenLib.install(table, ifac, ntf);
		globals.put("BitmapTween", table);
	}
	
	@Override
	protected void doPrepare() {
		super.doPrepare();
		
		//Get interpolation function values
		interpolation = new int[interpMax+1];
		interpolation[0] = 0;
		for (int n = 1; n < interpMax; n++) {
			int i = Math.round(interpMax * remap(n / (float)interpMax));
			interpolation[n] = (short)Math.max(0, Math.min(interpMax, i));
		}
		interpolation[interpMax] = interpMax;
		
		//Get shader
		shader = fac.getGLShader("bitmap-tween");
		
		//Get texRects
		ITexture[] itexs = new ITexture[] {getStartTexture(), getEndTexture()};
		texs = new GLTexRect[itexs.length];
		int w = 0, h = 0;
		for (int n = 0; n < itexs.length; n++) {
			if (itexs[n] instanceof TextureAdapter) {
				TextureAdapter ta = (TextureAdapter)itexs[n];
				texs[n] = ta.getTexRect();
				w = Math.max(w, texs[n].getWidth());
				h = Math.max(h, texs[n].getHeight());
			}
		}

		//Calculate texRects' UV
		origUV = new float[texs.length+1][];
		uv = new float[texs.length+1][];
		for (int n = 0; n < texs.length; n++) {
			if (texs[n] == null) {
				origUV[n] = new float[] {0, 1, 0, 1};
				uv[n] = new float[] {0, 1, 0, 1};
			} else {			
				GLTexture t = texs[n].getTexture();
				origUV[n] = GLUtil.getUV(t.getTexWidth(), t.getTexHeight(), texs[n].getRect());				
				uv[n] = scaleUV(origUV[n],
						w/(float)itexs[n].getWidth(), h/(float)itexs[n].getHeight(),
						anchorX, anchorY);
			}
		}
		
		//Create fade texture
		int fadeTexARGB[] = null;
		int fadeTexW = 16, fadeTexH = 16;
		if (fadeFilename != null) {
			try {
				//Load image
				BufferedImage src = fac.getBufferedImage(fadeFilename);			
				if (!fadeTexTile) {
					//Take the region of the image we want to use
					src = fadeImageSubRect(src, w, h);
				}
				
				//Get 16-bit grayscale pixels from image
				fadeTexARGB = toGrayScale(src);
	
				fadeTexW = src.getWidth();
				fadeTexH = src.getHeight();
			} catch (IOException ioe) {
				if (ioe instanceof FileNotFoundException) {
					notifier.fnf("Fade image not found: " + fadeFilename, ioe);
				} else {
					notifier.w("Error while loading fade image (" + fadeFilename + ")", ioe);
				}			
			}
		}
		
		//Init a default value in case there's no bitmap specified or it doesn't exist
		if (fadeTexARGB == null) {
			fadeTexARGB = new int[fadeTexW * fadeTexH];
			Arrays.fill(fadeTexARGB, (interpMax+1) / 2);
		}

		//Add an alpha channel for debugging purposes
		for (int n = 0; n < fadeTexARGB.length; n++) {
			fadeTexARGB[n] = 0xFF000000 | fadeTexARGB[n];
		}

		fadeTex = fac.createGLTexture(fadeTexARGB, fadeTexW, fadeTexH,
				(fadeTexLerp ? GL.GL_LINEAR : GL.GL_NEAREST),
				(fadeTexLerp ? GL.GL_LINEAR : GL.GL_NEAREST),
				(fadeTexTile ? GL.GL_REPEAT : GL.GL_CLAMP_TO_EDGE));		
		
		int fadeUVIndex = texs.length;
		
		origUV[fadeUVIndex] = GLUtil.getUV(fadeTex.getTexWidth(), fadeTex.getTexHeight(), fadeTex.getCrop());
		if (fadeTexTile) {
			uv[fadeUVIndex] = scaleUV(origUV[fadeUVIndex],
					w/(float)fadeTexW, h/(float)fadeTexH,
					fadeTexAnchorX, fadeTexAnchorY);
		} else {
			uv[fadeUVIndex] = origUV[fadeUVIndex].clone();
		}
		
		//Create remap texture
		int remapTexW = 256;
		int remapTexH = 256;
		int remapTexARGB[] = new int[remapTexW * remapTexH];
		remapTex = fac.createGLTexture(remapTexARGB, remapTexW, remapTexH,
				GL.GL_NEAREST, GL.GL_NEAREST, GL.GL_CLAMP_TO_EDGE);
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
	
	private float[] scaleUV(float[] uv, float scaleX, float scaleY,
			float anchorX, float anchorY)
	{
		float w = uv[1] - uv[0];
		float h = uv[3] - uv[2];
		float x = uv[0] - w * (scaleX-1) * anchorX;
		float y = uv[2] - h * (scaleY-1) * anchorY;
		
		return new float[] { x, x + w * scaleX, y, y + h * scaleY };
	}
	
	@Override
	public boolean update(double effectSpeed) {
		boolean changed = super.update(effectSpeed);		
		changed |= updateRemapTex();
		return changed;
	}
	
	protected boolean updateRemapTex() {
		double mia = interpMax * (getNormalizedTime()-range) / (1 - range);
		double maa = interpMax * (getNormalizedTime()      ) / (1 - range);
		int minA = Math.min(interpMax, Math.max(0, (int)Math.round(mia)));
		int maxA = Math.min(interpMax, Math.max(0, (int)Math.round(maa)));

		int requiredLen = remapTex.getCropWidth() * remapTex.getCropHeight();
		if (remapTemp == null || remapTemp.length < requiredLen) {
			remapTemp = new int[requiredLen];
		}
		
		Arrays.fill(remapTemp, 0, minA, interpMax);		
		Arrays.fill(remapTemp, maxA, remapTemp.length, 0);		
		double inc = interpMax / (maa - mia);
		double cur = (minA - mia) * inc;			
		for (int n = minA; n <= maxA && n <= interpMax; n++) {
			int ar = Math.max(0, Math.min(interpMax, (int)Math.round(cur)));
			remapTemp[n] = interpMax - interpolation[ar];				
			cur += inc;
		}
		
		//Add an alpha channel for debugging purposes
		for (int n = 0; n < remapTemp.length; n++) {
			remapTemp[n] = 0xFF000000 | remapTemp[n];
		}
		
		remapTex.setARGB(remapTemp);
		
		return true;
	}
	
	@Override
	public void draw(IRenderer r) {
		super.draw(r);
				
		BaseRenderer rr = (BaseRenderer)r;
		rr.draw(new RenderCommand(drawable, texs, origUV, uv, fadeTex, remapTex, shader));
	}
	
	protected float remap(float u) {
		if (u >= .5f) {
			u = 1f - u;
			return -1.5f + 2.5f / (1f + u * u);			
		} else {
			return  2.5f - 2.5f / (1f + u * u);
		}
	}
	
	//Getters
	
	//Setters
	
	//Inner Classes
	protected static final class RenderCommand extends CustomRenderCommand {

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
			
			this.x = (float)id.getX();
			this.y = (float)id.getY();
			this.w = (float)id.getWidth();
			this.h = (float)id.getHeight();			
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
			int duration = vm.tointeger(2);
			double range = vm.tonumber(3);
			vm.resettop();
			BitmapTween tween = new BitmapTween(fac, ntf, fadeFilename, duration, range);
			vm.pushlvalue(LuajavaLib.toUserdata(tween, BitmapTween.class));
			return 1;
		}
		
		protected int isAvailable(LuaState vm) {
			vm.resettop();
			vm.pushboolean(fac.getGlslVersion().compareTo(requiredGlslVersion) >= 0);
			return 1;
		}
		
	}
}
