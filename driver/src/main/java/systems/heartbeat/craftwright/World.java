package systems.heartbeat.craftwright;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import systems.heartbeat.craftwright.internal.Json;

/**
 * The world as this client has it loaded. Only what the server sent to this client is visible here,
 * which is exactly what makes it useful for testing vanish, anti-xray or hidden containers.
 */
public final class World {
	private final Remote remote;

	World(Remote remote) {
		this.remote = remote;
	}

	/** The block the client has at this position. Unloaded chunks report {@code minecraft:void_air}. */
	public Block block(int x, int y, int z) {
		return Block.of(remote.call("world.block", Json.object("x", x, "y", y, "z", z)));
	}

	/** Waits until the client has this block at the position, e.g. after the server placed it. */
	public Block awaitBlock(int x, int y, int z, String blockId) {
		return awaitBlock(x, y, z, "to be " + blockId, block -> block.is(blockId));
	}

	public Block awaitBlock(int x, int y, int z, String description, Predicate<Block> condition) {
		return remote.await("the block at " + x + " " + y + " " + z + " " + description,
			() -> Optional.of(block(x, y, z)).filter(condition), () -> "Block there: " + block(x, y, z).id());
	}

	/** Every entity the client knows about, the own player excluded. */
	public List<Entity> entities() {
		return Json.list(remote.call("world.entities"), "entities", Entity::of);
	}

	/** Entities of one type, e.g. {@code entities("villager")}. */
	public List<Entity> entities(String typeId) {
		return entities().stream().filter(entity -> entity.is(typeId)).toList();
	}

	/** Another player this client can see in the world. */
	public Optional<Entity> player(String name) {
		return entities("player").stream().filter(entity -> entity.name().plain().equals(name)).findFirst();
	}

	/** Names of the other players this client can see in the world. */
	public List<String> visiblePlayers() {
		return entities("player").stream().map(entity -> entity.name().plain()).toList();
	}

	/** Waits for an entity that fulfils the condition, e.g. a firework or a named villager. */
	public Entity awaitEntity(String description, Predicate<Entity> condition) {
		return remote.await("an entity " + description, () -> entities().stream().filter(condition).findFirst(),
			() -> "Entities seen: " + entities().stream().map(Entity::type).distinct().toList());
	}

	/** Time of day in ticks, 0 to 23999. A server can show each player a different time. */
	public long timeOfDay() {
		return remote.call("world.time").get("dayTime").getAsLong();
	}

	public boolean isRaining() {
		return remote.call("world.time").get("raining").getAsBoolean();
	}

	public WorldBorder border() {
		return WorldBorder.of(remote.call("world.border"));
	}
}
