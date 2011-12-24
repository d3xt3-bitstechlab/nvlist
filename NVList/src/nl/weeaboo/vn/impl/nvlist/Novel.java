package nl.weeaboo.vn.impl.nvlist;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import nl.weeaboo.filemanager.FileManager;
import nl.weeaboo.game.input.IKeyConfig;
import nl.weeaboo.game.input.Keys;
import nl.weeaboo.lua.LuaException;
import nl.weeaboo.lua.LuaRunState;
import nl.weeaboo.lua.LuaUtil;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.vn.IAnalytics;
import nl.weeaboo.vn.IImageState;
import nl.weeaboo.vn.IInput;
import nl.weeaboo.vn.INovelConfig;
import nl.weeaboo.vn.IPersistentStorage;
import nl.weeaboo.vn.ISeenLog;
import nl.weeaboo.vn.ISoundState;
import nl.weeaboo.vn.IStorage;
import nl.weeaboo.vn.ITextState;
import nl.weeaboo.vn.ITimer;
import nl.weeaboo.vn.IVideoState;
import nl.weeaboo.vn.impl.lua.AbstractKeyCodeMetaFunction;
import nl.weeaboo.vn.impl.lua.LuaMediaPreloader;
import nl.weeaboo.vn.impl.lua.LuaNovel;

import org.luaj.vm.LInteger;
import org.luaj.vm.LNil;
import org.luaj.vm.LTable;
import org.luaj.vm.LValue;
import org.luaj.vm.LuaErrorException;
import org.luaj.vm.LuaState;

public class Novel extends LuaNovel {

	private transient FileManager fm;
	private transient IKeyConfig keyConfig;
	
	// !!WARNING!! Do not add properties without adding code for saving/loading
	
	public Novel(INovelConfig nc, ImageFactory imgfac, IImageState is, ImageFxLib fxlib,
			SoundFactory sndfac, ISoundState ss, VideoFactory vf, IVideoState vs,
			ITextState ts, NovelNotifier n, IInput in, SystemLib syslib, SaveHandler sh,
			ScriptLib scrlib, TweenLib tl, IPersistentStorage sysVars, IStorage globals,
			ISeenLog seenLog, IAnalytics analytics, ITimer tmr,
			FileManager fm, IKeyConfig kc)
	{
		super(nc, imgfac, is, fxlib, sndfac, ss, vf, vs, ts, n, in, syslib, sh, scrlib, tl,
				sysVars, globals, seenLog, analytics, tmr);
		
		this.fm = fm;
		this.keyConfig = kc;
	}
	
	//Functions	
	@Override
	protected void initPreloader(LuaMediaPreloader preloader) {
		preloader.clear();
		try {
			try {
				InputStream in = new BufferedInputStream(fm.getInputStream("preloader-default.bin"), 4096);
				try {
					preloader.load(in);
				} finally {
					in.close();
				}
			} catch (FileNotFoundException fnfe) {
				//Ignore
			}
			
			try {
				InputStream in = new BufferedInputStream(fm.getInputStream("preloader.bin"), 4096);
				try {
					preloader.load(in);
				} finally {
					in.close();
				}
			} catch (FileNotFoundException fnfe) {
				//Ignore
			}
		} catch (IOException ioe) {
			getNotifier().w("Error initializing preloader", ioe);
		}
	}
	
	@Override
	public void initLuaRunState() {
		super.initLuaRunState();
		
		LuaRunState lrs = getLuaRunState();
		LuaState vm = lrs.vm;
		try {
			LuaUtil.registerClass(lrs, vm, FreeRotationGS.class);
		} catch (LuaException e) {
			onScriptError(e);
		}
		
		LTable globals = vm._G;
		try {
			globals.put("VNDS", createVNDSLib(globals));
		} catch (LuaException e) {
			onScriptError(e);
		}
		GLSLPS.install(globals, (ImageFactory)getImageFactory(), getNotifier());
	}

	@Override
	protected void onScriptError(Exception e) {		
		Throwable t = e;
		
		StringBuilder message = new StringBuilder("Script Error");
		if (t instanceof LuaException && t.getCause() != null) {
			message.append(" :: ");
			message.append(t.getMessage());
			t = t.getCause();
		}
		while (t instanceof LuaErrorException && t.getCause() != null) {
			message.append(" :: ");
			message.append(t.getMessage());
			t = t.getCause();
		}
		
		getNotifier().e(message.toString(), t);		
		
		setWait(60);
	}

	@Override
	protected void addKeyCodeConstants(LTable table) throws LuaException {
		Keys keys = keyConfig.getKeys();
		//for (Entry<String, Integer> entry : keys) {
		//	table.put(LString.valueOf(entry.getKey()), LInteger.valueOf(entry.getValue()));
		//}
		addKeyCodeConstants(table, new KeyCodeMetaFunction(keys, table));
	}
	
	//Getters
	
	//Setters
	
	//Inner classes
	@LuaSerializable
	private static class KeyCodeMetaFunction extends AbstractKeyCodeMetaFunction {

		private static final long serialVersionUID = 1L;
		
		private static Keys keys;
		
		public KeyCodeMetaFunction(Keys k, LTable t) {
			super(t);
			
			keys = k;
		}

		@Override
		protected LValue getKeyCode(String name) {
			int retval = keys.get(name);
			return (retval == 0 ? LNil.NIL : LInteger.valueOf(retval));
		}
		
	}
	
}
