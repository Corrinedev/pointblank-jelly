//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.pbj.event.RenderHandEvent;
import mod.pbj.item.GunItem;
import mod.pbj.item.ThrowableItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {ItemInHandRenderer.class}, remap = false)
public class ItemInHandRendererMixin {
	public ItemInHandRendererMixin() {}

	@Inject(method = {"renderArmWithItem"}, cancellable = true, at = { @At("HEAD") })
	public void onRenderArmWithItem(
		AbstractClientPlayer player,
		float p_109373_,
		float p_109374_,
		InteractionHand hand,
		float p_109376_,
		ItemStack itemStack,
		float p_109378_,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int packedLight,
		CallbackInfo callbackInfo) {
		if (itemStack.getItem() instanceof GunItem || itemStack.getItem() instanceof ThrowableItem) {
			boolean flag = hand == InteractionHand.MAIN_HAND;
			HumanoidArm humanoidarm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
			boolean isRightHand = humanoidarm == HumanoidArm.RIGHT;
			ItemInHandRenderer itemInHandRenderer = (ItemInHandRenderer)(Object)this;
			poseStack.pushPose();
			itemInHandRenderer.renderItem(
				player,
				itemStack,
				isRightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
				!isRightHand,
				poseStack,
				bufferSource,
				packedLight);
			poseStack.popPose();
			callbackInfo.cancel();
		}
	}

	@Inject(method = {"renderHandsWithItems"}, at = { @At("HEAD") })
	private void onRenderHandsWithItems(
		float p_109315_,
		PoseStack poseStack,
		MultiBufferSource.BufferSource bufferSource,
		LocalPlayer player,
		int p_109319_,
		CallbackInfo callbackInfo) {
		RenderHandEvent event = new RenderHandEvent.Pre(poseStack, null, 0.0F);
		MinecraftForge.EVENT_BUS.post(event);
	}
}
