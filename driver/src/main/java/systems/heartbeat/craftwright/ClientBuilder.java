package systems.heartbeat.craftwright;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import systems.heartbeat.craftwright.internal.AgentConnection;
import systems.heartbeat.craftwright.internal.Downloads;
import systems.heartbeat.craftwright.internal.ManagedProcess;

/**
 * Describes a client before it starts.
 *
 * <pre>{@code
 * Client alex = craftwright.client("Alex").start();
 * Client camera = craftwright.client("Camera").rendering(true).start();
 * }</pre>
 */
public final class ClientBuilder {
	private static final String HEADLESSMC_VERSION = System.getProperty("craftwright.headlessmc.version", "3.0.0-RC9");
	private static final Object FIRST_LAUNCH_LOCK = new Object();
	private static final java.util.Set<String> INSTALLED = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private final Craftwright session;
	private final String name;
	private String minecraftVersion;
	private boolean rendering;
	private String memory = "2G";
	private Duration startTimeout = Duration.ofMinutes(10);
	private final List<Path> mods = new ArrayList<>();
	private final Map<String, String> options = new LinkedHashMap<>();

	ClientBuilder(Craftwright session, String name) {
		if (!name.matches("[A-Za-z0-9_]{1,16}")) {
			throw new IllegalArgumentException("A player name has 1 to 16 letters, digits or underscores: " + name);
		}
		this.session = session;
		this.name = name;
		this.minecraftVersion = session.minecraftVersion();
		options.put("onboardAccessibility", "false");
		options.put("pauseOnLostFocus", "false");
		options.put("skipMultiplayerWarning", "true");
		options.put("joinedFirstServer", "true");
		options.put("tutorialStep", "none");
		options.put("narrator", "0");
		options.put("soundCategory_master", "0.0");
		options.put("renderDistance", "4");
		options.put("simulationDistance", "5");
		options.put("maxFps", "30");
	}

	/** Minecraft version of the client. Defaults to the version of the session. */
	public ClientBuilder minecraft(String version) {
		this.minecraftVersion = version;
		return this;
	}

	/**
	 * With rendering the client draws real frames into a virtual display (Xvfb on Linux),
	 * which allows screenshots but is slower. Without it nothing is drawn at all.
	 */
	public ClientBuilder rendering(boolean rendering) {
		this.rendering = rendering;
		return this;
	}

	/** Heap size for the client, e.g. {@code 2G}. */
	public ClientBuilder memory(String memory) {
		this.memory = memory;
		return this;
	}

	/** Adds another Fabric mod to this client, e.g. the client half of what is being tested. */
	public ClientBuilder mod(Path jar) {
		mods.add(jar);
		return this;
	}

	/** Sets a line of the client's {@code options.txt}, e.g. {@code option("lang", "de_de")}. */
	public ClientBuilder option(String key, String value) {
		options.put(key, value);
		return this;
	}

	/** Shortcut for the game language, e.g. {@code de_de}. */
	public ClientBuilder language(String locale) {
		return option("lang", locale);
	}

	public ClientBuilder startTimeout(Duration timeout) {
		this.startTimeout = timeout;
		return this;
	}

	/** Installs the game if needed, starts it and waits until it reaches the title screen. */
	public Client start() {
		Path cache = session.cacheDirectory();
		Path clientDirectory = session.directory().resolve("clients").resolve(name);
		Path gameDirectory = clientDirectory.resolve("game");
		Path launcherDirectory = clientDirectory.resolve("headlessmc");
		Path headlessMc = Downloads.file("https://github.com/headlesshq/headlessmc/releases/download/" + HEADLESSMC_VERSION
			+ "/headlessmc.jar", cache.resolve("headlessmc").resolve("headlessmc-" + HEADLESSMC_VERSION + ".jar"));
		String javaHome = System.getProperty("java.home");
		try {
			Files.createDirectories(gameDirectory.resolve("mods"));
			Files.createDirectories(launcherDirectory);
			Files.copy(agentJar(cache), gameDirectory.resolve("mods").resolve("craftwright-agent.jar"), StandardCopyOption.REPLACE_EXISTING);
			Path fabricApi = fabricApi(cache);
			Files.copy(fabricApi, gameDirectory.resolve("mods").resolve(fabricApi.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			for (Path mod : mods) {
				Files.copy(mod, gameDirectory.resolve("mods").resolve(mod.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
			StringBuilder optionsFile = new StringBuilder();
			options.forEach((key, value) -> optionsFile.append(key).append(':').append(value).append('\n'));
			Files.writeString(gameDirectory.resolve("options.txt"), optionsFile.toString(), StandardCharsets.UTF_8);
			Files.writeString(launcherDirectory.resolve("config.properties"), String.join("\n",
				"hmc.java.versions=" + escape(javaHome),
				"hmc.files.game=" + escape(gameDirectory.toAbsolutePath().toString()),
				"hmc.files.game-for-each-version=false",
				"hmc.files.mc=" + escape(cache.resolve("minecraft").toAbsolutePath().toString()),
				"hmc.jline.enabled=false",
				"hmc.launcher.crash-report-watcher=true",
				"hmc.assets.dummy=" + !rendering,
				""), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new CraftwrightException("Cannot prepare the client directory " + clientDirectory, e);
		}

		String jvmArgs = String.join(" ",
			"-Xms512M",
			"-Xmx" + memory,
			"-Djava.awt.headless=true",
			"-Dcraftwright.connect=127.0.0.1:" + session.control().port(),
			"-Dcraftwright.token=" + session.control().token(),
			"-Dcraftwright.client=" + name,
			"-Dcraftwright.username=" + name);
		List<String> command = new ArrayList<>();
		if (rendering) {
			command.addAll(List.of("xvfb-run", "-a", "-s", "-screen 0 1280x720x24 +extension GLX +render -noreset"));
		}
		command.add(Path.of(javaHome, "bin", "java").toString());
		command.add("-Dhmc.files.location=" + launcherDirectory.toAbsolutePath());
		if (rendering) {
			command.add("-Dhmc.xvfb.check=true");
		}
		command.addAll(List.of("-jar", headlessMc.toAbsolutePath().toString(), "launch", "--offline"));
		if (!rendering) {
			command.add("-lwjgl");
		}
		command.addAll(List.of("--jvm", jvmArgs, "fabric", minecraftVersion));

		Map<String, String> environment = Map.of("HMC_FILES_LOCATION", launcherDirectory.toAbsolutePath().toString());
		ManagedProcess process;
		AgentConnection connection;
		// The first launch of a version downloads the game. Later launches may run side by side.
		if (INSTALLED.contains(minecraftVersion)) {
			process = new ManagedProcess("client " + name, command, clientDirectory, environment, clientDirectory.resolve("launcher.log"));
			connection = awaitAgent(process);
		} else {
			synchronized (FIRST_LAUNCH_LOCK) {
				process = new ManagedProcess("client " + name, command, clientDirectory, environment, clientDirectory.resolve("launcher.log"));
				connection = awaitAgent(process);
				INSTALLED.add(minecraftVersion);
			}
		}
		ManagedProcess started = process;
		Client client = new Client(name, connection, () -> {
			if (!started.waitFor(10)) {
				started.destroy();
			}
		}, () -> started.tail(60), gameDirectory, rendering, session.defaultTimeout());
		session.register(client);
		return client;
	}

	private AgentConnection awaitAgent(ManagedProcess process) {
		try {
			return session.control().awaitClient(name, startTimeout, () -> {
				if (!process.isAlive()) {
					return java.util.Optional.of("the launcher process has stopped");
				}
				// HeadlessMC stays open after the game has exited, so its output is the only sign.
				return process.lines().stream().filter(line -> line.contains("failed with code")).findFirst()
					.map(line -> "the game exited (" + line.trim() + ")");
			}, () -> "Last output:\n" + process.tail(60));
		} catch (RuntimeException e) {
			process.destroy();
			throw e;
		}
	}

	private Path agentJar(Path cache) throws IOException {
		String override = System.getProperty("craftwright.agent.jar");
		if (override != null) {
			return Path.of(override);
		}
		String resource = "/craftwright/agents/fabric-" + minecraftVersion + ".jar";
		try (InputStream in = ClientBuilder.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new CraftwrightException("This Craftwright version has no agent for Minecraft " + minecraftVersion
					+ ". Supported: " + Craftwright.SUPPORTED_MINECRAFT_VERSIONS);
			}
			Path target = session.directory().resolve("agent-fabric-" + minecraftVersion + ".jar");
			Files.createDirectories(target.getParent());
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			return target;
		}
	}

	private Path fabricApi(Path cache) {
		Path directory = cache.resolve("fabric-api").resolve(minecraftVersion);
		try {
			if (Files.isDirectory(directory)) {
				try (var files = Files.list(directory)) {
					var existing = files.filter(file -> file.toString().endsWith(".jar")).findFirst();
					if (existing.isPresent()) {
						return existing.get();
					}
				}
			}
		} catch (IOException ignored) {
			// downloaded again below
		}
		String query = "game_versions=" + URLEncoder.encode("[\"" + minecraftVersion + "\"]", StandardCharsets.UTF_8)
			+ "&loaders=" + URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
		JsonArray versions = JsonParser.parseString(Downloads.text("https://api.modrinth.com/v2/project/fabric-api/version?" + query)).getAsJsonArray();
		if (versions.isEmpty()) {
			throw new CraftwrightException("No Fabric API release found for Minecraft " + minecraftVersion);
		}
		var file = versions.get(0).getAsJsonObject().getAsJsonArray("files").get(0).getAsJsonObject();
		return Downloads.file(file.get("url").getAsString(), directory.resolve(file.get("filename").getAsString()));
	}

	private static String escape(String path) {
		return path.replace("\\", "\\\\");
	}
}
