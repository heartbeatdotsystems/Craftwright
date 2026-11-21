package systems.heartbeat.craftwright;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.AgentConnection;
import systems.heartbeat.craftwright.internal.ManagedProcess;
import systems.heartbeat.craftwright.internal.launch.AgentAddress;
import systems.heartbeat.craftwright.internal.launch.AgentJar;
import systems.heartbeat.craftwright.internal.launch.ClientDirectories;
import systems.heartbeat.craftwright.internal.launch.ClientSpec;
import systems.heartbeat.craftwright.internal.launch.FirstLaunch;
import systems.heartbeat.craftwright.internal.launch.GameDirectory;
import systems.heartbeat.craftwright.internal.launch.HeadlessMc;

/**
 * Describes a client before it starts.
 *
 * <pre>{@code
 * Client alex = craftwright.client("Alex").start();
 * Client camera = craftwright.client("Camera").rendering(true).start();
 * }</pre>
 */
public final class ClientBuilder {
	private static final int OUTPUT_ON_FAILURE = 60;
	/** A client that asks nothing on first start, makes no noise and draws as little as possible. */
	private static final Map<String, String> DEFAULT_OPTIONS = defaultOptions();

	private final Craftwright session;
	private final String name;
	private final List<Path> mods = new ArrayList<>();
	private final Map<String, String> options = new LinkedHashMap<>(DEFAULT_OPTIONS);
	private String minecraftVersion;
	private boolean rendering;
	private String memory = "2G";
	private Duration startTimeout = Duration.ofMinutes(10);

	ClientBuilder(Craftwright session, String name) {
		if (!name.matches("[A-Za-z0-9_]{1,16}")) {
			throw new IllegalArgumentException("A player name has 1 to 16 letters, digits or underscores: " + name);
		}
		this.session = session;
		this.name = name;
		this.minecraftVersion = session.minecraftVersion();
	}

	private static Map<String, String> defaultOptions() {
		Map<String, String> options = new LinkedHashMap<>();
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
		return options;
	}

	/** The Minecraft version of the client. Defaults to the version of the server started last. */
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

	/** Maximum heap of the game, e.g. {@code 2G}. */
	public ClientBuilder memory(String memory) {
		this.memory = memory;
		return this;
	}

	/** Adds a Fabric mod next to the agent, e.g. to test a server together with its client mod. */
	public ClientBuilder mod(Path jar) {
		mods.add(jar);
		return this;
	}

	/** Sets a line of {@code options.txt}. */
	public ClientBuilder option(String key, String value) {
		options.put(key, value);
		return this;
	}

	/** The game language, e.g. {@code de_de}. Servers see it as the player's locale. */
	public ClientBuilder language(String locale) {
		return option("lang", locale);
	}

	/** How long the first start may take. It includes downloading the game. */
	public ClientBuilder startTimeout(Duration timeout) {
		this.startTimeout = timeout;
		return this;
	}

	/** Installs the game if needed, starts it and waits until it reaches the title screen. */
	public Client start() {
		if (!AgentJar.existsFor(minecraftVersion)) {
			throw new CraftwrightException("This Craftwright version has no agent for Minecraft " + minecraftVersion
				+ ". Supported: " + Craftwright.SUPPORTED_MINECRAFT_VERSIONS);
		}
		ClientSpec spec = new ClientSpec(name, minecraftVersion, rendering, memory, mods, options);
		ClientDirectories directories = ClientDirectories.in(session.directory(), name);
		GameDirectory.prepare(directories.game(), spec);
		HeadlessMc launcher = new HeadlessMc(session.cacheDirectory());
		List<String> command = launcher.command(spec, directories, new AgentAddress(session.control().port(), session.control().token()));

		return FirstLaunch.guarded(minecraftVersion, () -> {
			ManagedProcess process = new ManagedProcess("client " + name, command, directories.root(),
				launcher.environment(directories), directories.launcherLog());
			AgentConnection connection = awaitAgent(process);
			Client client = new Client(new Remote(name, connection, session.defaultTimeout()),
				new ClientProcess(process, directories.game(), rendering));
			session.register(client);
			return client;
		});
	}

	private AgentConnection awaitAgent(ManagedProcess process) {
		try {
			return session.control().awaitClient(name, startTimeout, () -> startFailure(process),
				() -> "Last output:\n" + process.tail(OUTPUT_ON_FAILURE));
		} catch (RuntimeException e) {
			process.destroy();
			throw e;
		}
	}

	/** Why waiting for the agent makes no sense any more, if that is the case. */
	private static Optional<String> startFailure(ManagedProcess process) {
		if (!process.isAlive()) {
			return Optional.of("the launcher process has stopped");
		}
		return process.lines().stream().filter(HeadlessMc::reportsGameExit).findFirst()
			.map(line -> "the game exited (" + line.trim() + ")");
	}
}
