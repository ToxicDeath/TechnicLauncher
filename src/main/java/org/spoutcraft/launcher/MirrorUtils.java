package org.spoutcraft.launcher;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.util.config.Configuration;
import org.spoutcraft.launcher.async.DownloadListener;

public class MirrorUtils {

	public static final String[]	MIRRORS_URL	= { "http://mirror.technicpack.net/Technic/mirrors.yml", "https://raw.github.com/TechnicPack/Technic/master/mirrors.yml" };
	public static File						mirrorsYML	= new File(GameUpdater.workDir, "mirrors.yml");
	private static boolean				updated			= false;


	@SuppressWarnings("unchecked")
	public static Map<String, Integer> getMirrors() {
		Configuration config = getMirrorsYML();
		return (Map<String, Integer>) config.getProperty("mirrors");
	}


	public static Configuration getMirrorsYML() {
		updateMirrorsYMLCache();
		Configuration config = new Configuration(mirrorsYML);
		config.load();
		return config;
	}
	
	/**
	 * @return false if mirror.yml couldn't be updated(but an offline version exists)
	 * @throws FileNotFoundException When mirrors.yml doesn't exist
	 */
	public static boolean updateMirrorsYMLCache() {
		if (updated) { return true; }
		updated = true;
		
		boolean downloaded = false;
		for (String urlentry : MIRRORS_URL) {
			if (YmlUtils.downloadMirrorsYmlFile(urlentry)) { return true; }
		}
		return false;
	}
}
