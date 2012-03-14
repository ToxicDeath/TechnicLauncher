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
import org.spoutcraft.launcher.exception.DownloadFailedException;

public class MD5Utils {

	private static final String								CHECKSUM_MD5	= "CHECKSUM.md5";
	private static final File									CHECKSUM_FILE	= new File(GameUpdater.workDir, CHECKSUM_MD5);

	private MirrorDownloader 												downloader;
	private MinecraftYML											mineYML;
	private Map<String, String>								md5Map				= new HashMap<String, String>();

	
	public MD5Utils(MirrorDownloader downloader, MinecraftYML mineYML) throws FileNotFoundException {
		this.downloader = downloader;
		this.mineYML = mineYML;
		
		updateMD5Cache();
	}
	
	public static String getMD5(File file) throws IOException {
		FileInputStream stream;
		stream = new FileInputStream(file);
		try {
			String md5Hex = DigestUtils.md5Hex(stream);
			return md5Hex;
		} finally {
			stream.close();
		}
	}

	public String getMD5(FileType type) throws FileNotFoundException {
		return getMD5(type, mineYML.getLatestMinecraftVersion());
	}

	@SuppressWarnings("unchecked")
	public String getMD5(FileType type, String version) throws FileNotFoundException {
		Configuration config = mineYML.getConfig();
		Map<String, Map<String, String>> builds = (Map<String, Map<String, String>>) config.getProperty("versions");
		if (builds.containsKey(version)) {
			Map<String, String> files = builds.get(version);
			return files.get(type.name());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public String getMinecraftMD5(String md5Hash) throws FileNotFoundException {
		Configuration config = mineYML.getConfig();
		Map<String, Map<String, String>> builds = (Map<String, Map<String, String>>) config.getProperty("versions");
		for (String version : builds.keySet()) {
			String minecraftMD5 = builds.get(version).get("minecraft");
			if (minecraftMD5.equalsIgnoreCase(md5Hash)) { return version; }
		}
		return null;
	}

	public void updateMD5Cache() throws FileNotFoundException {
		try {
			// FIXME: set fallback url
			downloader.downloadFile(CHECKSUM_MD5, null, CHECKSUM_FILE);
		} catch (DownloadFailedException e) {
			Util.log("Download failed fot '%s', trying offline version.", CHECKSUM_MD5);
		}

		parseChecksumFile();
	}

	private void parseChecksumFile() throws FileNotFoundException {
		md5Map.clear();
		Scanner scanner = new Scanner(CHECKSUM_FILE).useDelimiter("\\||\n");
		while (scanner.hasNext()) {
			String md5 = scanner.next().toLowerCase();
			String path = scanner.next().replace("\r", "").replace('/', '\\');
			md5Map.put(path, md5);
			scanner.nextLine();
		}
	}

	public boolean checksumPath(String relativePath) {
		return checksumPath(relativePath, relativePath);
	}

	public boolean checksumPath(String filePath, String md5Path) {
		return checksumPath(new File(GameUpdater.workDir, filePath), md5Path);
	}

	public boolean checksumCachePath(String filePath, String md5Path) {
		return checksumPath(new File(GameUpdater.cacheDir, filePath), md5Path);
	}

	public boolean checksumPath(File file, String md5Path) {
		if (!file.exists()) { return false; }
		try {
			String fileMD5 = getMD5(file);
			String storedMD5 = getMD5FromList(md5Path);
			if (storedMD5 == null) {
				Util.log("MD5 hash not found for '%s'", md5Path);
			}
			boolean doesMD5Match = (storedMD5 == null) ? false : storedMD5.equalsIgnoreCase(fileMD5);
			if (!doesMD5Match) {
				Util.log("[MD5 Mismatch] File '%s' has md5 of '%s' instead of '%s'", file, fileMD5, storedMD5);
			}
			return doesMD5Match;
		} catch (IOException e) {
			return false;
		}
	}

	public String getMD5FromList(String md5Path) {
		md5Path = md5Path.replace('/', '\\');
		return (!md5Map.containsKey(md5Path)) ? null : md5Map.get(md5Path);
	}
}
