package systems.heartbeat.craftwright.agent.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** The shapes of the lines that go to the driver. */
public final class Messages {
	public static final int PROTOCOL_VERSION = 1;

	private Messages() {
	}

	public static JsonObject result(JsonElement id, JsonElement result) {
		JsonObject message = new JsonObject();
		message.add("id", id);
		message.add("result", result == null ? new JsonObject() : result);
		return message;
	}

	public static JsonObject error(JsonElement id, String text) {
		JsonObject error = new JsonObject();
		error.addProperty("message", text);
		JsonObject message = new JsonObject();
		message.add("id", id);
		message.add("error", error);
		return message;
	}

	public static JsonObject event(String name, JsonElement data) {
		JsonObject message = new JsonObject();
		message.addProperty("event", name);
		message.add("data", data);
		return message;
	}

	public static JsonObject hello(String token, String client, String agentVersion, String minecraftVersion, String player) {
		JsonObject hello = new JsonObject();
		hello.addProperty("token", token);
		hello.addProperty("client", client);
		hello.addProperty("protocol", PROTOCOL_VERSION);
		hello.addProperty("agent", agentVersion);
		hello.addProperty("minecraft", minecraftVersion);
		hello.addProperty("player", player);
		return hello;
	}
}
