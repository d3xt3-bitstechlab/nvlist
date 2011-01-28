package nl.weeaboo.vn.impl.nvlist;

import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import nl.weeaboo.awt.ImageUtil;
import nl.weeaboo.common.Dim;
import nl.weeaboo.filemanager.FileManager;
import nl.weeaboo.io.ByteChunkOutputStream;
import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.ObjectSerializer;
import nl.weeaboo.nvlist.Game;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.IPersistentStorage;
import nl.weeaboo.vn.IScreenshot;
import nl.weeaboo.vn.ISeenLog;
import nl.weeaboo.vn.impl.lua.LuaSaveHandler;
import nl.weeaboo.vn.impl.lua.LuaSaveInfo;

public class SaveHandler extends LuaSaveHandler implements Serializable {

	private static final Dim screenshotSaveSize = new Dim(256, 256);
	private static final Dim screenshotLoadSize = new Dim(224, 128);
	
	private final EnvironmentSerializable es;
	
	private final String pathPrefix = "";
	private final FileManager fm;
	private final INotifier notifier;
	private final IPersistentStorage sysVars;
	private final ISeenLog seenLog;
	
	public SaveHandler(FileManager fm, INotifier n) {
		super(Game.VERSION);
		
		this.fm = fm;
		this.notifier = n;

		es = new EnvironmentSerializable(this);
		
		sysVars = new SystemVars(fm, pathPrefix + "sysvars.bin", n);
		try {
			sysVars.load();
		} catch (IOException ioe) {
			n.fnf("Error loading sysVars", ioe);
		}
		
		seenLog = new SeenLog(fm, pathPrefix + "seen.bin");
		try {
			seenLog.load();
		} catch (IOException ioe) {
			n.fnf("Error loading seenLog", ioe);
		}
		
		addAllowedPackages("nl.weeaboo.gl", "nl.weeaboo.gl.capture", "nl.weeaboo.gl.texture");
	}

	//Functions
	@Override
	public void delete(int slot) throws IOException {
		if (!fm.delete(getFilename(slot))) {
			if (getSaveExists(slot)) {
				throw new IOException("Deletion of slot " + slot + " failed");
			}
		}		
	}
	
	@Override
	protected byte[] encodeScreenshot(IScreenshot ss) {
		if (ss == null || ss.isCancelled() || !ss.isAvailable()) {
			return new byte[0];
		}
		
		int argb[] = ss.getARGB();
		int w = ss.getWidth();
		int h = ss.getHeight();
				
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, w, h, argb, 0, w);
		if (screenshotSaveSize != null) {
			image = ImageUtil.getScaledImageProp(image,
					screenshotSaveSize.w, screenshotSaveSize.h,
					Transparency.OPAQUE, Image.SCALE_AREA_AVERAGING);
		}		
		ByteChunkOutputStream bout = new ByteChunkOutputStream(32<<10);
		try {
			ImageIO.write(image, "jpg", bout);
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
		return fm.getFileExists(getFilename(slot));
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
				if (fm.getFileExists(filename)) {
					try {
						result.add(loadSaveInfo(slot));
					} catch (IOException e) {
						notifier.v("Unreadable save slot: " + filename, e);
						removeInvalidSave(slot);
					}
				}
			}
		} catch (IOException e) {
			//Ignore
		}
		return result.toArray(new LuaSaveInfo[result.size()]);
	}

	@Override
	public IPersistentStorage getSystemVars() {
		return sysVars;
	}

	@Override
	public ISeenLog getSeenLog() {
		return seenLog;
	}
	
	@Override
	protected IScreenshot newDecodingScreenshot(byte[] b) {
		return new ImageDecodingScreenshot(b, screenshotLoadSize.w, screenshotLoadSize.h);
	}

	@Override
	protected InputStream openSaveInputStream(int slot) throws IOException {
		return fm.getInputStream(getFilename(slot));
	}

	@Override
	protected OutputStream openSaveOutputStream(int slot) throws IOException {
		return fm.getOutputStream(getFilename(slot));
	}
	
	@Override
	protected void onSaveWarnings(String[] warnings) {
		notifier.w(ObjectSerializer.toErrorString(Arrays.asList(warnings)));
	}
	
	@Override
	protected void removeInvalidSave(int slot) throws IOException {
		delete(slot);
	}
	
}
