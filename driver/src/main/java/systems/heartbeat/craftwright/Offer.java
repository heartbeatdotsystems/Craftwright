package systems.heartbeat.craftwright;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;

/**
 * One trade a merchant offers.
 *
 * @param index      position in the merchant's list, for {@link Merchant#select(int)}
 * @param firstCost  what the player pays, with the current price
 * @param secondCost the second item some trades ask for
 * @param result     what the player gets
 */
public record Offer(int index, Item firstCost, Optional<Item> secondCost, Item result, int uses, int maxUses, boolean outOfStock) {
	static Offer of(JsonElement element) {
		JsonObject offer = element.getAsJsonObject();
		return new Offer(offer.get("index").getAsInt(), Item.of(offer.get("firstCost")).orElseThrow(), Item.of(offer.get("secondCost")),
			Item.of(offer.get("result")).orElseThrow(), offer.get("uses").getAsInt(), offer.get("maxUses").getAsInt(),
			offer.get("outOfStock").getAsBoolean());
	}

	@Override
	public String toString() {
		return firstCost + secondCost.map(second -> " + " + second).orElse("") + " -> " + result;
	}
}
