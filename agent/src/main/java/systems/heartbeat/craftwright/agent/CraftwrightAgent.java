package systems.heartbeat.craftwright.agent;

import net.fabricmc.api.ClientModInitializer;

public final class CraftwrightAgent implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		System.out.println("[Craftwright] agent loaded");
	}
}
