package nl.weeaboo.nvlist;

import static nl.weeaboo.game.BaseGameConfig.HEIGHT;
import static nl.weeaboo.game.BaseGameConfig.TITLE;
import static nl.weeaboo.game.BaseGameConfig.WIDTH;
import static nl.weeaboo.nvlist.NovelPrefs.AUTO_READ;
import static nl.weeaboo.nvlist.NovelPrefs.AUTO_READ_WAIT;
import static nl.weeaboo.nvlist.NovelPrefs.EFFECT_SPEED;
import static nl.weeaboo.nvlist.NovelPrefs.ENGINE_MIN_VERSION;
import static nl.weeaboo.nvlist.NovelPrefs.TEXT_SPEED;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

import nl.weeaboo.awt.AwtUtil;
import nl.weeaboo.common.Dim;
import nl.weeaboo.filemanager.FileManager;
import nl.weeaboo.game.BaseGame;
import nl.weeaboo.game.DebugPanel;
import nl.weeaboo.game.GameDisplay;
import nl.weeaboo.game.GameLog;
import nl.weeaboo.game.IGameDisplay;
import nl.weeaboo.game.RenderMode;
import nl.weeaboo.game.input.IKeyConfig;
import nl.weeaboo.game.input.UserInput;
import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.GLResourceCache;
import nl.weeaboo.gl.shader.ShaderCache;
import nl.weeaboo.gl.text.FontManager;
import nl.weeaboo.gl.text.GLTextRendererStore;
import nl.weeaboo.gl.text.ParagraphRenderer;
import nl.weeaboo.gl.texture.TextureCache;
import nl.weeaboo.lua.LuaException;
import nl.weeaboo.lua.io.LuaSerializer;
import nl.weeaboo.nvlist.debug.DebugImagePanel;
import nl.weeaboo.nvlist.debug.DebugLuaPanel;
import nl.weeaboo.nvlist.debug.DebugOutputPanel;
import nl.weeaboo.nvlist.menu.GameMenuFactory;
import nl.weeaboo.settings.IConfig;
import nl.weeaboo.sound.SoundManager;
import nl.weeaboo.vn.IAnalytics;
import nl.weeaboo.vn.IImageState;
import nl.weeaboo.vn.IInput;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.INovelConfig;
import nl.weeaboo.vn.IPersistentStorage;
import nl.weeaboo.vn.IScriptFactory;
import nl.weeaboo.vn.ISeenLog;
import nl.weeaboo.vn.IStorage;
import nl.weeaboo.vn.ITextState;
import nl.weeaboo.vn.IVideoState;
import nl.weeaboo.vn.impl.base.BaseAnalytics;
import nl.weeaboo.vn.impl.base.BaseNovelConfig;
import nl.weeaboo.vn.impl.base.NullAnalytics;
import nl.weeaboo.vn.impl.base.PreloaderData;
import nl.weeaboo.vn.impl.lua.EnvLuaSerializer;
import nl.weeaboo.vn.impl.nvlist.Analytics;
import nl.weeaboo.vn.impl.nvlist.Globals;
import nl.weeaboo.vn.impl.nvlist.GuiFactory;
import nl.weeaboo.vn.impl.nvlist.ImageFactory;
import nl.weeaboo.vn.impl.nvlist.ImageFxFactory;
import nl.weeaboo.vn.impl.nvlist.ImageState;
import nl.weeaboo.vn.impl.nvlist.InputAdapter;
import nl.weeaboo.vn.impl.nvlist.Movie;
import nl.weeaboo.vn.impl.nvlist.Novel;
import nl.weeaboo.vn.impl.nvlist.NovelNotifier;
import nl.weeaboo.vn.impl.nvlist.Renderer;
import nl.weeaboo.vn.impl.nvlist.SaveHandler;
import nl.weeaboo.vn.impl.nvlist.ScriptFactory;
import nl.weeaboo.vn.impl.nvlist.SeenLog;
import nl.weeaboo.vn.impl.nvlist.SoundFactory;
import nl.weeaboo.vn.impl.nvlist.SoundState;
import nl.weeaboo.vn.impl.nvlist.SystemVars;
import nl.weeaboo.vn.impl.nvlist.TextState;
import nl.weeaboo.vn.impl.nvlist.VideoFactory;
import nl.weeaboo.vn.impl.nvlist.VideoState;

public class Game extends BaseGame {

	public static final int VERSION_MAJOR = 2;
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
			FontManager fontman, TextureCache tc, ShaderCache sc, GLResourceCache rc,
			GLTextRendererStore trs, SoundManager sm, UserInput in, IKeyConfig kc)
	{
		super(cfg, e, gd, fm, fontman, tc, sc, rc, trs, sm, in, kc);
		
		gd.setJMenuBar(GameMenuFactory.createPlaceholderJMenuBar()); //Forces GameDisplay to use a JFrame
		gd.setRenderMode(RenderMode.MANUAL);
		
		pr = trs.createParagraphRenderer();
	}

	//Functions
	@Override
	public void stop(final boolean force, final Runnable onStop) {
		super.stop(force, new Runnable() {
			public void run() {				
				if (novel != null) {
					novel.savePersistent();
					novel.reset();
					novel = null;
				}
				if (gmf != null) {
					gmf.dispose();
					gmf = null;
				}
				
				if (onStop != null) {
					onStop.run();
				}				
			}
		});
	}
	
	@Override
	public void start() {		
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
		GLResourceCache resCache = getGLResourceCache();
		ShaderCache shCache = getShaderCache();		
		GLTextRendererStore trStore = getTextRendererStore();
		SoundManager sm = getSoundManager();
		INovelConfig novelConfig = new BaseNovelConfig(config.get(TITLE), config.get(WIDTH), config.get(HEIGHT));
		Dim imgSize = NovelUtil.getImageSize(fm, config);
		Dim nvlSize = new Dim(novelConfig.getWidth(), novelConfig.getHeight());
				
		NovelNotifier notifier = new NovelNotifier(getNotifier());
		SaveHandler saveHandler = new SaveHandler(fm, notifier);
		
		IPersistentStorage sysVars = new SystemVars(fm, "sysvars.bin", notifier);
		try {
			sysVars.load();
		} catch (IOException ioe) {
			notifier.fnf("Error loading sysVars", ioe);
			try { sysVars.save(); } catch (IOException e) { }
		}
		
		ISeenLog seenLog = new SeenLog(fm, "seen.bin");
		try {
			seenLog.load();
		} catch (IOException ioe) {
			notifier.fnf("Error loading seenLog", ioe);
			try { seenLog.save(); } catch (IOException e) { }
		}
		
		IAnalytics an;
		if (!isDebug()) {
			an = new NullAnalytics();
		} else {
			an = new Analytics(fm, "analytics.bin", notifier);
			try {
				an.load();
			} catch (IOException ioe) {
				notifier.fnf("Error loading analytics", ioe);
				try { an.save(); } catch (IOException e) { }
			}
		}
		
		ImageFactory imgfac = new ImageFactory(texCache, shCache, trStore,
				an, seenLog, notifier, imgSize.w, imgSize.h, nvlSize.w, nvlSize.h);
		ImageFxFactory imgfxfac = new ImageFxFactory(imgfac);
		SoundFactory sndfac = new SoundFactory(sm, an, seenLog, notifier);
		VideoFactory vidfac = new VideoFactory(fm, texCache, resCache, seenLog, notifier);		
		GuiFactory guifac = new GuiFactory(this);
		IScriptFactory scrfac = new ScriptFactory(fm, notifier);
		
		ImageState is = new ImageState(nvlSize.w, nvlSize.h);		
		SoundState ss = new SoundState(sndfac);
		VideoState vs = new VideoState();
		TextState ts = new TextState();
		IInput in = new InputAdapter(getInput());	
		IStorage globals = new Globals();
				
		novel = new Novel(novelConfig, imgfac, is, imgfxfac, sndfac, ss, vidfac, vs, ts,
				notifier, in, guifac, saveHandler, scrfac, sysVars, globals, seenLog, an,
				fm, getKeyConfig());
        luaSerializer = new EnvLuaSerializer();
        saveHandler.setNovel(novel, luaSerializer);
        
		super.start();
        
		restart("main");
		
		onConfigPropertiesChanged(); //Needs to be called again now novel is initialized		
		generatePreloaderData(); //Generate a preloader info from analytics
	}
	
	public void restart() {
		restart("titlescreen");
	}
	protected void restart(final String mainFunc) {		
		novel.restart(luaSerializer, mainFunc);

		onConfigPropertiesChanged();
	}
	
	@Override
	public boolean update(UserInput input, float dt) {
		boolean changed = super.update(input, dt);

		IGameDisplay display = getDisplay();
		boolean allowMenuBarToggle = display.isEmbedded() || display.isFullscreen();
		
		if (display.isMenuBarVisible()) {
			if (allowMenuBarToggle
				&& (input.consumeMouse() || display.isFullscreenExclusive()))
			{
				display.setMenuBarVisible(false);
			}
		} else if (!allowMenuBarToggle) {
			if (display.isFullscreenExclusive()) {
				display.setFullscreen(false);
			}
			display.setMenuBarVisible(true);
		}
		
		changed |= novel.update();
		
		if (novel.getInput().consumeCancel()) {
			if (display.isMenuBarVisible() && allowMenuBarToggle) {
				display.setMenuBarVisible(false);
			} else {
				if (display.isFullscreenExclusive()) {
					display.setFullscreen(false);
				}

				GameMenuFactory gameMenu = new GameMenuFactory(this);
				display.setJMenuBar(gameMenu.createJMenuBar());
				display.setMenuBarVisible(true);
			}
		}
		
		if (isDebug()) {
			if (input.consumeKey(KeyEvent.VK_ASTERISK)) {
				int a = 0;
				a = 0 / a; //Boom shakalaka
			}
			
			if (input.consumeKey(KeyEvent.VK_F2)) {
				novel.printStackTrace(System.out);
			} else if (input.consumeKey(KeyEvent.VK_F3)) {				
				IAnalytics an = novel.getAnalytics();
				if (an instanceof BaseAnalytics) {
					getNotifier().addMessage(this, "Dumping analytics");
					
					BaseAnalytics ba = (BaseAnalytics)an;
					
					try {
						OutputStream out = getFileManager().getOutputStream("imageload.csv");
						try {
							out.write(ba.getImageLoadCSV().getBytes("UTF-8"));
						} finally {
							out.close();
						}
					} catch (IOException ioe) {
						GameLog.w("Error dumping analytics", ioe);
					}
					try {
						OutputStream out = getFileManager().getOutputStream("soundload.csv");
						try {
							out.write(ba.getSoundLoadCSV().getBytes("UTF-8"));
						} finally {
							out.close();
						}
					} catch (IOException ioe) {
						GameLog.w("Error dumping analytics", ioe);
					}
					
					generatePreloaderData();
				}
			}
		}
		
		IVideoState vs = novel.getVideoState();
		changed |= vs.isBlocking();
		
		return changed;
	}
	
	protected void generatePreloaderData() {
		IAnalytics an = novel.getAnalytics();
		if (an instanceof BaseAnalytics) {
			BaseAnalytics ba = (BaseAnalytics)an;
			try {
				PreloaderData pd = new PreloaderData();
				ba.getPreloaderData(pd);						
				ObjectOutputStream out = new ObjectOutputStream(
						getFileManager().getOutputStream("preloader.bin"));
				try {
					out.writeObject(pd);
				} finally {
					out.close();
				}
			} catch (IOException ioe) {
				GameLog.w("Error dumping analytics", ioe);
			}
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
	        renderer.render(null);
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
				ts.setBaseTextSpeed(config.get(TEXT_SPEED));
			}
			
			novel.setScriptDebug(isDebug());
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
		
		getDisplay().repaint();
	}
	
	@Override
	protected DebugPanel createDebugPanel() {
		DebugPanel debugPanel = super.createDebugPanel();
		debugPanel.addTab("Lua", new DebugLuaPanel(this, getNovel()));
		debugPanel.addTab("Image", new DebugImagePanel(this, getNovel()));
		debugPanel.addTab("Log Output", new DebugOutputPanel(this, getNovel()));
		return debugPanel;
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
