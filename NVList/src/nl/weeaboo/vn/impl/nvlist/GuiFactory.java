package nl.weeaboo.vn.impl.nvlist;

import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.vn.IChoice;
import nl.weeaboo.vn.IGuiFactory;
import nl.weeaboo.vn.ISaveLoadScreen;

@LuaSerializable
public class GuiFactory extends EnvironmentSerializable implements IGuiFactory {

	//Functions
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
	
	//Setters
	
}
