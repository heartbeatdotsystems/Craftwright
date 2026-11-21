package systems.heartbeat.craftwright;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.regex.Pattern;

/**
 * A piece of text as the client shows it.
 *
 * @param plain the visible characters without colours or formatting
 * @param json  the full text component (colours, click and hover events), may be JSON null
 */
public record Text(String plain, JsonElement json) {

	public static Text of(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return new Text("", com.google.gson.JsonNull.INSTANCE);
		}
		if (element.isJsonObject() && element.getAsJsonObject().has("plain")) {
			JsonObject object = element.getAsJsonObject();
			return new Text(object.get("plain").getAsString(), object.get("json"));
		}
		return fromComponent(element);
	}

	/** Reads a raw text component, e.g. the description of a server list ping. */
	public static Text fromComponent(JsonElement component) {
		StringBuilder plain = new StringBuilder();
		flatten(component, plain);
		return new Text(plain.toString(), component);
	}

	private static void flatten(JsonElement element, StringBuilder out) {
		if (element == null || element.isJsonNull()) {
			return;
		}
		if (element.isJsonPrimitive()) {
			out.append(element.getAsString());
		} else if (element.isJsonArray()) {
			for (JsonElement child : element.getAsJsonArray()) {
				flatten(child, out);
			}
		} else {
			JsonObject object = element.getAsJsonObject();
			if (object.has("text")) {
				out.append(object.get("text").getAsString());
			} else if (object.has("fallback")) {
				out.append(object.get("fallback").getAsString());
			} else if (object.has("translate")) {
				out.append(object.get("translate").getAsString());
			}
			if (object.has("extra")) {
				JsonArray extra = object.getAsJsonArray("extra");
				for (JsonElement child : extra) {
					flatten(child, out);
				}
			}
		}
	}

	public boolean contains(String part) {
		return plain.contains(part);
	}

	public boolean matches(Pattern pattern) {
		return pattern.matcher(plain).find();
	}

	public boolean isEmpty() {
		return plain.isEmpty();
	}

	@Override
	public String toString() {
		return plain;
	}
}
