package nl.weeaboo.vn.impl.nvlist;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import nl.weeaboo.gl.GLInfo;
import nl.weeaboo.gl.shader.GLShader;
import nl.weeaboo.gl.shader.ShaderCache;
import nl.weeaboo.gl.text.GLTextRendererStore;
import nl.weeaboo.gl.texture.GLGeneratedTexture;
import nl.weeaboo.gl.texture.GLTexRect;
import nl.weeaboo.gl.texture.GLTexture;
import nl.weeaboo.gl.texture.TextureCache;
import nl.weeaboo.gl.texture.TextureData;
import nl.weeaboo.gl.texture.loader.ImageFormatException;
import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua2.io.LuaSerializable;
import nl.weeaboo.vn.IAnalytics;
import nl.weeaboo.vn.IButtonDrawable;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.IScreenshot;
import nl.weeaboo.vn.ISeenLog;
import nl.weeaboo.vn.ITextRenderer;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.BaseImageFactory;
import nl.weeaboo.vn.impl.lua.LuaNovelUtil;

@LuaSerializable
public class ImageFactory extends BaseImageFactory implements Serializable {

	private static final boolean RENDER_TEXT_TO_TEXTURE = false;
	
	private final EnvironmentSerializable es;
	private final IAnalytics analytics;
	private final TextureCache texCache;
	private final ShaderCache shCache;
	private final GLTextRendererStore trStore;
	
	private int imgWidth, imgHeight;
	private int subTexLim; //Max size to try and put in a GLPackedTexture instead of generating a whole new texture.
	
	public ImageFactory(TextureCache tc, ShaderCache sc, GLTextRendererStore trStore,
			IAnalytics an, ISeenLog sl, INotifier ntf, int w, int h)
	{
		super(sl, ntf, w, h);
		
		this.analytics = an;
		this.texCache = tc;
		this.shCache = sc;
		this.trStore = trStore;		
		this.imgWidth = w;
		this.imgHeight = h;
		this.subTexLim = 128;
		
		this.es = new EnvironmentSerializable(this);

		setDefaultExts("ktx", "png", "jpg", "jng");
	}
	
	//Functions
	private Object writeReplace() throws ObjectStreamException {	
		return es.writeReplace();
	}
	
	@Override
	protected void preloadNormalized(String filename) {
		texCache.preload(filename, false);
	}
		
	@Override
	public ImageDrawable createImageDrawable() {
		return new ImageDrawable();
	}

	@Override
	public TextDrawable createTextDrawable() {
		return new TextDrawable(createTextRenderer());
	}

	@Override
	public IButtonDrawable createButtonDrawable() {
		return new ButtonDrawable(createTextRenderer());
	}
	
	protected ITextRenderer createTextRenderer() {
		if (RENDER_TEXT_TO_TEXTURE) {
			return new TextureTR(this, trStore);
		}
		return new GlyphTR(this, trStore);
	}
	
	@Override
	public IScreenshot screenshot(short z) {
		return new Screenshot(z);
	}
	
	@Override
	public ITexture createTexture(int[] argb, int w, int h, double sx, double sy) {
		if (w <= subTexLim && h <= subTexLim) {
			return createTexture(createGLTexRect(argb, w, h), sx, sy);
		} else {
			return createTexture(createGLTexture(argb, w, h, 0, 0, 0), sx, sy);
		}
	}
	
	@Override
	public ITexture createTexture(IScreenshot ss) {
		return createTexture(ss.getARGB(), ss.getWidth(), ss.getHeight(),
				width / (double)ss.getScreenWidth(),
				height / (double)ss.getScreenHeight());
	}
	
	public ITexture createTexture(GLTexture tex, double sx, double sy) {
		if (tex == null) {
			return null;
		}
		return createTexture(tex.getTexRect(null), sx, sy);
	}

	public ITexture createTexture(GLTexRect tr, double sx, double sy) {
		if (tr == null) {
			return null;
		}

		TextureAdapter ta = new TextureAdapter(this);
		ta.setTexRect(tr, sx, sy);
		//System.out.println(ta.getWidth()+"x"+ta.getHeight() + " " + tr.getRect() + " " + tr.getUV() + " " + tr.getTexture().getTexWidth()+"x"+tr.getTexture().getTexHeight());
		return ta;
	}
	
	public GLGeneratedTexture createGLTexture(int[] argb, int w, int h,
			int glMinFilter, int glMagFilter, int glWrap)
	{	
		return texCache.newTexture(argb, w, h, glMinFilter, glMagFilter, glWrap);		
	}
	
	public GLTexRect createGLTexRect(int[] argb, int w, int h) {
		return texCache.newTexRect(argb, w, h, false);
	}
	
	public TextureData createTextureData(BufferedImage image) throws ImageFormatException {
		return texCache.newTextureData(image);
	}
	
	//Getters
	@Override
	protected boolean isValidFilename(String id) {
		if (id == null) return false;
		
		return texCache.getImageExists(id);
	}

	public GLTexRect getTexRect(String filename, String[] luaStack) {
		return getTexRectNormalized(filename, normalizeFilename(filename), luaStack);
	}
	
	protected GLTexRect getTexRectNormalized(String filename, String normalized, String[] luaStack) {
		if (normalized == null) {
			return null;
		}
		
		long loadNanos = 0L;
		
		GLTexRect tr;
		if (!texCache.isLoaded(normalized)) {
			long t0 = System.nanoTime();			
			tr = texCache.get(normalized);
			loadNanos = System.nanoTime() - t0;			
		} else {
			tr = texCache.get(normalized);
		}
		
		if (tr != null) {
			String callSite = LuaNovelUtil.getNearestLVNSrcloc(luaStack);
			if (callSite != null) {
				analytics.logImageLoad(callSite, filename, loadNanos);
				//System.out.println("Image Load: " + filename);
			}
		}
		
		return tr;
	}
	
	@Override
	protected ITexture getTextureNormalized(String filename, String normalized, String[] luaStack) {
		GLTexRect tr = getTexRectNormalized(filename, normalized, luaStack);

		//Returning null prevents reloading the image if it's available in a different resolution only
		//if (tr == null) {
		//	return null;
		//}
		
		ImageTextureAdapter ita = new ImageTextureAdapter(this, normalized);
		double scale = getImageScale();
		ita.setTexRect(tr, scale, scale);
		return ita;
	}
		
	public BufferedImage getBufferedImage(String filename) throws IOException {
		String normalized = normalizeFilename(filename);
		if (normalized == null) {
			throw new FileNotFoundException(filename);
		}
		
		try {
			return texCache.loadBufferedImage(normalized);
		} catch (ImageFormatException e) {
			throw new IOException("Unsupported image format: " + filename, e);
		}
	}
	
	public GLShader getGLShader(String filename) {
		return shCache.get(filename);
	}
		
	public String getGlslVersion() {
		return shCache.getGlslVersion();
	}
	
	public boolean isGLExtensionAvailable(String ext) {
		GLInfo info = texCache.getGLInfo();
		return info != null && info.isExtensionAvailable(ext);
	}

	@Override
	protected Collection<String> getFiles(String folder) {
		try {
			return texCache.getImageFiles(folder, true);
		} catch (IOException e) {
			notifier.d("Folder doesn't exist or can't be read: " + folder, e);
		}
		return Collections.emptyList();
	}
	
	public double getImageScale() {
		return Math.min(width / (double)imgWidth, height / (double)imgHeight);
	}
	
	//Setters
	public void setImageSize(int iw, int ih) {
		imgWidth = iw;
		imgHeight = ih;
	}
	
}