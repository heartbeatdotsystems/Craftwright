package systems.heartbeat.craftwright.internal.launch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import systems.heartbeat.craftwright.CraftwrightException;

/** Fills a client's game directory before the first start: the agent, extra mods and the options file. */
public final class GameDirectory {
	private GameDirectory() {
	}

	public static void prepare(Path gameDirectory, ClientSpec spec) {
		try {
			Path mods = gameDirectory.resolve("mods");
			Files.createDirectories(mods);
			AgentJar.copyTo(spec.minecraftVersion(), mods.resolve("craftwright-agent.jar"));
			for (Path mod : spec.mods()) {
				Files.copy(mod, mods.resolve(mod.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
			StringBuilder options = new StringBuilder();
			spec.options().forEach((key, value) -> options.append(key).append(':').append(value).append('\n'));
			Files.writeString(gameDirectory.resolve("options.txt"), options.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new CraftwrightException("Cannot prepare the game directory " + gameDirectory, e);
		}
	}
}
