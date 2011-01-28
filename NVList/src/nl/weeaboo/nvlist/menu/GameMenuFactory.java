package nl.weeaboo.nvlist.menu;

import static nl.weeaboo.game.BaseGameConfig.MUSIC_VOLUME;
import static nl.weeaboo.game.BaseGameConfig.SOUND_VOLUME;
import static nl.weeaboo.game.BaseGameConfig.VOICE_VOLUME;
import static nl.weeaboo.nvlist.NovelPrefs.AUTO_READ;
import static nl.weeaboo.nvlist.NovelPrefs.AUTO_READ_WAIT;
import static nl.weeaboo.nvlist.NovelPrefs.EFFECT_SPEED;
import static nl.weeaboo.nvlist.NovelPrefs.TEXT_SPEED;

import java.awt.Component;

import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;

import nl.weeaboo.nvlist.Game;
import nl.weeaboo.settings.ConfigPropertyListener;
import nl.weeaboo.settings.IConfig;
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
		IConfig config = game.getConfig();
		
		GameMenu menu = createMenu("Text", 'T');
		menu.add(new TextSpeedMenu(config.get(TEXT_SPEED)));
		menu.add(new AutoReadWaitMenu(config.get(AUTO_READ_WAIT)));
		menu.add(new AutoReadItem(config, AUTO_READ));
		menu.addSeparator();
		menu.add(new TextLogItem());		
		return menu;
	}
	
	protected GameMenu createImageMenu() {
		IConfig config = game.getConfig();
		
		GameMenu menu = createMenu("Image", 'I');
		menu.add(new EffectSpeedMenu(config.get(EFFECT_SPEED)));
		menu.addSeparator();
		menu.add(new ViewCGItem());		
		return menu;
	}
	
	protected GameMenu createSoundMenu() {
		IConfig config = game.getConfig();
		
		GameMenu menu = createMenu("Sound", 'S');
		menu.add(new AudioVolumeMenu(MUSIC_VOLUME, "Music Volume", config.get(MUSIC_VOLUME)));
		menu.add(new AudioVolumeMenu(SOUND_VOLUME, "Sound Volume", config.get(SOUND_VOLUME)));
		menu.add(new AudioVolumeMenu(VOICE_VOLUME, "Voice Volume", config.get(VOICE_VOLUME)));
		return menu;
	}
	
	protected GameMenu createWindowMenu() {
		GameMenu menu = createMenu("Window", 'W');
		menu.add(new FullscreenItem());
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
