package systems.heartbeat.craftwright.agent.wire;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

/** Writes game values as JSON with the game's own codecs, so the wire form is the one data packs use. */
public final class Codecs {
	private Codecs() {
	}

	public static <T> JsonElement json(Codec<T> codec, T value) {
		try {
			return codec.encodeStart(ops(), value).result().orElse(JsonNull.INSTANCE);
		} catch (RuntimeException e) {
			return JsonNull.INSTANCE;
		}
	}

	public static JsonElement json(Tag tag) {
		return tag == null ? JsonNull.INSTANCE : NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tag);
	}

	/** Registry-aware once the client is on a server, plain JSON on the title screen. */
	private static DynamicOps<JsonElement> ops() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection == null ? JsonOps.INSTANCE : RegistryOps.create(JsonOps.INSTANCE, connection.registryAccess());
	}
}
