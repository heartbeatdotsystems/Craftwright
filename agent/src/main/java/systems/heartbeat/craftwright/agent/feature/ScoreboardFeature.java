package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.protocol.Params;
import systems.heartbeat.craftwright.agent.protocol.RequestException;
import systems.heartbeat.craftwright.agent.protocol.Routes;
import systems.heartbeat.craftwright.agent.wire.Ids;
import systems.heartbeat.craftwright.agent.wire.TextWire;

/** The scoreboard as the client has it: what is shown in the sidebar, player list and below names, and the teams. */
public final class ScoreboardFeature implements Feature {
	@Override
	public void register(Routes routes) {
		routes.on("scoreboard.display", this::display);
		routes.on("scoreboard.teams", (minecraft, params) -> teams(minecraft));
	}

	private JsonElement display(Minecraft minecraft, Params params) {
		Scoreboard scoreboard = Game.connection(minecraft).scoreboard();
		Objective objective = scoreboard.getDisplayObjective(slot(params.string("slot")));
		JsonObject result = new JsonObject();
		result.addProperty("shown", objective != null);
		if (objective == null) {
			return result;
		}
		List<PlayerScoreEntry> scores = new ArrayList<>(scoreboard.listPlayerScores(objective));
		scores.removeIf(PlayerScoreEntry::isHidden);
		scores.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
			.thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER));
		JsonArray lines = new JsonArray();
		for (PlayerScoreEntry score : scores) {
			JsonObject line = new JsonObject();
			line.addProperty("owner", score.owner());
			line.add("text", TextWire.of(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(score.owner()), score.ownerName())));
			line.addProperty("score", score.value());
			line.add("formatted", TextWire.of(score.formatValue(objective.numberFormatOrDefault(StyledFormat.NO_STYLE))));
			lines.add(line);
		}
		result.addProperty("objective", objective.getName());
		result.add("title", TextWire.of(objective.getDisplayName()));
		result.add("lines", lines);
		return result;
	}

	private static DisplaySlot slot(String name) {
		return switch (name) {
			case "sidebar" -> DisplaySlot.SIDEBAR;
			case "list" -> DisplaySlot.LIST;
			case "below_name" -> DisplaySlot.BELOW_NAME;
			default -> throw new RequestException("Unknown scoreboard slot '" + name + "'. Use sidebar, list or below_name.");
		};
	}

	private JsonElement teams(Minecraft minecraft) {
		JsonArray teams = new JsonArray();
		for (PlayerTeam team : Game.connection(minecraft).scoreboard().getPlayerTeams()) {
			JsonObject wire = new JsonObject();
			wire.addProperty("name", team.getName());
			wire.add("displayName", TextWire.of(team.getDisplayName()));
			wire.add("prefix", TextWire.of(team.getPlayerPrefix()));
			wire.add("suffix", TextWire.of(team.getPlayerSuffix()));
			wire.addProperty("nameTagVisibility", Ids.of(team.getNameTagVisibility()));
			JsonArray members = new JsonArray();
			team.getPlayers().forEach(members::add);
			wire.add("members", members);
			teams.add(wire);
		}
		JsonObject result = new JsonObject();
		result.add("teams", teams);
		return result;
	}
}
