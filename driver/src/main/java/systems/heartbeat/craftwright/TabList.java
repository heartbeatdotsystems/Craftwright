package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.Json;

/** The player list shown while holding tab. */
public record TabList(Text header, Text footer, List<TabEntry> entries) {
	static TabList of(JsonObject result) {
		return new TabList(Text.of(result.get("header")), Text.of(result.get("footer")), Json.list(result, "entries", TabEntry::of));
	}

	/** Account names of everyone listed. */
	public List<String> players() {
		return entries.stream().map(TabEntry::name).toList();
	}

	public Optional<TabEntry> entry(String playerName) {
		return entries.stream().filter(entry -> entry.name().equals(playerName)).findFirst();
	}
}
