package systems.heartbeat.craftwright.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Short forms for building request parameters and reading result lists. */
public final class Json {
	private Json() {
	}

	/** {@code Json.object("x", 1, "name", "Alex")}: keys and values in turn. Values are numbers, booleans, strings or JSON. */
	public static JsonObject object(Object... keysAndValues) {
		if (keysAndValues.length % 2 != 0) {
			throw new IllegalArgumentException("Keys and values come in pairs");
		}
		JsonObject object = new JsonObject();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			put(object, (String) keysAndValues[i], keysAndValues[i + 1]);
		}
		return object;
	}

	private static void put(JsonObject object, String key, Object value) {
		switch (value) {
			case JsonElement element -> object.add(key, element);
			case Number number -> object.addProperty(key, number);
			case Boolean flag -> object.addProperty(key, flag);
			case String text -> object.addProperty(key, text);
			default -> throw new IllegalArgumentException("Cannot send " + value.getClass().getSimpleName() + " as '" + key + "'");
		}
	}

	public static JsonArray array(Iterable<? extends Number> numbers) {
		JsonArray array = new JsonArray();
		numbers.forEach(array::add);
		return array;
	}

	/** Reads the list {@code name} of {@code object}, one entry per element. */
	public static <T> List<T> list(JsonObject object, String name, Function<JsonElement, T> reader) {
		List<T> result = new ArrayList<>();
		for (JsonElement element : object.getAsJsonArray(name)) {
			result.add(reader.apply(element));
		}
		return List.copyOf(result);
	}

	public static boolean isNull(JsonElement element) {
		return element == null || element.isJsonNull();
	}
}
