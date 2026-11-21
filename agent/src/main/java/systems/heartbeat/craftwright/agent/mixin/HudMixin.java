package systems.heartbeat.craftwright.agent.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import systems.heartbeat.craftwright.agent.Agent;

@Mixin(Hud.class)
public abstract class HudMixin {
	@Inject(method = "setTitle", at = @At("HEAD"))
	private void craftwright$title(Component text, CallbackInfo callback) {
		Agent.get().textEvent("title", text);
	}

	@Inject(method = "setSubtitle", at = @At("HEAD"))
	private void craftwright$subtitle(Component text, CallbackInfo callback) {
		Agent.get().textEvent("subtitle", text);
	}

	@Inject(method = "setOverlayMessage", at = @At("HEAD"))
	private void craftwright$actionBar(Component text, boolean animateColor, CallbackInfo callback) {
		Agent.get().textEvent("actionBar", text);
	}
}
