package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The world as this client has it loaded. Only what the server sent to this client is visible here,
 * which is exactly what makes it useful for testing vanish, anti-xray or hidden containers.
 */
public final class World {
	private final Client client;

	World(Client client) {
		this.client = client;
	}

	/** The block the client has at this position. Unloaded chunks report {@code minecraft:void_air}. */
	public Block block(int x, int y, int z) {
		JsonObject params = new JsonObject();
		params.addProperty("x", x);
		params.addProperty("y", y);
		params.addProperty("z", z);
		JsonObject result = client.call("world.block", params);
		Map<String, String> properties = new LinkedHashMap<>();
		result.getAsJsonObject("properties").entrySet().forEach(entry -> properties.put(entry.getKey(), entry.getValue().getAsString()));
		return new Block(result.get("id").getAsString(), Map.copyOf(properties), result.get("blockEntity").getAsBoolean());
	}

	/** Every entity the client knows about, the own player excluded. */
	public List<Entity> entities() {
		List<Entity> entities = new ArrayList<>();
		for (JsonElement element : client.call("world.entities").getAsJsonArray("entities")) {
			JsonObject entity = element.getAsJsonObject();
			entities.add(new Entity(entity.get("id").getAsInt(), entity.get("uuid").getAsString(), entity.get("type").getAsString(),
				Text.of(entity.get("name")), new Position(entity.get("x").getAsDouble(), entity.get("y").getAsDouble(),
				entity.get("z").getAsDouble()), entity.get("invisible").getAsBoolean()));
		}
		return entities;
	}

	/** Names of the other players this client can see in the world. */
	public List<String> visiblePlayers() {
		return entities().stream().filter(entity -> entity.type().equals("minecraft:player")).map(entity -> entity.name().plain()).toList();
	}

	/** Time of day in ticks, 0 to 23999. */
	public long timeOfDay() {
		return client.call("world.time").get("dayTime").getAsLong();
	}

	public boolean isRaining() {
		return client.call("world.time").get("raining").getAsBoolean();
	}

	/**
	 * @param id          registry id, e.g. {@code minecraft:chest}
	 * @param properties  block state properties, e.g. {@code facing=north}
	 * @param blockEntity true if the client holds a block entity here (chest, sign, …)
	 */
	public record Block(String id, Map<String, String> properties, boolean blockEntity) {

		public boolean is(String blockId) {
			return id.equals(blockId) || id.equals("minecraft:" + blockId);
		}

		public boolean isAir() {
			return id.endsWith("air");
		}
	}

	/**
	 * @param type registry id, e.g. {@code minecraft:villager}
	 * @param name custom name, player name or the type's name
	 */
	public record Entity(int id, String uuid, String type, Text name, Position position, boolean invisible) {
	}
}
