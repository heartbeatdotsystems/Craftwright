package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import systems.heartbeat.craftwright.internal.AgentConnection;
import systems.heartbeat.craftwright.internal.Await;

/**
 * The line to one client's agent, as the parts of the API need it: send a request,
 * know whose client it is, and wait with that client's timeout.
 */
final class Remote {
	private final String name;
	private final AgentConnection connection;
	private volatile Duration timeout;

	Remote(String name, AgentConnection connection, Duration timeout) {
		this.name = name;
		this.connection = connection;
		this.timeout = timeout;
	}

	String name() {
		return name;
	}

	Duration timeout() {
		return timeout;
	}

	void timeout(Duration timeout) {
		this.timeout = timeout;
	}

	AgentConnection connection() {
		return connection;
	}

	JsonObject call(String method) {
		return connection.call(method, null);
	}

	JsonObject call(String method, JsonObject params) {
		return connection.call(method, params);
	}

	/** Polls until the probe has a value. The failure names the client and adds {@code context}. */
	<T> T await(String what, Duration limit, Supplier<Optional<T>> probe, Supplier<String> context) {
		return Await.until(what + " for " + name, limit, probe, context);
	}

	<T> T await(String what, Supplier<Optional<T>> probe, Supplier<String> context) {
		return await(what, timeout, probe, context);
	}

	void awaitTrue(String what, Duration limit, Supplier<Boolean> probe, Supplier<String> context) {
		Await.untilTrue(what + " for " + name, limit, probe, context);
	}

	void awaitTrue(String what, Supplier<Boolean> probe, Supplier<String> context) {
		awaitTrue(what, timeout, probe, context);
	}
}
