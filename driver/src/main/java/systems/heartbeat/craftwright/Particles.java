package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Particles the server showed this client in one go.
 *
 * @param id      e.g. {@code minecraft:dust}
 * @param options the particle with its settings, e.g. the colour and size of dust
 * @param count   how many
 */
public record Particles(String id, JsonElement options, Position position, int count) {
	static Particles of(JsonObject data) {
		return new Particles(data.get("id").getAsString(), data.get("options"), Position.of(data), data.get("count").getAsInt());
	}

	public boolean is(String particleId) {
		return Ids.matches(id, particleId);
	}

	@Override
	public String toString() {
		return count + "x " + id + " at " + position;
	}
}
