package mod.pbj.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collection;
import java.util.List;
import mod.pbj.client.controller.GlowAnimationController;
import mod.pbj.client.render.layer.AttachmentLayer;
import mod.pbj.client.render.layer.GlowingItemLayer;
import mod.pbj.feature.ConditionContext;
import mod.pbj.feature.Feature;
import mod.pbj.feature.PartVisibilityFeature;
import mod.pbj.feature.SkinFeature;
import mod.pbj.item.ArmorItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

public class ArmorInHandRenderer extends GeoItemRenderer<ArmorItem> implements RenderPassGeoRenderer<ArmorItem> {
	public ArmorInHandRenderer(ResourceLocation assetPath, List<GlowAnimationController.Builder> glowEffectBuilders) {
		super(new DefaultedItemGeoModel<>(assetPath));
		for (GlowAnimationController.Builder glowEffectBuilder : glowEffectBuilders) {
			ResourceLocation glowTexture = glowEffectBuilder.getTexture();
			if (glowTexture == null)
				glowTexture = this.getGeoModel().getTextureResource(this.animatable);

			this.addRenderLayer(new GlowingItemLayer<>(this, glowEffectBuilder.getEffectId(), glowTexture));
		}
		this.addRenderLayer(new AttachmentLayer<>(this));
	}

	@Override
	public void renderByItem(
		ItemStack stack,
		ItemDisplayContext transformType,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int packedLight,
		int packedOverlay) {
		try (HierarchicalRenderContext hrc = HierarchicalRenderContext.push(stack, transformType)) {
			super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
		}
	}

	@Override
	public void renderRecursively(
		PoseStack poseStack,
		ArmorItem animatable,
		GeoBone bone,
		RenderType renderType,
		MultiBufferSource bufferSource,
		VertexConsumer buffer,
		boolean isReRender,
		float partialTick,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		if (approveRendering(bone.getName(), this.currentItemStack))
			super.renderRecursively(
				poseStack,
				animatable,
				bone,
				renderType,
				bufferSource,
				buffer,
				isReRender,
				partialTick,
				packedLight,
				packedOverlay,
				red,
				green,
				blue,
				alpha);
	}

	public boolean approveRendering(String boneName, ItemStack rootStack) {
		Collection<Feature> features = ((ArmorItem)rootStack.getItem()).getFeatures();
		ConditionContext conditionContext = new ConditionContext(null, rootStack, rootStack, null, null);

		for (Feature feature : features) {
			if (feature instanceof PartVisibilityFeature visibilityFeature) {
				if (visibilityFeature.isEnabled(rootStack)) {
					if (!visibilityFeature.isPartVisible(rootStack.getItem(), boneName, conditionContext)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	@Override
	public GeoRenderer<ArmorItem> getRenderer() {
		return this;
	}

	@Override
	public boolean isEffectLayer() {
		return false;
	}

	@Override
	public RenderType getRenderType() {
		return null;
	}

	@Override
	public boolean isSupportedItemDisplayContext(ItemDisplayContext var1) {
		return false;
	}

	@Override
	public RenderPass getRenderPass() {
		return RenderPass.MAIN_ITEM;
	}

	public ResourceLocation getTextureLocation(ArmorItem animatable) {
		ResourceLocation texture = null;
		HierarchicalRenderContext hrc = HierarchicalRenderContext.getRoot();
		if (hrc != null) {
			ItemStack itemStack = hrc.getItemStack();
			texture = SkinFeature.getTexture(itemStack);
		}

		if (texture == null) {
			texture = super.getTextureLocation(animatable);
		}
		return texture;
	}
}
