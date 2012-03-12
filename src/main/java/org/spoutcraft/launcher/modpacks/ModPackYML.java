package org.spoutcraft.launcher.modpacks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.bukkit.util.config.Configuration;
import org.spoutcraft.launcher.CachedYMLFile;
import org.spoutcraft.launcher.Main;
import org.spoutcraft.launcher.YmlUtils;

public class ModPackYML {

	private static final String			MODPACK_YML		= "modpack.yml";
	private static final String			FALLBACK_URL	= String.format("http://technic.freeworldsgaming.com/%s", MODPACK_YML);

	private static volatile boolean	updated				= false;
	private static final Object			key						= new Object();

	private static final CachedYMLFile 	ymlFile;
	
	static {
		ymlFile = new CachedYMLFile(MODPACK_YML, 
				FALLBACK_URL, 
				getModPackYMLFile());
	}

	
	private static File getModPackYMLFile() {
		return new File(ModPackListYML.currentModPackDirectory, MODPACK_YML);
	}

	
	public static Configuration getModPackYML() throws FileNotFoundException {
		return ymlFile.getConfig();
	}

	public static void updateModPackYML() throws FileNotFoundException {
		updateModPackYML(false);
	}

	public static boolean updateModPackYML(boolean doUpdate) throws FileNotFoundException {
		
		if (updated && !doUpdate) {return true; }
		updated = true;
		
		boolean downloaded;
		
		synchronized (key) {
			String current;
			
			// the file won't be there for the first run
			if (ymlFile.hasConfig()) {
				current = getSelectedBuild();  
			} else {
				current = null;
			}

			downloaded = ymlFile.updateYMLCache();
			
			Configuration config = getModPackYML();
			
			config.setProperty("current", current);
			config.setProperty("launcher", Main.build);
			config.save();
		}
		
		return downloaded;
	}

	private static String getSelectedBuild() throws FileNotFoundException {
		Configuration config = getModPackYML();
		String selected = config.getString("current");
		if (selected == null || !isValidBuild(selected)) {
			selected = config.getString("recommended");
		}
		return selected;
	}

	private static boolean isValidBuild(String selected) {
		return !selected.equals("-1");
	}

	public static List<Map<String, String>> getModList() {
		// TODO Auto-generated method stub
		return null;
	}

	public static String getModPackIcon() {
		return new File(ModPackListYML.currentModPackDirectory, "resources" + File.separator + ModPackListYML.getIconName()).getAbsolutePath();
	}

	public static String getModPackLogo() {
		return new File(ModPackListYML.currentModPackDirectory, "resources" + File.separator + "logo.png").getAbsolutePath();
	}

	public static String getModPackFavIcon() {
		return new File(ModPackListYML.currentModPackDirectory, "resources" + File.separator + "favicon.png").getAbsolutePath();
	}

	@SuppressWarnings("unchecked")
	public static String[] getModpackBuilds() throws FileNotFoundException {
		Configuration config = getModPackYML();
		Map<String, Object> builds = (Map<String, Object>) config.getProperty("builds");
		String latest = config.getString("latest", null);
		String recommended = config.getString("recommended", null);

		if (builds != null) {
			String[] results = new String[builds.size()];
			int index = 0;
			for (String i : builds.keySet()) {
				results[index] = i.toString();
				Map<String, Object> map = (Map<String, Object>) builds.get(i);
				String version = String.valueOf(map.get("minecraft"));
				results[index] += "| " + version;
				if (i.equals(latest)) {
					results[index] += " | Latest";
				}
				if (i.equals(recommended)) {
					results[index] += " | Rec. Build";
				}
				index++;
			}
			return results;
		}
		return null;
	}
}
