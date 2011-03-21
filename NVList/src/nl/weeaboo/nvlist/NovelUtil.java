package nl.weeaboo.nvlist;

import static nl.weeaboo.game.BaseGameConfig.HEIGHT;
import static nl.weeaboo.game.BaseGameConfig.WIDTH;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nl.weeaboo.common.Dim;
import nl.weeaboo.filemanager.FileManager;
import nl.weeaboo.game.GameLog;
import nl.weeaboo.game.GameUtil;
import nl.weeaboo.settings.IConfig;
import nl.weeaboo.settings.INIFile;

public final class NovelUtil {
	
	public static Dim getImageSize(FileManager fm, IConfig config) {		
		int width = 0;
		int height = 0;
		try {
			InputStream in = fm.getInputStream(GameUtil.IMAGE_FOLDER + "img.ini");
			try {
				INIFile iniFile = new INIFile();
				iniFile.read(new BufferedReader(new InputStreamReader(in, "UTF-8"), 1024));
				width = iniFile.getInt("width", width);
				height = iniFile.getInt("height", height);
			} finally {
				in.close();
			}
		} catch (FileNotFoundException fnfe) {
			GameLog.v("No img.ini file found");
		} catch (IOException ioe) {
			GameLog.w("Error opening img.ini", ioe);
		}
		
		if (width <= 0) width = config.get(WIDTH);
		if (height <= 0) height = config.get(HEIGHT);
		
		return new Dim(width, height);
	}
	
}
