package mod.pbj.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import mod.pbj.client.render.GunItemRenderer;
import mod.pbj.client.render.HierarchicalRenderContext;
import mod.pbj.client.render.RenderPass;
import mod.pbj.client.render.RenderTypeProvider;
import mod.pbj.feature.FeatureProvider;
import mod.pbj.feature.ReticleFeature;
import mod.pbj.item.GunItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class ReticleItemLayer extends FeaturePassLayer<GunItem> {
	public static final String HRC_ATTRIBUTE_PARALLAX_ENABLED = "is_parallax_enabled";
	public static final String HRC_ATTRIBUTE_MAX_ANGULAR_OFFSET_COS = "max_angular_offset_cos";

	public ReticleItemLayer(GunItemRenderer renderer) {
		super(renderer, ReticleFeature.class, RenderPass.RETICLE, List.of("scope", "reticle"), true, null);
	}

	public void render(
		BakedGeoModel attachmentModel,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		GunItem animatable,
		RenderType origRenderType,
		VertexConsumer origBuffer,
		float partialTick,
		int packedLight,
		int overlay,
		float red,
		float green,
		float blue,
		float alpha) {
		boolean isParallaxEnabled = false;
		float maxAngularOffsetCos = ReticleFeature.DEFAULT_MAX_ANGULAR_OFFSET_COS;
		RenderType renderType = null;
		ReticleFeature feature = null;
		HierarchicalRenderContext hrc = HierarchicalRenderContext.current();
		if (hrc != null) {
			Item var19 = hrc.getItemStack().getItem();
			if (var19 instanceof FeatureProvider fp) {
				feature = fp.getFeature(ReticleFeature.class);
				if (feature != null && feature.isEnabled(hrc.getItemStack())) {
					isParallaxEnabled = feature.isParallaxEnabled();
					maxAngularOffsetCos = feature.getMaxAngularOffsetCos();
					RenderTypeProvider renderTypeProvider = RenderTypeProvider.getInstance();
					ResourceLocation reticleTexture = feature.getTexture();
					if (feature.hasFunction("getReticleTexture"))
						reticleTexture =
							(ResourceLocation)feature.invokeFunction("getReticleTexture", hrc.getItemStack(), feature);
					renderType = renderTypeProvider.getReticleRenderType(reticleTexture, isParallaxEnabled);
				}
			}
		}

		if (renderType != null) {
			VertexConsumer buffer = bufferSource.getBuffer(renderType);
			RenderTypeProvider renderTypeProvider = RenderTypeProvider.getInstance();
			float reticleBrightness = renderTypeProvider.getReticleBrightness();
			HierarchicalRenderContext subHrc = HierarchicalRenderContext.push();

			try {
				subHrc.setAttribute("is_parallax_enabled", isParallaxEnabled);
				subHrc.setAttribute("max_angular_offset_cos", maxAngularOffsetCos);
				if (feature.hasFunction("renderReticleBefore"))
					feature.invokeFunction(
						"renderReticleBefore",
						feature,
						attachmentModel,
						poseStack,
						bufferSource,
						animatable,
						renderType,
						buffer,
						partialTick,
						packedLight,
						reticleBrightness);
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
					reticleBrightness,
					reticleBrightness,
					reticleBrightness,
					1.0F);
				if (feature.hasFunction("renderReticleAfter"))
					feature.invokeFunction(
						"renderReticleAfter",
						feature,
						attachmentModel,
						poseStack,
						bufferSource,
						animatable,
						renderType,
						buffer,
						partialTick,
						packedLight,
						reticleBrightness);
			} catch (Throwable var25) {
				if (subHrc != null) {
					try {
						subHrc.close();
					} catch (Throwable var24) {
						var25.addSuppressed(var24);
					}
				}

				throw var25;
			}

			if (subHrc != null) {
				subHrc.close();
			}
		}
	}

	public RenderType getRenderType() {
		return RenderTypeProvider.NO_RENDER_TYPE;
	}

	public boolean isSupportedItemDisplayContext(ItemDisplayContext context) {
		return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
			context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
	}

	public static boolean isParallaxEnabled() {
		HierarchicalRenderContext current = HierarchicalRenderContext.current();
		Boolean isParallaxEnabled = (Boolean)current.getAttribute("is_parallax_enabled");
		return isParallaxEnabled != null && isParallaxEnabled;
	}
}
