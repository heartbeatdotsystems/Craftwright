package systems.heartbeat.craftwright.agent.mixin;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import systems.heartbeat.craftwright.agent.Agent;

/**
 * Keeps the reason of a kick the moment the packet arrives. The server closes the connection
 * right after it, and a client thread that comes too late only finds a closed connection.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerMixin {
	@Inject(method = "handleDisconnect", at = @At("HEAD"))
	private void craftwright$kicked(ClientboundDisconnectPacket packet, CallbackInfo callback) {
		Agent.get().disconnects().serverSaid(packet.reason());
	}
}
