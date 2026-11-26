package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.AwaitTimeoutException;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Item;
import systems.heartbeat.craftwright.Menu;
import systems.heartbeat.craftwright.Menus;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** Typing a new name into an anvil: the result slot shows the item with that name. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class AnvilE2ETest {

	@Test
	void renamingShowsTheNewNameInTheResult(Server server, @Player("Alex") Client alex) {
		Stage stage = Stage.enter(server, alex, 80, 0);
		stage.give("paper", 1);
		stage.place(1, 0, 0, "anvil");
		Menu anvil = open(alex, stage);
		assertEquals("minecraft:anvil", anvil.type());

		anvil.findItem("paper").orElseThrow().shiftClick();
		alex.menus().await(menu -> !menu.slot(0).isEmpty());
		alex.menus().rename("Notizen");

		Item result = alex.menus().await(menu -> menu.slot(Menus.ANVIL_RESULT_SLOT).item()
			.map(item -> item.name().plain().contains("Notizen")).orElse(false))
			.slot(Menus.ANVIL_RESULT_SLOT).item().orElseThrow();
		assertTrue(result.is("paper"));
		alex.menus().current().ifPresent(Menu::close);
	}

	/**
	 * Right after joining and the teleport onto the stage the server can drop the first click on a
	 * block (it still waits for the teleport to settle). Clicks again a few times before giving up.
	 */
	private static Menu open(Client alex, Stage stage) {
		AwaitTimeoutException last = null;
		for (int attempt = 0; attempt < 3; attempt++) {
			alex.player().useBlock(stage.x(1), stage.y(0), stage.z(0));
			try {
				return alex.menus().await(menu -> true, Duration.ofSeconds(4));
			} catch (AwaitTimeoutException timeout) {
				last = timeout;
			}
		}
		throw last;
	}
}
