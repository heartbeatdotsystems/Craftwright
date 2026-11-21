package systems.heartbeat.craftwright.internal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import systems.heartbeat.craftwright.CraftwrightException;

/** Listens on localhost. Every started client connects back here and says which client it is. */
public final class ControlServer implements AutoCloseable {
	/** Version of the wire protocol. The agent refuses a driver with another major version. */
	public static final int PROTOCOL_VERSION = 1;

	private final ServerSocket serverSocket;
	private final String token;
	private final Map<String, AgentConnection> connections = new ConcurrentHashMap<>();

	public ControlServer() {
		try {
			this.serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
		} catch (IOException e) {
			throw new CraftwrightException("Cannot open the control port on localhost", e);
		}
		this.token = Long.toHexString(new java.security.SecureRandom().nextLong());
		Thread thread = new Thread(this::acceptLoop, "craftwright-control");
		thread.setDaemon(true);
		thread.start();
	}

	public int port() {
		return serverSocket.getLocalPort();
	}

	/** Shared secret, so that no other local process can pose as a client. */
	public String token() {
		return token;
	}

	public AgentConnection awaitClient(String clientId, Duration timeout, Supplier<String> context) {
		return Await.until("client '" + clientId + "' to start and report in", timeout,
			() -> Optional.ofNullable(connections.get(clientId)), context);
	}

	private void acceptLoop() {
		while (!serverSocket.isClosed()) {
			try {
				Socket socket = serverSocket.accept();
				socket.setTcpNoDelay(true);
				BufferedReader reader = AgentConnection.readerOf(socket);
				String line = reader.readLine();
				if (line == null) {
					socket.close();
					continue;
				}
				JsonObject hello = JsonParser.parseString(line).getAsJsonObject();
				if (!hello.has("token") || !token.equals(hello.get("token").getAsString())) {
					socket.close();
					continue;
				}
				AgentConnection connection = new AgentConnection(socket, reader, hello);
				connections.put(connection.clientId(), connection);
			} catch (IOException | RuntimeException e) {
				if (serverSocket.isClosed()) {
					return;
				}
			}
		}
	}

	@Override
	public void close() {
		try {
			serverSocket.close();
		} catch (IOException ignored) {
			// already closed
		}
		connections.values().forEach(AgentConnection::close);
	}
}
