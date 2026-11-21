package systems.heartbeat.craftwright.agent.wire;

import java.util.Locale;
import net.minecraft.core.Holder;

/** Names on the wire: namespaced ids and lower-case enum constants. */
public final class Ids {
	private Ids() {
	}

	public static String of(Holder<?> holder) {
		return holder.unwrapKey().map(key -> String.valueOf(key.identifier())).orElse("");
	}

	public static String of(Enum<?> constant) {
		return constant.name().toLowerCase(Locale.ROOT);
	}

	/** Accepts "zombie" for "minecraft:zombie". */
	public static String qualify(String id) {
		return id.contains(":") ? id : "minecraft:" + id;
	}
}
