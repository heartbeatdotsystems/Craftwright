package systems.heartbeat.craftwright.agent;

/** A request that cannot be fulfilled, with a message meant for the test author. */
final class RequestException extends RuntimeException {
	RequestException(String message) {
		super(message);
	}
}
