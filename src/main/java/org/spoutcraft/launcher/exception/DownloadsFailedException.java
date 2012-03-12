package org.spoutcraft.launcher.exception;

public class DownloadsFailedException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5390206432346852674L;
	private String[] urls; 
	private String message;
	
	
	public DownloadsFailedException() {
		super();
	}

	public DownloadsFailedException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DownloadsFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public DownloadsFailedException(String message) {
		super(message);
	}

	public DownloadsFailedException(String message, String[] urls, Throwable cause) {
		super(null, cause);
		
		StringBuilder sb = new StringBuilder();
		
		if (message == null) {
			sb.append("Failed to download files");
		} else {
			sb.append(message);
		}
		
		if (urls != null) {
			sb.append(" : ");
			for(String url : urls) {
				sb.append("\n").append(url);
			}
		}
		
		message = sb.toString();
	}

	public DownloadsFailedException(String message, String[] urls) {
		this(message, urls, null);
	}

	public DownloadsFailedException(String message, String url, Throwable cause) {
		this(message, new String[]{url}, cause);
	}

	public DownloadsFailedException(String message, String url) {
		this(message, url, null);
	}
	
	public DownloadsFailedException(String[] urls, Throwable cause) {
		this(null, urls, cause);
	}

	public DownloadsFailedException(String[] urls) {
		this(urls, null);
	}

	public DownloadsFailedException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getMessage() {
		if (message == null) {
			return super.getMessage();
		}
		return message;
	}

	public String[] getUrls() {
		return urls;
	}
	
	
}
