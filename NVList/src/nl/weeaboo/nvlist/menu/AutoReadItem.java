package nl.weeaboo.nvlist.menu;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import nl.weeaboo.nvlist.Game;
import nl.weeaboo.settings.IConfig;
import nl.weeaboo.settings.Preference;
import nl.weeaboo.vn.impl.nvlist.Novel;

public class AutoReadItem extends GameMenuAction {

	private final IConfig config;
	private final Preference<Boolean> pref;	
	
	public AutoReadItem(IConfig config, Preference<Boolean> pref) {
		this.config = config;
		this.pref = pref;
	}
	
	@Override
	public JMenuItem createItem(Game game, Novel nvl) {
		JCheckBoxMenuItem item = new JCheckBoxMenuItem("Auto Read");
		item.setSelected(config.get(pref));
		return item;
	}

	@Override
	public void actionPerformed(JMenuItem item, ActionEvent e, Game game, Novel nvl) {
		config.set(pref, item.isSelected());
	}

	@Override
	public <T> void onPropertyChanged(JMenuItem item, Preference<T> p, T oldval, T newval) {
		if (pref.equals(p)) {
			((JCheckBoxMenuItem)item).setSelected(config.get(pref));
		}
	}

}
