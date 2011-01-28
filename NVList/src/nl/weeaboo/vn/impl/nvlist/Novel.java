package nl.weeaboo.vn.impl.nvlist;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import nl.weeaboo.filemanager.FileManager;
import nl.weeaboo.game.input.IKeyConfig;
import nl.weeaboo.game.input.Keys;
import nl.weeaboo.gl.shader.ShaderCache;
import nl.weeaboo.gl.text.GLTextRendererStore;
import nl.weeaboo.gl.texture.TextureCache;
import nl.weeaboo.lua.LuaException;
import nl.weeaboo.lua.LuaRunState;
import nl.weeaboo.lua.LuaUtil;
import nl.weeaboo.lua.io.DefaultEnvironment;
import nl.weeaboo.lua.io.LuaSerializer;
import nl.weeaboo.vn.IGuiFactory;
import nl.weeaboo.vn.IImageFactory;
import nl.weeaboo.vn.IImageState;
import nl.weeaboo.vn.IInput;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.INovelConfig;
import nl.weeaboo.vn.ISaveHandler;
import nl.weeaboo.vn.ISoundFactory;
import nl.weeaboo.vn.ISoundState;
import nl.weeaboo.vn.ITextState;
import nl.weeaboo.vn.IVideoFactory;
import nl.weeaboo.vn.IVideoState;
import nl.weeaboo.vn.impl.lua.LuaNovel;

import org.luaj.vm.LTable;
import org.luaj.vm.LuaErrorException;
import org.luaj.vm.LuaState;

public abstract class Novel extends LuaNovel {

	private transient FileManager fm;
	private transient IKeyConfig keyConfig;
	
	// !!WARNING!! Do not add properties without adding code for saving/loading
	
	public Novel(INovelConfig nc, IImageFactory imgfac, IImageState is, ISoundFactory sndfac,
			ISoundState ss, IVideoFactory vf, IVideoState vs, ITextState ts,
			INotifier n, IInput in, IGuiFactory guifac, ISaveHandler sh,
			FileManager fm, IKeyConfig kc, TextureCache tc, ShaderCache sc,
			GLTextRendererStore trStore)
	{
		super(nc, imgfac, is, sndfac, ss, vf, vs, ts, n, in, guifac, sh);
		
		this.fm = fm;
		this.keyConfig = kc;
	}
	
	//Functions
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
	
	@Override
	public void initLuaRunState(LuaSerializer ls, LuaRunState lrs) {
		super.initLuaRunState(ls, lrs);
						
		LuaState vm = lrs.vm;
		try {
			LuaUtil.registerClass(lrs, vm, FreeRotationGS.class);
		} catch (LuaException e) {
			onScriptError(e);
		}
		
		BitmapTween.install(vm._G, (ImageFactory)getImageFactory(), getNotifier());
		GLSLPS.install(vm._G, (ImageFactory)getImageFactory(), getNotifier());
		
		ls.setEnvironment(new DefaultEnvironment());
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
		
		notifier.e(message, t);
		
		setWait(60);		
	}

	@Override
	protected void addKeyCodeConstants(LTable table) {
		Keys keys = keyConfig.getKeys();
		for (Entry<String, Integer> entry : keys) {
			table.put(entry.getKey(), entry.getValue());
		}
	}
	
	//Getters
	
	//Setters
	
}
