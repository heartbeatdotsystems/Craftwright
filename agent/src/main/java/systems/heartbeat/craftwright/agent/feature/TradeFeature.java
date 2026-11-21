package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.protocol.Params;
import systems.heartbeat.craftwright.agent.protocol.RequestException;
import systems.heartbeat.craftwright.agent.protocol.Routes;
import systems.heartbeat.craftwright.agent.wire.ItemWire;

/** Trading with villagers and other merchants: reading the offers and choosing one. */
public final class TradeFeature implements Feature {
	@Override
	public void register(Routes routes) {
		routes.on("trade.offers", (minecraft, params) -> offers(minecraft));
		routes.on("trade.select", this::select);
	}

	private JsonElement offers(Minecraft minecraft) {
		MerchantMenu menu = merchant(minecraft);
		JsonArray offers = new JsonArray();
		MerchantOffers list = menu.getOffers();
		for (int index = 0; index < list.size(); index++) {
			MerchantOffer offer = list.get(index);
			JsonObject wire = new JsonObject();
			wire.addProperty("index", index);
			wire.add("firstCost", ItemWire.of(offer.getCostA()));
			wire.add("secondCost", ItemWire.of(offer.getCostB()));
			wire.add("result", ItemWire.of(offer.getResult()));
			wire.addProperty("uses", offer.getUses());
			wire.addProperty("maxUses", offer.getMaxUses());
			wire.addProperty("outOfStock", offer.isOutOfStock());
			offers.add(wire);
		}
		JsonObject result = new JsonObject();
		result.addProperty("merchantLevel", menu.getTraderLevel());
		result.add("offers", offers);
		return result;
	}

	/** Like a click on the offer in the list: the game fills the price slots from the inventory. */
	private JsonElement select(Minecraft minecraft, Params params) {
		MerchantMenu menu = merchant(minecraft);
		int index = params.integer("index");
		if (index < 0 || index >= menu.getOffers().size()) {
			throw new RequestException("This merchant has " + menu.getOffers().size() + " offers, there is no offer " + index);
		}
		menu.setSelectionHint(index);
		menu.tryMoveItems(index);
		Game.connection(minecraft).send(new ServerboundSelectTradePacket(index));
		return Feature.done();
	}

	private static MerchantMenu merchant(Minecraft minecraft) {
		if (Game.player(minecraft).containerMenu instanceof MerchantMenu menu) {
			return menu;
		}
		throw new RequestException("No trading menu is open. Use the villager first.");
	}
}
