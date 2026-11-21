package systems.heartbeat.craftwright.agent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** What the driver passed on the command line. Without {@code craftwright.connect} the agent stays off. */
public final class AgentConfig {
	public static final String CONNECT = System.getProperty("craftwright.connect");
	public static final String TOKEN = System.getProperty("craftwright.token", "");
	public static final String CLIENT_ID = System.getProperty("craftwright.client", "client");
	public static final String USERNAME = CONNECT == null ? null : System.getProperty("craftwright.username");
	public static final UUID OFFLINE_UUID = USERNAME == null ? null
		: UUID.nameUUIDFromBytes(("OfflinePlayer:" + USERNAME).getBytes(StandardCharsets.UTF_8));

	private AgentConfig() {
	}

	public static boolean enabled() {
		return CONNECT != null;
	}
}
