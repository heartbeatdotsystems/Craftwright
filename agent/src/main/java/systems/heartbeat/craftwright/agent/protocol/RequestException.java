package systems.heartbeat.craftwright.agent.protocol;

/** A request that cannot be fulfilled. The message goes to the test as it is, so it says what to do next. */
public final class RequestException extends RuntimeException {
	public RequestException(String message) {
		super(message);
	}
}
