package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.Json;

/**
 * A snapshot of a screen with slots: a chest, a plugin menu, an anvil, or the player's own inventory.
 * Clicking does not change this snapshot, ask {@link Menus} again for the new state.
 */
public final class Menu {
	/** The slot number the game uses for a click next to the menu, which drops what the cursor carries. */
	private static final int OUTSIDE = -999;

	private final Remote remote;
	private final int id;
	private final String type;
	private final Text title;
	private final List<Slot> slots;
	private final Optional<Item> carried;

	private Menu(Remote remote, JsonObject result) {
		this.remote = remote;
		this.id = result.get("id").getAsInt();
		this.type = result.get("type").getAsString();
		this.title = Text.of(result.get("title"));
		this.slots = Json.list(result, "slots", this::slot);
		this.carried = Item.of(result.get("carried"));
	}

	/** Empty if the result says that no menu is open. */
	static Optional<Menu> of(Remote remote, JsonObject result) {
		return result.get("open").getAsBoolean() ? Optional.of(new Menu(remote, result)) : Optional.empty();
	}

	private Slot slot(JsonElement element) {
		JsonObject slot = element.getAsJsonObject();
		return new Slot(remote, id, slot.get("index").getAsInt(), Item.of(slot.get("item")), slot.get("playerInventory").getAsBoolean());
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
		return slots.stream().filter(slot -> !slot.isPlayerInventory()).toList();
	}

	public Slot slot(int index) {
		if (index < 0 || index >= slots.size()) {
			throw new CraftwrightException("Menu \"" + title.plain() + "\" has " + slots.size() + " slots, there is no slot " + index);
		}
		return slots.get(index);
	}

	/** The first slot whose item name contains the given words. */
	public Slot slotNamed(String words) {
		return findSlot(words).orElseThrow(() -> new CraftwrightException("Menu \"" + title.plain()
			+ "\" has no item named \"" + words + "\". Items: " + itemNames()));
	}

	public Optional<Slot> findSlot(String words) {
		return slots.stream().filter(slot -> slot.item().filter(item -> item.name().contains(words)).isPresent()).findFirst();
	}

	/** The first slot holding this item id, with or without the {@code minecraft:} namespace. */
	public Optional<Slot> findItem(String itemId) {
		return slots.stream().filter(slot -> slot.item().filter(item -> item.is(itemId)).isPresent()).findFirst();
	}

	/** Names of all items in the container part, for messages and quick assertions. */
	public List<String> itemNames() {
		return containerSlots().stream().flatMap(slot -> slot.item().stream()).map(item -> item.name().plain()).toList();
	}

	/** How many items of this id the menu holds, the player's inventory included. */
	public int count(String itemId) {
		return slots.stream().flatMap(slot -> slot.item().stream()).filter(item -> item.is(itemId)).mapToInt(Item::count).sum();
	}

	/** The stack on the cursor, if any. */
	public Optional<Item> carried() {
		return carried;
	}

	/** Spreads the carried stack evenly over the slots, like dragging with the left mouse button held. */
	public void drag(int... slotIndexes) {
		drag(0, slotIndexes);
	}

	/** Puts one carried item into each slot, like dragging with the right mouse button held. */
	public void dragOneEach(int... slotIndexes) {
		drag(1, slotIndexes);
	}

	private void drag(int button, int[] slotIndexes) {
		remote.call("menu.drag", Json.object("menu", id, "button", button,
			"slots", Json.array(Arrays.stream(slotIndexes).boxed().toList())));
	}

	/** Clicks next to the menu, which throws the carried stack on the ground. */
	public void dropCarried() {
		remote.call("menu.click", Json.object("menu", id, "slot", OUTSIDE, "button", 0, "mode", "pickup"));
	}

	/** Closes the screen, like pressing escape. */
	public void close() {
		remote.call("menu.close");
	}
}
