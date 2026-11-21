package systems.heartbeat.craftwright;

/** Something went wrong while driving a server or a client. */
public class CraftwrightException extends RuntimeException {
	public CraftwrightException(String message) {
		super(message);
	}

	public CraftwrightException(String message, Throwable cause) {
		super(message, cause);
	}
}
