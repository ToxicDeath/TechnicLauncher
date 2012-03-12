package org.spoutcraft.launcher.modpacks;

import java.io.File;
import java.io.FileNotFoundException;

import org.bukkit.util.config.Configuration;
import org.spoutcraft.launcher.CachedYMLFile;
import org.spoutcraft.launcher.GameUpdater;

public class ModLibraryYML {

	public static final String					MODLIBRARY_YML	= "modlibrary.yml";
	public static final File						modLibraryYML		= new File(GameUpdater.workDir, MODLIBRARY_YML);

	private static final CachedYMLFile 	ymlFile;
	
	static {
		ymlFile = new CachedYMLFile(MODLIBRARY_YML, 
				null, 
				modLibraryYML);
	}
	
	
	/**
	 * @return false if cache couldn't be downloaded(but an offline version exists)
	 * @throws FileNotFoundException when md5 file doesn't exist locally
	 */
	public static boolean updateModLibraryYML() throws FileNotFoundException {
		return ymlFile.updateYMLCache();
	}

	public static Configuration getModLibraryYML() throws FileNotFoundException {
		return ymlFile.getConfig();
	}
}
