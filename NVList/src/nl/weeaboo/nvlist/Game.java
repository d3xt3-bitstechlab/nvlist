package nl.weeaboo.nvlist;

import static nl.weeaboo.game.BaseGameConfig.DEBUG;
import static nl.weeaboo.game.BaseGameConfig.HEIGHT;
import static nl.weeaboo.game.BaseGameConfig.TITLE;
import static nl.weeaboo.game.BaseGameConfig.WIDTH;
import static nl.weeaboo.nvlist.NovelPrefs.AUTO_READ;
import static nl.weeaboo.nvlist.NovelPrefs.AUTO_READ_WAIT;
import static nl.weeaboo.nvlist.NovelPrefs.EFFECT_SPEED;
import static nl.weeaboo.nvlist.NovelPrefs.ENGINE_MIN_VERSION;
import static nl.weeaboo.nvlist.NovelPrefs.TEXT_SPEED;

import java.util.concurrent.ExecutorService;

import nl.weeaboo.awt.AwtUtil;
import nl.weeaboo.common.Dim;
import nl.weeaboo.filemanager.FileManager;
import nl.weeaboo.game.BaseGame;
import nl.weeaboo.game.GameDisplay;
import nl.weeaboo.game.IGameDisplay;
import nl.weeaboo.game.input.IKeyConfig;
import nl.weeaboo.game.input.UserInput;
import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.shader.ShaderCache;
import nl.weeaboo.gl.text.FontManager;
import nl.weeaboo.gl.text.GLTextRendererStore;
import nl.weeaboo.gl.text.ParagraphRenderer;
import nl.weeaboo.gl.texture.TextureCache;
import nl.weeaboo.lua.LuaException;
import nl.weeaboo.lua.io.LuaSerializer;
import nl.weeaboo.nvlist.menu.GameMenuFactory;
import nl.weeaboo.settings.IConfig;
import nl.weeaboo.sound.SoundManager;
import nl.weeaboo.vn.IImageState;
import nl.weeaboo.vn.IInput;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.INovelConfig;
import nl.weeaboo.vn.ISeenLog;
import nl.weeaboo.vn.ITextState;
import nl.weeaboo.vn.IVideoState;
import nl.weeaboo.vn.impl.base.BaseNovelConfig;
import nl.weeaboo.vn.impl.lua.EnvLuaSerializer;
import nl.weeaboo.vn.impl.nvlist.GuiFactory;
import nl.weeaboo.vn.impl.nvlist.ImageFactory;
import nl.weeaboo.vn.impl.nvlist.ImageState;
import nl.weeaboo.vn.impl.nvlist.InputAdapter;
import nl.weeaboo.vn.impl.nvlist.Movie;
import nl.weeaboo.vn.impl.nvlist.Novel;
import nl.weeaboo.vn.impl.nvlist.NovelNotifier;
import nl.weeaboo.vn.impl.nvlist.Renderer;
import nl.weeaboo.vn.impl.nvlist.SaveHandler;
import nl.weeaboo.vn.impl.nvlist.SoundFactory;
import nl.weeaboo.vn.impl.nvlist.SoundState;
import nl.weeaboo.vn.impl.nvlist.TextState;
import nl.weeaboo.vn.impl.nvlist.VideoFactory;
import nl.weeaboo.vn.impl.nvlist.VideoState;

public class Game extends BaseGame {

	public static final int VERSION_MAJOR = 1;
	public static final int VERSION_MINOR = 0;
	public static final int VERSION = 10000 * VERSION_MAJOR + 100 * VERSION_MINOR;
	public static final String VERSION_STRING = VERSION_MAJOR + "." + VERSION_MINOR;
	
	private final ParagraphRenderer pr;
	
	private Novel novel;
	private LuaSerializer luaSerializer;
	private GameMenuFactory gmf;
	private Renderer renderer;
	private Movie movie;
	
	public Game(IConfig cfg, ExecutorService e, GameDisplay gd, FileManager fm,
			FontManager fontman, TextureCache tc, ShaderCache sc, GLTextRendererStore trs,
			SoundManager sm, UserInput in, IKeyConfig kc)
	{
		super(cfg, e, gd, fm, fontman, tc, sc, trs, sm, in, kc);
		
		gd.setJMenuBar(GameMenuFactory.createPlaceholderJMenuBar()); //Forces GameDisplay to use a JFrame
		
		pr = trs.createParagraphRenderer();
	}

	//Functions
	@Override
	public boolean stop(boolean force) {
		if (force || askStop()) {
			if (novel != null) {
				novel.reset();
			}
			if (gmf != null) {
				gmf.dispose();
				gmf = null;
			}
			return super.stop(true);
		}
		return false;
	}
	
	@Override
	public void start() {
		super.start();
		
		IConfig config = getConfig();		
		if (VERSION_STRING.compareTo(config.get(ENGINE_MIN_VERSION)) < 0) {
			//Our version number is too old to run the game
			AwtUtil.showError(String.format( "NVList version number (%s) " +
				"is below the minimum acceptable version for this game (%s)",
				VERSION_STRING, config.get(ENGINE_MIN_VERSION)));			
		}
		
		if (gmf != null) {
			gmf.dispose();
		}
		gmf = new GameMenuFactory(this);		
		getDisplay().setJMenuBar(gmf.createJMenuBar());
		
		FileManager fm = getFileManager();
		TextureCache texCache = getTextureCache();
		ShaderCache shCache = getShaderCache();
		GLTextRendererStore trStore = getTextRendererStore();
		SoundManager sm = getSoundManager();
		INovelConfig novelConfig = new BaseNovelConfig(config.get(TITLE), config.get(WIDTH), config.get(HEIGHT));
		Dim imgSize = NovelUtil.getImageSize(fm, config);
		Dim nvlSize = new Dim(novelConfig.getWidth(), novelConfig.getHeight());
		
		NovelNotifier notifier = new NovelNotifier(getNotifier());
		SaveHandler saveHandler = new SaveHandler(fm, notifier);
		ISeenLog seenLog = saveHandler.getSeenLog();
		ImageFactory imgfac = new ImageFactory(texCache, shCache, trStore,
				seenLog, notifier, imgSize.w, imgSize.h, nvlSize.w, nvlSize.h);
		SoundFactory sndfac = new SoundFactory(sm, seenLog, notifier);
		VideoFactory vidfac = new VideoFactory(fm, texCache, seenLog, notifier);		
		GuiFactory guifac = new GuiFactory();

		ImageState is = new ImageState(nvlSize.w, nvlSize.h);		
		SoundState ss = new SoundState(sndfac);
		VideoState vs = new VideoState();
		TextState ts = new TextState();		
		IInput in = new InputAdapter(getInput());		
		
		novel = new Novel(novelConfig, imgfac, is, sndfac, ss, vidfac, vs, ts,
				notifier, in, guifac, saveHandler,
				fm, getKeyConfig(), texCache, shCache, trStore) {
			
			public boolean exit(boolean force) {
				return Game.this.stop(force);
			}
		};
        luaSerializer = new EnvLuaSerializer(novel);
        saveHandler.setNovel(novel, luaSerializer);
        
		restart("main");		
	}
	
	public void restart() {
		restart("titlescreen");
	}
	protected void restart(final String mainFunc) {		
		novel.restart(luaSerializer, mainFunc);
		onConfigPropertiesChanged();
	}
	
	/**
	 * Requests a repaint of the game display
	 */
	protected void repaint() {
		//Don't do anything, NVList only has active rendering right now...
	}
	
	@Override
	public void update(UserInput input, float dt) {
		super.update(input, dt);

		IGameDisplay display = getDisplay();
		boolean allowMenuBarToggle = display.isEmbedded() || display.isFullscreen();
		
		if (display.isMenuBarVisible()) {
			if (allowMenuBarToggle && input.consumeMouse()) {
				display.setMenuBarVisible(false);
			}
		} else if (!allowMenuBarToggle) {
			display.setMenuBarVisible(true);
		}
		
		boolean changed = false;
		
		changed |= novel.update();
		
		if (novel.getInput().consumeCancel()) {
			if (display.isMenuBarVisible() && allowMenuBarToggle) {
				display.setMenuBarVisible(false);
			} else {
				GameMenuFactory gameMenu = new GameMenuFactory(this);
				display.setJMenuBar(gameMenu.createJMenuBar());
				display.setMenuBarVisible(true);
			}
		}
		
		if (changed) {
			repaint();			
		}
	}
	
	@Override
	public void draw(GLManager glm) {
		IImageState is = novel.getImageState();
		IVideoState vs = novel.getVideoState();
		
		if (vs.isBlocking()) {
			Movie movie = (Movie)vs.getBlocking();
			movie.draw(glm, getWidth(), getHeight());
		} else {
			if (renderer == null) {
				renderer = new Renderer(glm, pr, getWidth(), getHeight(),
						getRealX(), getRealY(), getRealW(), getRealH(),
						getScreenW(), getScreenH());
			}
			is.draw(renderer);
	        renderer.render();
			renderer.reset();
		}
		
		super.draw(glm);
	}
	
	@Override
	public void onConfigPropertiesChanged() {
		super.onConfigPropertiesChanged();

		IConfig config = getConfig();
		if (novel != null) {
			INotifier ntf = novel.getNotifier();
			
			double effectSpeed = config.get(EFFECT_SPEED);
			novel.setEffectSpeed(effectSpeed, 8 * effectSpeed);
			
			ITextState ts = novel.getTextState();
			if (ts != null) {
				ts.setTextSpeed(config.get(TEXT_SPEED));
			}
			
			novel.setScriptDebug(config.get(DEBUG));
			try {
				novel.setAutoRead(config.get(AUTO_READ),
						60 * config.get(AUTO_READ_WAIT) / 1000);
			} catch (LuaException e) {
				ntf.w("Error occurred when changing auto read", e);
			}
			
			/*
			 * LightNVL volume settings aren't used, we instead use the ones in game-core.
			 * Because of this, we don't need to pass these settings to Novel.
			 */
		}
		
		repaint();
	}
	
	//Getters
	public Novel getNovel() { return novel; }
	
	//Setters
	@Override
	public void setScreenBounds(int rx, int ry, int rw, int rh, int sw, int sh) {
		if (rx != getRealX() || ry != getRealY() || rw != getRealW() || rh != getRealH()
				|| sw != getScreenW() || sh != getScreenH())
		{
			super.setScreenBounds(rx, ry, rw, rh, sw, sh);
			
			renderer = null;
			
			repaint();
		}
	}
	
	public void setMovie(Movie m) {
		if (movie != m) {
			if (movie != null) {
				movie.stop();
			}
			movie = m;
		}
	}
	
}
