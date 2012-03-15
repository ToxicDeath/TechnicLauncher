package org.spoutcraft.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.bukkit.util.config.Configuration;


public class CachedYmlFile {
	private final Object 	key = new Object();
	
	private String	mirrorPath;
	private String	fallBackUrl;
	private File    configFile;
	private Configuration config = null;
	
	public CachedYmlFile(String mirrorPath, String fallBackUrl, File configFile) throws FileNotFoundException {
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
		
		boolean downloaded = false;
		synchronized (key) {

			try {
				String url = MirrorUtils.getMirrorUrl(mirrorPath, fallBackUrl);
				if (url != null && DownloadUtils.downloadFile(url, configFile.getPath()).isSuccess()) {
					downloaded = true;
				} else {
					Util.log("Warning: Couldn't download file '%s'", configFile);
				}
			} catch (IOException e) {
				Util.log("Warning: Exception during download '%s'", configFile);
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
