package nl.weeaboo.vn;

import static nl.weeaboo.settings.Preference.newPreference;
import static nl.weeaboo.styledtext.TextStylePreference.newPreference;
import nl.weeaboo.settings.Preference;
import nl.weeaboo.styledtext.FontStyle;
import nl.weeaboo.styledtext.MutableTextStyle;
import nl.weeaboo.styledtext.TextStyle;

public final class NovelPrefs {

	public static final Preference<Boolean> SCRIPT_DEBUG = newPreference("vn.scriptDebug", false, "Script Debug", "Certain functions detect and warn about additional errors when script debug is turned on.");
	public static final Preference<Integer> SAVE_SCREENSHOT_WIDTH = newPreference("vn.saveScreenshotWidth", 224, "Save Screenshot Width", "Width (in pixels) to store the save slot screenshots at.");
	public static final Preference<Integer> SAVE_SCREENSHOT_HEIGHT = newPreference("vn.saveScreenshotHeight", 126, "Save Screenshot Height", "Height (in pixels) to store the save slot screenshots at.");
	public static final Preference<TextStyle> TEXT_STYLE = newPreference("vn.textStyle", new TextStyle(null, FontStyle.PLAIN, 30), "Default Text Style", "The default style to be used for rendered text.");
	public static final Preference<TextStyle> TEXT_READ_STYLE = newPreference("vn.textReadStyle", TextStyle.defaultInstance(), "Read Text Style", "The text style to use for previously read text");
	public static final Preference<TextStyle> TEXT_LOG_STYLE = newPreference("vn.textLogStyle", coloredTextStyle(0xFFFFFF80), "Text Log Style", "The text style to use for the text log.");
	public static final Preference<TextStyle> CHOICE_STYLE = newPreference("vn.choiceStyle", TextStyle.defaultInstance(), "Choice Style", "Text style used for options of choices");
	public static final Preference<TextStyle> SELECTED_CHOICE_STYLE = newPreference("vn.selectedChoiceStyle", coloredTextStyle(0xFF808080), "Selected Choice Style", "Text style used for previously chosen options of choices.");
	public static final Preference<Double>  TEXT_SPEED = newPreference("vn.textSpeed", 0.5, "Text Speed", "The text fade-in speed in characters per frame.");
	public static final Preference<Double>  EFFECT_SPEED = newPreference("vn.effectSpeed", 1.0, "Effect Speed", "Effect speed modifier. The base effect speed is multiplied by the specified amount.");
	public static final Preference<Boolean> AUTO_READ = newPreference("vn.autoRead", false, "Auto Read", "Toggles auto read mode. In this mode, all wait-for-clicks are replaced by a timed wait.");
	public static final Preference<Integer> AUTO_READ_WAIT = newPreference("vn.autoReadWait", 1000, "Auto Read Wait", "The wait time (in milliseconds) for the timed waits used by auto read mode.");
	public static final Preference<Boolean> SKIP_UNREAD = newPreference("vn.skipUnread", true, "Skip Unread", "If set to false, skip mode will stop at unread text.");
	public static final Preference<Integer> TEXTLOG_PAGE_LIMIT = newPreference("vn.textLogPageLimit", 50, "Textlog Page Limit", "The number of pages the textlog keeps in memory.");
	public static final Preference<Integer> PRELOADER_LOOK_AHEAD = newPreference("vn.preloaderLookAhead", 30, "Preloader Lookahead", "The number of lines the preloader looks ahead to determine what to preload.");
	public static final Preference<Integer> PRELOADER_MAX_PER_LINE = newPreference("vn.preloaderMaxPerLine", 3, "Preloader Max Per Line", "The maximum number of items the preloader is allowed to preload based on a single script line. This limit prevents the preloader from choking the system by preloading a very large number of images/sounds at once.");
	public static final Preference<Double>  MUSIC_VOLUME = newPreference("vn.musicVolume", 0.7, "Music Volume", "Volume (between 0.0 and 1.0) of background music.");
	public static final Preference<Double>  SOUND_VOLUME = newPreference("vn.soundVolume", 0.8, "Sound Volume", "Volume (between 0.0 and 1.0) of sound effects.");
	public static final Preference<Double>  VOICE_VOLUME = newPreference("vn.voiceVolume", 1.0, "Voice Volume", "Volume (between 0.0 and 1.0) of voices.");
	public static final Preference<String>  ENGINE_MIN_VERSION = newPreference("vn.engineMinVersion", "4.0", "Engine Minimum Version", "The minimum allowable version of NVList that can be used to read your novel. Raises an error if the current version is less than the required version.");
	public static final Preference<String>  ENGINE_TARGET_VERSION = newPreference("vn.engineTargetVersion", "4.0", "Engine Target Version", "The version of NVList this VN was created for.");
	public static final Preference<Integer> TIMER_IDLE_TIMEOUT = newPreference("vn.timerIdleTimeout", 30, "Timer Idle Timeout", "The number of seconds of user inactivity that are tolerated before the playtime timer is stopped.");
	public static final Preference<Boolean> ENABLE_PROOFREADER_TOOLS = newPreference("vn.enableProofreaderTools", false, "Enable Proofreader Tools", "Enables available bug reporting features for proofreaders/editors.");
	public static final Preference<Boolean> RTL = newPreference("vn.rtl", false, "Right-to-Left Text", "Sets the default text direction to RTL (right to left).");

	private NovelPrefs() {
	}

	private static TextStyle coloredTextStyle(int argb) {
		MutableTextStyle mts = new MutableTextStyle();
		mts.setColor(argb);
		return mts.immutableCopy();
	}

}
