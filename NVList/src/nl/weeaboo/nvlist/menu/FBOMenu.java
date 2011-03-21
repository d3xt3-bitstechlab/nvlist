package nl.weeaboo.nvlist.menu;

import static nl.weeaboo.game.BaseGameConfig.FBO;
import static nl.weeaboo.game.BaseGameConfig.FBO_MIPMAP;

import java.awt.event.ActionEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import nl.weeaboo.nvlist.Game;
import nl.weeaboo.settings.IConfig;
import nl.weeaboo.vn.impl.nvlist.Novel;

public class FBOMenu extends GameMenuAction {
	
	//Functions
	@Override
	protected JMenuItem createItem(Game game, Novel nvl) {
		JMenu menu = new JMenu("Frame Buffer Object");
		
		ButtonGroup group = new ButtonGroup();
		
		JRadioButtonMenuItem offItem = new JRadioButtonMenuItem("Off");
		offItem.addActionListener(new SubItemActionListener(menu, 0));
		group.add(offItem);
		menu.add(offItem);
		
		JRadioButtonMenuItem onItem = new JRadioButtonMenuItem("On");
		onItem.addActionListener(new SubItemActionListener(menu, 1));
		group.add(onItem);
		menu.add(onItem);
		
		JRadioButtonMenuItem onAndMipmapItem = new JRadioButtonMenuItem("On + mipmap minification");
		onAndMipmapItem.addActionListener(new SubItemActionListener(menu, 2));
		group.add(onAndMipmapItem);
		menu.add(onAndMipmapItem);
		
		IConfig config = game.getConfig();
		if (config.get(FBO)) {
			if (config.get(FBO_MIPMAP)) {
				onAndMipmapItem.setSelected(true);
			} else {
				onItem.setSelected(true);
			}
		} else {
			offItem.setSelected(true);
		}
		
		return menu;
	}
	
	@Override
	public void actionPerformed(JMenuItem item, ActionEvent e, Game game, Novel nvl) {
		IConfig config = game.getConfig();
		if (e.getSource() instanceof Integer) {
			int index = (Integer)e.getSource();
			config.set(FBO, index >= 1);
			config.set(FBO_MIPMAP, index >= 2);
		}
	}

	//Getters
	
	//Setters
	
}
