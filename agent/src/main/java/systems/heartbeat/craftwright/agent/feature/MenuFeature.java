package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.protocol.Params;
import systems.heartbeat.craftwright.agent.protocol.RequestException;
import systems.heartbeat.craftwright.agent.protocol.Routes;
import systems.heartbeat.craftwright.agent.wire.ItemWire;
import systems.heartbeat.craftwright.agent.wire.TextWire;

/**
 * Menus with slots: chests and plugin menus the server opened, and the player's own inventory.
 * Every click goes through the same path as a mouse click, so the server sees a normal player.
 */
public final class MenuFeature implements Feature {
	/** The slot number the game uses for a click next to the menu. */
	private static final int OUTSIDE = AbstractContainerMenu.SLOT_CLICKED_OUTSIDE;

	@Override
	public void register(Routes routes) {
		routes.on("menu.get", (minecraft, params) -> opened(minecraft));
		routes.on("menu.inventory", (minecraft, params) -> inventory(minecraft));
		routes.on("menu.click", this::click);
		routes.on("menu.drag", this::drag);
		routes.on("menu.close", (minecraft, params) -> close(minecraft));
	}

	private JsonElement opened(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || player.containerMenu == player.inventoryMenu) {
			return Feature.value("open", false);
		}
		Screen screen = minecraft.gui.screen();
		Component title = screen instanceof AbstractContainerScreen<?> ? screen.getTitle() : Component.empty();
		return describe(player.containerMenu, title);
	}

	private JsonElement inventory(Minecraft minecraft) {
		LocalPlayer player = Game.player(minecraft);
		if (player.containerMenu != player.inventoryMenu) {
			throw new RequestException("Another menu is open. Its last 36 slots are the player's inventory, use those or close it first.");
		}
		return describe(player.inventoryMenu, Component.translatable("container.inventory"));
	}

	private static JsonObject describe(AbstractContainerMenu menu, Component title) {
		JsonArray slots = new JsonArray();
		for (int index = 0; index < menu.slots.size(); index++) {
			Slot slot = menu.slots.get(index);
			JsonObject entry = new JsonObject();
			entry.addProperty("index", index);
			entry.add("item", ItemWire.of(slot.getItem()));
			entry.addProperty("playerInventory", slot.container instanceof Inventory);
			slots.add(entry);
		}
		JsonObject result = new JsonObject();
		result.addProperty("open", true);
		result.addProperty("id", menu.containerId);
		result.addProperty("type", type(menu));
		result.add("title", TextWire.of(title));
		result.add("slots", slots);
		result.add("carried", ItemWire.of(menu.getCarried()));
		return result;
	}

	/** The player's own inventory has no menu type. */
	private static String type(AbstractContainerMenu menu) {
		try {
			return String.valueOf(BuiltInRegistries.MENU.getKey(menu.getType()));
		} catch (UnsupportedOperationException e) {
			return "minecraft:inventory";
		}
	}

	private JsonElement click(Minecraft minecraft, Params params) {
		LocalPlayer player = Game.player(minecraft);
		AbstractContainerMenu menu = current(player, params);
		int slot = params.integer("slot");
		requireSlot(menu, slot);
		minecraft.gameMode.handleContainerInput(menu.containerId, slot, params.integer("button", 0), input(params.string("mode", "pickup")), player);
		return Feature.done();
	}

	/** Holds the carried stack and paints it over the slots, like dragging with the mouse button down. */
	private JsonElement drag(Minecraft minecraft, Params params) {
		LocalPlayer player = Game.player(minecraft);
		AbstractContainerMenu menu = current(player, params);
		if (menu.getCarried().isEmpty()) {
			throw new RequestException("Nothing is on the cursor. Pick up a stack first, then drag it.");
		}
		List<Integer> slots = params.integers("slots");
		slots.forEach(slot -> requireSlot(menu, slot));
		int button = params.integer("button", 0);
		quickCraft(minecraft, player, menu, OUTSIDE, AbstractContainerMenu.QUICKCRAFT_HEADER_START, button);
		for (int slot : slots) {
			quickCraft(minecraft, player, menu, slot, AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE, button);
		}
		quickCraft(minecraft, player, menu, OUTSIDE, AbstractContainerMenu.QUICKCRAFT_HEADER_END, button);
		return Feature.done();
	}

	private static void quickCraft(Minecraft minecraft, LocalPlayer player, AbstractContainerMenu menu, int slot, int header, int button) {
		minecraft.gameMode.handleContainerInput(menu.containerId, slot,
			AbstractContainerMenu.getQuickcraftMask(header, button), ContainerInput.QUICK_CRAFT, player);
	}

	private JsonElement close(Minecraft minecraft) {
		Game.player(minecraft).closeContainer();
		return Feature.done();
	}

	/** The menu the player has in front of them. A stale id means the server replaced the menu in the meantime. */
	private static AbstractContainerMenu current(LocalPlayer player, Params params) {
		AbstractContainerMenu menu = player.containerMenu;
		if (params.has("menu") && params.integer("menu") != menu.containerId) {
			throw new RequestException("That menu is no longer open, the server opened another one. Ask for the menu again.");
		}
		return menu;
	}

	private static void requireSlot(AbstractContainerMenu menu, int slot) {
		if (slot != OUTSIDE && !menu.isValidSlotIndex(slot)) {
			throw new RequestException("The open menu has no slot " + slot);
		}
	}

	private static ContainerInput input(String mode) {
		return switch (mode) {
			case "pickup" -> ContainerInput.PICKUP;
			case "quick_move" -> ContainerInput.QUICK_MOVE;
			case "swap" -> ContainerInput.SWAP;
			case "clone" -> ContainerInput.CLONE;
			case "throw" -> ContainerInput.THROW;
			case "pickup_all" -> ContainerInput.PICKUP_ALL;
			default -> throw new RequestException("Unknown click mode '" + mode + "'");
		};
	}
}
