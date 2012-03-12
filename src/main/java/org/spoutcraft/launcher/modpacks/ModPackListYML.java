package org.spoutcraft.launcher.modpacks;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;

import org.bukkit.util.config.Configuration;
import org.spoutcraft.launcher.CachedYMLFile;
import org.spoutcraft.launcher.DownloadUtils;
import org.spoutcraft.launcher.GameUpdater;
import org.spoutcraft.launcher.MD5Utils;
import org.spoutcraft.launcher.MirrorUtils;
import org.spoutcraft.launcher.PlatformUtils;
import org.spoutcraft.launcher.SettingsUtil;
import org.spoutcraft.launcher.Util;
import org.spoutcraft.launcher.exception.DownloadsFailedException;
import org.spoutcraft.launcher.exception.NoMirrorsAvailableException;

public class ModPackListYML {

	public static final File										ORIGINAL_PROPERTIES			= new File(GameUpdater.cacheDir, "launcher.properties");
	private static final String									RESOURCES_PATH					= "resources";
	private static final String									ICON_ICO								= "icon.ico";
	private static final String									ICON_ICNS								= "icon.icns";
	private static final String									ICON_PNG								= "icon.png";
	private static final String									FAVICON_PNG							= "favicon.png";
	private static final String									LOGO_PNG								= "logo.png";
	private static final String									MODPACKS_YML						= "modpacks.yml";

	private static final List<String>						RESOURCES								= new LinkedList<String>();
	private static final File										MODPACKS_YML_FILE				= new File(GameUpdater.workDir, MODPACKS_YML);
	private static final Object									key											= new Object();

	public static Map<String, String>						modpackMap;
	public static final Map<String, ImageIcon>	modpackLogoList					= new HashMap<String, ImageIcon>();

	private static volatile boolean							updated									= false;

	public static String												currentModPack					= null;
	public static String												currentModPackLabel			= null;
	public static File													currentModPackDirectory	= null;

	public static Image													favIcon									= null;
	public static Image													icon										= null;
	public static Image													logo										= null;

	private static final CachedYMLFile 	ymlFile;
	
	static {
		RESOURCES.add(FAVICON_PNG);
		RESOURCES.add(LOGO_PNG);
		RESOURCES.add(getIconName());

		ymlFile = new CachedYMLFile(MODPACKS_YML, 
				null, 
				MODPACKS_YML_FILE);
	}

	public static String getIconName() {
		if (PlatformUtils.getPlatform() == PlatformUtils.OS.windows) {
			return ICON_PNG;
		} else if (PlatformUtils.getPlatform() == PlatformUtils.OS.macos) { return ICON_ICNS; }
		return ICON_PNG;
	}

	public static Configuration getConfig() throws FileNotFoundException {
		return ymlFile.getConfig();
	}

	/**
	 * @return false if cache couldn't be updated(but an offline version exists)
	 * @throws FileNotFoundException if minecraft.yml is not available
	 */
	public static boolean updateModPacksYMLCache() throws FileNotFoundException{
		if (!updated) {
			return true;
		}
		updated = true;
		
		boolean downloaded;
		
		synchronized (key) {
			downloaded = ymlFile.updateYMLCache();
			modpackMap = getModPacks();
		}
		
		return downloaded;
	}

	public static void setCurrentModpack() throws IOException {
		Map<String, String> modPackMap = getModPacks();
		setModPack(SettingsUtil.getModPackSelection(), modPackMap.get(SettingsUtil.getModPackSelection()), false);
		File propFile = new File(GameUpdater.modpackDir, "launcher.properties");
		if (!ORIGINAL_PROPERTIES.exists()) {
			GameUpdater.copy(SettingsUtil.settingsFile, ORIGINAL_PROPERTIES);
		}

		if (!propFile.exists()) {
			GameUpdater.copy(ORIGINAL_PROPERTIES, SettingsUtil.settingsFile);
			SettingsUtil.reload();
			SettingsUtil.setModPack(ModPackListYML.currentModPack);
			GameUpdater.copy(SettingsUtil.settingsFile, propFile);
		} else {
			GameUpdater.copy(propFile, SettingsUtil.settingsFile);
			SettingsUtil.reload();
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> getModPacks() throws FileNotFoundException {
		return (Map<String, String>) getConfig().getProperty("modpacks");
	}

	public static boolean setModPack(String modPack, String modPackLabel) 
			 throws FileNotFoundException {
		return setModPack(modPack, modPackLabel, false);
	}

	public static boolean setModPack(String modPack, String modPackLabel, boolean ignoreCheck) 
			 throws FileNotFoundException {
		if (modPack.equalsIgnoreCase(currentModPack)) { return true; }

		if (!ignoreCheck) {
			Map<String, String> modPacks = getModPacks();
			if (!modPacks.containsKey(modPack)) {
				// Mod Pack not in list
				Util.log("ModPack '%s' not in '%s' file.", modPackLabel, MODPACKS_YML);
				return false;
			}
		}

		SettingsUtil.setModPack(modPack);

		currentModPack = modPack;
		currentModPackLabel = modPackLabel;

		GameUpdater.setModpackDirectory(currentModPack);

		currentModPackDirectory = new File(GameUpdater.workDir, currentModPack);
		currentModPackDirectory.mkdirs();

		ModPackYML.updateModPackYML(true);

		return true;
	}

	public static void downloadModPackResources() throws FileNotFoundException {
		downloadModPackResources(currentModPack, currentModPackLabel, currentModPackDirectory);
	}

	public static void downloadModPackResources(String name, String label, File path) 
			throws FileNotFoundException {
		Map<String, String> downloadFileList;
		try {
			downloadFileList = getModPackResources(name, path);
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("For modpack '"+label+"' : " + e.getMessage());
		}

		if (downloadFileList.size() > 0) {
			try {
				DownloadUtils.downloadFiles(downloadFileList, 30, TimeUnit.SECONDS);
			} catch (DownloadsFailedException e) {
				Util.log("Couldn't download files after checking that they exist on remote servers.");
				e.printStackTrace();
			}
		}
	}

	private static Map<String, String> getModPackResources(String name, File path) 
			throws FileNotFoundException {
		return getModPackResources(name, path, true);
	}

	private static Map<String, String> getModPackResources(String name, File path, 
			boolean doCheck) throws FileNotFoundException {
		Map<String, String> fileMap = new HashMap<String, String>();

		for (String resource : RESOURCES) {
			String relativeFilePath = name + "/resources/" + resource;

			if (doCheck && MD5Utils.checksumPath(relativeFilePath)) {
				continue;
			}

			File dir = new File(path, RESOURCES_PATH);
			dir.mkdirs();
			File file = new File(dir, resource);
			String filePath = file.getAbsolutePath();

			String fileURL;
			try {
				fileURL = MirrorUtils.getMirrorUrl(relativeFilePath, null);
			} catch (NoMirrorsAvailableException e) {
				if (!file.exists()) {
					throw new FileNotFoundException("Unable to find in mirrors and doesn't exist locally: '"+file+"'");
				}
				
				continue;
			}
			fileMap.put(fileURL, filePath);
		}

		return fileMap;
	}

	public static void getAllModPackResources() throws FileNotFoundException {
		updateModPacksYMLCache();
		Map<String, String> fileMap = new HashMap<String, String>();
		for (String modPack : modpackMap.keySet()) {
			File modPackDir = new File(GameUpdater.workDir, modPack);
			modPackDir.mkdirs();
			fileMap = getModPackResources(modPack, modPackDir);
			try {
				downloadAllFiles(fileMap);
			} catch(DownloadsFailedException e) {
				Util.log("Couldn't download files after checking that they exist on remote servers.");
				e.printStackTrace();
			}
		}
	}

	public static void loadModpackLogos() throws FileNotFoundException {
		updateModPacksYMLCache();
		for (String modPack : modpackMap.keySet()) {
			File modPackDir = new File(GameUpdater.workDir, modPack);
			File resourcesPath = new File(modPackDir, RESOURCES_PATH);
			File modPackLogo = new File(resourcesPath, LOGO_PNG);
			if (!modPackLogo.exists()) continue;
			modpackLogoList.put(modPack, new ImageIcon(Toolkit.getDefaultToolkit().getImage(modPackLogo.getAbsolutePath())));
		}
	}

	public static void downloadAllFiles(Map<String, String> fileMap) throws DownloadsFailedException {
		int size = fileMap.size();
		if (size > 0) {
			DownloadUtils.downloadFiles(fileMap, 30, TimeUnit.SECONDS); 
		}
	}

	public static ImageIcon getModPackLogo(String item) {
		return (modpackLogoList.containsKey(item)) ? modpackLogoList.get(item) : null;
	}
}
