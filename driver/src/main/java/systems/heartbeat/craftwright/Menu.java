package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import systems.heartbeat.craftwright.internal.Await;

/**
 * A snapshot of the container screen the client has open: a chest, a plugin menu, an anvil.
 * Clicking does not change this snapshot, ask {@link Client#menu()} again for the new state.
 */
public final class Menu {
	private final Client client;
	private final int id;
	private final String type;
	private final Text title;
	private final List<Slot> slots;
	private final Item carried;

	private Menu(Client client, JsonObject object) {
		this.client = client;
		this.id = object.get("id").getAsInt();
		this.type = object.get("type").getAsString();
		this.title = Text.of(object.get("title"));
		List<Slot> list = new ArrayList<>();
		for (JsonElement element : object.getAsJsonArray("slots")) {
			JsonObject slot = element.getAsJsonObject();
			list.add(new Slot(slot.get("index").getAsInt(), Item.of(slot.get("item")), slot.get("playerInventory").getAsBoolean()));
		}
		this.slots = List.copyOf(list);
		this.carried = Item.of(object.get("carried"));
	}

	static Optional<Menu> current(Client client) {
		JsonObject result = client.call("menu.get");
		return result.get("open").getAsBoolean() ? Optional.of(new Menu(client, result)) : Optional.empty();
	}

	static Menu awaitOpen(Client client, Predicate<Menu> condition, String description, Duration timeout) {
		return Await.until("a menu " + description + " to open for " + client.name(), timeout,
			() -> current(client).filter(condition),
			() -> current(client).map(menu -> "Open menu: \"" + menu.title().plain() + "\"").orElse("No menu is open."));
	}

	/** Registry id of the menu type, e.g. {@code minecraft:generic_9x6}. */
	public String type() {
		return type;
	}

	public Text title() {
		return title;
	}

	/** All slots, the player's own inventory at the end included. */
	public List<Slot> slots() {
		return slots;
	}

	/** Only the slots of the container itself, without the player's inventory. */
	public List<Slot> containerSlots() {
		return slots.stream().filter(slot -> !slot.playerInventory).toList();
	}

	public Slot slot(int index) {
		if (index < 0 || index >= slots.size()) {
			throw new CraftwrightException("Menu \"" + title.plain() + "\" has " + slots.size() + " slots, there is no slot " + index);
		}
		return slots.get(index);
	}

	/** The first slot whose item name contains the given words. */
	public Slot slotNamed(String nameContains) {
		return findSlot(nameContains).orElseThrow(() -> new CraftwrightException("Menu \"" + title.plain()
			+ "\" has no item named \"" + nameContains + "\". Items: " + itemNames()));
	}

	public Optional<Slot> findSlot(String nameContains) {
		return slots.stream().filter(slot -> slot.item != null && slot.item.name().contains(nameContains)).findFirst();
	}

	/** The first slot holding this item id, with or without the {@code minecraft:} namespace. */
	public Optional<Slot> findItem(String itemId) {
		return slots.stream().filter(slot -> slot.item != null && slot.item.is(itemId)).findFirst();
	}

	/** Names of all items in the container part, for messages and quick assertions. */
	public List<String> itemNames() {
		return containerSlots().stream().filter(slot -> slot.item != null).map(slot -> slot.item.name().plain()).toList();
	}

	/** The item currently held on the cursor, if any. */
	public Optional<Item> carried() {
		return Optional.ofNullable(carried);
	}

	/** Closes the screen, like pressing escape. */
	public void close() {
		client.call("menu.close");
	}

	/** One slot of an open menu. */
	public final class Slot {
		private final int index;
		private final Item item;
		private final boolean playerInventory;

		private Slot(int index, Item item, boolean playerInventory) {
			this.index = index;
			this.item = item;
			this.playerInventory = playerInventory;
		}

		public int index() {
			return index;
		}

		public Optional<Item> item() {
			return Optional.ofNullable(item);
		}

		public boolean isEmpty() {
			return item == null;
		}

		/** True if this slot belongs to the player's own inventory below the container. */
		public boolean isPlayerInventory() {
			return playerInventory;
		}

		/** Left click. */
		public void click() {
			send(0, "pickup");
		}

		public void rightClick() {
			send(1, "pickup");
		}

		public void shiftClick() {
			send(0, "quick_move");
		}

		public void middleClick() {
			send(2, "clone");
		}

		/** Presses a number key over the slot, swapping with that hotbar slot (0 to 8). */
		public void swapWithHotbar(int hotbarSlot) {
			send(hotbarSlot, "swap");
		}

		/** Presses the drop key over the slot. */
		public void drop(boolean wholeStack) {
			send(wholeStack ? 1 : 0, "throw");
		}

		private void send(int button, String mode) {
			JsonObject params = new JsonObject();
			params.addProperty("menu", id);
			params.addProperty("slot", index);
			params.addProperty("button", button);
			params.addProperty("mode", mode);
			client.call("menu.click", params);
		}

		@Override
		public String toString() {
			return "slot " + index + (item == null ? " (empty)" : " (" + item.count() + "x " + item.name().plain() + ")");
		}
	}
}
