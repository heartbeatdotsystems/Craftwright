package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.BossBar;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.TabEntry;
import systems.heartbeat.craftwright.Title;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** What the server draws over the game: titles, the action bar, boss bars and the player list. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class HudE2ETest {

	@Test
	void titleArrivesWithItsColourAndTimes(Server server, @Player("Alex") Client alex) {
		server.console("title Alex times 5 40 10");
		server.console("title Alex subtitle {\"text\":\"Enjoy your stay\"}");
		server.console("title Alex title {\"text\":\"Welcome\",\"color\":\"gold\"}");

		Title title = alex.hud().titles().await("Welcome");

		assertTrue(title.text().json().toString().contains("gold"), title.text().json().toString());
		assertEquals(5, title.fadeIn());
		assertEquals(40, title.stay());
		assertEquals(10, title.fadeOut());
		alex.hud().subtitles().await("Enjoy your stay");
	}

	@Test
	void actionBarArrives(Server server, @Player("Alex") Client alex) {
		server.console("title Alex actionbar {\"text\":\"Above the hotbar\"}");
		alex.hud().actionBar().await("Above the hotbar");
	}

	@Test
	void bossBarShowsNameColourAndProgress(Server server, @Player("Alex") Client alex) {
		server.console("bossbar add craftwright:hud \"Loading the fun\"");
		try {
			server.console("bossbar set craftwright:hud color red");
			server.console("bossbar set craftwright:hud max 100");
			server.console("bossbar set craftwright:hud value 50");
			server.console("bossbar set craftwright:hud players Alex");

			BossBar bar = alex.hud().awaitBossBar("Loading the fun");

			assertEquals("red", bar.color());
			assertEquals(0.5f, bar.progress(), 0.001f);
		} finally {
			server.console("bossbar remove craftwright:hud");
		}
		alex.awaitTrue("the boss bar to disappear", () -> alex.hud().bossBars().isEmpty());
	}

	@Test
	void playerListKnowsThePlayerAndItsGameMode(Server server, @Player("Alex") Client alex) {
		server.console("gamemode adventure Alex");
		TabEntry entry = alex.await("Alex in adventure mode in the player list",
			() -> alex.hud().tabList().entry("Alex").filter(alexEntry -> alexEntry.gameMode().equals("adventure")));
		assertTrue(entry.latency() >= 0);
		server.console("gamemode survival Alex");
	}
}
