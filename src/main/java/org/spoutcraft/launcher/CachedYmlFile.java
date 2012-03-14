package org.spoutcraft.launcher;

import java.io.File;
import java.io.FileNotFoundException;

import org.bukkit.util.config.Configuration;
import org.spoutcraft.launcher.exception.DownloadFailedException;


public class CachedYmlFile {
	private final Object 	key = new Object();
	
	private String	mirrorPath;
	private String	fallBackUrl;
	private File    configFile;
	private Configuration config = null;
	private MirrorDownloader downloader;
	
	public CachedYmlFile(MirrorDownloader downloader, String mirrorPath, 
			String fallBackUrl, File configFile) throws FileNotFoundException {
		this.downloader = downloader;
		this.mirrorPath = mirrorPath;
		this.fallBackUrl = fallBackUrl;
		this.configFile = configFile;
		
		updateYMLCache();
	}
	
	public File getConfigFile() {
		return configFile;
	}
	
	public Configuration getConfig() throws FileNotFoundException {
		if (config != null) {
			return config;
		}
		
		updateYMLCache();
		return config;
	}

	public boolean hasConfig() {
		return config != null;
	}
	
  protected Configuration parseConfig() throws FileNotFoundException {
		File currentConfigFile = getConfigFile();
		
		if (!currentConfigFile.canRead()) {
			throw new FileNotFoundException("Can't access file '"+currentConfigFile+"'");				
		}
		
		Configuration config = new Configuration(getConfigFile());
		config.load();
		return config;
  }
	
	/**
	 * @return false if cache couldn't be updated(but an offline version exists)
	 * @throws FileNotFoundException if minecraft.yml is not available
	 */
	public boolean updateYMLCache() throws FileNotFoundException {
		if (config != null && getConfigFile().exists()) {
			return true;
		}
		
		boolean downloaded;
		synchronized (key) {

			try {
				downloader.downloadFile(mirrorPath, fallBackUrl, configFile);
				downloaded = true;
			} catch (DownloadFailedException e) {
				downloaded = false;
				Util.log("Warning: Couldn't download file '%s'", configFile);
				e.printStackTrace();
			} 

			/*
			 * ignore attempted re-reads as long as it has been read atleast once 
			 */
			if (config == null) {
				config = parseConfig();
			} else {
				try {
					config = parseConfig();
				} catch(FileNotFoundException ignore) {
				}
			}
			
		}
		
		return downloaded;
	}
}
