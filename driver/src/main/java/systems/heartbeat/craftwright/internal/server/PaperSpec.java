package systems.heartbeat.craftwright.internal.server;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything that describes one Paper server before it starts.
 *
 * @param name       directory name inside the session directory
 * @param plugins    plugin jars on disk, the file name may contain {@code *}
 * @param pluginUrls plugin jars to download
 * @param files      extra files by path relative to the server directory
 * @param properties lines of {@code server.properties}
 * @param template   a directory copied in first: a prepared world, configs
 * @param memory     maximum heap, e.g. {@code 2G}
 */
public record PaperSpec(String name, String version, List<Path> plugins, List<String> pluginUrls, Map<String, String> files,
		Map<String, String> properties, List<String> jvmArguments, Optional<Path> template, String memory, Duration startTimeout) {
	public PaperSpec {
		plugins = List.copyOf(plugins);
		pluginUrls = List.copyOf(pluginUrls);
		files = Map.copyOf(files);
		properties = Map.copyOf(properties);
		jvmArguments = List.copyOf(jvmArguments);
	}
}
