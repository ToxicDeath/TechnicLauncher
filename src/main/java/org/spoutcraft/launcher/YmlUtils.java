package org.spoutcraft.launcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.spoutcraft.launcher.exception.DownloadsFailedException;
import org.spoutcraft.launcher.exception.NoMirrorsAvailableException;
import org.yaml.snakeyaml.Yaml;

public class YmlUtils {

	public static void downloadMirrorsYmlFile(String mirrorYmlUrl) 
			throws NoMirrorsAvailableException, DownloadsFailedException {
		downloadYmlFile(mirrorYmlUrl, null, MirrorUtils.mirrorsYML);
	}

	public static void downloadRelativeYmlFile(String relativePath) 
			throws NoMirrorsAvailableException, DownloadsFailedException{
		downloadYmlFile(relativePath, null, new File(GameUpdater.workDir, relativePath));
	}
 
	/**
  * @throws IOException if file could not be downloaded
  */
	public static void downloadYmlFile(String ymlUrl, String fallbackUrl, File ymlFile) 
			throws NoMirrorsAvailableException, DownloadsFailedException {
		boolean isRelative = !ymlUrl.contains("http");

		GameUpdater.tempDir.mkdirs();

		if (isRelative && ymlFile.exists() && MD5Utils.checksumPath(ymlUrl)) { return; }

		URL url = null;
		InputStream io = null;
		OutputStream out = null;
		try {
			if (!isRelative && !MirrorUtils.isAddressReachable(ymlUrl)) {
				throw new DownloadsFailedException("Url is innaccessible", ymlUrl);
			} else if (isRelative) {
				ymlUrl = MirrorUtils.getMirrorUrl(ymlUrl, fallbackUrl);
			}

			Util.log("[Info] Downloading '%s' from '%s'.", ymlFile.getName(), ymlUrl);

			url = new URL(ymlUrl);
			URLConnection con = (url.openConnection());

			System.setProperty("http.agent", "");
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.100 Safari/534.30");

			// Download to temporary file
			File tempFile = new File(GameUpdater.tempDir, ymlFile.getName());
			out = new BufferedOutputStream(new FileOutputStream(tempFile));

			try {
				if (GameUpdater.copy(con.getInputStream(), out) <= 0) {
					throw new DownloadsFailedException("Download URL was empty: '"+url+"'");
				}
			} finally {
				out.close();
			}
		
			// Test yml loading
			Yaml yamlFile = new Yaml();
			io = new BufferedInputStream(new FileInputStream(tempFile));
			try {
				yamlFile.load(io);
	
				// If no Exception then file loaded fine, copy to output file
				GameUpdater.copy(tempFile, ymlFile);
			} finally {
				io.close();
			}

			tempFile.delete();
	
			return;
		} catch (MalformedURLException e) {
			throw new DownloadsFailedException("Download URL badly formed: '"+url+"'", e);
		} catch (IOException e) {
			throw new DownloadsFailedException(new String[]{ymlUrl}, e);
		}
	}
}
