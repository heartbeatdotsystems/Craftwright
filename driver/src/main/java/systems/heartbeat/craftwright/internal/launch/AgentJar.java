package systems.heartbeat.craftwright.internal.launch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import systems.heartbeat.craftwright.CraftwrightException;

/** The agent mod that ships inside the driver jar, one per supported Minecraft version. */
public final class AgentJar {
	/** Points to an agent jar on disk instead of the bundled one, for work on the agent itself. */
	private static final String OVERRIDE_PROPERTY = "craftwright.agent.jar";

	private AgentJar() {
	}

	public static boolean existsFor(String minecraftVersion) {
		return System.getProperty(OVERRIDE_PROPERTY) != null || AgentJar.class.getResource(resource(minecraftVersion)) != null;
	}

	/** Copies the agent for this version to {@code target}. */
	public static void copyTo(String minecraftVersion, Path target) throws IOException {
		String override = System.getProperty(OVERRIDE_PROPERTY);
		if (override != null) {
			Files.copy(Path.of(override), target, StandardCopyOption.REPLACE_EXISTING);
			return;
		}
		try (InputStream in = AgentJar.class.getResourceAsStream(resource(minecraftVersion))) {
			if (in == null) {
				throw new CraftwrightException("This Craftwright version has no agent for Minecraft " + minecraftVersion);
			}
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static String resource(String minecraftVersion) {
		return "/craftwright/agents/fabric-" + minecraftVersion + ".jar";
	}
}
