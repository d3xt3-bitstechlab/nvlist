package nl.weeaboo.vn.impl.nvlist;

import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.sound.SoundManager;
import nl.weeaboo.vn.SoundType;
import nl.weeaboo.vn.impl.base.BaseSoundState;

@LuaSerializable
public class SoundState extends BaseSoundState {

	private static final long serialVersionUID = NVListImpl.serialVersionUID;

	private final SoundFactory soundFactory;
	
	public SoundState(SoundFactory sfac) {
		soundFactory = sfac;
		
	}
	
	//Functions
	@Override
	public void setMasterVolume(SoundType type, double vol) {
		SoundManager sm = soundFactory.getSoundManager();
		sm.setMasterVolume(SoundFactory.convertSoundType(type), vol);
	}
	
	//Getters
	@Override
	public double getMasterVolume(SoundType type) {
		SoundManager sm = soundFactory.getSoundManager();
		return sm.getMasterVolume(SoundFactory.convertSoundType(type));
	}
	
	//Setters
	
}
