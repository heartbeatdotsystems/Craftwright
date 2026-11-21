package systems.heartbeat.craftwright.agent;

import net.fabricmc.api.ClientModInitializer;

public final class CraftwrightAgent implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (AgentConfig.enabled()) {
			System.out.println("[Craftwright] agent loaded, will report to " + AgentConfig.CONNECT + " as '" + AgentConfig.CLIENT_ID + "'");
		} else {
			System.out.println("[Craftwright] agent installed but switched off: no -Dcraftwright.connect given");
		}
	}
}
