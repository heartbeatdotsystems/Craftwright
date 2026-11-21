package systems.heartbeat.craftwright.agent.wire;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/** An item stack on the wire: what a player sees in the tooltip, plus the full stack data for exact checks. */
public final class ItemWire {
	private ItemWire() {
	}

	public static JsonElement of(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		JsonObject item = new JsonObject();
		item.addProperty("id", String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
		item.addProperty("count", stack.getCount());
		item.add("name", TextWire.of(stack.getHoverName()));
		item.add("lore", lore(stack));
		item.addProperty("enchanted", stack.isEnchanted());
		item.addProperty("glint", stack.hasFoil());
		item.addProperty("damage", stack.getDamageValue());
		item.add("data", Codecs.json(ItemStack.CODEC, stack));
		return item;
	}

	private static JsonArray lore(ItemStack stack) {
		JsonArray lore = new JsonArray();
		ItemLore lines = stack.get(DataComponents.LORE);
		if (lines != null) {
			for (Component line : lines.lines()) {
				lore.add(TextWire.of(line));
			}
		}
		return lore;
	}
}
