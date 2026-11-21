package systems.heartbeat.craftwright.junit;

import systems.heartbeat.craftwright.PaperServerBuilder;

/**
 * A server setup with a name, to be used by several test classes.
 *
 * <pre>{@code
 * public final class LobbyServer implements ServerSetup {
 *     public void configure(PaperServerBuilder paper) {
 *         paper.plugin("build/libs/Lobby-*.jar").file("plugins/Lobby/config.yml", "spawn: 0,64,0\n");
 *     }
 * }
 *
 * @CraftwrightTest(setup = LobbyServer.class)
 * class WelcomeTest { … }
 * }</pre>
 *
 * <p>All test classes that name the same setup with the same annotation settings share one
 * server and its clients for the whole test run. The server then starts once, not once per class.</p>
 */
public interface ServerSetup {
	void configure(PaperServerBuilder paper);

	/** The default of {@link CraftwrightTest#setup()}: the test class gets a server of its own. */
	final class PerClass implements ServerSetup {
		@Override
		public void configure(PaperServerBuilder paper) {
			// the annotation attributes and @ConfigureServer say everything
		}
	}
}
