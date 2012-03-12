package org.spoutcraft.launcher;

import java.io.File;
import java.io.FileNotFoundException;

import org.bukkit.util.config.Configuration;

public class MinecraftYML{
	private static final String					MINECRAFT_YML	= "minecraft.yml";
	private static String								latest				= null;
	private static String								recommended		= null;
	private static final Object					key = new Object();
	private static boolean 							updated = false;

	private static final CachedYMLFile 	ymlFile;
	
	static {
		ymlFile = new CachedYMLFile(MINECRAFT_YML, null, 
				new File(GameUpdater.modpackDir, MINECRAFT_YML));
	}
	
	public static Configuration getMinecraftYML() throws FileNotFoundException {
		return ymlFile.getConfig();
	}

	
	/**
	 * @return false if cache couldn't be updated(but an offline version exists)
	 * @throws FileNotFoundException if minecraft.yml is not available
	 */
	public static boolean updateMinecraftYMLCache() throws FileNotFoundException {
		if (!updated) {
			return true;
		}
		updated = false;
		
		
		boolean downloaded;
		
		synchronized (key) {

			Configuration config;
			String current;
			
			if (ymlFile.hasConfig()) {
				config = ymlFile.getConfig();
				current = config.getString("current");
			} else {
				current = null;
			}
			
			downloaded = ymlFile.updateYMLCache(false);
			
			config = ymlFile.getConfig();
			// GameUpdater.copy(getConfigFile(), output)
			latest = config.getString("latest");
			recommended = config.getString("recommended");
			if (current != null) {
				config.setProperty("current", current);
				config.save();
			}
		}
		
		return downloaded;
	}


	
	public static String getLatestMinecraftVersion() throws FileNotFoundException {
		updateMinecraftYMLCache();
		return latest;
	}

	public static String getRecommendedMinecraftVersion() throws FileNotFoundException {
		updateMinecraftYMLCache();
		return recommended;
	}

	public static void setInstalledVersion(String version) throws FileNotFoundException  {
		Configuration config = ymlFile.getConfig();
		config.setProperty("current", version);
		config.save();
	}

	public static String getInstalledVersion() throws FileNotFoundException {
		Configuration config = ymlFile.getConfig();
		return config.getString("current");
	}
}

