package systems.heartbeat.craftwright.agent.mixin;

import java.util.UUID;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import systems.heartbeat.craftwright.agent.AgentConfig;

/** Gives the test client the player name the test asked for. Offline accounts only. */
@Mixin(User.class)
public abstract class UserMixin {
	@Inject(method = "getName", at = @At("HEAD"), cancellable = true)
	private void craftwright$name(CallbackInfoReturnable<String> callback) {
		if (AgentConfig.USERNAME != null) {
			callback.setReturnValue(AgentConfig.USERNAME);
		}
	}

	@Inject(method = "getProfileId", at = @At("HEAD"), cancellable = true)
	private void craftwright$profileId(CallbackInfoReturnable<UUID> callback) {
		if (AgentConfig.USERNAME != null) {
			callback.setReturnValue(AgentConfig.OFFLINE_UUID);
		}
	}
}
