package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.Json;

/**
 * An entity the server showed this client.
 *
 * @param type        registry id, e.g. {@code minecraft:villager}
 * @param name        player name, custom name or the type's name
 * @param displayName the name as shown above the head, team prefix and suffix included
 * @param customName  the name tag, if the entity has one
 * @param health      for living entities
 * @param equipment   what a living entity wears and holds, by slot: mainhand, offhand, head, chest, legs, feet
 */
public record Entity(int id, String uuid, String type, Text name, Text displayName, Optional<Text> customName, Position position,
		boolean invisible, boolean glowing, boolean sneaking, Optional<Float> health, Map<String, Item> equipment) {
	static Entity of(JsonElement element) {
		JsonObject entity = element.getAsJsonObject();
		Map<String, Item> equipment = new LinkedHashMap<>();
		if (entity.has("equipment")) {
			entity.getAsJsonObject("equipment").entrySet().forEach(entry ->
				Item.of(entry.getValue()).ifPresent(item -> equipment.put(entry.getKey(), item)));
		}
		return new Entity(entity.get("id").getAsInt(), entity.get("uuid").getAsString(), entity.get("type").getAsString(),
			Text.of(entity.get("name")), Text.of(entity.get("displayName")),
			Json.isNull(entity.get("customName")) ? Optional.empty() : Optional.of(Text.of(entity.get("customName"))),
			Position.of(entity), entity.get("invisible").getAsBoolean(), entity.get("glowing").getAsBoolean(),
			entity.get("sneaking").getAsBoolean(),
			entity.has("health") ? Optional.of(entity.get("health").getAsFloat()) : Optional.empty(), Map.copyOf(equipment));
	}

	public boolean is(String typeId) {
		return Ids.matches(type, typeId);
	}

	public boolean isPlayer() {
		return is("player");
	}
}
