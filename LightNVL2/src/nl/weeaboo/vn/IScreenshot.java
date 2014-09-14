package nl.weeaboo.vn;

import java.io.Serializable;

/**
 * Wrapper object for screenshot operations. Since screenshots can only be taken during rendering, it may take
 * several frames for a scheduled screenshot to become available.
 */
public interface IScreenshot extends Serializable {

	public void cancel();

	/**
	 * Marks this screenshot object as transient; its pixels won't be serialized.
	 */
	public void markTransient();

	@Deprecated
	public void makeTransient();

	/** @return {@code true} when the screenshot operation has completed succesfully. */
	public boolean isAvailable();

	/**
	 * A volatile screenshot may lose its pixels at any time and doesn't support the {@link #getPixels()}
	 * method. Use {@link #getVolatilePixels()} instead.
	 */
	public boolean isVolatile();

	/** @see #markTransient() */
	public boolean isTransient();

	/** @see #cancel() */
	public boolean isCancelled();

	public int getScreenWidth();
	public int getScreenHeight();

	/**
	 * Warning: May return {@code null}. For volatile screenshots, use {@link #getVolatilePixels()} instead.
	 */
	public int[] getPixels();
	public int getPixelsWidth();
	public int getPixelsHeight();

	/**
	 * Warning: May return {@code null}.
	 */
	public ITexture getVolatilePixels();

	public short getZ();

}
