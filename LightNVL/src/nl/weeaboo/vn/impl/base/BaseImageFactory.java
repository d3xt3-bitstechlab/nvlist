package nl.weeaboo.vn.impl.base;

import java.io.Serializable;
import java.util.Collection;

import nl.weeaboo.vn.IImageFactory;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.IScreenshot;
import nl.weeaboo.vn.ISeenLog;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.lua.LuaEventHandler;

public abstract class BaseImageFactory extends BaseMediaFactory implements IImageFactory, Serializable {

	private static final long serialVersionUID = BaseImpl.serialVersionUID;
	
	protected final LuaEventHandler eventHandler;
	protected final ISeenLog seenLog;
	//protected final int imgWidth, imgHeight; //The screen size according to img.ini
	protected final int width, height; //Virtual width, usually the same as ImageState's
	
	private boolean isTextRightToLeft;
	
	public BaseImageFactory(LuaEventHandler eh, ISeenLog sl, INotifier ntf, int w, int h) {
		super(new String[]{"png", "jpg"}, sl, ntf);
		
		if (sl instanceof BaseSeenLog) {
			((BaseSeenLog)sl).setImageFactory(this);
		}
		
		this.eventHandler = eh;
		this.seenLog = sl;		
		this.width = w;
		this.height = h;
	}
	
	//Functions
	@Override
	public ITexture toTexture(IScreenshot ss) {
		if (ss.isVolatile()) {
			return ss.getVolatilePixels();
		} else {
			return createTexture(ss.getPixels(), ss.getPixelsWidth(), ss.getPixelsHeight(),
					width / (double)ss.getScreenWidth(),
					height / (double)ss.getScreenHeight());
		}
	}

	//Getters
	@Override
	public final ITexture getTexture(String filename, String[] callStack, boolean suppressErrors) {
		checkRedundantFileExt(filename);			

		String normalized = normalizeFilename(filename);
		if (normalized == null) {
			if (!suppressErrors) {
				notifier.d("Unable to find image file: " + filename);
			}
			return null;
		}
		
		seenLog.addImage(filename);		
		return getTextureNormalized(filename, normalized, callStack);
	}
	
	/**
	 * Gets called from {@link #getTexture(String, String[], boolean)}
	 */
	protected abstract ITexture getTextureNormalized(String filename, String normalized, String[] callStack);
	
	@Override
	public Collection<String> getImageFiles(String folder) {
		return getMediaFiles(folder);
	}
	
	public boolean isTextRightToLeft() {
		return isTextRightToLeft;
	}
	
	//Setters
	public void setTextRightToLeft(boolean rtl) {
		isTextRightToLeft = rtl;
	}
	
}
