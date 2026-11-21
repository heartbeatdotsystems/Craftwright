package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.Json;

/**
 * An item stack as the client sees it.
 *
 * @param id        registry id, e.g. {@code minecraft:diamond}
 * @param count     stack size
 * @param name      the name shown in the tooltip, custom name included
 * @param lore      the lore lines below the name
 * @param enchanted true if the stack carries enchantments
 * @param glint     true if it shimmers, with or without enchantments
 * @param damage    used durability, 0 for a new tool
 * @param data      the whole stack in the form data packs and {@code /give} use: {@code id}, {@code count}, {@code components}
 */
public record Item(String id, int count, Text name, List<Text> lore, boolean enchanted, boolean glint, int damage, JsonObject data) {
	/** An empty slot is no item. */
	static Optional<Item> of(JsonElement element) {
		if (Json.isNull(element)) {
			return Optional.empty();
		}
		JsonObject object = element.getAsJsonObject();
		return Optional.of(new Item(object.get("id").getAsString(), object.get("count").getAsInt(), Text.of(object.get("name")),
			Json.list(object, "lore", Text::of), object.get("enchanted").getAsBoolean(), object.get("glint").getAsBoolean(),
			object.get("damage").getAsInt(), object.get("data").isJsonObject() ? object.getAsJsonObject("data") : new JsonObject()));
	}

	/** True if the id matches, with or without the {@code minecraft:} namespace. */
	public boolean is(String itemId) {
		return Ids.matches(id, itemId);
	}

	public boolean loreContains(String words) {
		return lore.stream().anyMatch(line -> line.contains(words));
	}

	/** One data component of the stack, e.g. {@code component("minecraft:profile")} of a player head. */
	public Optional<JsonElement> component(String componentId) {
		JsonElement components = data.get("components");
		if (components == null || !components.isJsonObject()) {
			return Optional.empty();
		}
		return Optional.ofNullable(components.getAsJsonObject().get(Ids.qualify(componentId)));
	}

	@Override
	public String toString() {
		return count + "x " + name.plain() + " (" + id + ")";
	}
}
