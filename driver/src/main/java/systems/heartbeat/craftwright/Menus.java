package systems.heartbeat.craftwright;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;
import systems.heartbeat.craftwright.internal.Json;

/**
 * The screens with slots a client has in front of it.
 *
 * <pre>{@code
 * alex.chat().command("kosmetik");
 * Menu menu = alex.menus().await("Kosmetik");
 * menu.slotNamed("Pommes").click();
 * }</pre>
 */
public final class Menus {
	private final Remote remote;

	Menus(Remote remote) {
		this.remote = remote;
	}

	/** The chest, plugin menu or other container that is open right now, if any. */
	public Optional<Menu> current() {
		return Menu.of(remote, remote.call("menu.get"));
	}

	/** Waits for any container to open. */
	public Menu await() {
		return await("", menu -> true, remote.timeout());
	}

	/** Waits for a container whose title contains the given words. */
	public Menu await(String titleWords) {
		return await(" titled \"" + titleWords + "\"", menu -> menu.title().contains(titleWords), remote.timeout());
	}

	public Menu await(Predicate<Menu> condition) {
		return await(" matching the condition", condition, remote.timeout());
	}

	/** Waits until the open container fulfils the condition, e.g. after a click changed its items. */
	public Menu await(Predicate<Menu> condition, Duration timeout) {
		return await(" matching the condition", condition, timeout);
	}

	private Menu await(String description, Predicate<Menu> condition, Duration timeout) {
		return remote.await("a menu" + description + " to be open", timeout, () -> current().filter(condition),
			() -> current().map(menu -> "Open menu: \"" + menu.title().plain() + "\" with " + menu.itemNames()).orElse("No menu is open."));
	}

	/** Waits until no container is open. */
	public void awaitClosed() {
		remote.awaitTrue("the menu to close", () -> current().isEmpty(), null);
	}

	/**
	 * The player's own inventory as a menu, to move items around in it: slots 9 to 35 are the
	 * main inventory, 36 to 44 the hotbar, 5 to 8 the armour and 45 the off hand.
	 */
	public Menu inventory() {
		return Menu.of(remote, remote.call("menu.inventory")).orElseThrow();
	}

	/**
	 * Types into the name field of the anvil that is open right now, like a player would. The
	 * renamed item then shows in the result slot ({@link #ANVIL_RESULT_SLOT}) of the open {@link Menu}.
	 */
	public void rename(String name) {
		remote.call("anvil.rename", Json.object("name", name));
	}

	/** The slot of an anvil menu that holds the renamed item. */
	public static final int ANVIL_RESULT_SLOT = 2;

	/** The trading screen that is open right now. Use a villager first. */
	public Merchant merchant() {
		return Merchant.read(remote);
	}
}
