package systems.heartbeat.craftwright.agent.protocol;

import com.google.gson.JsonObject;

/** Where features and observers publish what happened in the game. */
@FunctionalInterface
public interface Events {
	void emit(String name, JsonObject data);
}
