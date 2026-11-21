package systems.heartbeat.craftwright;

import java.nio.file.Path;
import systems.heartbeat.craftwright.internal.ManagedProcess;

/** The running game behind a {@link Client}: where its files are and how to end it. */
final class ClientProcess {
	private static final int QUIT_GRACE_SECONDS = 10;
	private static final int RECENT_LINES = 60;

	private final ManagedProcess process;
	private final Path gameDirectory;
	private final boolean rendering;

	ClientProcess(ManagedProcess process, Path gameDirectory, boolean rendering) {
		this.process = process;
		this.gameDirectory = gameDirectory;
		this.rendering = rendering;
	}

	Path gameDirectory() {
		return gameDirectory;
	}

	boolean rendering() {
		return rendering;
	}

	String recentOutput() {
		return process.tail(RECENT_LINES);
	}

	/** Gives the game a moment to quit by itself, then ends it. */
	void stop() {
		if (!process.waitFor(QUIT_GRACE_SECONDS)) {
			process.destroy();
		}
	}
}
