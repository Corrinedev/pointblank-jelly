package mod.pbj.client.controller;

import mod.pbj.client.GunClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PostPassEffectController extends AbstractProceduralAnimationController {
	public PostPassEffectController(long duration) {
		super(duration);
	}

	public void reset() {
		super.reset();
		this.nanoDuration = 300000000L;
		Minecraft mc = Minecraft.getInstance();
		mc.gameRenderer.loadEffect(new ResourceLocation("pointblank", "shaders/post/ripple.json"));
	}

	public void onRenderTick(
		LivingEntity player,
		GunClientState state,
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		float partialTicks) {
		super.onRenderTick(player, state, itemStack, itemDisplayContext, partialTicks);
		if (this.isDone) {
			Minecraft mc = Minecraft.getInstance();
			PostChain postChain = mc.gameRenderer.currentEffect();
			if (postChain != null && postChain.getName().startsWith("pointblank:")) {
				mc.gameRenderer.shutdownEffect();
			}
		}
	}

	public void onGameTick(LivingEntity player, GunClientState gunClientState) {}

	public double getProgress() {
		return super.getProgress(null, 0.0F);
	}
}
