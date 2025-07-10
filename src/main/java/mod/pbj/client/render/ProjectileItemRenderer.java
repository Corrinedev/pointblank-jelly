package mod.pbj.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mod.pbj.client.effect.Effect;
import mod.pbj.client.effect.EffectRenderContext;
import mod.pbj.client.uv.StaticSpriteUVProvider;
import mod.pbj.entity.ProjectileLike;
import mod.pbj.item.AmmoItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

public class ProjectileItemRenderer extends GeoItemRenderer<AmmoItem> {
	public static final String BONE_NOZZLE = "nozzle";

	public ProjectileItemRenderer(String resourceName) {
		super(new DefaultedItemGeoModel<>(new ResourceLocation("pointblank", resourceName)));
		this.addRenderLayer(new EffectsLayer(this));
	}

	public void renderCubesOfBone(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		if (!"nozzle".equals(bone.getName())) {
			super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		}
	}

	public static class EffectsLayer extends GeoRenderLayer<AmmoItem> {
		public EffectsLayer(GeoRenderer<AmmoItem> entityRendererIn) {
			super(entityRendererIn);
		}

		public void render(
			PoseStack poseStack,
			AmmoItem animatable,
			BakedGeoModel bakedModel,
			RenderType renderType,
			MultiBufferSource bufferSource,
			VertexConsumer buffer,
			float partialTick,
			int packedLight,
			int packedOverlay) {
			ProjectileLike projectile = ProjectileItemEntityRenderer.getCurrentProjectile();
			PoseStack.Pose pose = ProjectileItemEntityRenderer.getCurrentPose();
			if (projectile != null && pose != null) {
				GeoBone nozzleBone = bakedModel.getBone("nozzle").orElse(null);
				if (nozzleBone != null) {
					poseStack.pushPose();
					RenderUtils.translateToPivotPoint(poseStack, nozzleBone);
					poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

					for (Effect attachedEffect : projectile.getActiveAttachedEffects()) {
						EffectRenderContext effectRenderContext =
							(new EffectRenderContext())
								.withPoseStack(poseStack)
								.withProgress(projectile.getProgress(partialTick))
								.withLightColor(packedLight)
								.withSpriteUVProvider(StaticSpriteUVProvider.INSTANCE)
								.withBufferSource(bufferSource);
						attachedEffect.render(effectRenderContext);
					}

					poseStack.popPose();
				}
			}
		}
	}
}
