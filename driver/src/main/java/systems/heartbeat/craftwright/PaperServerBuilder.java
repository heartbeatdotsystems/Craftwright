package systems.heartbeat.craftwright;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.server.PaperSpec;

/**
 * Describes a Paper server before it starts.
 *
 * <pre>{@code
 * Server server = craftwright.paper("26.3")
 *     .plugin(Path.of("build/libs/MyPlugin.jar"))
 *     .file("plugins/MyPlugin/config.yml", "welcome: true\n")
 *     .property("spawn-protection", "0")
 *     .start();
 * }</pre>
 */
public final class PaperServerBuilder {
	private final Craftwright session;
	private final String version;
	private final List<Path> plugins = new ArrayList<>();
	private final List<String> pluginUrls = new ArrayList<>();
	private final Map<String, String> files = new LinkedHashMap<>();
	private final Map<String, String> properties = new LinkedHashMap<>();
	private final List<String> jvmArguments = new ArrayList<>();
	private Path template;
	private String memory = "2G";
	private String name = "server";
	private Duration startTimeout = Duration.ofMinutes(5);

	PaperServerBuilder(Craftwright session, String version) {
		this.session = session;
		this.version = version;
		properties.put("online-mode", "false");
		properties.put("white-list", "false");
		properties.put("enforce-whitelist", "false");
		properties.put("spawn-protection", "0");
		properties.put("view-distance", "6");
		properties.put("simulation-distance", "6");
		properties.put("sync-chunk-writes", "false");
		properties.put("enforce-secure-profile", "false");
		properties.put("motd", "Craftwright test server");
	}

	/** Name of the server directory inside the session directory. Needed when a test starts more than one server. */
	public PaperServerBuilder name(String name) {
		this.name = name;
		return this;
	}

	/** Adds a plugin jar. The path may contain {@code *} in the file name, e.g. {@code build/libs/MyPlugin-*.jar}. */
	public PaperServerBuilder plugin(Path jar) {
		plugins.add(jar);
		return this;
	}

	public PaperServerBuilder plugin(String jar) {
		return plugin(Path.of(jar));
	}

	/** Adds a plugin that is downloaded once and cached, e.g. a dependency of the plugin under test. */
	public PaperServerBuilder pluginFromUrl(String url) {
		pluginUrls.add(url);
		return this;
	}

	/** Writes a file into the server directory before the start, e.g. a plugin config. */
	public PaperServerBuilder file(String relativePath, String content) {
		files.put(relativePath, content);
		return this;
	}

	/** Sets a line of {@code server.properties}. */
	public PaperServerBuilder property(String key, String value) {
		properties.put(key, value);
		return this;
	}

	/** Copies this directory into the server directory first: a prepared world, configs, anything. */
	public PaperServerBuilder template(Path directory) {
		this.template = directory;
		return this;
	}

	/** A flat world with nothing in it. Starts faster and keeps tests predictable. */
	public PaperServerBuilder flatWorld() {
		properties.put("level-type", "minecraft:flat");
		properties.put("generate-structures", "false");
		properties.put("spawn-monsters", "false");
		return this;
	}

	public PaperServerBuilder seed(String seed) {
		properties.put("level-seed", seed);
		return this;
	}

	/** Heap size for the server, e.g. {@code 2G}. */
	public PaperServerBuilder memory(String memory) {
		this.memory = memory;
		return this;
	}

	public PaperServerBuilder jvmArg(String arg) {
		jvmArguments.add(arg);
		return this;
	}

	public PaperServerBuilder startTimeout(Duration timeout) {
		this.startTimeout = timeout;
		return this;
	}

	/** Downloads Paper if needed, starts the server and waits until it accepts players. */
	public Server start() {
		PaperSpec spec = new PaperSpec(name, version, plugins, pluginUrls, files, properties, jvmArguments,
			Optional.ofNullable(template), memory, startTimeout);
		PaperServer server = new PaperServer(spec, session.directory(), session.defaultTimeout());
		session.register(server);
		server.start(session.cacheDirectory());
		return server;
	}
}
