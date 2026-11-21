package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * One player in the player list.
 *
 * @param name        account name
 * @param displayName the name as listed, with prefixes and colours the server set
 * @param gameMode    survival, creative, adventure or spectator
 */
public record TabEntry(String name, Text displayName, String gameMode, int latency) {
	static TabEntry of(JsonElement element) {
		JsonObject entry = element.getAsJsonObject();
		return new TabEntry(entry.get("name").getAsString(), Text.of(entry.get("displayName")), entry.get("gameMode").getAsString(),
			entry.get("latency").getAsInt());
	}
}
