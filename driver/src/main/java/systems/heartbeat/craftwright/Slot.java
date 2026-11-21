package systems.heartbeat.craftwright;

import java.util.Optional;
import systems.heartbeat.craftwright.internal.Json;

/** One slot of a {@link Menu}, with the clicks a mouse and keyboard can do on it. */
public final class Slot {
	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int MIDDLE = 2;

	private final Remote remote;
	private final int menuId;
	private final int index;
	private final Optional<Item> item;
	private final boolean playerInventory;

	Slot(Remote remote, int menuId, int index, Optional<Item> item, boolean playerInventory) {
		this.remote = remote;
		this.menuId = menuId;
		this.index = index;
		this.item = item;
		this.playerInventory = playerInventory;
	}

	public int index() {
		return index;
	}

	public Optional<Item> item() {
		return item;
	}

	public boolean isEmpty() {
		return item.isEmpty();
	}

	/** True if this slot belongs to the player's own inventory below the container. */
	public boolean isPlayerInventory() {
		return playerInventory;
	}

	/** Left click: picks the stack up, or puts the carried stack down. */
	public void click() {
		send(LEFT, "pickup");
	}

	/** Right click: picks up half, or puts one item down. */
	public void rightClick() {
		send(RIGHT, "pickup");
	}

	/** Shift click: moves the stack to the other side of the menu. */
	public void shiftClick() {
		send(LEFT, "quick_move");
	}

	public void middleClick() {
		send(MIDDLE, "clone");
	}

	/** Double click: collects all items like the carried one onto the cursor. */
	public void doubleClick() {
		send(LEFT, "pickup");
		send(LEFT, "pickup_all");
	}

	/** Presses a number key over the slot, swapping with that hotbar slot (0 to 8). */
	public void swapWithHotbar(int hotbarSlot) {
		send(hotbarSlot, "swap");
	}

	/** Presses the drop key over the slot. */
	public void drop(boolean wholeStack) {
		send(wholeStack ? RIGHT : LEFT, "throw");
	}

	/** Moves the whole stack to another slot of the same menu: pick up here, put down there. */
	public void moveTo(int targetSlot) {
		click();
		clickSlot(targetSlot, LEFT, "pickup");
	}

	private void send(int button, String mode) {
		clickSlot(index, button, mode);
	}

	private void clickSlot(int slot, int button, String mode) {
		remote.call("menu.click", Json.object("menu", menuId, "slot", slot, "button", button, "mode", mode));
	}

	@Override
	public String toString() {
		return "slot " + index + item.map(stack -> " (" + stack + ")").orElse(" (empty)");
	}
}
