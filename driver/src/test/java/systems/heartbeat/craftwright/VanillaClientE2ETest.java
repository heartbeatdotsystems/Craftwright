package systems.heartbeat.craftwright;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Craftwright tested with itself: a real Paper server and a real client. */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VanillaClientE2ETest {
	private static Craftwright craftwright;
	private static Server server;
	private static Client alex;

	@BeforeAll
	static void start() {
		craftwright = Craftwright.create().defaultTimeout(Duration.ofSeconds(30));
		server = craftwright.paper("26.3").flatWorld().property("motd", "Hello from Craftwright").start();
		alex = craftwright.client("Alex").start();
	}

	@AfterAll
	static void stop() {
		if (craftwright != null) {
			craftwright.close();
		}
	}

	@Test
	@Order(1)
	void serverListShowsTheServer() {
		ServerStatus status = server.status();
		assertEquals("Hello from Craftwright", status.motd().plain());
		assertEquals(0, status.onlinePlayers());
	}

	@Test
	@Order(2)
	void clientJoinsWithItsName() {
		alex.join(server);
		server.awaitLog("Alex joined the game");
		assertEquals("26.3", alex.minecraftVersion());
		assertTrue(alex.hud().tabList().players().contains("Alex"));
		assertEquals(1, server.status().onlinePlayers());
	}

	@Test
	@Order(3)
	void chatGoesBothWays() {
		alex.say("hello from a real client");
		server.awaitLog("hello from a real client");
		server.console("say greetings from the console");
		alex.chat().await("greetings from the console");
	}

	@Test
	@Order(4)
	void titlesAndActionBarArrive() {
		server.console("title Alex actionbar {\"text\":\"Above the hotbar\"}");
		alex.hud().actionBar().await("Above the hotbar");
		server.console("title Alex title {\"text\":\"Welcome\",\"color\":\"gold\"}");
		Text title = alex.hud().titles().await("Welcome");
		assertTrue(title.json().toString().contains("gold"), title.json().toString());
	}

	@Test
	@Order(5)
	void bossBarIsVisible() {
		server.console("bossbar add craftwright:test \"Loading the fun\"");
		server.console("bossbar set craftwright:test players Alex");
		Hud.BossBar bar = alex.hud().awaitBossBar("Loading the fun");
		assertEquals("white", bar.color());
		server.console("bossbar remove craftwright:test");
	}

	@Test
	@Order(6)
	void commandsRunAsThePlayer() {
		server.console("op Alex");
		alex.chat().await("Made Alex a server operator");
		alex.command("gamemode creative");
		alex.player().awaitState("to be in creative mode", state -> state.gameMode().equals("creative"));
	}

	@Test
	@Order(7)
	void clientSeesBlocksAndCanBreakThem() {
		Position position = alex.player().position();
		int x = position.blockX() + 2;
		int y = position.blockY();
		int z = position.blockZ();
		server.console("setblock " + x + " " + y + " " + z + " minecraft:diamond_block");
		alex.player().awaitState("to see the diamond block", state -> alex.world().block(x, y, z).is("diamond_block"));
		alex.player().breakBlock(x, y, z);
		assertTrue(alex.world().block(x, y, z).isAir());
	}

	@Test
	@Order(8)
	void playerWalks() {
		Position start = alex.player().position();
		alex.player().walkTo(start.x() + 4, start.z() + 3);
		Position end = alex.player().position();
		assertTrue(end.horizontalDistanceTo(start.x() + 4, start.z() + 3) < 1.0, "ended at " + end);
		server.console("tp Alex " + start.x() + " " + start.y() + " " + start.z());
		alex.player().awaitPosition(start.x(), start.y(), start.z(), 0.5);
	}

	@Test
	@Order(9)
	void chestMenuOpensAndItemsCanBeTaken() {
		Position position = alex.player().position();
		int x = position.blockX() + 1;
		int y = position.blockY();
		int z = position.blockZ() + 1;
		server.console("setblock " + x + " " + y + " " + z + " minecraft:chest{CustomName:\"Treasure\",Items:[{Slot:4b,id:\"minecraft:diamond\",count:3}]}");
		alex.player().awaitState("to see the chest", state -> alex.world().block(x, y, z).is("chest"));
		alex.player().useBlock(x, y, z);
		Menu menu = alex.awaitMenu("Treasure");
		assertEquals("minecraft:generic_9x3", menu.type());
		assertTrue(menu.slot(4).item().orElseThrow().is("diamond"));
		menu.slot(4).shiftClick();
		alex.player().awaitState("to have the diamonds", state -> alex.player().count("diamond") == 3);
		menu.close();
		alex.awaitMenuClosed();
	}

	@Test
	@Order(10)
	void kickShowsTheReason() {
		server.console("kick Alex Testing the kick screen");
		Text reason = alex.awaitDisconnect();
		assertTrue(reason.contains("Testing the kick screen"), reason.plain());
		assertFalse(alex.isInWorld());
		alex.join(server);
		server.awaitLog("Alex joined the game");
	}
}
