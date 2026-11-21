package systems.heartbeat.craftwright;

import java.util.List;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.Json;

/**
 * The scoreboard as the server sent it to this client: the three places an objective can be shown,
 * and the teams that put prefixes and suffixes around player names.
 */
public final class Scoreboard {
	private final Remote remote;

	Scoreboard(Remote remote) {
		this.remote = remote;
	}

	/** The objective on the right side of the screen, if one is shown. */
	public Optional<ScoreDisplay> sidebar() {
		return display("sidebar");
	}

	/** The numbers next to the names in the player list. */
	public Optional<ScoreDisplay> playerList() {
		return display("list");
	}

	/** The line under the name tags of other players. */
	public Optional<ScoreDisplay> belowName() {
		return display("below_name");
	}

	private Optional<ScoreDisplay> display(String slot) {
		return ScoreDisplay.of(remote.call("scoreboard.display", Json.object("slot", slot)));
	}

	public List<Team> teams() {
		return Json.list(remote.call("scoreboard.teams"), "teams", Team::of);
	}

	/** The team a player or other score holder is in. */
	public Optional<Team> teamOf(String member) {
		return teams().stream().filter(team -> team.has(member)).findFirst();
	}
}
