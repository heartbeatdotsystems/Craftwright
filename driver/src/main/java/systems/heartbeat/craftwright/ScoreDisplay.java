package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.Json;

/**
 * A scoreboard objective shown in one place: the sidebar, the player list or below name tags.
 *
 * @param objective internal name of the objective
 * @param lines     highest score first
 */
public record ScoreDisplay(String objective, Text title, List<ScoreLine> lines) {
	static Optional<ScoreDisplay> of(JsonObject result) {
		if (!result.get("shown").getAsBoolean()) {
			return Optional.empty();
		}
		return Optional.of(new ScoreDisplay(result.get("objective").getAsString(), Text.of(result.get("title")),
			Json.list(result, "lines", ScoreLine::of)));
	}

	/** The visible lines, top to bottom. */
	public List<String> texts() {
		return lines.stream().map(line -> line.text().plain()).toList();
	}

	/** The line of one score holder, e.g. a player's deaths in the player list. */
	public Optional<ScoreLine> line(String owner) {
		return lines.stream().filter(line -> line.owner().equals(owner)).findFirst();
	}
}
