package systems.heartbeat.craftwright.internal.launch;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Everything that describes one client before it starts.
 *
 * @param memory  maximum heap of the game, e.g. {@code 2G}
 * @param mods    extra Fabric mods next to the agent
 * @param options lines of {@code options.txt}
 */
public record ClientSpec(String name, String minecraftVersion, boolean rendering, String memory, List<Path> mods, Map<String, String> options) {
	public ClientSpec {
		mods = List.copyOf(mods);
		options = Map.copyOf(options);
	}
}
