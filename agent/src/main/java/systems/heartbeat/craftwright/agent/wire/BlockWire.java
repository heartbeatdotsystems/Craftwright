package systems.heartbeat.craftwright.agent.wire;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** A block on the wire: id, state properties and what the client knows of its block entity (sign text, head owner). */
public final class BlockWire {
	private static final String UNLOADED = "minecraft:void_air";

	private BlockWire() {
	}

	public static JsonObject at(ClientLevel level, BlockPos pos) {
		JsonObject block = new JsonObject();
		JsonObject properties = new JsonObject();
		if (level.isLoaded(pos)) {
			BlockState state = level.getBlockState(pos);
			block.addProperty("id", String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
			for (Property<?> property : state.getProperties()) {
				properties.addProperty(property.getName(), valueName(state, property));
			}
			BlockEntity entity = level.getBlockEntity(pos);
			block.add("blockEntity", entity == null ? JsonNull.INSTANCE : Codecs.json(entity.saveWithoutMetadata(level.registryAccess())));
		} else {
			block.addProperty("id", UNLOADED);
			block.add("blockEntity", JsonNull.INSTANCE);
		}
		block.add("properties", properties);
		return block;
	}

	private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
		return property.getName(state.getValue(property));
	}
}
