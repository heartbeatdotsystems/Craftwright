package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.ScoreDisplay;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.Team;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** Objectives in the sidebar and the player list, and teams around player names. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class ScoreboardE2ETest {

	@Test
	void sidebarShowsTitleAndScores(Server server, @Player("Alex") Client alex) {
		server.console("scoreboard objectives add cw_kills dummy \"Kills today\"");
		try {
			server.console("scoreboard players set Alex cw_kills 7");
			server.console("scoreboard objectives setdisplay sidebar cw_kills");

			ScoreDisplay sidebar = alex.await("the sidebar with Alex's score",
				() -> alex.scoreboard().sidebar().filter(display -> display.line("Alex").isPresent()));

			assertEquals("cw_kills", sidebar.objective());
			assertEquals("Kills today", sidebar.title().plain());
			assertEquals(7, sidebar.line("Alex").orElseThrow().score());
		} finally {
			server.console("scoreboard objectives remove cw_kills");
		}
		alex.awaitTrue("the sidebar to disappear", () -> alex.scoreboard().sidebar().isEmpty());
	}

	@Test
	void playerListShowsAScore(Server server, @Player("Alex") Client alex) {
		server.console("scoreboard objectives add cw_deaths dummy \"Deaths\"");
		try {
			server.console("scoreboard players set Alex cw_deaths 3");
			server.console("scoreboard objectives setdisplay list cw_deaths");

			ScoreDisplay list = alex.await("the player list with Alex's score",
				() -> alex.scoreboard().playerList().filter(display -> display.line("Alex").isPresent()));

			assertEquals(3, list.line("Alex").orElseThrow().score());
			assertTrue(alex.scoreboard().belowName().isEmpty(), "nothing is shown below names");
		} finally {
			server.console("scoreboard objectives remove cw_deaths");
		}
	}

	@Test
	void teamPutsPrefixAndSuffixAroundTheName(Server server, @Player("Alex") Client alex) {
		server.console("team add cw_red \"Red Team\"");
		try {
			server.console("team modify cw_red prefix \"[Red] \"");
			server.console("team modify cw_red suffix \" *\"");
			server.console("team modify cw_red nametagVisibility never");
			server.console("team join cw_red Alex");

			Team team = alex.await("Alex to be in a team", () -> alex.scoreboard().teamOf("Alex"));

			assertEquals("cw_red", team.name());
			assertEquals("Red Team", team.displayName().plain());
			assertEquals("[Red] ", team.prefix().plain());
			assertEquals(" *", team.suffix().plain());
			assertEquals("never", team.nameTagVisibility());
		} finally {
			server.console("team remove cw_red");
		}
		alex.awaitTrue("the team to be gone", () -> alex.scoreboard().teamOf("Alex").isEmpty());
	}
}
