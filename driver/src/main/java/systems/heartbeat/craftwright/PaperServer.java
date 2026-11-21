package systems.heartbeat.craftwright;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import systems.heartbeat.craftwright.internal.Await;
import systems.heartbeat.craftwright.internal.ManagedProcess;
import systems.heartbeat.craftwright.internal.Ports;
import systems.heartbeat.craftwright.internal.server.ConsoleLog;
import systems.heartbeat.craftwright.internal.server.PaperSpec;
import systems.heartbeat.craftwright.internal.server.ServerDirectory;

/** A Paper server process started from a {@link PaperSpec}. */
final class PaperServer implements Server {
	private static final String HOST = "127.0.0.1";
	private static final Pattern STARTED = Pattern.compile("Done \\(.*\\)! For help");
	private static final int STOP_GRACE_SECONDS = 60;
	private static final int OUTPUT_ON_FAILURE = 60;

	private final PaperSpec spec;
	private final Path directory;
	private final Duration defaultTimeout;
	private final int port = Ports.free();
	private ManagedProcess process;
	private ConsoleLog console;

	PaperServer(PaperSpec spec, Path sessionDirectory, Duration defaultTimeout) {
		this.spec = spec;
		this.directory = sessionDirectory.resolve(spec.name());
		this.defaultTimeout = defaultTimeout;
	}

	void start(Path cache) {
		ServerDirectory.prepare(directory, spec, port, cache);
		launch();
	}

	private void launch() {
		List<String> command = new ArrayList<>();
		command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
		command.add("-Xmx" + spec.memory());
		command.add("-Dcom.mojang.eula.agree=true");
		command.add("-Dpaper.disableStartupVersionCheck=true");
		command.addAll(spec.jvmArguments());
		command.addAll(List.of("-jar", ServerDirectory.SERVER_JAR, "--nogui"));
		process = new ManagedProcess("server " + spec.name(), command, directory, Map.of(), directory.resolve("console.log"));
		console = new ConsoleLog(process);
		Await.untilTrue("the Paper " + spec.version() + " server to finish starting", spec.startTimeout(), () -> {
			if (!process.isAlive()) {
				throw new CraftwrightException("The server stopped while starting. Last output:\n" + process.tail(OUTPUT_ON_FAILURE));
			}
			return process.lines().stream().anyMatch(line -> STARTED.matcher(line).find());
		}, () -> "Last output:\n" + process.tail(OUTPUT_ON_FAILURE));
	}

	@Override
	public String host() {
		return HOST;
	}

	@Override
	public int port() {
		return port;
	}

	@Override
	public Path directory() {
		return directory;
	}

	@Override
	public void console(String command) {
		process.writeLine(command.startsWith("/") ? command.substring(1) : command);
	}

	@Override
	public List<String> log() {
		return console.lines();
	}

	@Override
	public String awaitLog(String words) {
		return awaitLog(words, defaultTimeout);
	}

	@Override
	public String awaitLog(String words, Duration timeout) {
		return console.await("a console line containing \"" + words + "\"", line -> line.contains(words), timeout);
	}

	@Override
	public String awaitLog(Pattern pattern, Duration timeout) {
		return console.await("a console line matching /" + pattern + "/", line -> pattern.matcher(line).find(), timeout);
	}

	@Override
	public boolean logContains(String words) {
		return console.contains(words);
	}

	@Override
	public ServerStatus status() {
		return ServerStatus.ping(HOST, port);
	}

	@Override
	public void restart() {
		stop();
		launch();
	}

	@Override
	public void stop() {
		if (!isRunning()) {
			return;
		}
		try {
			process.writeLine("stop");
		} catch (CraftwrightException ignored) {
			// ended below
		}
		if (!process.waitFor(STOP_GRACE_SECONDS)) {
			process.destroy();
		}
	}

	@Override
	public boolean isRunning() {
		return process != null && process.isAlive();
	}
}
