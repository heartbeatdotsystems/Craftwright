package systems.heartbeat.craftwright.agent.wire;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/** A text on the wire: the plain string a player reads plus the full component with colours, clicks and hovers. */
public final class TextWire {
	private TextWire() {
	}

	public static JsonElement of(Component component) {
		if (component == null) {
			return JsonNull.INSTANCE;
		}
		JsonObject text = new JsonObject();
		text.addProperty("plain", component.getString());
		text.add("json", Codecs.json(ComponentSerialization.CODEC, component));
		return text;
	}
}
