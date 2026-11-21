package systems.heartbeat.craftwright.agent.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import systems.heartbeat.craftwright.agent.game.Game;
import systems.heartbeat.craftwright.agent.mixin.BossHealthOverlayAccessor;
import systems.heartbeat.craftwright.agent.mixin.PlayerTabOverlayAccessor;
import systems.heartbeat.craftwright.agent.protocol.Routes;
import systems.heartbeat.craftwright.agent.wire.Ids;
import systems.heartbeat.craftwright.agent.wire.TextWire;

/** What sits on the screen while playing: boss bars and the player list. Titles and the action bar arrive as events. */
public final class HudFeature implements Feature {
	@Override
	public void register(Routes routes) {
		routes.on("hud.bossBars", (minecraft, params) -> bossBars(minecraft));
		routes.on("hud.tabList", (minecraft, params) -> tabList(minecraft));
	}

	private JsonElement bossBars(Minecraft minecraft) {
		JsonArray bars = new JsonArray();
		for (LerpingBossEvent event : ((BossHealthOverlayAccessor) minecraft.gui.hud.getBossOverlay()).craftwright$events().values()) {
			JsonObject bar = new JsonObject();
			bar.add("name", TextWire.of(event.getName()));
			bar.addProperty("progress", event.getProgress());
			bar.addProperty("color", Ids.of(event.getColor()));
			bar.addProperty("overlay", Ids.of(event.getOverlay()));
			bars.add(bar);
		}
		JsonObject result = new JsonObject();
		result.add("bossBars", bars);
		return result;
	}

	private JsonElement tabList(Minecraft minecraft) {
		PlayerTabOverlay overlay = minecraft.gui.hud.getTabList();
		JsonArray entries = new JsonArray();
		for (PlayerInfo info : Game.connection(minecraft).getListedOnlinePlayers()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("name", info.getProfile().name());
			entry.add("displayName", TextWire.of(overlay.getNameForDisplay(info)));
			entry.addProperty("gameMode", info.getGameMode().getName());
			entry.addProperty("latency", info.getLatency());
			entries.add(entry);
		}
		PlayerTabOverlayAccessor texts = (PlayerTabOverlayAccessor) overlay;
		JsonObject result = new JsonObject();
		result.add("header", TextWire.of(texts.craftwright$header()));
		result.add("footer", TextWire.of(texts.craftwright$footer()));
		result.add("entries", entries);
		return result;
	}
}
