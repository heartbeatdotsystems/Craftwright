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

/**
 * The plain API without the JUnit extension: a server and a client are started, meet,
 * part and meet again. What a client can do in the world is tested in the junit module.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LifecycleE2ETest {
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
		assertTrue(alex.isInWorld());
		assertEquals(1, server.status().onlinePlayers());
	}

	@Test
	@Order(3)
	void kickShowsTheReason() {
		// A kick in the first moments after the login can reach the game as a bare "Disconnected":
		// the connection closes before the client has read the reason. A player who chats has arrived.
		alex.chat().say("here I am");
		server.awaitLog("here I am");
		server.console("kick Alex Testing the kick screen");
		Text reason = alex.awaitDisconnect();
		assertTrue(reason.contains("Testing the kick screen"), reason.plain());
		assertFalse(alex.isInWorld());
	}

	@Test
	@Order(4)
	void clientComesBackAfterAKick() {
		alex.join(server);
		assertTrue(alex.disconnectReason().isEmpty(), "a new connection forgets the old kick");
		alex.chat().say("back again");
		server.awaitLog("back again");
	}

	@Test
	@Order(5)
	void clientLeavesOnItsOwn() {
		alex.leave();
		server.awaitLog("Alex left the game");
		assertFalse(alex.isInWorld());
	}

	@Test
	@Order(6)
	void serverRestartsAndTheClientJoinsAgain() {
		server.restart();
		assertTrue(server.isRunning());
		alex.join(server);
		alex.awaitTrue("the server list to count Alex", () -> server.status().onlinePlayers() == 1);
	}
}
