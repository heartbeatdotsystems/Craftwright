package systems.heartbeat.craftwright.internal.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import systems.heartbeat.craftwright.CraftwrightException;
import systems.heartbeat.craftwright.internal.Downloads;

/** Lays out a server directory from its description: the server jar, plugins, files and settings. */
public final class ServerDirectory {
	public static final String SERVER_JAR = "paper.jar";

	private ServerDirectory() {
	}

	public static void prepare(Path directory, PaperSpec spec, int port, Path cache) {
		try {
			Path plugins = directory.resolve("plugins");
			Files.createDirectories(plugins);
			if (spec.template().isPresent()) {
				FileTrees.copy(spec.template().get(), directory);
			}
			Files.copy(PaperJar.forVersion(spec.version(), cache), directory.resolve(SERVER_JAR), StandardCopyOption.REPLACE_EXISTING);
			for (Path plugin : spec.plugins()) {
				Path jar = FileTrees.resolve(plugin);
				Files.copy(jar, plugins.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
			for (String url : spec.pluginUrls()) {
				String fileName = url.substring(url.lastIndexOf('/') + 1);
				Path cached = Downloads.file(url, cache.resolve("plugins").resolve(fileName));
				Files.copy(cached, plugins.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
			}
			for (Map.Entry<String, String> file : spec.files().entrySet()) {
				Path target = directory.resolve(file.getKey());
				Files.createDirectories(target.getParent());
				Files.writeString(target, file.getValue(), StandardCharsets.UTF_8);
			}
			Files.writeString(directory.resolve("eula.txt"), "eula=true\n");
			Files.writeString(directory.resolve("server.properties"), properties(spec, port), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new CraftwrightException("Cannot prepare the server directory " + directory, e);
		}
	}

	/** The test's settings, bound to localhost on the chosen port. */
	private static String properties(PaperSpec spec, int port) {
		StringBuilder properties = new StringBuilder();
		spec.properties().forEach((key, value) -> properties.append(key).append('=').append(value).append('\n'));
		properties.append("server-ip=127.0.0.1\n").append("server-port=").append(port).append('\n');
		return properties.toString();
	}
}
