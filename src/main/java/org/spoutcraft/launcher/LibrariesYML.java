package org.spoutcraft.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import org.bukkit.util.config.Configuration;

public class LibrariesYML {
	private static final String			LIBRARIES_YML			= "libraries.yml";
	private static File							librariesYMLFile	= new File(GameUpdater.workDir, LIBRARIES_YML);
	private static final CachedYMLFile 		ymlFile;
	
	static {
		ymlFile = new CachedYMLFile(LIBRARIES_YML, 
				"http://technic.freeworldsgaming.com/libraries.yml", 
				librariesYMLFile);
	}

	public static Configuration getLibrariesYML() throws FileNotFoundException {
		return ymlFile.getConfig();
	}

	
	/**
	 * @return false if cache couldn't be downloaded(but an offline version exists)
	 * @throws FileNotFoundException if libraries.yml does not exist locally 
	 */
	public static boolean updateLibrariesYMLCache() throws FileNotFoundException {
		return ymlFile.updateYMLCache();
	}

	@SuppressWarnings("unchecked")
	public static String getMD5(String library, String version) 
			throws FileNotFoundException {
		Configuration config = getLibrariesYML();
		Map<String, Object> libraries = (Map<String, Object>) config.getProperty(library);
		Map<String, String> versions = (Map<String, String>) libraries.get("versions");
		String result = versions.get(version);
		if (result == null) {
			try {
				result = versions.get(Double.parseDouble(version));
			} catch (NumberFormatException e) {
				result = null;
			}
		}
		return result;
	}

}
