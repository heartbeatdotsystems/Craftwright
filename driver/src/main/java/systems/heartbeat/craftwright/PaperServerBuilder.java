package systems.heartbeat.craftwright;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	final Craftwright session;
	final String version;
	final List<Path> plugins = new ArrayList<>();
	final List<String> pluginUrls = new ArrayList<>();
	final Map<String, String> files = new LinkedHashMap<>();
	final Map<String, String> properties = new LinkedHashMap<>();
	final List<String> jvmArgs = new ArrayList<>();
	Path template;
	String memory = "2G";
	String name = "server";
	Duration startTimeout = Duration.ofMinutes(5);

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
		jvmArgs.add(arg);
		return this;
	}

	public PaperServerBuilder startTimeout(Duration timeout) {
		this.startTimeout = timeout;
		return this;
	}

	/** Downloads Paper if needed, starts the server and waits until it accepts players. */
	public Server start() {
		PaperServer server = new PaperServer(this);
		session.register(server);
		server.start();
		return server;
	}
}
