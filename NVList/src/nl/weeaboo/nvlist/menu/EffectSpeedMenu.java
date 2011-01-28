package nl.weeaboo.nvlist.menu;

import static nl.weeaboo.nvlist.NovelPrefs.EFFECT_SPEED;
import nl.weeaboo.nvlist.Game;
import nl.weeaboo.settings.IConfig;
import nl.weeaboo.vn.impl.nvlist.Novel;

public class EffectSpeedMenu extends RangeMenu<Double> {

	private static final String labels[] = {
		"Slower",
		"Slow",
		"Normal",
		"Fast",
		"Faster",
		"Instant"
	};
	
	private static final Double values[] = {
		0.25,
		0.50,
		1.00,
		2.00,
		4.00,
		999.0
	};
	
	public EffectSpeedMenu(double currentEffectSpeed) {
		super("Effect Speed", '\0', labels, values, currentEffectSpeed);
	}

	@Override
	protected void onItemSelected(Game game, Novel nvl, int index, String label, Double value) {
		IConfig config = game.getConfig();
		config.set(EFFECT_SPEED, value);
	}
	
}
