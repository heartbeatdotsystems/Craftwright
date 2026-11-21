package systems.heartbeat.craftwright.internal.launch;

import java.nio.file.Path;

/**
 * Where one client keeps its files inside the session directory.
 *
 * @param root     the client's own directory with the launcher log
 * @param game     the {@code .minecraft} of this client: mods, options, logs
 * @param launcher settings of the launcher for this client
 */
public record ClientDirectories(Path root, Path game, Path launcher) {
	public static ClientDirectories in(Path sessionDirectory, String clientName) {
		Path root = sessionDirectory.resolve("clients").resolve(clientName);
		return new ClientDirectories(root, root.resolve("game"), root.resolve("headlessmc"));
	}

	public Path launcherLog() {
		return root.resolve("launcher.log");
	}
}
