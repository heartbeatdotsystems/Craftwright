package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Menu;
import systems.heartbeat.craftwright.Merchant;
import systems.heartbeat.craftwright.Offer;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** Trading with a villager: reading the offers, choosing one and taking the goods. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class TradeE2ETest {
	private static final String MASON = "{NoAI:1b,Silent:1b,Invulnerable:1b,VillagerData:{profession:\"minecraft:mason\",level:2,type:\"minecraft:plains\"},"
		+ "Offers:{Recipes:[{buy:{id:\"minecraft:emerald\",count:1},sell:{id:\"minecraft:quartz\",count:4},maxUses:10}]}}";

	@Test
	void emeraldBuysQuartz(Server server, @Player("Alex") Client alex) {
		Stage stage = Stage.enter(server, alex, 48, 0);
		stage.give("emerald", 1);
		server.console("summon minecraft:villager " + stage.at(2, 0, 0) + " " + MASON);
		try {
			alex.world().awaitEntity("that is a villager", entity -> entity.is("villager"));
			alex.player().useEntity("villager");
			Menu shop = alex.menus().await();
			assertEquals("minecraft:merchant", shop.type());

			Merchant merchant = alex.menus().merchant();
			assertEquals(2, merchant.level());
			assertEquals(1, merchant.offers().size());
			Offer quartz = merchant.offerFor("quartz").orElseThrow();
			assertTrue(quartz.firstCost().is("emerald"));
			assertTrue(quartz.secondCost().isEmpty());
			assertEquals(4, quartz.result().count());
			assertEquals(10, quartz.maxUses());
			assertFalse(quartz.outOfStock());

			merchant.select(quartz);
			alex.menus().await(menu -> !menu.slot(Merchant.RESULT_SLOT).isEmpty()).slot(Merchant.RESULT_SLOT).shiftClick();

			alex.player().awaitCount("quartz", 4);
			alex.player().awaitCount("emerald", 0);
		} finally {
			server.console("kill @e[type=minecraft:villager]");
		}
	}
}
