package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A bar at the top of the screen.
 *
 * @param progress 0 to 1
 * @param color    pink, blue, red, green, yellow, purple or white
 * @param overlay  progress or notched_6, notched_10, notched_12, notched_20
 */
public record BossBar(Text name, float progress, String color, String overlay) {
	static BossBar of(JsonElement element) {
		JsonObject bar = element.getAsJsonObject();
		return new BossBar(Text.of(bar.get("name")), bar.get("progress").getAsFloat(), bar.get("color").getAsString(),
			bar.get("overlay").getAsString());
	}
}
