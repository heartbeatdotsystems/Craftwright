package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * An item stack as the client sees it.
 *
 * @param id    registry id, e.g. {@code minecraft:diamond}
 * @param count stack size
 * @param name  the name shown in the tooltip, custom name included
 * @param lore  the lore lines below the name
 */
public record Item(String id, int count, Text name, List<Text> lore) {

	static Item of(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return null;
		}
		JsonObject object = element.getAsJsonObject();
		List<Text> lore = new ArrayList<>();
		if (object.has("lore")) {
			object.getAsJsonArray("lore").forEach(line -> lore.add(Text.of(line)));
		}
		return new Item(object.get("id").getAsString(), object.get("count").getAsInt(), Text.of(object.get("name")), List.copyOf(lore));
	}

	/** True if the id matches, with or without the {@code minecraft:} namespace. */
	public boolean is(String itemId) {
		return id.equals(itemId) || id.equals("minecraft:" + itemId);
	}

	public boolean loreContains(String part) {
		return lore.stream().anyMatch(line -> line.contains(part));
	}
}
