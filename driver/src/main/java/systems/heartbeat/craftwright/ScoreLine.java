package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * One line of a displayed scoreboard objective.
 *
 * @param owner     the score holder, usually a player name
 * @param text      the line as shown, team prefix and suffix included
 * @param score     the number
 * @param formatted the number as shown, which the server may have replaced by any text
 */
public record ScoreLine(String owner, Text text, int score, Text formatted) {
	static ScoreLine of(JsonElement element) {
		JsonObject line = element.getAsJsonObject();
		return new ScoreLine(line.get("owner").getAsString(), Text.of(line.get("text")), line.get("score").getAsInt(),
			Text.of(line.get("formatted")));
	}
}
