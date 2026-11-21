package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Item;
import systems.heartbeat.craftwright.Menu;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** A chest as the stand-in for every menu a plugin opens: reading items and every way to click them. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class MenuE2ETest {
	private static final int SWORD = 0;
	private static final int DIAMONDS = 4;
	private static final int MORE_DIAMONDS = 5;
	private static final String TREASURE = "minecraft:chest{CustomName:\"Treasure\",Items:["
		+ "{Slot:0b,id:\"minecraft:diamond_sword\",count:1,components:{\"minecraft:custom_name\":\"Excalibur\","
		+ "\"minecraft:lore\":[\"Forged in a test\"],\"minecraft:enchantments\":{\"minecraft:sharpness\":5}}},"
		+ "{Slot:4b,id:\"minecraft:diamond\",count:8},{Slot:5b,id:\"minecraft:diamond\",count:8}]}";

	private Client alex;
	private Menu chest;

	@BeforeEach
	void openTheChest(Server server, @Player("Alex") Client alex) {
		this.alex = alex;
		Stage stage = Stage.enter(server, alex, 16, 0);
		stage.place(1, 0, 0, "minecraft:air");
		stage.place(1, 0, 0, TREASURE);
		alex.player().useBlock(stage.x(1), stage.y(0), stage.z(0));
		chest = alex.menus().await("Treasure");
	}

	@Test
	void menuHasTypeTitleAndSlots() {
		assertEquals("minecraft:generic_9x3", chest.type());
		assertEquals("Treasure", chest.title().plain());
		assertEquals(27, chest.containerSlots().size());
		assertEquals(16, chest.count("diamond"));
		assertTrue(chest.slot(27).isPlayerInventory());
	}

	@Test
	void itemCarriesNameLoreAndEnchantments() {
		Item sword = chest.slotNamed("Excalibur").item().orElseThrow();
		assertTrue(sword.is("diamond_sword"));
		assertEquals(1, sword.count());
		assertTrue(sword.loreContains("Forged in a test"));
		assertTrue(sword.enchanted());
		assertTrue(sword.glint());
		assertTrue(sword.component("enchantments").orElseThrow().toString().contains("sharpness"));
		assertEquals(SWORD, chest.findItem("diamond_sword").orElseThrow().index());
	}

	@Test
	void shiftClickMovesTheStackToThePlayer() {
		chest.slot(DIAMONDS).shiftClick();
		alex.player().awaitCount("diamond", 8);
		alex.menus().await(menu -> menu.slot(DIAMONDS).isEmpty());
	}

	@Test
	void stackMovesFromOneSlotToAnother() {
		chest.slot(DIAMONDS).moveTo(13);
		Menu after = alex.menus().await(menu -> holds(menu, 13, 8));
		assertTrue(after.slot(DIAMONDS).isEmpty());
		assertTrue(after.carried().isEmpty());
	}

	@Test
	void rightClickTakesHalfAndDraggingSpreadsIt() {
		chest.slot(DIAMONDS).rightClick();
		Menu holdingHalf = alex.menus().await(menu -> menu.carried().filter(item -> item.count() == 4).isPresent());

		holdingHalf.drag(18, 19);

		alex.menus().await(menu -> holds(menu, 18, 2) && holds(menu, 19, 2) && menu.carried().isEmpty());
	}

	@Test
	void rightDragPutsOneItemInEachSlot() {
		chest.slot(DIAMONDS).click();
		alex.menus().await(menu -> menu.carried().isPresent()).dragOneEach(18, 19, 20);
		Menu after = alex.menus().await(menu -> holds(menu, 18, 1) && holds(menu, 19, 1) && holds(menu, 20, 1));
		assertEquals(5, after.carried().orElseThrow().count());
	}

	@Test
	void clickOutsideDropsWhatTheCursorCarries() {
		chest.slot(DIAMONDS).click();
		alex.menus().await(menu -> menu.carried().isPresent()).dropCarried();
		alex.menus().await(menu -> menu.carried().isEmpty() && menu.count("diamond") == 8);
		alex.world().awaitEntity("of dropped diamonds", entity -> entity.is("item"));
	}

	@Test
	void doubleClickCollectsEqualStacks() {
		chest.slot(DIAMONDS).doubleClick();
		Menu after = alex.menus().await(menu -> menu.carried().filter(item -> item.count() == 16).isPresent());
		assertTrue(after.slot(MORE_DIAMONDS).isEmpty());
	}

	@Test
	void numberKeySwapsWithTheHotbar() {
		chest.slot(SWORD).swapWithHotbar(2);
		alex.menus().await(menu -> menu.slot(SWORD).isEmpty());
		alex.player().selectHotbar(2);
		assertTrue(alex.player().heldItem().orElseThrow().name().contains("Excalibur"));
	}

	@Test
	void dropKeyThrowsTheStackOutOfTheSlot() {
		chest.slot(DIAMONDS).drop(true);
		alex.menus().await(menu -> menu.slot(DIAMONDS).isEmpty());
		alex.world().awaitEntity("of dropped diamonds", entity -> entity.is("item"));
	}

	@Test
	void closingTheMenuLeavesNoScreenOpen() {
		chest.close();
		alex.menus().awaitClosed();
		assertTrue(alex.menus().current().isEmpty());
	}

	private static boolean holds(Menu menu, int slot, int diamonds) {
		return menu.slot(slot).item().filter(item -> item.is("diamond") && item.count() == diamonds).isPresent();
	}
}
