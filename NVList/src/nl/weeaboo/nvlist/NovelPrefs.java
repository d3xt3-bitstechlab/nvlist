package nl.weeaboo.nvlist;

import static nl.weeaboo.settings.Preference.newPreference;
import nl.weeaboo.settings.Preference;

public final class NovelPrefs {

	public static final Preference<Double> TEXT_SPEED = newPreference("vn.textSpeed", 0.5, false);
	public static final Preference<Double> EFFECT_SPEED = newPreference("vn.effectSpeed", 1.0, false);
	public static final Preference<Boolean> AUTO_READ = newPreference("vn.autoRead", false, false);
	public static final Preference<Integer> AUTO_READ_WAIT = newPreference("vn.autoReadWait", 1000, false);
	public static final Preference<String> ENGINE_MIN_VERSION = newPreference("vn.engine.minVersion", "1.0", false);
	
	private NovelPrefs() {		
	}
	
}
