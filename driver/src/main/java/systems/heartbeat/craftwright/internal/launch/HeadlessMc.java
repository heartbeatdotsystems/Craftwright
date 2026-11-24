package systems.heartbeat.craftwright.internal.launch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import systems.heartbeat.craftwright.CraftwrightException;
import systems.heartbeat.craftwright.internal.Downloads;

/**
 * Starts the real game through HeadlessMC, which installs Minecraft and Fabric on first use
 * and can replace the graphics library so no window and no GPU are needed.
 */
public final class HeadlessMc {
	private static final String VERSION = "3.0.0-RC9";
	private static final String DOWNLOAD = "https://github.com/headlesshq/headlessmc/releases/download/" + VERSION + "/headlessmc.jar";
	/** A virtual display for clients that render. */
	private static final List<String> VIRTUAL_DISPLAY = List.of("xvfb-run", "-a", "-s", "-screen 0 1280x720x24 +extension GLX +render -noreset");
	/** HeadlessMC stays open after the game has exited and prints this line. */
	private static final String GAME_EXITED = "failed with code";

	private final Path cache;

	public HeadlessMc(Path cache) {
		this.cache = cache;
	}

	/** Writes the launcher settings for this client and returns the command that starts the game. */
	public List<String> command(ClientSpec spec, ClientDirectories directories, AgentAddress agent) {
		String javaHome = System.getProperty("java.home");
		writeConfig(spec, directories, javaHome);
		List<String> command = new ArrayList<>();
		if (spec.rendering()) {
			command.addAll(VIRTUAL_DISPLAY);
		}
		command.add(Path.of(javaHome, "bin", "java").toString());
		command.add("-Dhmc.files.location=" + directories.launcher().toAbsolutePath());
		if (spec.rendering()) {
			command.add("-Dhmc.xvfb.check=true");
		}
		command.addAll(List.of("-jar", launcherJar().toAbsolutePath().toString(), "launch", "--offline"));
		if (!spec.rendering()) {
			command.add("-lwjgl");
		}
		command.addAll(List.of("--jvm", gameJvmArguments(spec, agent), "fabric", spec.minecraftVersion()));
		return command;
	}

	public Map<String, String> environment(ClientDirectories directories) {
		return Map.of("HMC_FILES_LOCATION", directories.launcher().toAbsolutePath().toString());
	}

	/** True if this line of launcher output says that the game itself is gone. */
	public static boolean reportsGameExit(String outputLine) {
		return outputLine.contains(GAME_EXITED);
	}

	private Path launcherJar() {
		return Downloads.file(DOWNLOAD, cache.resolve("headlessmc").resolve("headlessmc-" + VERSION + ".jar"));
	}

	private void writeConfig(ClientSpec spec, ClientDirectories directories, String javaHome) {
		try {
			Files.createDirectories(directories.launcher());
			Files.writeString(directories.launcher().resolve("config.properties"), String.join("\n",
				// HeadlessMC liest die Liste ein zweites Mal ohne Escapes: unter Windows gingen die
				// Backslashes verloren. Java nimmt auch Schrägstriche.
				"hmc.java.versions=" + escape(javaHome.replace('\\', '/')),
				"hmc.files.game=" + escape(directories.game().toAbsolutePath().toString()),
				"hmc.files.game-for-each-version=false",
				"hmc.files.mc=" + escape(cache.resolve("minecraft").toAbsolutePath().toString()),
				"hmc.jline.enabled=false",
				"hmc.launcher.crash-report-watcher=true",
				"hmc.assets.dummy=" + !spec.rendering(),
				""), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new CraftwrightException("Cannot write the launcher settings in " + directories.launcher(), e);
		}
	}

	private static String gameJvmArguments(ClientSpec spec, AgentAddress agent) {
		return String.join(" ",
			"-Xms512M",
			"-Xmx" + spec.memory(),
			"-Djava.awt.headless=true",
			"-Dcraftwright.connect=127.0.0.1:" + agent.port(),
			"-Dcraftwright.token=" + agent.token(),
			"-Dcraftwright.client=" + spec.name(),
			"-Dcraftwright.username=" + spec.name());
	}

	/** Backslashes of Windows paths are escape characters in a properties file. */
	private static String escape(String path) {
		return path.replace("\\", "\\\\");
	}
}
