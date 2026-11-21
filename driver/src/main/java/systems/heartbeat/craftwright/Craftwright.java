package systems.heartbeat.craftwright;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import systems.heartbeat.craftwright.internal.ControlServer;

/**
 * A test session: starts real servers and real clients, and stops all of them when closed.
 *
 * <pre>{@code
 * try (Craftwright craftwright = Craftwright.create()) {
 *     Server server = craftwright.paper("26.3").plugin("build/libs/MyPlugin.jar").start();
 *     Client alex = craftwright.client("Alex").start();
 *     alex.join(server);
 *     alex.command("help");
 *     alex.chat().await("Help");
 * }
 * }</pre>
 */
public final class Craftwright implements AutoCloseable {
	/** Minecraft versions this release ships a client agent for. */
	public static final List<String> SUPPORTED_MINECRAFT_VERSIONS = List.of("26.3");

	private final Path directory;
	private final Path cacheDirectory;
	private final ControlServer control = new ControlServer();
	private final Deque<AutoCloseable> resources = new ArrayDeque<>();
	private volatile Duration defaultTimeout = Duration.ofSeconds(15);
	private volatile String minecraftVersion = SUPPORTED_MINECRAFT_VERSIONS.get(0);

	private Craftwright(Path directory, Path cacheDirectory) {
		this.directory = directory.toAbsolutePath();
		this.cacheDirectory = cacheDirectory.toAbsolutePath();
	}

	/**
	 * A session that keeps its servers and clients in {@code build/craftwright/<timestamp>}
	 * and its downloads in {@code ~/.craftwright}. Override with the system properties
	 * {@code craftwright.dir} and {@code craftwright.cache} (or the environment variable {@code CRAFTWRIGHT_CACHE}).
	 */
	public static Craftwright create() {
		String base = System.getProperty("craftwright.dir", "build/craftwright");
		return create(Path.of(base).resolve("run-" + System.currentTimeMillis()));
	}

	public static Craftwright create(Path directory) {
		String cache = System.getProperty("craftwright.cache", System.getenv("CRAFTWRIGHT_CACHE"));
		Path cacheDirectory = cache != null ? Path.of(cache) : Path.of(System.getProperty("user.home"), ".craftwright");
		return new Craftwright(directory, cacheDirectory);
	}

	/** Describes a Paper server of the given Minecraft version, e.g. {@code paper("26.3")}. */
	public PaperServerBuilder paper(String version) {
		this.minecraftVersion = version;
		return new PaperServerBuilder(this, version);
	}

	/** Describes a client with this player name. Servers started by Craftwright run in offline mode, so any name works. */
	public ClientBuilder client(String playerName) {
		return new ClientBuilder(this, playerName);
	}

	/** How long {@code await…} calls wait by default. Applies to servers and clients started afterwards. */
	public Craftwright defaultTimeout(Duration timeout) {
		this.defaultTimeout = timeout;
		return this;
	}

	public Duration defaultTimeout() {
		return defaultTimeout;
	}

	/** Where this session keeps its servers, clients and logs. */
	public Path directory() {
		return directory;
	}

	Path cacheDirectory() {
		return cacheDirectory;
	}

	String minecraftVersion() {
		return minecraftVersion;
	}

	ControlServer control() {
		return control;
	}

	synchronized void register(AutoCloseable resource) {
		resources.push(resource);
	}

	/** Quits every client and stops every server of this session. */
	@Override
	public synchronized void close() {
		while (!resources.isEmpty()) {
			try {
				resources.pop().close();
			} catch (Exception ignored) {
				// keep closing the rest
			}
		}
		control.close();
	}
}
