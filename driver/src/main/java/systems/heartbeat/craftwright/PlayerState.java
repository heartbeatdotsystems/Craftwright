package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;
import java.util.List;
import systems.heartbeat.craftwright.internal.Json;

/**
 * What the client knows about its own player right now.
 *
 * @param gameMode           survival, creative, adventure or spectator
 * @param dimension          e.g. {@code minecraft:overworld}
 * @param experienceProgress 0 to 1 within the current level
 */
public record PlayerState(Position position, float yaw, float pitch, float health, int food, float saturation,
		int experienceLevel, float experienceProgress, String gameMode, String dimension, boolean onGround, boolean dead,
		boolean sneaking, boolean sprinting, boolean flying, boolean mayFly, int selectedHotbarSlot, List<Effect> effects) {
	static PlayerState of(JsonObject state) {
		return new PlayerState(Position.of(state), state.get("yaw").getAsFloat(), state.get("pitch").getAsFloat(),
			state.get("health").getAsFloat(), state.get("food").getAsInt(), state.get("saturation").getAsFloat(),
			state.get("experienceLevel").getAsInt(), state.get("experienceProgress").getAsFloat(), state.get("gameMode").getAsString(),
			state.get("dimension").getAsString(), state.get("onGround").getAsBoolean(), state.get("dead").getAsBoolean(),
			state.get("sneaking").getAsBoolean(), state.get("sprinting").getAsBoolean(), state.get("flying").getAsBoolean(),
			state.get("mayFly").getAsBoolean(), state.get("selectedHotbarSlot").getAsInt(), Json.list(state, "effects", Effect::of));
	}

	public boolean hasEffect(String effectId) {
		return effects.stream().anyMatch(effect -> effect.is(effectId));
	}
}
