package systems.heartbeat.craftwright.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import systems.heartbeat.craftwright.CraftwrightException;

/**
 * One line of JSON per message, in both directions.
 * Requests carry an id and get exactly one response, events carry no id.
 */
public final class AgentConnection implements AutoCloseable {
	private static final long REQUEST_TIMEOUT_SECONDS = 30;

	private final Socket socket;
	private final BufferedWriter writer;
	private final JsonObject hello;
	private final AtomicLong nextId = new AtomicLong(1);
	private final Map<Long, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
	private final List<Consumer<JsonObject>> eventListeners = new CopyOnWriteArrayList<>();
	private volatile boolean closed;

	AgentConnection(Socket socket, BufferedReader reader, JsonObject hello) throws IOException {
		this.socket = socket;
		this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
		this.hello = hello;
		Thread thread = new Thread(() -> readLoop(reader), "craftwright-agent-" + clientId());
		thread.setDaemon(true);
		thread.start();
	}

	static BufferedReader readerOf(Socket socket) throws IOException {
		return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
	}

	public JsonObject hello() {
		return hello;
	}

	public String clientId() {
		return hello.get("client").getAsString();
	}

	public boolean isClosed() {
		return closed;
	}

	public void onEvent(Consumer<JsonObject> listener) {
		eventListeners.add(listener);
	}

	/** Sends a request and waits for the result. */
	public JsonObject call(String method, JsonObject params) {
		if (closed) {
			throw new CraftwrightException("Client '" + clientId() + "' is no longer running (" + method + ")");
		}
		long id = nextId.getAndIncrement();
		CompletableFuture<JsonObject> future = new CompletableFuture<>();
		pending.put(id, future);
		JsonObject request = new JsonObject();
		request.addProperty("id", id);
		request.addProperty("method", method);
		request.add("params", params == null ? new JsonObject() : params);
		try {
			synchronized (writer) {
				writer.write(request.toString());
				writer.write('\n');
				writer.flush();
			}
			JsonObject response = future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (response.has("error")) {
				JsonObject error = response.getAsJsonObject("error");
				throw new CraftwrightException(method + " failed on client '" + clientId() + "': "
					+ error.get("message").getAsString());
			}
			JsonElement result = response.get("result");
			return result != null && result.isJsonObject() ? result.getAsJsonObject() : new JsonObject();
		} catch (IOException e) {
			throw new CraftwrightException("Lost connection to client '" + clientId() + "' during " + method, e);
		} catch (TimeoutException e) {
			throw new CraftwrightException("Client '" + clientId() + "' did not answer " + method + " within "
				+ REQUEST_TIMEOUT_SECONDS + " s");
		} catch (ExecutionException e) {
			throw new CraftwrightException("Client '" + clientId() + "' stopped during " + method, e.getCause());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CraftwrightException("Interrupted during " + method);
		} finally {
			pending.remove(id);
		}
	}

	private void readLoop(BufferedReader reader) {
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				JsonObject message = JsonParser.parseString(line).getAsJsonObject();
				if (message.has("id")) {
					CompletableFuture<JsonObject> future = pending.get(message.get("id").getAsLong());
					if (future != null) {
						future.complete(message);
					}
				} else if (message.has("event")) {
					for (Consumer<JsonObject> listener : eventListeners) {
						listener.accept(message);
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			// handled below: every pending call fails
		} finally {
			closed = true;
			for (CompletableFuture<JsonObject> future : pending.values()) {
				future.completeExceptionally(new IOException("connection closed"));
			}
		}
	}

	@Override
	public void close() {
		closed = true;
		try {
			socket.close();
		} catch (IOException ignored) {
			// already gone
		}
	}
}
