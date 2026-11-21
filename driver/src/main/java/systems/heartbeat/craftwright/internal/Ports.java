package systems.heartbeat.craftwright.internal;

import java.io.IOException;
import java.net.ServerSocket;
import systems.heartbeat.craftwright.CraftwrightException;

/** Picks ports nobody uses, so several sessions can run on one machine. */
public final class Ports {
	private Ports() {
	}

	public static int free() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new CraftwrightException("No free port on this machine", e);
		}
	}
}
