package systems.heartbeat.craftwright.agent;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;

/** Turns text components into the wire form: the plain string plus the full component as JSON. */
final class Texts {
	private Texts() {
	}

	static JsonElement wire(Component component) {
		if (component == null) {
			return JsonNull.INSTANCE;
		}
		JsonObject object = new JsonObject();
		object.addProperty("plain", component.getString());
		object.add("json", json(component));
		return object;
	}

	private static JsonElement json(Component component) {
		try {
			DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
			ClientPacketListener connection = Minecraft.getInstance().getConnection();
			if (connection != null) {
				ops = RegistryOps.create(JsonOps.INSTANCE, connection.registryAccess());
			}
			return ComponentSerialization.CODEC.encodeStart(ops, component).result().orElse(JsonNull.INSTANCE);
		} catch (RuntimeException e) {
			return JsonNull.INSTANCE;
		}
	}
}
