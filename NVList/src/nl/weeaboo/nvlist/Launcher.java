package nl.weeaboo.nvlist;

import java.util.concurrent.ExecutorService;

import nl.weeaboo.filemanager.FileManager;
import nl.weeaboo.game.GameDisplay;
import nl.weeaboo.game.IGame;
import nl.weeaboo.game.input.IKeyConfig;
import nl.weeaboo.game.input.UserInput;
import nl.weeaboo.gl.GLResourceCache;
import nl.weeaboo.gl.shader.ShaderCache;
import nl.weeaboo.gl.text.FontManager;
import nl.weeaboo.gl.text.GLTextRendererStore;
import nl.weeaboo.gl.texture.TextureCache;
import nl.weeaboo.settings.IConfig;
import nl.weeaboo.sound.SoundManager;
import nl.weeaboo.vn.impl.base.Obfuscator;

public class Launcher extends nl.weeaboo.game.Launcher {
	
	public Launcher() {		
		setObfuscator(Obfuscator.getInstance());
	}
	
	//Functions
	public static void main(String args[]) {		
		//nl.weeaboo.game.GameLog.getLogger().setLevel(java.util.logging.Level.FINE);
		
		main(new Launcher(), args);
	}
	
	@Override
	protected IGame newGame(IConfig config, ExecutorService executor, GameDisplay display,
			FileManager fm, FontManager fontManager, TextureCache tc, ShaderCache sc,
			GLResourceCache rc, GLTextRendererStore trs, SoundManager sm, UserInput in,
			IKeyConfig kc)
	{		
		return new Game(config, executor, display, fm, fontManager, tc, sc, rc, trs, sm, in, kc);
	}
	
	//Getters
	protected String[] getZipFilenames(String gameId) {
		return new String[] {"res.zip", gameId+".nvl"};		
	}
	
	//Setters
	
}
