package nl.weeaboo.nvlist.build.android;

import static nl.weeaboo.nvlist.build.android.AndroidConfig.FOLDER;
import static nl.weeaboo.nvlist.build.android.AndroidConfig.ICON;
import static nl.weeaboo.nvlist.build.android.AndroidConfig.LVL_KEY_BASE64;
import static nl.weeaboo.nvlist.build.android.AndroidConfig.PACKAGE;
import static nl.weeaboo.nvlist.build.android.AndroidConfig.TITLE;
import static nl.weeaboo.nvlist.build.android.AndroidConfig.XAPK_MAIN_LENGTH;
import static nl.weeaboo.nvlist.build.android.AndroidConfig.XAPK_MAIN_VERSION;
import static nl.weeaboo.nvlist.build.android.AndroidConfig.XAPK_PATCH_LENGTH;
import static nl.weeaboo.nvlist.build.android.AndroidConfig.XAPK_PATCH_VERSION;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import nl.weeaboo.io.FileUtil;
import nl.weeaboo.nvlist.build.Build;

public class AndroidProjectCompiler {
	
	private final File gameFolder;
	private final File templateFolder;
	private final File dstFolder;
	private final Map<String, FileHandler> handlers;
	
	//TODO: Afterwards, app must still be signed: http://developer.android.com/tools/publishing/app-signing.html
	//TODO: How do I pass properties from the game to this? Make an Ant task? Then I can use the property expansion like installer-config.ini
	//TODO: Do I overwrite the template? That would make it easier to update an existing template.
        
	public AndroidProjectCompiler(File gameF, File templateF, File dstF, AndroidConfig config) {
		this.gameFolder = gameF;
		this.templateFolder = templateF;
		this.dstFolder = dstF;
		
		handlers = new HashMap<String, FileHandler>();
		
		handlers.put(null, Handlers.getDefault());
		
		handlers.put("src/nl/weeaboo/android/nvlist/ExpansionConstants.java", Handlers.expansionConstants(
				config.get(LVL_KEY_BASE64), config.get(XAPK_MAIN_VERSION),  config.get(XAPK_MAIN_LENGTH),
				config.get(XAPK_PATCH_VERSION),  config.get(XAPK_PATCH_LENGTH)));
		
		handlers.put("AndroidManifest.xml", Handlers.androidManifestHandler(config.get(PACKAGE)));
		
		handlers.put("res/values/strings.xml", Handlers.stringResHandler(
				config.get(TITLE), config.get(FOLDER)));
		
		handlers.put("res/drawable", Handlers.drawableHandler(new File(gameFolder, config.get(ICON))));
	}
	
	//Functions
	private static void printUsage() {
		System.err.println("Usage: java -cp Build.jar " + AndroidProjectCompiler.class.getName() + " <game> <template> <dst>");
	}
	
	public static void main(String[] args) throws IOException {
		File gameF = new File(args[0]);
		File templateF = new File(args[1]);
		File dstF = new File(args[2]);
		
		if (!gameF.exists() || !templateF.exists()) {
			if (!gameF.exists()) System.err.println("Folder doesn't exist: " + gameF);
			if (!templateF.exists()) System.err.println("Folder doesn't exist: " + templateF);
			printUsage();
			System.exit(1);
			return;
		}
		
		AndroidConfig config = AndroidConfig.fromFile(new File(gameF, Build.PATH_ANDROID_INI));
		
		AndroidProjectCompiler compiler = new AndroidProjectCompiler(gameF, templateF, dstF, config);
		compiler.compile();
	}
	
	public void compile() throws IOException {
		if (dstFolder.exists()) {
			String[] children = dstFolder.list();
			if (children != null && children.length > 0) {
				throw new IOException("Destination folder exists and is non-empty: " + dstFolder + ". Please delete it manually and retry.");
			}
		}
		
		Map<String, File> files = new TreeMap<String, File>();
		FileUtil.collectFiles(files, templateFolder, false, true, true);
		for (Entry<String, File> entry : files.entrySet()) {
			String relpath = entry.getKey();
			File templateF = entry.getValue();						
			File dstF = new File(dstFolder, relpath);
			
			if (templateF.isDirectory()) {
				templateF.mkdirs();
			} else {
				FileHandler handler = handlers.get(relpath);
				if (handler == null) {
					if (relpath.startsWith("res/drawable")) {
						handler = handlers.get("res/drawable");
					}
					if (handler == null) {
						handler = handlers.get(null);
					}
				}
				handler.process(relpath, templateF, dstF);
			}
		}
	}
	
	//Getters
	
	//Setters
	
}
