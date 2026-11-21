package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import systems.heartbeat.craftwright.internal.Json;

/**
 * A scoreboard team as the client knows it. Prefix and suffix are what shows around
 * the names of its members in chat, the player list and above their heads.
 *
 * @param nameTagVisibility always, never, hide_for_other_teams or hide_for_own_team
 */
public record Team(String name, Text displayName, Text prefix, Text suffix, String nameTagVisibility, List<String> members) {
	static Team of(JsonElement element) {
		JsonObject team = element.getAsJsonObject();
		return new Team(team.get("name").getAsString(), Text.of(team.get("displayName")), Text.of(team.get("prefix")),
			Text.of(team.get("suffix")), team.get("nameTagVisibility").getAsString(),
			systems.heartbeat.craftwright.internal.Json.list(team, "members", JsonElement::getAsString));
	}

	public boolean has(String member) {
		return members.contains(member);
	}
}
