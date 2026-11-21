package systems.heartbeat.craftwright;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import systems.heartbeat.craftwright.internal.Json;

/**
 * The trading screen of a villager or wandering trader that is open right now.
 * Choose an offer, then take the result from slot {@link #RESULT_SLOT} of the open {@link Menu}.
 */
public final class Merchant {
	/** The slot of the trading menu that holds what the player buys. */
	public static final int RESULT_SLOT = 2;

	private final Remote remote;
	private final int level;
	private final List<Offer> offers;

	private Merchant(Remote remote, JsonObject result) {
		this.remote = remote;
		this.level = result.get("merchantLevel").getAsInt();
		this.offers = Json.list(result, "offers", Offer::of);
	}

	static Merchant read(Remote remote) {
		return new Merchant(remote, remote.call("trade.offers"));
	}

	/** 1 novice to 5 master. */
	public int level() {
		return level;
	}

	public List<Offer> offers() {
		return offers;
	}

	/** The first offer that sells this item. */
	public Optional<Offer> offerFor(String resultItemId) {
		return offers.stream().filter(offer -> offer.result().is(resultItemId)).findFirst();
	}

	/** Clicks an offer in the list. The game moves the price from the inventory into the trade slots. */
	public void select(int offerIndex) {
		remote.call("trade.select", Json.object("index", offerIndex));
	}

	public void select(Offer offer) {
		select(offer.index());
	}
}
