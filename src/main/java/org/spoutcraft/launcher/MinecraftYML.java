package org.spoutcraft.launcher;

import java.io.File;
import java.io.FileNotFoundException;

import org.bukkit.util.config.Configuration;

public class MinecraftYML extends CachedYmlFile {
	private static final String			MINECRAFT_YML	= "minecraft.yml";
	private static final File			  CONFIG_FILE		= new File(GameUpdater.modpackDir, MINECRAFT_YML);
	
	private volatile boolean	updated				= false;
	private String						latest				= null;
	private String						recommended		= null;
	private final Object			key						= new Object();

	public MinecraftYML(MirrorDownloader downloader) throws FileNotFoundException {
		super(downloader, MINECRAFT_YML, null, CONFIG_FILE);
	}
	
	public String getLatestMinecraftVersion() throws FileNotFoundException {
		updateYMLCache();
		return latest;
	}

	public String getRecommendedMinecraftVersion() throws FileNotFoundException {
		updateYMLCache();
		return recommended;
	}

	public void setInstalledVersion(String version) throws FileNotFoundException {
		Configuration config = getConfig();
		config.setProperty("current", version);
		config.save();
	}

	public String getInstalledVersion() throws FileNotFoundException {
		Configuration config = getConfig();
		return config.getString("current");
	}

	public boolean updateYMLCache() throws FileNotFoundException {
		if (updated) { return true; }
		updated = true;
		
		boolean downloaded;
		synchronized (key) {
			String current;
			
			if (hasConfig()) {
				current = getConfig().getString("current");
			} else {
				current = null;
			}

			downloaded = super.updateYMLCache();
			
			Configuration config = getConfig();
			latest = config.getString("latest");
			recommended = config.getString("recommended");
			if (current != null) {
				config.setProperty("current", current);
				config.save();
			}
		}
		return downloaded;
	}
}
