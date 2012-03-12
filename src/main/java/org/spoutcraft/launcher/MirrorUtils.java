package org.spoutcraft.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.spoutcraft.launcher.exception.DownloadsFailedException;
import org.spoutcraft.launcher.exception.NoMirrorsAvailableException;

public class MirrorUtils {

	public static final String[]	MIRRORS_URL	= { "http://git.technicpack.net/Technic/mirrors.yml", 
		"https://raw.github.com/TechnicPack/Technic/master/mirrors.yml" };
	public static File						mirrorsYML	= new File(GameUpdater.workDir, "mirrors.yml");
	private static Map<String, Integer> mirrors = null;
	private static boolean				updated			= false;
	private static final Random		rand				= new Random();

	/**
	 * @throws IOException when no mirror is accessible(including fallbackUrl)
	 */
	public static String getMirrorUrl(String relativePath, String fallbackUrl, 
			DownloadListener listener) throws NoMirrorsAvailableException {
		try {
			Map<String, Integer> mirrors = getMirrors();
			Set<Entry<String, Integer>> set = mirrors.entrySet();

			int total = 0;
			Iterator<Entry<String, Integer>> iterator = set.iterator();
			while (iterator.hasNext()) {
				total += iterator.next().getValue();
			}

			int random = rand.nextInt(total);

			int count = 0;
			boolean isFinished = false;
			iterator = set.iterator();
			Entry<String, Integer> current = null;
			while (!isFinished) {
				while (iterator.hasNext()) {
					current = iterator.next();
					count += current.getValue();
					String url = current.getKey();
					if (count > random) {
						String mirror = (!url.contains("github.com")) ? "http://" + url + "/" + relativePath : "https://" + url + "/" + relativePath;
						if (isAddressReachable(mirror)) {
							return mirror;
						} else {
							break;
						}
					}
				}

				if (set.size() == 1) {
					break;
				} else {
					total -= current.getValue();
					random = rand.nextInt(total);
					set.remove(current);
					iterator = set.iterator();
				}
			}
		} catch (FileNotFoundException e) {
			Util.log("No mirrors.yml file available for download.");
		}

		if (fallbackUrl != null && isAddressReachable(fallbackUrl)) {
			Util.log("All mirrors failed, reverting to default: %s", fallbackUrl);
			return fallbackUrl;
		}
		
		throw new NoMirrorsAvailableException("No mirrors available for file" + relativePath);
	}

	public static String getMirrorUrl(String mirrorURI, String fallbackUrl) 
			throws NoMirrorsAvailableException {
		return getMirrorUrl(mirrorURI, fallbackUrl, null);
	}

	private static Map<String, Integer> getMirrors() throws FileNotFoundException {
		if (mirrors != null) {
			return mirrors;
		}

		updateMirrorsYMLCache();
		
		if (mirrors == null) {
			throw new FileNotFoundException("Can't read mirrors.yml.");
		}
		return mirrors;
	}

	public static boolean isAddressReachable(String url) {
		URLConnection urlConnection = null;
		try {
			urlConnection = new URL(url).openConnection();
			if (url.contains("https")) {
				HttpsURLConnection urlConnect = (HttpsURLConnection) urlConnection;
				urlConnect.setConnectTimeout(5000);
				urlConnect.setReadTimeout(30000);
				urlConnect.setInstanceFollowRedirects(false);
				urlConnect.setRequestMethod("HEAD");
				int responseCode = urlConnect.getResponseCode();
				urlConnect.disconnect();
				urlConnect = null;
				return (responseCode == HttpURLConnection.HTTP_OK);
			} else {
				HttpURLConnection urlConnect = (HttpURLConnection) urlConnection;
				urlConnect.setConnectTimeout(5000);
				urlConnect.setReadTimeout(30000);
				urlConnect.setInstanceFollowRedirects(false);
				urlConnect.setRequestMethod("HEAD");
				int responseCode = urlConnect.getResponseCode();
				urlConnect.disconnect();
				urlConnect = null;
				return (responseCode == HttpURLConnection.HTTP_OK);
			}
		} catch (Exception e) {
		} finally {
			if (urlConnection != null) {
				urlConnection = null;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static void parseMirrorsFile() throws FileNotFoundException {
		if (!mirrorsYML.canRead()) {
			throw new FileNotFoundException("file mirrors.yml can't be read.");
		}
		
		Configuration config = new Configuration(mirrorsYML);
		mirrors = (Map<String, Integer>) config.getProperty("mirrors");
	}

	/**
	 * @return false if mirror.yml couldn't be updated(but an offline version exists)
	 * @throws FileNotFoundException When mirrors.yml doesn't exist
	 */
	public static boolean updateMirrorsYMLCache() throws FileNotFoundException {
		if (updated) { return true; }
		
		boolean downloaded = false;
		
		for (String urlentry : MIRRORS_URL) {
			try {
				YmlUtils.downloadMirrorsYmlFile(urlentry);
				downloaded = true;
				break;
			} catch (DownloadsFailedException ignore) { 
			} catch (NoMirrorsAvailableException ignore) {
			}
		}
		
		parseMirrorsFile();
		
		if(!downloaded) {
			Util.log("Failed to download mirrors.yml, using current version.");
			return false;
		}
		return true;
	}
}
