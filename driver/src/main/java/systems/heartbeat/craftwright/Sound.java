package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;

/**
 * A sound the server played for this client.
 *
 * @param id     e.g. {@code minecraft:entity.player.levelup}
 * @param source the volume slider it belongs to: master, music, records, weather, blocks, hostile, neutral, players, ambient, voice
 */
public record Sound(String id, String source, Position position, float volume, float pitch) {
	static Sound of(JsonObject data) {
		return new Sound(data.get("id").getAsString(), data.get("source").getAsString(), Position.of(data),
			data.get("volume").getAsFloat(), data.get("pitch").getAsFloat());
	}

	public boolean is(String soundId) {
		return Ids.matches(id, soundId);
	}

	@Override
	public String toString() {
		return id + " at " + position + " (volume " + volume + ", pitch " + pitch + ")";
	}
}
