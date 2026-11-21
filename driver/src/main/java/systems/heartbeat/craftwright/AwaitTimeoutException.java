package systems.heartbeat.craftwright;

/** An {@code await…} call did not see what it was waiting for in time. */
public class AwaitTimeoutException extends CraftwrightException {
	public AwaitTimeoutException(String message) {
		super(message);
	}
}
