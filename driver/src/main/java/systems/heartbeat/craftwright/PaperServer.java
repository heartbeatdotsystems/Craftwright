package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import systems.heartbeat.craftwright.internal.Await;
import systems.heartbeat.craftwright.internal.Downloads;
import systems.heartbeat.craftwright.internal.ManagedProcess;

final class PaperServer implements Server {
	private static final Pattern DONE = Pattern.compile("Done \\(.*\\)! For help");

	private final PaperServerBuilder config;
	private final Path directory;
	private final int port;
	private ManagedProcess process;
	private final List<Integer> consumedLines = new ArrayList<>();

	PaperServer(PaperServerBuilder config) {
		this.config = config;
		this.directory = config.session.directory().resolve(config.name);
		this.port = freePort();
	}

	void start() {
		prepareDirectory();
		launch();
	}

	private void prepareDirectory() {
		try {
			Files.createDirectories(directory.resolve("plugins"));
			if (config.template != null) {
				copyTree(config.template, directory);
			}
			Path paperJar = paperJar(config.version, config.session.cacheDirectory());
			Files.copy(paperJar, directory.resolve("paper.jar"), StandardCopyOption.REPLACE_EXISTING);
			for (Path plugin : config.plugins) {
				Path resolved = resolveGlob(plugin);
				Files.copy(resolved, directory.resolve("plugins").resolve(resolved.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
			for (String url : config.pluginUrls) {
				String fileName = url.substring(url.lastIndexOf('/') + 1);
				Path cached = Downloads.file(url, config.session.cacheDirectory().resolve("plugins").resolve(fileName));
				Files.copy(cached, directory.resolve("plugins").resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
			}
			for (Map.Entry<String, String> file : config.files.entrySet()) {
				Path target = directory.resolve(file.getKey());
				Files.createDirectories(target.getParent());
				Files.writeString(target, file.getValue(), StandardCharsets.UTF_8);
			}
			Files.writeString(directory.resolve("eula.txt"), "eula=true\n");
			StringBuilder properties = new StringBuilder();
			config.properties.forEach((key, value) -> properties.append(key).append('=').append(value).append('\n'));
			properties.append("server-ip=127.0.0.1\n").append("server-port=").append(port).append('\n');
			Files.writeString(directory.resolve("server.properties"), properties.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new CraftwrightException("Cannot prepare the server directory " + directory, e);
		}
	}

	private void launch() {
		List<String> command = new ArrayList<>();
		command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
		command.add("-Xmx" + config.memory);
		command.add("-Dcom.mojang.eula.agree=true");
		command.add("-Dpaper.disableStartupVersionCheck=true");
		command.addAll(config.jvmArgs);
		command.add("-jar");
		command.add("paper.jar");
		command.add("--nogui");
		process = new ManagedProcess("server " + config.name, command, directory, Map.of(), directory.resolve("console.log"));
		consumedLines.clear();
		Await.untilTrue("the Paper " + config.version + " server to finish starting", config.startTimeout, () -> {
			if (!process.isAlive()) {
				throw new CraftwrightException("The server stopped while starting. Last output:\n" + process.tail(60));
			}
			return process.lines().stream().anyMatch(line -> DONE.matcher(line).find());
		}, () -> "Last output:\n" + process.tail(40));
	}

	@Override
	public String host() {
		return "127.0.0.1";
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
		return process.lines();
	}

	@Override
	public String awaitLog(String contains) {
		return awaitLog(contains, config.session.defaultTimeout());
	}

	@Override
	public String awaitLog(String contains, Duration timeout) {
		return awaitLine("a console line containing \"" + contains + "\"", line -> line.contains(contains), timeout);
	}

	@Override
	public String awaitLog(Pattern pattern, Duration timeout) {
		return awaitLine("a console line matching /" + pattern + "/", line -> pattern.matcher(line).find(), timeout);
	}

	private String awaitLine(String what, Predicate<String> condition, Duration timeout) {
		return Await.until(what, timeout, () -> takeLine(condition), () -> "Last output:\n" + process.tail(25));
	}

	private synchronized Optional<String> takeLine(Predicate<String> condition) {
		int count = process.lineCount();
		for (int i = 0; i < count; i++) {
			if (!consumedLines.contains(i)) {
				String line = process.line(i);
				if (condition.test(line)) {
					consumedLines.add(i);
					return Optional.of(line);
				}
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean logContains(String part) {
		return process.lines().stream().anyMatch(line -> line.contains(part));
	}

	@Override
	public ServerStatus status() {
		return ServerStatus.ping(host(), port);
	}

	@Override
	public void restart() {
		stop();
		launch();
	}

	@Override
	public void stop() {
		if (process == null || !process.isAlive()) {
			return;
		}
		try {
			process.writeLine("stop");
		} catch (CraftwrightException ignored) {
			// killed below
		}
		if (!process.waitFor(60)) {
			process.destroy();
		}
	}

	@Override
	public boolean isRunning() {
		return process != null && process.isAlive();
	}

	private static Path paperJar(String version, Path cache) {
		Path target = cache.resolve("paper").resolve("paper-" + version + ".jar");
		if (Files.isRegularFile(target)) {
			return target;
		}
		String build = Downloads.text("https://fill.papermc.io/v3/projects/paper/versions/" + version + "/builds/latest");
		JsonObject downloads = JsonParser.parseString(build).getAsJsonObject().getAsJsonObject("downloads");
		String url = downloads.getAsJsonObject("server:default").get("url").getAsString();
		return Downloads.file(url, target);
	}

	private static Path resolveGlob(Path path) throws IOException {
		String fileName = path.getFileName().toString();
		if (!fileName.contains("*")) {
			if (!Files.isRegularFile(path)) {
				throw new CraftwrightException("Plugin jar not found: " + path.toAbsolutePath());
			}
			return path;
		}
		Path parent = path.toAbsolutePath().getParent();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, fileName)) {
			List<Path> matches = new ArrayList<>();
			stream.forEach(matches::add);
			if (matches.isEmpty()) {
				throw new CraftwrightException("No plugin jar matches " + path.toAbsolutePath());
			}
			matches.sort(null);
			return matches.get(matches.size() - 1);
		} catch (java.nio.file.NoSuchFileException e) {
			throw new CraftwrightException("No plugin jar matches " + path.toAbsolutePath());
		}
	}

	private static void copyTree(Path from, Path to) throws IOException {
		try (Stream<Path> paths = Files.walk(from)) {
			for (Path source : (Iterable<Path>) paths::iterator) {
				Path target = to.resolve(from.relativize(source).toString());
				if (Files.isDirectory(source)) {
					Files.createDirectories(target);
				} else {
					Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private static int freePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new CraftwrightException("No free port for the server", e);
		}
	}
}
