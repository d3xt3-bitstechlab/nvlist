package nl.weeaboo.vn.impl.nvlist;

import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.nvlist.Game;
import nl.weeaboo.vn.IChoice;
import nl.weeaboo.vn.ISaveLoadScreen;
import nl.weeaboo.vn.ISystemLib;

@LuaSerializable
public class SystemLib extends EnvironmentSerializable implements ISystemLib {

	private final Game game;
	private final boolean isEmbedded;
	private final boolean isLowEnd;
	private final boolean isTouchScreen;
	
	public SystemLib(Game game) {
		this.game = game;
		this.isEmbedded = game.getDisplay().isEmbedded();
		this.isLowEnd = false;
		this.isTouchScreen = false;
	}
	
	
	//Functions
	@Override
	public void exit(boolean force) {
		game.stop(false, new Runnable() {
			public void run() {
				game.dispose();
			}
		});
	}
	
	@Override
	public IChoice createChoice(String... options) {
		return null;
	}

	@Override
	public ISaveLoadScreen createSaveScreen() {
		return null;
	}

	@Override
	public ISaveLoadScreen createLoadScreen() {
		return null;
	}
	
	//Getters
	@Override
	public boolean canExit() {
		return !isEmbedded;
	}

	@Override
	public boolean isTouchScreen() {
		return isTouchScreen;
	}

	@Override
	public boolean isLowEnd() {
		return isLowEnd;
	}
	
	//Setters
	
}
