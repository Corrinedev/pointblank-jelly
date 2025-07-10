package mod.pbj.client.render;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mod.pbj.client.EntityRendererBuilder;
import mod.pbj.entity.ProjectileLike;
import mod.pbj.util.MiscUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ProjectileItemEntityRenderer<T extends Entity & ProjectileLike> extends EntityRenderer<T> {
	private final ItemRenderer itemRenderer;
	private static ProjectileLike currentProjectile;
	private static PoseStack.Pose currentPose;

	public ProjectileItemEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
	}

	static ProjectileLike getCurrentProjectile() {
		return currentProjectile;
	}

	static PoseStack.Pose getCurrentPose() {
		return currentPose;
	}

	public void render(
		T projectile,
		float yRot,
		float partialTick,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int packedLight) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(projectile.getYRot()));
		poseStack.mulPose(Axis.XP.rotationDegrees(180.0F - projectile.getXRot()));
		currentProjectile = projectile;
		currentPose = poseStack.last();
		this.itemRenderer.renderStatic(
			projectile.getItem(),
			ItemDisplayContext.GROUND,
			packedLight,
			OverlayTexture.NO_OVERLAY,
			poseStack,
			bufferSource,
			MiscUtil.getLevel(projectile),
			projectile.getId());
		currentProjectile = null;
		currentPose = null;
		poseStack.popPose();
	}

	public ResourceLocation getTextureLocation(Entity entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}

	public static class Builder<T extends Entity & ProjectileLike>
		implements EntityRendererBuilder<Builder<T>, T, EntityRenderer<T>> {
		public Builder() {}

		public Builder<T> withJsonObject(JsonObject obj) {
			return null;
		}

		public EntityRenderer<T> build(EntityRendererProvider.Context context) {
			return new ProjectileItemEntityRenderer<T>(context);
		}
	}
}
