package nl.weeaboo.nvlist.menu;

import static nl.weeaboo.game.BaseGameConfig.MUSIC_VOLUME;
import static nl.weeaboo.game.BaseGameConfig.SOUND_VOLUME;
import static nl.weeaboo.game.BaseGameConfig.VOICE_VOLUME;

import java.awt.Component;

import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;

import nl.weeaboo.nvlist.Game;
import nl.weeaboo.settings.ConfigPropertyListener;
import nl.weeaboo.settings.Preference;

public class GameMenuFactory {

	private final Game game;
	private final ConfigListener configListener;
	
	public GameMenuFactory(Game game) {
		this.game = game;
		this.configListener = new ConfigListener();
		
		//game.getConfig().addPropertyListener(configListener);
	}
	
	//Functions
	public void dispose() {
		game.getConfig().removePropertyListener(configListener);
	}
	
	public static JMenuBar createPlaceholderJMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JPopupMenu popup = new JPopupMenu();
		popup.setLightWeightPopupEnabled(false);
		menuBar.setComponentPopupMenu(popup);

		GameMenu menu = new JGameMenu(null, "Game", 'G');
		menu.add(new QuitItem());
		menuBar.add((Component)menu);
				
		return menuBar;
	}
	
	public JMenuBar createJMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JPopupMenu popup = new JPopupMenu();
		popup.setLightWeightPopupEnabled(false);
		menuBar.setComponentPopupMenu(popup);

		GameMenu[] menus = createMenus();
		configListener.setGameMenus(menus);
		for (GameMenu gm : menus) {
			menuBar.add((Component)gm);
		}
		
		return menuBar;
	}
	
	protected GameMenu[] createMenus() {
		return new GameMenu[] {
			createGameMenu(),
			createTextMenu(),
			createImageMenu(),
			createSoundMenu(),
			createWindowMenu(),
			createAdvancedMenu(),
			createInfoMenu()
		};
	}
	
	protected GameMenu createGameMenu() {
		GameMenu menu = createMenu("Game", 'G');
		menu.add(SaveLoadItem.createLoadItem());
		menu.add(SaveLoadItem.createSaveItem());
		menu.addSeparator();
		menu.add(new RestartItem());
		
		if (!game.getDisplay().isEmbedded()) {
			menu.add(new QuitItem());
		}
		
		return menu;
	}

	protected GameMenu createTextMenu() {
		GameMenu menu = createMenu("Text", 'T');
		menu.add(new TextSpeedMenu());
		menu.add(new AutoReadWaitMenu());
		menu.add(new AutoReadItem());
		menu.addSeparator();
		menu.add(new TextLogItem());		
		return menu;
	}
	
	protected GameMenu createImageMenu() {
		GameMenu menu = createMenu("Image", 'I');
		menu.add(new EffectSpeedMenu());
		menu.addSeparator();
		menu.add(new ViewCGItem());		
		menu.add(new ScreenshotItem());		
		return menu;
	}
	
	protected GameMenu createSoundMenu() {
		GameMenu menu = createMenu("Sound", 'S');
		menu.add(new AudioVolumeMenu(MUSIC_VOLUME, "Music Volume"));
		menu.add(new AudioVolumeMenu(SOUND_VOLUME, "Sound Volume"));
		menu.add(new AudioVolumeMenu(VOICE_VOLUME, "Voice Volume"));
		return menu;
	}
	
	protected GameMenu createWindowMenu() {
		GameMenu menu = createMenu("Window", 'W');
		menu.add(new FullscreenItem());
		return menu;
	}
	
	protected GameMenu createAdvancedMenu() {
		GameMenu menu = createMenu("Advanced", 'A');
		menu.add(new ImageCacheMenu());
		menu.add(new MaxTexSizeMenu());
		menu.add(new FBOMenu());
		menu.add(new GLSLItem());
		menu.add(new VSyncItem());
		return menu;
	}
	
	protected GameMenu createInfoMenu() {
		GameMenu menu = createMenu("Info", '\0');
		menu.add(new AboutItem());
		return menu;
	}
	
	protected GameMenu createMenu(String label, char mnemonic) {
		return new JGameMenu(game, label, mnemonic);
	}
	
	//Inner Classes
	private static class ConfigListener implements ConfigPropertyListener {

		private GameMenu[] menus;
		
		public ConfigListener() {			
		}
		
		@Override
		public <T> void onPropertyChanged(Preference<T> pref, T oldval, T newval) {
			final GameMenu[] m = menus;
			if (m != null) {
				for (GameMenu gm : m) {
					gm.onPropertyChanged(pref, oldval, newval);
				}
			}
		}
		
		public void setGameMenus(GameMenu[] ms) {
			menus = ms.clone();
		}
		
	}
		
}
