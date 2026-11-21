package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;

/**
 * The world border as the client draws it.
 *
 * @param size side length in blocks
 */
public record WorldBorder(double centerX, double centerZ, double size) {
	static WorldBorder of(JsonObject result) {
		return new WorldBorder(result.get("centerX").getAsDouble(), result.get("centerZ").getAsDouble(), result.get("size").getAsDouble());
	}
}
