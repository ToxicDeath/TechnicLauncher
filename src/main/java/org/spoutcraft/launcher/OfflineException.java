package org.spoutcraft.launcher;

/**
 * An exception that is thrown when network access isn't available but offline 
 *  files exist.
 *
 */
public class OfflineException extends Exception {
	private static final long serialVersionUID = 1L;

	public OfflineException() {
		super();
	}

	public OfflineException(String message, Throwable cause) {
		super(message, cause);
	}

	public OfflineException(String message) {
		super(message);
	}

	public OfflineException(Throwable cause) {
		super(cause);
	}
}