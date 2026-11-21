package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Menu;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** The player's own items: hotbar, both hands, the inventory screen and dropping. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class InventoryE2ETest {
	/** Slot numbers of the inventory screen, see {@link systems.heartbeat.craftwright.Menus#inventory()}. */
	private static final int FIRST_HOTBAR_SLOT = 36;
	private static final int FIRST_MAIN_SLOT = 9;

	private Client alex;

	@BeforeEach
	void startWithFiveEmeralds(Server server, @Player("Alex") Client alex) {
		this.alex = alex;
		Stage stage = Stage.enter(server, alex, 32, 0);
		stage.give("emerald", 5);
		alex.player().selectHotbar(0);
	}

	@Test
	void givenItemsAreInTheHand() {
		assertEquals(5, alex.player().count("emerald"));
		assertTrue(alex.player().heldItem().orElseThrow().is("emerald"));
		assertEquals(0, alex.player().state().selectedHotbarSlot());
	}

	@Test
	void otherHotbarSlotIsEmpty() {
		alex.player().selectHotbar(3);
		assertTrue(alex.player().heldItem().isEmpty());
		assertEquals(3, alex.player().state().selectedHotbarSlot());
	}

	@Test
	void swappingHandsMovesTheItemToTheOffHand() {
		alex.player().swapHands();
		alex.awaitTrue("the emeralds to be in the off hand", () -> alex.player().offHandItem().isPresent());
		assertTrue(alex.player().heldItem().isEmpty());
		assertEquals(5, alex.player().offHandItem().orElseThrow().count());
	}

	@Test
	void inventoryScreenMovesItemsBetweenSlots() {
		Menu inventory = alex.menus().inventory();
		assertTrue(inventory.slot(FIRST_HOTBAR_SLOT).item().orElseThrow().is("emerald"));

		inventory.slot(FIRST_HOTBAR_SLOT).moveTo(FIRST_MAIN_SLOT);

		alex.awaitTrue("the emeralds to be in the main inventory",
			() -> alex.menus().inventory().slot(FIRST_MAIN_SLOT).item().filter(item -> item.count() == 5).isPresent());
		assertTrue(alex.player().heldItem().isEmpty());
	}

	@Test
	void droppingOneItemLeavesTheRest() {
		alex.player().drop(false);
		alex.player().awaitCount("emerald", 4);
		alex.world().awaitEntity("of a dropped emerald", entity -> entity.is("item"));
	}

	@Test
	void droppingTheStackEmptiesTheHand() {
		alex.player().drop(true);
		alex.player().awaitCount("emerald", 0);
	}
}
