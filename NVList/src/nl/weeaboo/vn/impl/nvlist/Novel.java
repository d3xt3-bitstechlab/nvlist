package nl.weeaboo.vn.impl.nvlist;

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
import nl.weeaboo.vn.IGuiFactory;
import nl.weeaboo.vn.IImageFactory;
import nl.weeaboo.vn.IImageFxFactory;
import nl.weeaboo.vn.IImageState;
import nl.weeaboo.vn.IInput;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.INovelConfig;
import nl.weeaboo.vn.IPersistentStorage;
import nl.weeaboo.vn.ISaveHandler;
import nl.weeaboo.vn.IScriptFactory;
import nl.weeaboo.vn.ISeenLog;
import nl.weeaboo.vn.ISoundFactory;
import nl.weeaboo.vn.ISoundState;
import nl.weeaboo.vn.IStorage;
import nl.weeaboo.vn.ITextState;
import nl.weeaboo.vn.IVideoFactory;
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
	
	public Novel(INovelConfig nc, IImageFactory imgfac, IImageState is, IImageFxFactory imgfxfac,
			ISoundFactory sndfac, ISoundState ss, IVideoFactory vf, IVideoState vs,
			ITextState ts, INotifier n, IInput in, IGuiFactory guifac, ISaveHandler sh,
			IScriptFactory scrfac, IPersistentStorage sysVars, IStorage globals,
			ISeenLog seenLog, IAnalytics analytics,
			FileManager fm, IKeyConfig kc)
	{
		super(nc, imgfac, is, imgfxfac, sndfac, ss, vf, vs, ts, n, in, guifac, sh, scrfac,
				sysVars, globals, seenLog, analytics);
		
		this.fm = fm;
		this.keyConfig = kc;
	}
	
	//Functions	
	@Override
	protected void initPreloader(LuaMediaPreloader preloader) {
		preloader.clear();
		try {
			try {
				InputStream in = fm.getInputStream("preloader-default.bin");
				try {
					preloader.load(in);
				} finally {
					in.close();
				}
			} catch (FileNotFoundException fnfe) {
				//Ignore
			}
			
			try {
				InputStream in = fm.getInputStream("preloader.bin");
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
		
		BitmapTween.install(vm._G, (ImageFactory)getImageFactory(), getNotifier());
		GLSLPS.install(vm._G, (ImageFactory)getImageFactory(), getNotifier());
	}

	@Override
	protected void onScriptError(Exception e) {		
		Throwable t = e;
		
		String message = "Script Error";
		if (t instanceof LuaException && t.getCause() != null) {
			message += " :: " + t.getMessage();
			t = t.getCause();
		}
		while (t instanceof LuaErrorException && t.getCause() != null) {
			message += " :: " + t.getMessage();
			t = t.getCause();
		}
		
		getNotifier().e(message, t);
		
		//printStackTrace(System.out);
		
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
