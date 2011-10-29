package nl.weeaboo.vn.impl.nvlist;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import nl.weeaboo.filemanager.IFileManager;
import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.impl.base.BaseScriptLib;
import nl.weeaboo.vn.impl.lua.LuaNovel;

@LuaSerializable
public class ScriptLib extends BaseScriptLib implements Serializable {

	private static final String prefix = "script/";
	
	private final IFileManager fm;
	private final EnvironmentSerializable es;
	
	public ScriptLib(IFileManager fm, INotifier ntf) {
		this.fm = fm;
		this.es = new EnvironmentSerializable(this);
	}
	
	//Functions
	private Object writeReplace() throws ObjectStreamException {	
		return es.writeReplace();
	}

	@Override
	public InputStream openScriptFile(String filename) throws IOException {
		if (LuaNovel.isBuiltInScript(filename)) {
			return LuaNovel.openBuiltInScript(filename);
		} else {
			return fm.getInputStream(prefix + filename);
		}
	}
	
	//Getters
	@Override
	public long getScriptModificationTime(String filename) throws IOException {
		if (LuaNovel.isBuiltInScript(filename)) {
			return 0;
		}		
		return fm.getFileModifiedTime(prefix + filename);
	}
	
	//Setters
	
}
