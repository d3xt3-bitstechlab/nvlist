package nl.weeaboo.vn.impl.nvlist;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.sound.SoundDesc;
import nl.weeaboo.sound.SoundManager;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.ISeenLog;
import nl.weeaboo.vn.ISound;
import nl.weeaboo.vn.SoundType;
import nl.weeaboo.vn.impl.base.BaseSoundFactory;

@LuaSerializable
public class SoundFactory extends BaseSoundFactory implements Serializable {

	private final SoundManager sm;
	private final EnvironmentSerializable es;
	
	public SoundFactory(SoundManager sm, ISeenLog sl, INotifier ntf) {
		super(sl, ntf);
		
		this.sm = sm;
		this.es = new EnvironmentSerializable(this);
	}
	
	//Functions	
	private Object writeReplace() throws ObjectStreamException {	
		return es.writeReplace();
	}
	
	@Override
	public ISound createSoundNormalized(SoundType stype, String filename) throws IOException {
		return new NovelSound(this, stype, filename);
	}
	
	public static nl.weeaboo.sound.SoundType convertSoundType(SoundType type) {
		switch (type) {
		case MUSIC: return nl.weeaboo.sound.SoundType.MUSIC;
		case SOUND: return nl.weeaboo.sound.SoundType.SOUND;
		case VOICE: return nl.weeaboo.sound.SoundType.VOICE;
		default: return nl.weeaboo.sound.SoundType.SOUND;
		}
	}
	
	//Getters		
	public SoundManager getSoundManager() {
		return sm;
	}

	@Override
	public String getNameNormalized(String filename) {
		SoundDesc desc = sm.getSoundDesc(filename);		
		return (desc != null ? desc.getName() : null);
	}

	@Override
	protected boolean isValidFilename(String filename) {
		return sm.getSoundFileExists(filename);
	}
	
	@Override
	protected Collection<String> getFiles(String folder) {
		try {
			return sm.getSoundFiles(folder, true);
		} catch (IOException e) {
			notifier.fnf("Folder doesn't exist or can't be read: " + folder, e);
		}
		return Collections.emptyList();
	}
	
	//Setters
	
}
