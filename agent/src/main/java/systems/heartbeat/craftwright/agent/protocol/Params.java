package systems.heartbeat.craftwright.agent.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** The parameters of one request, read by name with errors a test author can act on. */
public final class Params {
	private final String method;
	private final JsonObject values;

	public Params(String method, JsonObject values) {
		this.method = method;
		this.values = values;
	}

	public boolean has(String name) {
		return values.has(name) && !values.get(name).isJsonNull();
	}

	public String string(String name) {
		return required(name).getAsString();
	}

	public String string(String name, String fallback) {
		return has(name) ? values.get(name).getAsString() : fallback;
	}

	public int integer(String name) {
		return required(name).getAsInt();
	}

	public int integer(String name, int fallback) {
		return has(name) ? values.get(name).getAsInt() : fallback;
	}

	public double number(String name) {
		return required(name).getAsDouble();
	}

	public boolean flag(String name) {
		return has(name) && values.get(name).getAsBoolean();
	}

	public List<Integer> integers(String name) {
		JsonElement element = required(name);
		if (!element.isJsonArray()) {
			throw new RequestException(method + ": '" + name + "' must be a list");
		}
		List<Integer> result = new ArrayList<>();
		for (JsonElement entry : (JsonArray) element) {
			result.add(entry.getAsInt());
		}
		return result;
	}

	private JsonElement required(String name) {
		if (!has(name)) {
			throw new RequestException(method + " needs the parameter '" + name + "'");
		}
		return values.get(name);
	}
}
