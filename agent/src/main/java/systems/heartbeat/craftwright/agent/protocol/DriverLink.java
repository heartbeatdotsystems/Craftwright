package systems.heartbeat.craftwright.agent.protocol;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** The socket to the driver: one JSON object per line in both directions. Knows nothing about the game. */
public final class DriverLink {
	private final Socket socket;
	private final BufferedWriter writer;

	private DriverLink(Socket socket) throws IOException {
		this.socket = socket;
		this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
	}

	public static DriverLink open(String host, int port) throws IOException {
		Socket socket = new Socket(host, port);
		socket.setTcpNoDelay(true);
		return new DriverLink(socket);
	}

	/** Safe from any thread. A lost connection is noticed by the reader, not here. */
	public void send(JsonObject message) {
		try {
			synchronized (writer) {
				writer.write(message.toString());
				writer.write('\n');
				writer.flush();
			}
		} catch (IOException ignored) {
			// the reader thread reports the closed connection
		}
	}

	/** Reads on a daemon thread until the driver goes away, then calls {@code onClosed} once. */
	public void listen(Consumer<JsonObject> onRequest, Runnable onClosed) {
		Thread reader = new Thread(() -> {
			try (BufferedReader lines = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = lines.readLine()) != null) {
					if (!line.isEmpty()) {
						onRequest.accept(JsonParser.parseString(line).getAsJsonObject());
					}
				}
			} catch (IOException | RuntimeException ignored) {
				// treated like a closed connection
			}
			onClosed.run();
		}, "craftwright-agent");
		reader.setDaemon(true);
		reader.start();
	}
}
