package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A status effect on the player.
 *
 * @param id        e.g. {@code minecraft:speed}
 * @param amplifier 0 for level I
 * @param duration  ticks left, -1 for an effect without end
 */
public record Effect(String id, int amplifier, int duration) {
	static Effect of(JsonElement element) {
		JsonObject effect = element.getAsJsonObject();
		return new Effect(effect.get("id").getAsString(), effect.get("amplifier").getAsInt(), effect.get("duration").getAsInt());
	}

	public boolean is(String effectId) {
		return Ids.matches(id, effectId);
	}
}
