//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.mixin;

import mod.pbj.client.ClientSystem;
import mod.pbj.client.GunClientState;
import mod.pbj.client.render.AuxLevelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {GameRenderer.class}, remap = false)
public class GameRendererMixin {
	public GameRendererMixin() {}

	@Inject(
		method = {"render"},
		at =
		{
			@At(value = "INVOKE",
				target =
					"Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V")
		})
	public void
	render(float partialTick, long time, boolean p_109096_, CallbackInfo callbackInfo) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.getCameraType().isFirstPerson()) {
			GunClientState state = GunClientState.getMainHeldState();
			if (state != null) {
				ClientSystem.getInstance().renderAux(state, partialTick, time);
			}
		}
	}

	@ModifyArg(
		method = {"renderLevel"},
		at =
			@At(value = "INVOKE",
				target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix (D)Lorg/joml/Matrix4f;",
				ordinal = 0),
		index = 0)
	private double
	modifyFov(double fov) {
		AuxLevelRenderer auxRenderer = ClientSystem.getInstance().getAuxLevelRenderer();
		if (auxRenderer.isRenderingPip()) {
			fov = auxRenderer.getFov();
		}

		return fov;
	}

	@ModifyArg(
		method = {"renderLevel"},
		at =
			@At(value = "INVOKE",
				target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix (D)Lorg/joml/Matrix4f;",
				ordinal = 1),
		index = 0)
	private double
	modifyCullFrustrumFov(double fov) {
		AuxLevelRenderer auxRenderer = ClientSystem.getInstance().getAuxLevelRenderer();
		if (auxRenderer.isRenderingPip()) {
			fov = auxRenderer.getCullFrustrumFov();
		}

		return fov;
	}
}
