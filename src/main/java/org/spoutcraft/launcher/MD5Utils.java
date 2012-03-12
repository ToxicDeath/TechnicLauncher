package org.spoutcraft.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.codec.digest.DigestUtils;
import org.bukkit.util.config.Configuration;
import org.spoutcraft.launcher.exception.DownloadsFailedException;
import org.spoutcraft.launcher.exception.NoMirrorsAvailableException;

public class MD5Utils {

	private static final String								CHECKSUM_MD5	= "CHECKSUM.md5";
	private static final File									CHECKSUM_FILE	= new File(GameUpdater.workDir, CHECKSUM_MD5);
	private static boolean										updated;
	private static final Map<String, String>	md5Map				= new HashMap<String, String>();

	public static String getMD5(File file) throws FileNotFoundException {
		FileInputStream stream = null;
		stream = new FileInputStream(file);
		String md5Hex;
		try {
			 md5Hex = DigestUtils.md5Hex(stream);
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return md5Hex;
	}

	public static String getMD5(FileType type) throws FileNotFoundException {
		return getMD5(type, MinecraftYML.getLatestMinecraftVersion());
	}

	@SuppressWarnings("unchecked")
	public static String getMD5(FileType type, String version) 
			throws FileNotFoundException {
		Configuration config = MinecraftYML.getMinecraftYML();
		Map<String, Map<String, String>> builds = (Map<String, Map<String, String>>) config.getProperty("versions");
		if (builds.containsKey(version)) {
			Map<String, String> files = builds.get(version);
			return files.get(type.name());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static String getMinecraftMD5(String md5Hash) throws FileNotFoundException {
		Configuration config = MinecraftYML.getMinecraftYML();
		Map<String, Map<String, String>> builds = (Map<String, Map<String, String>>) config.getProperty("versions");
		for (String version : builds.keySet()) {
			String minecraftMD5 = builds.get(version).get("minecraft");
			if (minecraftMD5.equalsIgnoreCase(md5Hash)) { return version; }
		}
		return null;
	}

	/**
	 * @return false if cache couldn't be downloaded(but an offline version exists)
	 * @throws FileNotFoundException when md5 file doesn't exist locally
	 */
	public static boolean updateMD5Cache() throws FileNotFoundException {
		if (updated) return true;
		updated = true;
		
		boolean downloaded = false;
		try {
			String url = MirrorUtils.getMirrorUrl(CHECKSUM_MD5, null);
			DownloadUtils.downloadFile(url, CHECKSUM_FILE.getPath());
			downloaded = true;
		} catch (NoMirrorsAvailableException e) {
			Util.log("Warning: No mirrors available for 'checksum.md5'");
		} catch (DownloadsFailedException e) {
			Util.log("Warning: couldn't download 'checksum.md5'");
			e.printStackTrace();
		}

		parseChecksumFile();
		return downloaded;
	}

	private static void parseChecksumFile() throws FileNotFoundException {
		md5Map.clear();
		Scanner scanner = new Scanner(CHECKSUM_FILE).useDelimiter("\\||\n");
		while (scanner.hasNext()) {
			String md5 = scanner.next().toLowerCase();
			String path = scanner.next().replace("\r", "").replace('/', '\\');
			md5Map.put(path, md5);
			scanner.nextLine();
		}
	}

	public static boolean checksumPath(String relativePath) {
		return checksumPath(relativePath, relativePath);
	}

	public static boolean checksumPath(String filePath, String md5Path) {
		return checksumPath(new File(GameUpdater.workDir, filePath), md5Path);
	}

	public static boolean checksumCachePath(String filePath, String md5Path) 
			throws FileNotFoundException {
		return checksumPath(new File(GameUpdater.cacheDir, filePath), md5Path);
	}

	public static boolean checksumPath(File file, String md5Path) {
		if (!file.exists()) { return false; }
		String fileMD5;
		try {
			fileMD5 = getMD5(file);
		} catch (FileNotFoundException e) {
			return false;
		}
		String storedMD5 = getMD5FromList(md5Path);
		if (storedMD5 == null) {
			Util.log("MD5 hash not found for '%s'", md5Path);
		}
		boolean doesMD5Match = (storedMD5 == null) ? false : storedMD5.equalsIgnoreCase(fileMD5);
		if (!doesMD5Match) {
			Util.log("[MD5 Mismatch] File '%s' has md5 of '%s' instead of '%s'", file, fileMD5, storedMD5);
		}
		return doesMD5Match;
	}

	public static String getMD5FromList(String md5Path) {
		md5Path = md5Path.replace('/', '\\');
		return (!md5Map.containsKey(md5Path)) ? null : md5Map.get(md5Path);
	}
}
