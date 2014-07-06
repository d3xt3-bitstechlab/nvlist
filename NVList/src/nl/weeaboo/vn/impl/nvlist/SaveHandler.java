package nl.weeaboo.vn.impl.nvlist;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.weeaboo.awt.ImageUtil;
import nl.weeaboo.common.Dim;
import nl.weeaboo.filesystem.IFileSystem;
import nl.weeaboo.io.ByteChunkOutputStream;
import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua2.io.ObjectSerializer;
import nl.weeaboo.nvlist.Game;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.IScreenshot;
import nl.weeaboo.vn.impl.lua.LuaSaveHandler;
import nl.weeaboo.vn.impl.lua.LuaSaveInfo;

public class SaveHandler extends LuaSaveHandler implements Serializable {
	
	private final EnvironmentSerializable es;
	
	private static final String pathPrefix = "";
	private final IFileSystem fs;
	private final INotifier notifier;
	
	public SaveHandler(IFileSystem fs, INotifier n) {
		super(Game.VERSION);
		
		this.fs = fs;
		this.notifier = n;

		es = new EnvironmentSerializable(this);

		addAllowedPackages(
			"nl.weeaboo.image",			//For ImageDesc (NVList 3.x, used by TexLoadParam)
			"nl.weeaboo.media.image",	//For ImageDesc (NVList 4.x, used by TexLoadParam)
			"nl.weeaboo.game.resmgr",	//For DataHolder
			"nl.weeaboo.gl",
			"nl.weeaboo.gl.shader",
			"nl.weeaboo.gl.tex",
			"nl.weeaboo.gl.tex.loader",	//For TexLoadParam
			"nl.weeaboo.gl.jogl"		//For JoglTextureData
		);
	}

	//Functions
	@Override
	public void delete(int slot) throws IOException {
		try {
			fs.delete(getFilename(slot));
		} catch (FileNotFoundException fnfe) {
			//Ignore
		}
		
		if (getSaveExists(slot)) {
			throw new IOException("Deletion of slot " + slot + " failed");
		}
	}
	
	@Override
	protected byte[] encodeScreenshot(IScreenshot ss, Dim maxSize) {
		if (ss == null || ss.isCancelled() || !ss.isAvailable()) {
			return new byte[0];
		}
		
		int[] argb = ss.getPixels();
		int w = ss.getPixelsWidth();
		int h = ss.getPixelsHeight();
		if (argb == null || w <= 0 || h <= 0) {
			return new byte[0];
		}
		
		BufferedImage image = ImageUtil.createBufferedImage(w, h, argb, false);		
		if (maxSize != null) {
			image = ImageUtil.getScaledImageProp(image, maxSize.w, maxSize.h, Image.SCALE_AREA_AVERAGING);
		}
		
		ByteChunkOutputStream bout = new ByteChunkOutputStream(32 << 10);
		try {
			ImageUtil.writeJPEG(bout, image, 0.95f);
			
			//ImageUtil.getPixels(image, argb, 0, image.getWidth());
			//TGAUtil.writeTGA(bout, argb, image.getWidth(), image.getHeight(), false);
	        
			return bout.toByteArray();
		} catch (IOException ioe) {
			notifier.w("Error while encoding screenshot", ioe);
			return new byte[0];
		}
	}
	
	private Object writeReplace() throws ObjectStreamException {	
		return es.writeReplace();
	}
	
	//Getters
	protected String getFilename(int slot) {
		return String.format("%ssave-%03d.sav", pathPrefix, slot);		
	}
	
	protected int getSlot(String filename) {
		if (filename.endsWith(".sav")) {
			int index = filename.lastIndexOf('-');
			String part = filename.substring(index+1, filename.length()-4);
			try {
				return Integer.parseInt(part);
			} catch (NumberFormatException nfe) {
				//Ignore
			}
		}
		return 0;
	}
	
	@Override
	public boolean getSaveExists(int slot) {
		return fs.getFileExists(getFilename(slot));
	}

	@Override
	public LuaSaveInfo[] getSaves(int start, int end) {
		List<LuaSaveInfo> result = new ArrayList<LuaSaveInfo>();
		try {
			//for (String filename : fm.getFolderContents(pathPrefix, false)) {
			//    int slot = getSlot(filename);
			
			if (end - (long)start > 1000L) {
				start = Math.max(-1000, start);
				end = Math.min(1000, end);
			}
			
			for (int slot = start; slot < end; slot++) {
				String filename = getFilename(slot);
				if (fs.getFileExists(filename)) {
					try {
						result.add(loadSaveInfo(slot));
					} catch (IOException e) {
						notifier.v("Unreadable save slot: " + filename, e);
						delete(slot);
					}
				}
			}
		} catch (IOException e) {
			//Ignore
		}
		return result.toArray(new LuaSaveInfo[result.size()]);
	}
	
	@Override
	protected LuaSaveInfo newSaveInfo(int slot) {
		return new SaveInfo(slot);
	}

	@Override
	protected InputStream openSaveInputStream(int slot) throws IOException {
		return fs.newInputStream(getFilename(slot));
	}

	@Override
	protected OutputStream openSaveOutputStream(int slot) throws IOException {
		return fs.newOutputStream(getFilename(slot), false);
	}
	
	@Override
	protected void onSaveWarnings(String[] warnings) {
		notifier.w(ObjectSerializer.toErrorString(Arrays.asList(warnings)));
	}
	
	//Inner Classes
	private static class SaveInfo extends LuaSaveInfo {
		
		public SaveInfo(int slot) {
			super(slot);
		}
		
		@Override
		protected IScreenshot decodeScreenshot(ByteBuffer data, Dim maxSize) {
			return new ImageDecodingScreenshot(data, maxSize);			
		}
	}
	
}
