package systems.heartbeat.craftwright.selftest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import systems.heartbeat.craftwright.Block;
import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Entity;
import systems.heartbeat.craftwright.Server;
import systems.heartbeat.craftwright.WorldBorder;
import systems.heartbeat.craftwright.junit.CraftwrightTest;
import systems.heartbeat.craftwright.junit.Player;

/** The world as the client has it: blocks, entities, border, time and weather, and changing blocks by hand. */
@Tag("e2e")
@CraftwrightTest(setup = SelfTestServer.class)
class WorldE2ETest {
	private Server server;
	private Client alex;
	private Stage stage;

	@BeforeEach
	void enter(Server server, @Player("Alex") Client alex) {
		this.server = server;
		this.alex = alex;
		this.stage = Stage.enter(server, alex, 64, 0);
	}

	@Test
	void blockHasItsState() {
		stage.place(2, 0, 0, "minecraft:oak_stairs[facing=east,half=top]");
		Block stairs = alex.world().block(stage.x(2), stage.y(0), stage.z(0));
		assertEquals("minecraft:oak_stairs", stairs.id());
		assertEquals("east", stairs.properties().get("facing"));
		assertEquals("top", stairs.properties().get("half"));
		assertTrue(stairs.blockEntity().isEmpty());
		assertTrue(alex.world().block(stage.x(2), stage.y(3), stage.z(0)).isAir());
	}

	@Test
	void signCarriesItsText() {
		stage.place(0, 0, 2, "minecraft:oak_sign{front_text:{messages:[\"Welcome\",\"to the\",\"self test\",\"\"]}}");
		Block sign = alex.world().awaitBlock(stage.x(0), stage.y(0), stage.z(2), "to carry its text",
			block -> block.blockEntity().filter(data -> data.toString().contains("self test")).isPresent());
		assertTrue(sign.is("oak_sign"));
	}

	@Test
	void playerPlacesAndBreaksABlock() {
		stage.gameMode("creative");
		try {
			stage.place(-2, 0, 0, "minecraft:air");
			stage.give("stone", 1);
			alex.player().selectHotbar(0);

			alex.player().placeAgainst(stage.x(-2), stage.y(-1), stage.z(0), "up");
			alex.world().awaitBlock(stage.x(-2), stage.y(0), stage.z(0), "stone");

			alex.player().breakBlock(stage.x(-2), stage.y(0), stage.z(0));
			alex.world().awaitBlock(stage.x(-2), stage.y(0), stage.z(0), "air");
		} finally {
			stage.gameMode("survival");
		}
	}

	@Test
	void entityHasNameAndEquipment() {
		server.console("summon minecraft:armor_stand " + stage.at(0, 0, -3)
			+ " {CustomName:\"Guard\",CustomNameVisible:1b,NoGravity:1b,equipment:{head:{id:\"minecraft:golden_helmet\",count:1}}}");
		try {
			Entity guard = alex.world().awaitEntity("named Guard with a helmet",
				entity -> entity.customName().filter(name -> name.contains("Guard")).isPresent() && entity.equipment().containsKey("head"));
			assertTrue(guard.is("armor_stand"));
			assertEquals("Guard", guard.displayName().plain());
			assertTrue(guard.equipment().get("head").is("golden_helmet"));
			assertTrue(guard.position().distanceTo(alex.player().position()) < 4.0);
			assertEquals(1, alex.world().entities("armor_stand").size());
		} finally {
			server.console("kill @e[type=minecraft:armor_stand]");
		}
		alex.awaitTrue("the armor stand to be gone", () -> alex.world().entities("armor_stand").isEmpty());
	}

	@Test
	void borderHasCentreAndSize() {
		server.console("worldborder center 100.0 -50.0");
		server.console("worldborder set 300");
		try {
			WorldBorder border = alex.await("the border to be 300 blocks wide",
				() -> java.util.Optional.of(alex.world().border()).filter(seen -> seen.size() == 300.0 && seen.centerX() == 100.0));
			assertEquals(-50.0, border.centerZ());
		} finally {
			server.console("worldborder center 0.0 0.0");
			server.console("worldborder set 59999968");
		}
	}

	@Test
	void timeAndWeatherFollowTheServer() {
		server.console("time set 6000");
		alex.awaitTrue("noon", () -> alex.world().timeOfDay() >= 6000 && alex.world().timeOfDay() < 7000);
		server.console("weather rain");
		try {
			alex.awaitTrue("rain", () -> alex.world().isRaining());
		} finally {
			server.console("weather clear");
		}
		alex.awaitTrue("the rain to stop", () -> !alex.world().isRaining());
	}
}
