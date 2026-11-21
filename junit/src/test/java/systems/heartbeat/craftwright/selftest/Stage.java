package systems.heartbeat.craftwright.selftest;

import systems.heartbeat.craftwright.Client;
import systems.heartbeat.craftwright.Menu;
import systems.heartbeat.craftwright.Server;

/**
 * A spot of the shared flat world that belongs to one test class. Entering it gives every test
 * the same start: the player stands at the centre with empty hands, no screen open and nothing lying around.
 * Blocks are addressed relative to the centre, so tests never write coordinates.
 */
final class Stage {
	/** The height a player stands at in the default flat world. */
	private static final int FLOOR = -60;

	private final Server server;
	private final Client client;
	private final int x;
	private final int z;

	private Stage(Server server, Client client, int x, int z) {
		this.server = server;
		this.client = client;
		this.x = x;
		this.z = z;
	}

	static Stage enter(Server server, Client client, int x, int z) {
		Stage stage = new Stage(server, client, x, z);
		client.menus().current().ifPresent(Menu::close);
		server.console("clear " + client.name());
		server.console("kill @e[type=minecraft:item]");
		server.console("tp " + client.name() + " " + (x + 0.5) + " " + FLOOR + " " + (z + 0.5));
		client.player().awaitPosition(x + 0.5, FLOOR, z + 0.5, 0.3);
		return stage;
	}

	int x(int east) {
		return x + east;
	}

	/** The height {@code up} blocks above the floor the player stands on. */
	int y(int up) {
		return FLOOR + up;
	}

	int z(int south) {
		return z + south;
	}

	/** Coordinates for a console command, e.g. {@code "setblock " + stage.at(1, 0, 0) + " stone"}. */
	String at(int east, int up, int south) {
		return x(east) + " " + y(up) + " " + z(south);
	}

	/** Lets the server place a block and waits until the client sees it. */
	void place(int east, int up, int south, String block) {
		server.console("setblock " + at(east, up, south) + " " + block);
		String id = block.replaceAll("[\\[{].*", "");
		client.world().awaitBlock(x(east), y(up), z(south), id);
	}

	/** Puts the player into a game mode and waits until the client knows. */
	void gameMode(String mode) {
		server.console("gamemode " + mode + " " + client.name());
		client.player().awaitState("to be in " + mode + " mode", state -> state.gameMode().equals(mode));
	}

	/** Puts items into the player's inventory and waits until the client has them. */
	void give(String item, int count) {
		int before = client.player().count(item);
		server.console("give " + client.name() + " " + item + " " + count);
		client.player().awaitCount(item, before + count);
	}
}
