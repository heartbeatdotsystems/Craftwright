package systems.heartbeat.craftwright.internal.server;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import systems.heartbeat.craftwright.CraftwrightException;

/** File work the server setup needs: copying directories and finding a jar by pattern. */
final class FileTrees {
	private FileTrees() {
	}

	static void copy(Path from, Path to) throws IOException {
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

	/**
	 * The file itself, or for a name with {@code *} the match that sorts last.
	 * {@code build/libs/MyPlugin-*.jar} so finds the jar whatever the version is.
	 */
	static Path resolve(Path path) throws IOException {
		String fileName = path.getFileName().toString();
		if (!fileName.contains("*")) {
			if (!Files.isRegularFile(path)) {
				throw new CraftwrightException("File not found: " + path.toAbsolutePath());
			}
			return path;
		}
		List<Path> matches = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path.toAbsolutePath().getParent(), fileName)) {
			stream.forEach(matches::add);
		} catch (NoSuchFileException missingDirectory) {
			// reported below like a pattern without matches
		}
		if (matches.isEmpty()) {
			throw new CraftwrightException("No file matches " + path.toAbsolutePath());
		}
		matches.sort(null);
		return matches.get(matches.size() - 1);
	}
}
