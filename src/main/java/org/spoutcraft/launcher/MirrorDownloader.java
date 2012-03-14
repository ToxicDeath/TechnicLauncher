package org.spoutcraft.launcher;

import java.io.File;

import org.spoutcraft.launcher.exception.DownloadFailedException;

/**
 *  
 *
 */
public interface MirrorDownloader {

	public void downloadFile(String relativePath, String fallbackUrl) throws DownloadFailedException;
	public void downloadFile(String relativePath, String fallbackUrl, File outputFile) throws DownloadFailedException;
	
}
