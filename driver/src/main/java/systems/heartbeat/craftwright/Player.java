package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import systems.heartbeat.craftwright.internal.Await;

/** The client's own player: where it is, what it carries and what it does. */
public final class Player {
	private final Client client;

	Player(Client client) {
		this.client = client;
	}

	public PlayerState state() {
		return PlayerState.of(client.call("player.state"));
	}

	public Position position() {
		return state().position();
	}

	/** Waits until the player's state fulfils the condition, e.g. after a teleport. */
	public PlayerState awaitState(String description, Predicate<PlayerState> condition) {
		return Await.until(client.name() + " " + description, client.timeout(),
			() -> Optional.of(state()).filter(condition), () -> "State: " + state());
	}

	/** Waits until the player is within {@code tolerance} blocks of the point. */
	public PlayerState awaitPosition(double x, double y, double z, double tolerance) {
		Position target = new Position(x, y, z);
		return awaitState("to be at " + target, state -> state.position().distanceTo(target) <= tolerance);
	}

	/** Turns the head. Yaw 0 is south, 90 is west. Pitch -90 is up, 90 is down. */
	public void look(float yaw, float pitch) {
		JsonObject params = new JsonObject();
		params.addProperty("yaw", yaw);
		params.addProperty("pitch", pitch);
		client.call("player.look", params);
	}

	/** Turns the head towards a point in the world. */
	public void lookAt(double x, double y, double z) {
		client.call("player.lookAt", xyz(x, y, z));
	}

	/**
	 * Walks in a straight line to the point and waits until the player is there.
	 * Jumps up single blocks, does not find a way around walls.
	 */
	public void walkTo(double x, double z) {
		walkTo(x, z, client.timeout());
	}

	public void walkTo(double x, double z, Duration timeout) {
		JsonObject params = new JsonObject();
		params.addProperty("x", x);
		params.addProperty("z", z);
		client.call("player.walkTo", params);
		try {
			Await.untilTrue(client.name() + " to walk to x=" + x + " z=" + z, timeout,
				() -> !client.call("player.walking").get("walking").getAsBoolean(), () -> "Position: " + position());
		} finally {
			stop();
		}
		Position reached = position();
		if (reached.horizontalDistanceTo(x, z) > 1.0) {
			throw new CraftwrightException(client.name() + " could not walk to x=" + x + " z=" + z + ", got stuck at " + reached);
		}
	}

	/**
	 * Holds the given movement keys until {@link #stop()} or the next call.
	 * Example: {@code hold(Key.FORWARD, Key.SPRINT)}.
	 */
	public void hold(Key... keys) {
		JsonObject params = new JsonObject();
		for (Key key : Key.values()) {
			params.addProperty(key.wireName, false);
		}
		for (Key key : keys) {
			params.addProperty(key.wireName, true);
		}
		client.call("player.input", params);
	}

	/** Releases all movement keys. */
	public void stop() {
		hold();
	}

	/** Jumps once. */
	public void jump() {
		client.call("player.jump");
	}

	/** Selects a hotbar slot, 0 to 8. */
	public void selectHotbar(int slot) {
		JsonObject params = new JsonObject();
		params.addProperty("slot", slot);
		client.call("player.selectHotbar", params);
	}

	/** Right click with the item in hand at whatever the player looks at. */
	public void use() {
		client.call("player.use");
	}

	/** Left click: hits the entity or block the player looks at, or swings in the air. */
	public void attack() {
		client.call("player.attack");
	}

	/** Right clicks a block, e.g. to open a chest, press a button or place the held block against it. */
	public void useBlock(int x, int y, int z) {
		client.call("player.useBlock", xyz(x, y, z));
	}

	/** Mines a block and waits until it is gone. The block has to be in reach. */
	public void breakBlock(int x, int y, int z) {
		client.call("player.breakBlock", xyz(x, y, z));
		try {
			Await.untilTrue(client.name() + " to break the block at " + x + " " + y + " " + z, client.timeout(),
				() -> !client.call("player.breaking").get("breaking").getAsBoolean(), () -> "Position: " + position());
		} finally {
			client.call("player.stopBreaking");
		}
	}

	/** Right clicks the nearest entity of this type or with this name, within reach. */
	public void useEntity(String typeOrName) {
		JsonObject params = new JsonObject();
		params.addProperty("target", typeOrName);
		client.call("player.useEntity", params);
	}

	/** Hits the nearest entity of this type or with this name, within reach. */
	public void attackEntity(String typeOrName) {
		JsonObject params = new JsonObject();
		params.addProperty("target", typeOrName);
		client.call("player.attackEntity", params);
	}

	/** Drops the held item, the whole stack if asked. */
	public void drop(boolean wholeStack) {
		JsonObject params = new JsonObject();
		params.addProperty("wholeStack", wholeStack);
		client.call("player.drop", params);
	}

	/** Respawns after death. */
	public void respawn() {
		client.call("player.respawn");
	}

	/** The 36 inventory slots plus armour and off hand. Slots 0 to 8 are the hotbar. */
	public List<Optional<Item>> inventory() {
		List<Optional<Item>> items = new ArrayList<>();
		for (JsonElement element : client.call("player.inventory").getAsJsonArray("slots")) {
			items.add(Optional.ofNullable(Item.of(element)));
		}
		return items;
	}

	/** How many items of this id the player carries in total. */
	public int count(String itemId) {
		return inventory().stream().flatMap(Optional::stream).filter(item -> item.is(itemId)).mapToInt(Item::count).sum();
	}

	/** The item in the main hand. */
	public Optional<Item> heldItem() {
		return Optional.ofNullable(Item.of(client.call("player.heldItem").get("item")));
	}

	private static JsonObject xyz(double x, double y, double z) {
		JsonObject params = new JsonObject();
		params.addProperty("x", x);
		params.addProperty("y", y);
		params.addProperty("z", z);
		return params;
	}

	/** Movement keys for {@link #hold(Key...)}. */
	public enum Key {
		FORWARD("forward"), BACK("back"), LEFT("left"), RIGHT("right"), JUMP("jump"), SNEAK("sneak"), SPRINT("sprint");

		private final String wireName;

		Key(String wireName) {
			this.wireName = wireName;
		}
	}
}
