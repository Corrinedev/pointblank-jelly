package mod.pbj.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collection;
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
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

public class ArmorItemRenderer extends GeoArmorRenderer<ArmorItem> implements RenderPassGeoRenderer<ArmorItem> {
	public ArmorInHandRenderer internal;
	public ArmorItemRenderer(ResourceLocation assetSubpath, ArmorInHandRenderer internal) {
		super(new DefaultedItemGeoModel<>(assetSubpath));
		this.internal = internal;
	}

	public boolean approveRendering(String boneName, ItemStack rootStack) {
		if (boneName.charAt(0) == '_')
			return false;

		Collection<Feature> features = ((ArmorItem)rootStack.getItem()).getFeatures();
		ConditionContext conditionContext = new ConditionContext(null, rootStack, currentStack, null, null);

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
	public void defaultRender(
		PoseStack poseStack,
		ArmorItem animatable,
		MultiBufferSource bufferSource,
		@Nullable RenderType renderType,
		@Nullable VertexConsumer buffer,
		float yaw,
		float partialTick,
		int packedLight) {
		try (
			HierarchicalRenderContext hrc =
				HierarchicalRenderContext.push(getCurrentStack(), ItemDisplayContext.FIXED)) {
			poseStack.pushPose();
			Color renderColor = this.getRenderColor(animatable, partialTick, packedLight);
			float red = renderColor.getRedFloat();
			float green = renderColor.getGreenFloat();
			float blue = renderColor.getBlueFloat();
			float alpha = renderColor.getAlphaFloat();
			int packedOverlay = this.getPackedOverlay(animatable, 0.0F, partialTick);
			BakedGeoModel model =
				this.getGeoModel().getBakedModel(this.getGeoModel().getModelResource(animatable, this));
			if (renderType == null) {
				renderType =
					this.getRenderType(animatable, this.getTextureLocation(animatable), bufferSource, partialTick);
			}

			if (buffer == null) {
				buffer = bufferSource.getBuffer(renderType);
			}

			this.preRender(
				poseStack,
				animatable,
				model,
				bufferSource,
				buffer,
				false,
				partialTick,
				packedLight,
				packedOverlay,
				red,
				green,
				blue,
				alpha);
			if (this.firePreRenderEvent(poseStack, model, bufferSource, partialTick, packedLight)) {
				poseStack.pushPose();
				poseStack.scale(-1, -1, 1);
				poseStack.translate(-0.5, -2, -0.5);
				internal.renderByItem(
					currentStack, ItemDisplayContext.HEAD, poseStack, bufferSource, packedLight, packedOverlay);
				poseStack.popPose();
				this.postRender(
					poseStack,
					animatable,
					model,
					bufferSource,
					buffer,
					false,
					partialTick,
					packedLight,
					packedOverlay,
					red,
					green,
					blue,
					alpha);
				this.firePostRenderEvent(poseStack, model, bufferSource, partialTick, packedLight);
			}

			poseStack.popPose();
			this.renderFinal(
				poseStack,
				animatable,
				model,
				bufferSource,
				buffer,
				partialTick,
				packedLight,
				packedOverlay,
				red,
				green,
				blue,
				alpha);
			this.doPostRenderCleanup();
		}
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
}
