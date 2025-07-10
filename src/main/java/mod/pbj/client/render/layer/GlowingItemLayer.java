package mod.pbj.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.pbj.client.render.HierarchicalRenderContext;
import mod.pbj.client.render.RenderPass;
import mod.pbj.client.render.RenderTypeProvider;
import mod.pbj.feature.GlowFeature;
import mod.pbj.item.GunItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;

public class GlowingItemLayer<T extends GeoAnimatable> extends FeaturePassLayer<T> {
	public static final String HRC_ATTRIBUTE_GLOW_ENABLED = "isGlowEnabled";
	private RenderType renderType;

	public GlowingItemLayer(GeoRenderer<T> renderer, int effectId, ResourceLocation texture) {
		super(renderer, GlowFeature.class, RenderPass.GLOW, ALL_PARTS, true, effectId);
		RenderTypeProvider renderTypeProvider = RenderTypeProvider.getInstance();
		this.renderType = renderTypeProvider.getGlowRenderType(texture);
	}

	protected RenderType getRenderType(GunItem animatable) {
		return this.renderType;
	}

	public void render(
		BakedGeoModel attachmentModel,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		T animatable,
		RenderType renderType,
		VertexConsumer buffer,
		float partialTick,
		int packedLight,
		int overlay,
		float red,
		float green,
		float blue,
		float alpha) {
		RenderPass.push(this.getRenderPass());

		try {
			RenderPass.setEffectId(this.effectId);
			RenderTypeProvider renderTypeProvider = RenderTypeProvider.getInstance();
			float glowBrightness = renderTypeProvider.getGlowBrightness();
			super.render(
				attachmentModel,
				poseStack,
				bufferSource,
				animatable,
				renderType,
				buffer,
				partialTick,
				packedLight,
				overlay,
				glowBrightness,
				glowBrightness,
				glowBrightness,
				1.0F);
		} finally {
			RenderPass.pop();
		}
	}

	public RenderType getRenderType() {
		return this.renderType;
	}

	public boolean isSupportedItemDisplayContext(ItemDisplayContext context) {
		return true;
	}

	public boolean approveRendering(
		RenderPass renderPass,
		String partName,
		ItemStack rootStack,
		ItemStack currentStack,
		String path,
		ItemDisplayContext itemDisplayContext) {
		return isGlowEnabled()
			? true
			: super.approveRendering(renderPass, partName, rootStack, currentStack, path, itemDisplayContext);
	}

	public static boolean isGlowEnabled() {
		HierarchicalRenderContext current = HierarchicalRenderContext.current();
		Boolean isGlowEnabled = (Boolean)current.getAttribute("isGlowEnabled");
		return isGlowEnabled != null && isGlowEnabled;
	}

	public static void setGlowEnabled(boolean isGlowEnabled) {
		HierarchicalRenderContext current = HierarchicalRenderContext.current();
		current.setAttribute("isGlowEnabled", isGlowEnabled);
	}
}
