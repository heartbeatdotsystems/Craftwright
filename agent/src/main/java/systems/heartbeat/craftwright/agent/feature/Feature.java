package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonObject;
import systems.heartbeat.craftwright.agent.protocol.Routes;

/** One area of the game a test can reach, with the protocol methods that belong to it. */
public interface Feature {
	void register(Routes routes);

	/** The answer of a request that only does something. */
	static JsonObject done() {
		return new JsonObject();
	}

	static JsonObject value(String name, boolean value) {
		JsonObject result = new JsonObject();
		result.addProperty(name, value);
		return result;
	}
}
