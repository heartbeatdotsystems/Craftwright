package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.Json;

/**
 * A block as the client has it.
 *
 * @param id          registry id, e.g. {@code minecraft:chest}
 * @param properties  block state properties, e.g. {@code facing=north}
 * @param blockEntity what the client knows beyond the state: the text of a sign, the owner of a head
 */
public record Block(String id, Map<String, String> properties, Optional<JsonObject> blockEntity) {
	static Block of(JsonObject result) {
		Map<String, String> properties = new LinkedHashMap<>();
		result.getAsJsonObject("properties").entrySet().forEach(entry -> properties.put(entry.getKey(), entry.getValue().getAsString()));
		JsonElement entity = result.get("blockEntity");
		return new Block(result.get("id").getAsString(), Map.copyOf(properties),
			Json.isNull(entity) ? Optional.empty() : Optional.of(entity.getAsJsonObject()));
	}

	public boolean is(String blockId) {
		return Ids.matches(id, blockId);
	}

	public boolean isAir() {
		return id.endsWith("air");
	}
}
