package nl.weeaboo.nvlist.menu;

import static nl.weeaboo.nvlist.NovelPrefs.AUTO_READ_WAIT;
import nl.weeaboo.nvlist.Game;
import nl.weeaboo.settings.IConfig;
import nl.weeaboo.vn.impl.nvlist.Novel;

public class AutoReadWaitMenu extends RangeMenu<Integer> {

	private static final String labels[] = {
		"0 ms",
		"500 ms",
		"1000 ms",
		"2000 ms",
		"4000 ms"
	};
	
	private static final Integer values[] = {
		0,
		500,
		1000,
		2000,
		4000
	};
	
	public AutoReadWaitMenu(int currentAutoReadWait) {
		super("Auto Read Delay", '\0', labels, values, currentAutoReadWait);
	}

	@Override
	protected void onItemSelected(Game game, Novel nvl, int index, String label, Integer value) {
		IConfig config = game.getConfig();
		config.set(AUTO_READ_WAIT, value);
	}
	
}
