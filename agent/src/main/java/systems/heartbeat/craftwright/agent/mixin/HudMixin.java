package systems.heartbeat.craftwright.agent.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import systems.heartbeat.craftwright.agent.Agent;

/** Reports the big texts in the middle of the screen and the line above the hotbar. */
@Mixin(Hud.class)
public abstract class HudMixin {
	@Shadow
	private int titleFadeInTime;
	@Shadow
	private int titleStayTime;
	@Shadow
	private int titleFadeOutTime;

	@Inject(method = "setTitle", at = @At("HEAD"))
	private void craftwright$title(Component text, CallbackInfo callback) {
		Agent.get().observer().title(text, titleFadeInTime, titleStayTime, titleFadeOutTime);
	}

	@Inject(method = "setSubtitle", at = @At("HEAD"))
	private void craftwright$subtitle(Component text, CallbackInfo callback) {
		Agent.get().observer().subtitle(text);
	}

	@Inject(method = "setOverlayMessage", at = @At("HEAD"))
	private void craftwright$actionBar(Component text, boolean animateColor, CallbackInfo callback) {
		Agent.get().observer().actionBar(text);
	}

	@Inject(method = "clearTitles", at = @At("HEAD"))
	private void craftwright$clearTitles(CallbackInfo callback) {
		Agent.get().observer().titlesCleared();
	}
}
