package systems.heartbeat.craftwright.selftest;

import systems.heartbeat.craftwright.PaperServerBuilder;
import systems.heartbeat.craftwright.junit.ServerSetup;

/** The one server all self tests share: a flat world where nothing happens unless a test makes it happen. */
public final class SelfTestServer implements ServerSetup {
	@Override
	public void configure(PaperServerBuilder paper) {
		paper.property("motd", "Craftwright self test").property("difficulty", "peaceful");
	}
}
