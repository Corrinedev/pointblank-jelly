package mod.pbj.compat.iris.mixin;

import mod.pbj.compat.iris.IrisCompat;
import net.irisshaders.iris.shadows.ShadowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ShadowRenderer.class})
public class IrisShadowRendererMixin16 {
	@Inject(method = {"renderShadows"}, at = { @At("HEAD") }, remap = false)
	private void onStartRenderShadows(CallbackInfo callbackInfo) {
		IrisCompat.getInstance().onStartRenderShadows();
	}

	@Inject(method = {"renderShadows"}, at = { @At("TAIL") }, remap = false)
	private void onEndRenderShadows(CallbackInfo callbackInfo) {
		IrisCompat.getInstance().onEndRenderShadows();
	}
}
