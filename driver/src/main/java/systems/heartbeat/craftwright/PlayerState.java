package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;

/**
 * What the client knows about its own player right now.
 *
 * @param gameMode  survival, creative, adventure or spectator
 * @param dimension e.g. {@code minecraft:overworld}
 */
public record PlayerState(Position position, float yaw, float pitch, float health, int food, float saturation,
		int experienceLevel, String gameMode, String dimension, boolean onGround, boolean dead, int selectedHotbarSlot) {

	static PlayerState of(JsonObject object) {
		return new PlayerState(
			new Position(object.get("x").getAsDouble(), object.get("y").getAsDouble(), object.get("z").getAsDouble()),
			object.get("yaw").getAsFloat(), object.get("pitch").getAsFloat(), object.get("health").getAsFloat(),
			object.get("food").getAsInt(), object.get("saturation").getAsFloat(), object.get("experienceLevel").getAsInt(),
			object.get("gameMode").getAsString(), object.get("dimension").getAsString(), object.get("onGround").getAsBoolean(),
			object.get("dead").getAsBoolean(), object.get("selectedHotbarSlot").getAsInt());
	}
}
