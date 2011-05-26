package nl.weeaboo.vn.impl.nvlist;

import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.nvlist.Game;
import nl.weeaboo.vn.IChoice;
import nl.weeaboo.vn.IGuiFactory;
import nl.weeaboo.vn.ISaveLoadScreen;

@LuaSerializable
public class GuiFactory extends EnvironmentSerializable implements IGuiFactory {

	private final Game game;
	private final boolean isEmbedded;
	
	public GuiFactory(Game game) {
		this.game = game;
		this.isEmbedded = game.getDisplay().isEmbedded();
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
	
	//Setters
	
}
