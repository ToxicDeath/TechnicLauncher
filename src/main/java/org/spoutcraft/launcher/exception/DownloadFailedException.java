package org.spoutcraft.launcher.exception;

import java.io.IOException;

public class DownloadFailedException extends IOException {

	private static final long	serialVersionUID	= 8271732284321983462L;

	public DownloadFailedException() {
		super();
	}

	public DownloadFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public DownloadFailedException(String message) {
		super(message);
	}

	public DownloadFailedException(Throwable cause) {
		super(cause);
	}


}
