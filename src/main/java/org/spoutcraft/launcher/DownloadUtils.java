package org.spoutcraft.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.spoutcraft.launcher.async.Download;
import org.spoutcraft.launcher.async.DownloadListener;
import org.spoutcraft.launcher.exception.DownloadsFailedException;
import org.spoutcraft.launcher.exception.NoMirrorsAvailableException;

public class DownloadUtils {

	public static void downloadFile(String url, String output, String cacheName, 
			String md5, DownloadListener listener) 
					throws DownloadsFailedException{
			try {
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
					throw new DownloadsFailedException(new String[]{url});
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
			} catch (MalformedURLException e) {
				throw new DownloadsFailedException("Malformed URL during download", new String[]{url}, e);
			} catch (IOException e) {
				throw new DownloadsFailedException("File error during download", new String[]{url}, e);
			}
		
	}

	public static void downloadFile(String url, String output, String cacheName) throws DownloadsFailedException {
		downloadFile(url, output, cacheName, null, null);
	}

	public static void downloadFile(String url, String output) throws DownloadsFailedException {
		downloadFile(url, output, null, null, null);
	}

	private static int					filesToDownload	= 0;	
	private static List<String> failedDownloads = 
			Collections.synchronizedList(new ArrayList<String>());

	public static void downloadFiles(Map<String, String> downloadFileList, 
			long timeout, TimeUnit unit) throws DownloadsFailedException {
		filesToDownload = downloadFileList.size();
		failedDownloads.clear();

		ExecutorService es = Executors.newCachedThreadPool();
		for (final Map.Entry<String, String> file : downloadFileList.entrySet()) {
			es.execute(new Runnable() {

				@Override
				public void run() {
					try {
						downloadFile(file.getKey(), file.getValue());
						return;
					} catch (DownloadsFailedException e) {
						failedDownloads.add(file.getKey());
					} 
				}
			});
		}
		es.shutdown();
		try {
			if (es.awaitTermination(timeout, unit)) {
				
				if (!failedDownloads.isEmpty()) {
					throw new DownloadsFailedException(failedDownloads.toArray(new String[]{}));
				}
				
				return; 
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void downloadFile(String relativePath) 
			throws NoMirrorsAvailableException, DownloadsFailedException {
		if (MD5Utils.checksumPath(relativePath)) { return; }

		URL url = null;
		File tempFile = null;
		String mirrorUrl = null;
		try {
			mirrorUrl = MirrorUtils.getMirrorUrl(relativePath, null);
			
			url = new URL(mirrorUrl);
			URLConnection con = (url.openConnection());

			System.setProperty("http.agent", "");
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.100 Safari/534.30");

			tempFile = File.createTempFile("Modpack", null);

			// Download to temporary file
			OutputStream baos = new FileOutputStream(tempFile);
			// new FileOutputStream(tempFile)
			if (GameUpdater.copy(con.getInputStream(), baos) <= 0) {
				throw new DownloadsFailedException("Download URL was empty: '"+url+"'");
			}

			// If no Exception then file loaded fine, copy to output file
			GameUpdater.copy(tempFile, new File(relativePath));
			tempFile.delete();

			return;
		} catch (MalformedURLException e) {
			throw new DownloadsFailedException("Download URL badly formed: '"+url+"'", e);
		} catch (FileNotFoundException e) {
			throw new DownloadsFailedException("Could not write to temp file: '"+tempFile+"'", e);
		} catch (IOException e) {
			throw new DownloadsFailedException("File error during download", new String[]{mirrorUrl}, e);
		}
	}
}
