package example;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.PaperServerBuilder;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.junit.ConfigureServer;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** The annotation style, with two players who see each other. */
@Tag("e2e")
@CraftwrightTest(timeoutSeconds = 30)
class TwoPlayersE2ETest {

	@ConfigureServer
	static void server(PaperServerBuilder paper) {
		paper.property("motd", "Two players");
	}

	@Test
	void playersSeeEachOther(Server server, @Player("Alex") Client alex, @Player("Steve") Client steve) {
		assertTrue(alex.hud().tabList().players().contains("Steve"));
		steve.say("hi Alex");
		alex.chat().await("hi Alex");
		server.console("tp Steve Alex");
		alex.player().awaitState("to see Steve", state -> alex.world().visiblePlayers().contains("Steve"));
	}

	@Test
	void whisperReachesOnlyTheTarget(@Player("Alex") Client alex, @Player("Steve") Client steve) {
		alex.command("msg Steve psst");
		steve.chat().await("psst");
	}
}
