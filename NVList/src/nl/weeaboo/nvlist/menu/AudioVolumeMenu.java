package nl.weeaboo.nvlist.menu;

import nl.weeaboo.nvlist.Game;
import nl.weeaboo.settings.IConfig;
import nl.weeaboo.settings.Preference;
import nl.weeaboo.vn.impl.nvlist.Novel;

public class AudioVolumeMenu extends RangeMenu<Double> {

	private static final String labels[] = {
		"0%", "10%", "20%", "30%", "40%", "50%", "60%", "70%", "80%", "90%", "100%"
	};
	
	private static final Double values[] = {
		0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0
	};
	
	private final Preference<Double> pref;
	
	public AudioVolumeMenu(Preference<Double> pref, String label, double currentValue) {
		super(label, '\0', labels, values, currentValue);
		
		this.pref = pref;
	}

	@Override
	protected void onItemSelected(Game game, Novel nvl, int index, String label, Double value) {
		IConfig config = game.getConfig();
		config.set(pref, value);
	}
	
}
