package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * What a click on a chat text does.
 *
 * @param action run_command, suggest_command, open_url, copy_to_clipboard, change_page, …
 * @param event  the click event as chat components write it
 */
public record Click(String action, JsonObject event) {
	static Click of(JsonElement element) {
		JsonObject event = element.getAsJsonObject();
		return new Click(event.get("action").getAsString(), event);
	}

	/** The command, link or text behind the click, whichever this action carries. */
	public Optional<String> value() {
		return Stream.of("command", "url", "value").filter(event::has).map(key -> event.get(key).getAsString()).findFirst();
	}
}
