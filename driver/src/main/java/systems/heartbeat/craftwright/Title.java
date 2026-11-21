package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;

/**
 * A title in the middle of the screen, with how long it shows.
 *
 * @param fadeIn  ticks
 * @param stay    ticks
 * @param fadeOut ticks
 */
public record Title(Text text, int fadeIn, int stay, int fadeOut) {
	static Title of(JsonObject data) {
		return new Title(Text.of(data.get("text")), data.get("fadeIn").getAsInt(), data.get("stay").getAsInt(), data.get("fadeOut").getAsInt());
	}

	@Override
	public String toString() {
		return text.plain();
	}
}
