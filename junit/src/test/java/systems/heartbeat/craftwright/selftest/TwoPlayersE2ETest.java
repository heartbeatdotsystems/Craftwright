package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Entity;
import systems.heartbeat.craftwright.Key;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** Two clients on one server: what one does, the other sees. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class TwoPlayersE2ETest {
	private Client alex;
	private Client steve;

	@BeforeEach
	void standSideBySide(Server server, @Player("Alex") Client alex, @Player("Steve") Client steve) {
		this.alex = alex;
		this.steve = steve;
		Stage.enter(server, alex, 0, 64);
		Stage.enter(server, steve, 2, 64);
	}

	@Test
	void playersSeeEachOther() {
		assertTrue(alex.hud().tabList().players().contains("Steve"));
		assertTrue(steve.hud().tabList().players().contains("Alex"));
		alex.awaitTrue("Steve to be visible", () -> alex.world().visiblePlayers().contains("Steve"));
		steve.awaitTrue("Alex to be visible", () -> steve.world().visiblePlayers().contains("Alex"));
	}

	@Test
	void chatReachesTheOtherPlayer() {
		steve.chat().say("hi Alex");
		alex.chat().await("hi Alex");
	}

	@Test
	void whisperReachesTheTargetOnly() {
		steve.chat().command("msg Alex psst");
		alex.chat().await("Steve whispers to you: psst");
		steve.chat().received().expectNone("whispers to you", Duration.ofMillis(500));
	}

	@Test
	void sneakingAndHeldItemsAreVisibleToOthers(Server server) {
		server.console("give Steve minecraft:diamond_sword 1");
		steve.player().awaitCount("diamond_sword", 1);
		steve.player().selectHotbar(0);
		steve.player().hold(Key.SNEAK);
		try {
			Entity seen = alex.world().awaitEntity("Steve sneaking with a sword", entity -> entity.isPlayer()
				&& entity.name().plain().equals("Steve") && entity.sneaking() && entity.equipment().containsKey("mainhand"));
			assertTrue(seen.equipment().get("mainhand").is("diamond_sword"));
		} finally {
			steve.player().stop();
		}
	}
}
