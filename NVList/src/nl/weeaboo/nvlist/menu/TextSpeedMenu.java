package nl.weeaboo.nvlist.menu;

import static nl.weeaboo.nvlist.NovelPrefs.TEXT_SPEED;
import nl.weeaboo.nvlist.Game;
import nl.weeaboo.settings.IConfig;
import nl.weeaboo.vn.impl.nvlist.Novel;

public class TextSpeedMenu extends RangeMenu<Double> {

	private static final double baseSpeed = 0.5;
	
	private static final String labels[] = {
		"Slower",
		"Slow",
		"Normal",
		"Fast",
		"Faster",
		"Instant"
	};
	
	private static final Double values[] = {
		0.25 * baseSpeed,
		0.50 * baseSpeed,
		1.00 * baseSpeed,
		2.00 * baseSpeed,
		4.00 * baseSpeed,
		999999.0
	};
	
	public TextSpeedMenu(double currentTextSpeed) {
		super("Text Speed", '\0', labels, values, currentTextSpeed);
	}

	@Override
	protected void onItemSelected(Game game, Novel nvl, int index, String label, Double value) {
		IConfig config = game.getConfig();
		config.set(TEXT_SPEED, value);
	}
	
}
