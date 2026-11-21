package systems.heartbeat.craftwright;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/** A real Minecraft server started for the test. */
public interface Server extends AutoCloseable {

	String host();

	int port();

	/** The server's directory with {@code plugins/}, the world and {@code logs/}. */
	Path directory();

	/** Types a command into the server console, without a slash. */
	void console(String command);

	/** Everything the server printed so far, one entry per line. */
	List<String> log();

	/** Waits until a console line contains the given words and returns that line. Each line satisfies one await. */
	String awaitLog(String contains);

	String awaitLog(String contains, Duration timeout);

	String awaitLog(Pattern pattern, Duration timeout);

	/** True if any console line so far contains the given words. */
	boolean logContains(String part);

	/** What the multiplayer server list would show for this server. */
	ServerStatus status();

	/** Stops the server with {@code stop} and starts it again in the same directory. */
	void restart();

	/** Stops the server cleanly. */
	void stop();

	boolean isRunning();

	@Override
	default void close() {
		stop();
	}
}
