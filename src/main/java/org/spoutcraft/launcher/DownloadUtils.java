package org.spoutcraft.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.util.config.Configuration;
import org.spoutcraft.launcher.async.Download;
import org.spoutcraft.launcher.async.DownloadListener;
import org.spoutcraft.launcher.exception.DownloadFailedException;

public class DownloadUtils implements MirrorDownloader {
	private static final Random		rand				= new Random();
	
	private String[]							mirrorYmlUrls;
	private MD5Utils							md5;
	private MinecraftYML					mineYml;
	private Map<String, Integer> 	mirrors;
	
	public DownloadUtils(String[] mirrorYmlUrls) throws FileNotFoundException {
		this.mirrorYmlUrls = mirrorYmlUrls;

		updateMirrors();
		mineYml = new MinecraftYML(this);
		md5 = new MD5Utils(this, mineYml);
	}
	
	public boolean hasMirrors() {
		return !mirrors.isEmpty();
	}
	
	@SuppressWarnings("unchecked")
	public boolean updateMirrors() {
		File mirrorFile = new File(GameUpdater.workDir, "mirrors.yml");

		boolean downloaded = false;
		
		for (String mirrorYMLUrl : mirrorYmlUrls ){
			try {
				downloadFromUrl(mirrorYMLUrl, mirrorFile.getPath());
				downloaded = true;
				break;
			} catch (IOException ignore) { }
		}
		
		if (!downloaded) {
			if (mirrorFile.canRead()) {
				Util.log("Warning: mirrors.yml not avialable for download, trying local copy");
				Configuration config = new Configuration(mirrorFile);
				mirrors = (Map<String, Integer>) config.getProperty("mirrors");
				
				// verify mirrors exist
				for (String mirror : mirrors.keySet()) {
					if (!isAddressReachable(mirror)) {
						mirrors.remove(mirror);
					}
				}
			} else {
				Util.log("Warning: Cannot download initial mirrors.yml");
				mirrors = new HashMap<String, Integer>();
			}
		}

		
		if (mirrors.isEmpty()) {
			return false;
		}
		
		return true;
	}
		
	private static boolean isAddressReachable(String url) {
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
		} catch (IOException ignore) {
		}
		finally {
			if (urlConnection != null) {
				urlConnection = null;
			}
		}
		return false;
	}

	private String getMirrorUrl(String relativePath, String fallbackUrl, 
			DownloadListener listener) throws DownloadFailedException {
		if (hasMirrors()) {
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
		} else {
			Util.log("No mirrors available from mirrors.yml.");
		}

		if (fallbackUrl != null && isAddressReachable(fallbackUrl)) {
			Util.log("All mirrors failed, reverting to default: %s", fallbackUrl);
			return fallbackUrl;
		}
		
		throw new DownloadFailedException("No mirrors available for file" + relativePath);
	}

	private String getMirrorUrl(String mirrorURI, String fallbackUrl) 
			throws DownloadFailedException {
		return getMirrorUrl(mirrorURI, fallbackUrl, null);
	}

	
	private static Download downloadFromUrl(String url, String output, 
			String cacheName, String md5, DownloadListener listener) 
					throws DownloadFailedException, IOException {
		int tries = SettingsUtil.getLoginTries();
		File outputFile = new File(output);
		File tempfile = File.createTempFile("file", null, GameUpdater.tempDir);
		tempfile.mkdirs();
		Download download = null;
		boolean areFilesIdentical = tempfile.getPath().equalsIgnoreCase(outputFile.getPath());
		while (tries > 0) {
			Util.logi("Starting download of '%s', with %s trie(s) remaining", url, tries);
			tries--;
			download = new Download(url, tempfile.getPath());
			download.setListener(listener);
			download.run();
			if (!download.isSuccess()) {
				if (download.getOutFile() != null) {
					download.getOutFile().delete();
				}
				Util.log("Download of " + url + " Failed!");
				if (listener != null) {
					listener.stateChanged("Download Failed, retries remaining: " + tries, 0F);
				}
			} else {
				String fileMD5 = MD5Utils.getMD5(download.getOutFile());
				if (md5 == null || fileMD5.equals(md5)) {
					Util.logi("Copying: %s to: %s", tempfile, outputFile);
					if (!areFilesIdentical) {
						GameUpdater.copy(tempfile, outputFile);
					}
					Util.logi("File Downloaded: %s", outputFile);
					break;
				} else if (md5 != null && !fileMD5.equals(md5)) {
					Util.log("Expected MD5: %s Calculated MD5: %s", md5, fileMD5);
				}
			}
		}
		
		if (!download.isSuccess()) {
			throw new DownloadFailedException(String.format("Couldn't download file: '%s'", url));
		}

		if (cacheName != null) {
			if (tempfile.exists()) {
				GameUpdater.copy(tempfile, new File(GameUpdater.cacheDir, cacheName));
			} else {
				Util.log("Could not copy file to cache: %s", tempfile);
			}
		}

		if (!areFilesIdentical) {
			tempfile.delete();
		}
		return download;
	}

	private static Download downloadFromUrl(String url, String output, String cacheName) throws DownloadFailedException, IOException {
		return downloadFromUrl(url, output, cacheName, null, null);
	}

	private static Download downloadFromUrl(String url, String output) throws IOException {
		return downloadFromUrl(url, output, null, null, null);
	}

	private static int					filesToDownload	= 0;	
	private static List<String> failedDownloads = 
			Collections.synchronizedList(new ArrayList<String>());

	public static void downloadFiles(Map<String, String> downloadFileList, 
			long timeout, TimeUnit unit) throws DownloadFailedException {
		filesToDownload = downloadFileList.size();
		failedDownloads.clear();

		ExecutorService es = Executors.newCachedThreadPool();
		for (final Map.Entry<String, String> file : downloadFileList.entrySet()) {
			es.execute(new Runnable() {

				@Override
				public void run() {
					try {
						downloadFromUrl(file.getKey(), file.getValue());
						return;
					} catch (IOException e) {
						failedDownloads.add(file.getKey());
						Util.log("Download failed : '%s'", file.getKey());
						e.printStackTrace();
					}
				}
			});
		}
		es.shutdown();
		try {
			if (es.awaitTermination(timeout, unit)) { 
				
				if (!failedDownloads.isEmpty()) {
					throw new DownloadFailedException("Multiple Downloads failed");
				}
				return; 
			}
		} catch (InterruptedException e) {
			throw new DownloadFailedException("Interuption during downloads", e);
		}
	}

	public void downloadFile(String relativePath, String fallbackUrl) throws DownloadFailedException {
		downloadFile(relativePath, fallbackUrl, null);
	}

	public void downloadFile(String relativePath, String fallbackUrl, File outputFile) throws DownloadFailedException {
		if (md5.checksumPath(relativePath)) { return; }

		URL url = null;
		File tempFile = null;
		
		if (outputFile == null) {
			outputFile = new File(relativePath);
		}
		
		try {
			String mirrorUrl = getMirrorUrl(relativePath, fallbackUrl);
			url = new URL(mirrorUrl);
			URLConnection con = (url.openConnection());

			System.setProperty("http.agent", "");
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.100 Safari/534.30");

			tempFile = File.createTempFile("Modpack", null);

			// Download to temporary file
			OutputStream baos = new FileOutputStream(tempFile);
			// new FileOutputStream(tempFile)
			try {
				if (GameUpdater.copy(con.getInputStream(), baos) <= 0) {
					throw new DownloadFailedException(String.format("Download URL was empty: '%s'", url));
				}
				baos.flush();
				// If no Exception then file loaded fine, copy to output file
				GameUpdater.copy(tempFile, new File(relativePath));
			} finally {
				baos.close();
				
				// If no Exception then file loaded fine, copy to output file
				GameUpdater.copy(tempFile, outputFile);
				tempFile.delete();
			}

		} catch (MalformedURLException e) {
			throw new DownloadFailedException(String.format("While Downloading '%s'", relativePath), e);
		} catch (FileNotFoundException e) {
			throw new DownloadFailedException(String.format("Could not write to temp file: '%s'", tempFile), e);
		} catch (IOException e) {
			throw new DownloadFailedException(String.format("While Downloading '%s'", relativePath), e);
		}
	}
	
	/**
	 * 
	 * @return false if download failed, but local copy available
	 * @throws DownloadFailedException if download failed, and no local copy
	 * @throws IOException if copy operation fails
	 */
	// TODO: make caching work by passing a boolean to downloadFile
	public boolean downloadCachedFile(String relativePath, File cachedFile, File outputFile) 
			throws DownloadFailedException, IOException {
		if (md5.checksumPath(relativePath)) { return true; }
		
		if (md5.checksumPath(cachedFile.getPath(), relativePath)) {
			GameUpdater.copy(cachedFile, outputFile);
			return true;
		}
		
		try {
			downloadFile(relativePath, null, cachedFile);
			GameUpdater.copy(cachedFile, outputFile);
			return true;
		} catch (DownloadFailedException e) {
			if (!cachedFile.canRead() && !outputFile.canRead()) {
				throw e;
			}
			if (!outputFile.canRead()) {
				GameUpdater.copy(cachedFile, outputFile);
			}
			return false;
		}
	}
}
