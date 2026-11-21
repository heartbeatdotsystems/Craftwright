package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Effect;
import systems.heartbeat.craftwright.Key;
import systems.heartbeat.craftwright.PlayerState;
import systems.heartbeat.craftwright.Position;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** The player's body: walking, looking, the movement keys, and what the server does to it. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class MovementE2ETest {
	private Server server;
	private Client alex;
	private Stage stage;
	private Position start;

	@BeforeEach
	void enter(Server server, @Player("Alex") Client alex) {
		this.server = server;
		this.alex = alex;
		this.stage = Stage.enter(server, alex, 0, 32);
		this.start = alex.player().position();
	}

	@Test
	void playerWalksToAPoint() {
		alex.player().walkTo(start.x() + 4, start.z() + 3);
		assertTrue(alex.player().position().horizontalDistanceTo(start.x() + 4, start.z() + 3) < 1.0, "ended at " + alex.player().position());
	}

	@Test
	void wallStopsTheWalk() {
		server.console("fill " + stage.at(-1, 0, 2) + " " + stage.at(1, 1, 2) + " minecraft:bedrock");
		try {
			alex.world().awaitBlock(stage.x(0), stage.y(1), stage.z(2), "bedrock");
			assertFalse(alex.player().tryWalkTo(start.x(), start.z() + 5, Duration.ofSeconds(3)));
			assertTrue(alex.player().position().z() < stage.z(2));
		} finally {
			server.console("fill " + stage.at(-1, 0, 2) + " " + stage.at(1, 1, 2) + " minecraft:air");
		}
	}

	@Test
	void heldForwardKeyMovesWhereThePlayerLooks() {
		alex.player().look(0, 0);
		alex.player().hold(Key.FORWARD);
		try {
			alex.player().awaitState("to have walked two blocks south", state -> state.position().z() > start.z() + 2);
		} finally {
			alex.player().stop();
		}
	}

	@Test
	void lookTurnsTheHead() {
		alex.player().look(90, 30);
		PlayerState state = alex.player().state();
		assertEquals(90f, state.yaw(), 0.01f);
		assertEquals(30f, state.pitch(), 0.01f);

		alex.player().lookAt(start.x() - 5, start.y() + 1.62, start.z());
		assertEquals(90f, alex.player().state().yaw(), 1f);
		assertEquals(0f, alex.player().state().pitch(), 1f);
	}

	@Test
	void jumpLiftsThePlayer() {
		alex.player().jump();
		alex.player().awaitState("to be in the air", state -> !state.onGround());
		alex.player().awaitState("to land again", PlayerState::onGround);
	}

	@Test
	void sneakKeyMakesThePlayerSneak() {
		alex.player().hold(Key.SNEAK);
		alex.player().awaitState("to sneak", PlayerState::sneaking);
		alex.player().stop();
		alex.player().awaitState("to stand up", state -> !state.sneaking());
	}

	@Test
	void effectsAndExperienceArrive() {
		server.console("effect give Alex minecraft:speed 60 1");
		server.console("experience set Alex 5 levels");
		try {
			PlayerState state = alex.player().awaitState("to be fast and experienced",
				seen -> seen.hasEffect("speed") && seen.experienceLevel() == 5);
			Effect speed = state.effects().stream().filter(effect -> effect.is("speed")).findFirst().orElseThrow();
			assertEquals(1, speed.amplifier());
			assertTrue(speed.duration() > 0);
		} finally {
			server.console("effect clear Alex");
			server.console("experience set Alex 0 levels");
		}
	}

	@Test
	void playerDiesAndRespawns() {
		server.console("kill Alex");
		alex.player().awaitState("to be dead", PlayerState::dead);
		alex.chat().await("Alex was killed");

		alex.player().respawn();

		PlayerState alive = alex.player().awaitState("to be alive again", state -> !state.dead() && state.health() > 0);
		assertEquals(20f, alive.health(), 0.01f);
	}
}
