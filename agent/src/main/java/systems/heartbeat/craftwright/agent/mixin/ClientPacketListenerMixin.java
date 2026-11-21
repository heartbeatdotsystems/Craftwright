package systems.heartbeat.craftwright.agent.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import systems.heartbeat.craftwright.agent.Agent;

/**
 * Reports the sounds and particles the server sent to this player. A headless client plays
 * and draws nothing, so the packet is the one place where both are still visible.
 * The handlers run twice, first on the network thread; only the run on the client thread counts.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleSoundEvent", at = @At("TAIL"))
	private void craftwright$sound(ClientboundSoundPacket packet, CallbackInfo callback) {
		if (Minecraft.getInstance().isSameThread()) {
			Agent.get().observer().sound(packet.getSound(), packet.getSource(), packet.getX(), packet.getY(), packet.getZ(),
				packet.getVolume(), packet.getPitch());
		}
	}

	@Inject(method = "handleSoundEntityEvent", at = @At("TAIL"))
	private void craftwright$entitySound(ClientboundSoundEntityPacket packet, CallbackInfo callback) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread() || minecraft.level == null) {
			return;
		}
		Entity entity = minecraft.level.getEntity(packet.getId());
		if (entity != null) {
			Agent.get().observer().sound(packet.getSound(), packet.getSource(), entity.getX(), entity.getY(), entity.getZ(),
				packet.getVolume(), packet.getPitch());
		}
	}

	@Inject(method = "handleParticleEvent", at = @At("TAIL"))
	private void craftwright$particles(ClientboundLevelParticlesPacket packet, CallbackInfo callback) {
		if (Minecraft.getInstance().isSameThread()) {
			Agent.get().observer().particles(packet.particle(), packet.x(), packet.y(), packet.z(), packet.count());
		}
	}
}
