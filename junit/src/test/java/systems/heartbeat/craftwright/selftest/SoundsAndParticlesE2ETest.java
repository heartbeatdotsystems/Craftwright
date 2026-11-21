package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Particles;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.Sound;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** What a plugin plays and sprays: sounds and particles as the client receives them. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class SoundsAndParticlesE2ETest {
	private Server server;
	private Client alex;
	private Stage stage;

	@BeforeEach
	void enter(Server server, @Player("Alex") Client alex) {
		this.server = server;
		this.alex = alex;
		this.stage = Stage.enter(server, alex, 0, 48);
	}

	@Test
	void soundArrivesWithSourceVolumeAndPitch() {
		server.console("playsound minecraft:entity.player.levelup master Alex " + stage.at(0, 0, 0) + " 0.8 1.5");

		Sound sound = alex.sounds().await("entity.player.levelup");

		assertTrue(sound.is("entity.player.levelup"));
		assertEquals("master", sound.source());
		assertEquals(0.8f, sound.volume(), 0.001f);
		assertEquals(1.5f, sound.pitch(), 0.001f);
		assertTrue(sound.position().distanceTo(alex.player().position()) < 2.0, "played at " + sound.position());
	}

	@Test
	void particlesArriveWithTheirCount() {
		server.console("particle minecraft:flame " + stage.at(0, 2, 0) + " 0 0 0 0 5 force Alex");

		Particles flames = alex.particles().await("minecraft:flame");

		assertEquals(5, flames.count());
		assertEquals(stage.y(2), flames.position().y(), 0.01);
	}

	@Test
	void particlesCarryTheirOptions() {
		server.console("particle minecraft:dust{color:[1.0,0.0,0.0],scale:1.0} " + stage.at(0, 2, 0) + " 0 0 0 0 3 force Alex");

		Particles dust = alex.particles().await(particles -> particles.is("dust"));

		assertEquals(3, dust.count());
		assertTrue(dust.options().toString().contains("color"), dust.options().toString());
	}

	@Test
	void silenceCanBeAsserted() {
		alex.sounds().clear();
		alex.sounds().expectNone("entity.ender_dragon.death", Duration.ofMillis(500));
	}
}
