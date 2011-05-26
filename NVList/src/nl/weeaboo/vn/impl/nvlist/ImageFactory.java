package nl.weeaboo.vn.impl.nvlist;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import nl.weeaboo.gl.shader.GLShader;
import nl.weeaboo.gl.shader.ShaderCache;
import nl.weeaboo.gl.text.GLTextRendererStore;
import nl.weeaboo.gl.texture.GLGeneratedTexture;
import nl.weeaboo.gl.texture.GLTexRect;
import nl.weeaboo.gl.texture.GLTexture;
import nl.weeaboo.gl.texture.TextureCache;
import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.vn.IAnalytics;
import nl.weeaboo.vn.IButtonDrawable;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.IScreenshot;
import nl.weeaboo.vn.ISeenLog;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.BaseImageFactory;
import nl.weeaboo.vn.impl.lua.LuaNovelUtil;

@LuaSerializable
public class ImageFactory extends BaseImageFactory implements Serializable {

	private final EnvironmentSerializable es;
	private final IAnalytics analytics;
	private final TextureCache texCache;
	private final ShaderCache shCache;
	private final GLTextRendererStore trStore;
	
	public ImageFactory(TextureCache tc, ShaderCache sc, GLTextRendererStore trStore,
			IAnalytics an, ISeenLog sl, INotifier ntf, int iw, int ih, int w, int h)
	{
		super(sl, ntf, iw, ih, w, h);
		
		this.analytics = an;
		this.texCache = tc;
		this.shCache = sc;
		this.trStore = trStore;		

		this.es = new EnvironmentSerializable(this);
	}
	
	//Functions
	private Object writeReplace() throws ObjectStreamException {	
		return es.writeReplace();
	}
	
	@Override
	protected void preloadNormalized(String filename) {
		texCache.preload(filename);
	}
	
	@Override
	public ImageDrawable createImageDrawable() {
		return new ImageDrawable();
	}

	@Override
	public TextDrawable createTextDrawable() {
		return new TextDrawable(trStore);
	}

	@Override
	public IButtonDrawable createButtonDrawable() {
		return new ButtonDrawable();
	}
	
	@Override
	public IScreenshot screenshot(short z) {
		return new Screenshot(z);
	}
	
	@Override
	public ITexture createTexture(int[] argb, int w, int h, double sx, double sy) {
		GLTexture tex = createGLTexture(argb, w, h);
		if (tex == null) {
			return null;
		}
		return new TextureAdapter(tex.getTexRect(null), sx, sy);
	}
	
	@Override
	public ITexture createTexture(IScreenshot ss) {
		return createTexture(ss.getARGB(), ss.getWidth(), ss.getHeight(),
				width / ss.getScreenWidth(), height / ss.getScreenHeight());
	}
	
	public GLGeneratedTexture createGLTexture(int[] argb, int w, int h) {
		return createGLTexture(argb, w, h, 0, 0, 0);
	}	
	
	public GLGeneratedTexture createGLTexture(int[] argb, int w, int h,
			int glMinFilter, int glMagFilter, int glWrap)
	{	
		return texCache.generateTexture(argb, w, h, glMinFilter, glMagFilter, glWrap);		
	}
	
	//Getters
	@Override
	protected boolean isValidFilename(String filename) {
		return texCache.getImageFileExists(filename);
	}
	
	@Override
	public ITexture getTextureNormalized(String filename, String[] luaStack) {
		GLTexRect tr;
		if (!texCache.isLoaded(filename)) {
			long t0 = System.nanoTime();			
			tr = texCache.get(filename);
			long t1 = System.nanoTime();
			
			if (tr != null) {
				String callSite = LuaNovelUtil.getNearestLVNSrcloc(luaStack);
				if (callSite != null) {
					analytics.logImageLoad(filename, callSite, t1-t0);
					//System.out.println("Image Load: " + filename);
				}
			}
		} else {
			tr = texCache.get(filename);
		}

		if (tr == null) {
			return null;
		}
				
		return new TextureAdapter(tr, width / (double)imgWidth, height / (double)imgHeight);
	}
		
	public BufferedImage getBufferedImage(String filename) throws IOException {
		filename = normalizeFilename(filename);
		return texCache.loadBufferedImage(filename);
	}
	
	public GLShader getGLShader(String filename) {
		return shCache.get(filename);
	}
		
	public String getGlslVersion() {
		return shCache.getGlslVersion();
	}

	@Override
	protected Collection<String> getFiles(String folder) {
		try {
			return texCache.getImageFiles(folder, true);
		} catch (IOException e) {
			notifier.fnf("Folder doesn't exist or can't be read: " + folder, e);
		}
		return Collections.emptyList();
	}
	
}
