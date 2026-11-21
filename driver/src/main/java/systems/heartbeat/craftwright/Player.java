package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import systems.heartbeat.craftwright.internal.Json;

/** The client's own player: where it is, what it carries and what it does. */
public final class Player {
	private static final double WALK_TOLERANCE = 1.0;

	private final Remote remote;

	Player(Remote remote) {
		this.remote = remote;
	}

	// ── state ───────────────────────────────────────────────────────────────

	public PlayerState state() {
		return PlayerState.of(remote.call("player.state"));
	}

	public Position position() {
		return state().position();
	}

	/** Waits until the player's state fulfils the condition, e.g. after a teleport. */
	public PlayerState awaitState(String description, Predicate<PlayerState> condition) {
		return remote.await(description, () -> Optional.of(state()).filter(condition), () -> "State: " + state());
	}

	/** Waits until the player is within {@code tolerance} blocks of the point. */
	public PlayerState awaitPosition(double x, double y, double z, double tolerance) {
		Position target = new Position(x, y, z);
		return awaitState("to be at " + target, state -> state.position().distanceTo(target) <= tolerance);
	}

	// ── moving ──────────────────────────────────────────────────────────────

	/** Turns the head. Yaw 0 is south, 90 is west. Pitch -90 is up, 90 is down. */
	public void look(float yaw, float pitch) {
		remote.call("player.look", Json.object("yaw", yaw, "pitch", pitch));
	}

	/** Turns the head towards a point in the world. */
	public void lookAt(double x, double y, double z) {
		remote.call("player.lookAt", xyz(x, y, z));
	}

	/**
	 * Walks in a straight line to the point and waits until the player is there.
	 * Jumps up single blocks, does not find a way around walls.
	 */
	public void walkTo(double x, double z) {
		walkTo(x, z, remote.timeout());
	}

	public void walkTo(double x, double z, Duration timeout) {
		if (!tryWalkTo(x, z, timeout)) {
			throw new CraftwrightException(remote.name() + " could not walk to x=" + x + " z=" + z + ", got stuck at " + position());
		}
	}

	/**
	 * Walks towards the point and reports whether the player arrived. For tests that expect
	 * the server to stop the player: a border, a locked area, a frozen player.
	 */
	public boolean tryWalkTo(double x, double z, Duration timeout) {
		remote.call("player.walkTo", Json.object("x", x, "z", z));
		try {
			remote.awaitTrue("to finish walking to x=" + x + " z=" + z, timeout,
				() -> !remote.call("player.walking").get("walking").getAsBoolean(), () -> "Position: " + position());
		} catch (AwaitTimeoutException stuck) {
			return false;
		} finally {
			stop();
		}
		return position().horizontalDistanceTo(x, z) <= WALK_TOLERANCE;
	}

	/**
	 * Holds the given movement keys until {@link #stop()} or the next call.
	 * Example: {@code hold(Key.FORWARD, Key.SPRINT)}.
	 */
	public void hold(Key... keys) {
		JsonObject params = new JsonObject();
		for (Key key : keys) {
			params.addProperty(key.wireName(), true);
		}
		remote.call("player.input", params);
	}

	/** Releases all movement keys. */
	public void stop() {
		hold();
	}

	/** Jumps once. */
	public void jump() {
		remote.call("player.jump");
	}

	// ── using the world ─────────────────────────────────────────────────────

	/** Right click with the item in hand at whatever the player looks at. */
	public void use() {
		remote.call("player.use");
	}

	/** Left click: hits the entity or block the player looks at, or swings in the air. */
	public void attack() {
		remote.call("player.attack");
	}

	/** Right clicks a block, e.g. to open a chest or press a button. */
	public void useBlock(int x, int y, int z) {
		remote.call("player.useBlock", xyz(x, y, z));
	}

	/**
	 * Right clicks one side of a block. With a block in hand this places it against that side:
	 * {@code placeAgainst(0, 64, 0, "up")} puts it on top.
	 *
	 * @param side up, down, north, south, east or west
	 */
	public void placeAgainst(int x, int y, int z, String side) {
		JsonObject params = xyz(x, y, z);
		params.addProperty("face", side);
		remote.call("player.useBlock", params);
	}

	/** Mines a block and waits until it is gone. The block has to be in reach. */
	public void breakBlock(int x, int y, int z) {
		if (!tryBreakBlock(x, y, z, remote.timeout())) {
			throw new CraftwrightException(remote.name() + " could not break the block at " + x + " " + y + " " + z);
		}
	}

	/**
	 * Mines a block for at most the given time and reports whether the client saw it break.
	 * A server that protects the block puts it back, so check the block again afterwards.
	 */
	public boolean tryBreakBlock(int x, int y, int z, Duration timeout) {
		remote.call("player.breakBlock", xyz(x, y, z));
		try {
			remote.awaitTrue("to break the block at " + x + " " + y + " " + z, timeout,
				() -> !remote.call("player.breaking").get("breaking").getAsBoolean(), () -> "Position: " + position());
			return true;
		} catch (AwaitTimeoutException unbroken) {
			return false;
		} finally {
			remote.call("player.stopBreaking");
		}
	}

	/** Right clicks the nearest entity of this type, name or UUID, within reach. */
	public void useEntity(String typeOrName) {
		remote.call("player.useEntity", Json.object("target", typeOrName));
	}

	/** Hits the nearest entity of this type, name or UUID, within reach. */
	public void attackEntity(String typeOrName) {
		remote.call("player.attackEntity", Json.object("target", typeOrName));
	}

	/** Respawns after death. */
	public void respawn() {
		remote.call("player.respawn");
	}

	// ── items ───────────────────────────────────────────────────────────────

	/** Selects a hotbar slot, 0 to 8. */
	public void selectHotbar(int slot) {
		remote.call("player.selectHotbar", Json.object("slot", slot));
	}

	/** Swaps main hand and off hand, like the F key. */
	public void swapHands() {
		remote.call("player.swapHands");
	}

	/** Drops the held item, the whole stack if asked. */
	public void drop(boolean wholeStack) {
		remote.call("player.drop", Json.object("wholeStack", wholeStack));
	}

	/** The 36 inventory slots plus armour and off hand. Slots 0 to 8 are the hotbar. */
	public List<Optional<Item>> inventory() {
		return Json.list(remote.call("player.inventory"), "slots", Item::of);
	}

	/** How many items of this id the player carries in total. */
	public int count(String itemId) {
		return inventory().stream().flatMap(Optional::stream).filter(item -> item.is(itemId)).mapToInt(Item::count).sum();
	}

	/** Waits until the player carries exactly this many items of the id. */
	public void awaitCount(String itemId, int expected) {
		remote.awaitTrue("to carry " + expected + "x " + itemId, () -> count(itemId) == expected, () -> "Carries " + count(itemId));
	}

	/** The item in the main hand. */
	public Optional<Item> heldItem() {
		return Item.of(remote.call("player.heldItem").get("item"));
	}

	public Optional<Item> offHandItem() {
		return Item.of(remote.call("player.heldItem").get("offHand"));
	}

	private static JsonObject xyz(double x, double y, double z) {
		return Json.object("x", x, "y", y, "z", z);
	}
}
