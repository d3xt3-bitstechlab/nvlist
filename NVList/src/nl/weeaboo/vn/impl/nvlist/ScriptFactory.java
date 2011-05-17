package nl.weeaboo.vn.impl.nvlist;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import nl.weeaboo.filemanager.IFileManager;
import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.IScriptFactory;
import nl.weeaboo.vn.impl.lua.LuaNovel;

@LuaSerializable
public class ScriptFactory implements IScriptFactory, Serializable {

	private final IFileManager fm;
	private final INotifier notifier;
	private final EnvironmentSerializable es;
	
	public ScriptFactory(IFileManager fm, INotifier ntf) {
		this.fm = fm;
		this.notifier = ntf;
		this.es = new EnvironmentSerializable(this);
	}
	
	//Functions
	private Object writeReplace() throws ObjectStreamException {	
		return es.writeReplace();
	}

	@Override
	public InputStream openScriptFile(String filename) throws IOException {
		//First, try to open built-in file
		InputStream in = LuaNovel.openBuiltInScript(filename);
		
		if (in == null) {
			//Open regular file if not built-in
			in = fm.getInputStream("script/" + filename);
		}
		
		if (in == null) {
			notifier.fnf("Script not found: " + filename);
		}
		
		return in;
	}
	
	//Getters
	
	//Setters
	
}
