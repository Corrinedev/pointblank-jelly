package mod.pbj.mixin;

import mod.pbj.client.ClientEventHandler;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {PostPass.class}, remap = false)
public class PostPassMixin {
	private static String EFFECT_NAME_PREFIX = "pointblank:";

	public PostPassMixin() {}

	@Redirect(
		method = {"process"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EffectInstance;apply()V"))
	private void
	onApplyEffect(EffectInstance effect) {
		String effectName = effect.getName();
		if (effectName != null && effectName.startsWith(EFFECT_NAME_PREFIX)) {
			float progress = (float)ClientEventHandler.getPostPassEffectController().getProgress();
			effect.safeGetUniform("Progress").set(progress);
		}

		effect.apply();
	}
}
