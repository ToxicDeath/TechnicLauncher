package org.spoutcraft.launcher;

import java.io.File;
import java.io.FileNotFoundException;

import org.bukkit.util.config.Configuration;
import org.spoutcraft.launcher.exception.DownloadsFailedException;
import org.spoutcraft.launcher.exception.NoMirrorsAvailableException;


public class CachedYMLFile {
	private static final Object 	key = new Object();
	
	private String	mirrorPath;
	private String	fallBackUrl;
	private File    configFile;
	private Configuration config = null;
	
	public CachedYMLFile(String mirrorPath, String fallBackUrl, File configFile)	{
		this.mirrorPath = mirrorPath;
		this.fallBackUrl = fallBackUrl;
		this.configFile = configFile;
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
		return updateYMLCache(false);
	}
	
	/**
	 * @return false if cache couldn't be updated(but an offline version exists)
	 * @throws FileNotFoundException if minecraft.yml is not available
	 */
	public boolean updateYMLCache(boolean forceUpdate) throws FileNotFoundException {
		if (!forceUpdate && config != null && getConfigFile().exists()) {
			return true;
		}
		
		boolean downloaded;
		synchronized (key) {

			try {
				YmlUtils.downloadYmlFile(mirrorPath, fallBackUrl, configFile);
				downloaded = true;
			} catch (DownloadsFailedException e) {
				downloaded = false;
				Util.log("Warning: Couldn't download file '%s'", configFile);
				Util.log("Reason: '%s'", e.toString());
			} catch (NoMirrorsAvailableException e) {
				downloaded = false;
				Util.log("Warning: no mirror found for '%s'", mirrorPath);
				Util.log("Reason: '%s'", e.toString());
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
