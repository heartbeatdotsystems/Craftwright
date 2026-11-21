package systems.heartbeat.craftwright.agent.wire;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** An entity on the wire, as far as the client knows it. */
public final class EntityWire {
	private EntityWire() {
	}

	public static String type(Entity entity) {
		return String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
	}

	public static JsonObject of(Entity entity) {
		JsonObject wire = new JsonObject();
		wire.addProperty("id", entity.getId());
		wire.addProperty("uuid", entity.getUUID().toString());
		wire.addProperty("type", type(entity));
		wire.add("name", TextWire.of(entity.getName()));
		wire.add("displayName", TextWire.of(entity.getDisplayName()));
		wire.add("customName", TextWire.of(entity.getCustomName()));
		wire.addProperty("customNameVisible", entity.isCustomNameVisible());
		wire.addProperty("x", entity.getX());
		wire.addProperty("y", entity.getY());
		wire.addProperty("z", entity.getZ());
		wire.addProperty("invisible", entity.isInvisible());
		wire.addProperty("glowing", entity.isCurrentlyGlowing());
		wire.addProperty("sneaking", entity.isCrouching());
		if (entity instanceof LivingEntity living) {
			wire.addProperty("health", living.getHealth());
			wire.add("equipment", equipment(living));
		}
		return wire;
	}

	private static JsonObject equipment(LivingEntity living) {
		JsonObject equipment = new JsonObject();
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			ItemStack stack = living.getItemBySlot(slot);
			if (!stack.isEmpty()) {
				equipment.add(Ids.of(slot), ItemWire.of(stack));
			}
		}
		return equipment;
	}
}
