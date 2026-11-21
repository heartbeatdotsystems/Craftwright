package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.protocol.Params;
import systems.heartbeat.craftwright.agent.protocol.Routes;
import systems.heartbeat.craftwright.agent.wire.BlockWire;
import systems.heartbeat.craftwright.agent.wire.EntityWire;

/** The world as this client sees it: blocks, entities, time, weather and the world border. */
public final class WorldFeature implements Feature {
	private static final long TICKS_PER_DAY = 24000L;

	@Override
	public void register(Routes routes) {
		routes.on("world.block", this::block);
		routes.on("world.entities", (minecraft, params) -> entities(minecraft));
		routes.on("world.time", (minecraft, params) -> time(minecraft));
		routes.on("world.border", (minecraft, params) -> border(minecraft));
	}

	private JsonElement block(Minecraft minecraft, Params params) {
		BlockPos pos = BlockPos.containing(params.number("x"), params.number("y"), params.number("z"));
		return BlockWire.at(Game.level(minecraft), pos);
	}

	private JsonElement entities(Minecraft minecraft) {
		JsonArray entities = new JsonArray();
		for (Entity entity : Game.level(minecraft).entitiesForRendering()) {
			if (entity != minecraft.player) {
				entities.add(EntityWire.of(entity));
			}
		}
		JsonObject result = new JsonObject();
		result.add("entities", entities);
		return result;
	}

	private JsonElement time(Minecraft minecraft) {
		ClientLevel level = Game.level(minecraft);
		JsonObject result = new JsonObject();
		result.addProperty("dayTime", Math.floorMod(level.getDefaultClockTime(), TICKS_PER_DAY));
		result.addProperty("raining", level.isRaining());
		return result;
	}

	private JsonElement border(Minecraft minecraft) {
		WorldBorder border = Game.level(minecraft).getWorldBorder();
		JsonObject result = new JsonObject();
		result.addProperty("centerX", border.getCenterX());
		result.addProperty("centerZ", border.getCenterZ());
		result.addProperty("size", border.getSize());
		return result;
	}
}
