package mod.pbj.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collections;
import mod.pbj.client.render.Flushable;
import mod.pbj.client.render.GunItemRenderer;
import mod.pbj.client.render.HierarchicalRenderContext;
import mod.pbj.client.render.RenderPass;
import mod.pbj.client.render.RenderTypeProvider;
import mod.pbj.feature.PipFeature;
import mod.pbj.item.GunItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class PipItemLayer extends FeaturePassLayer<GunItem> {
	private RenderPass currentRenderPass;

	public PipItemLayer(GunItemRenderer renderer) {
		super(renderer, PipFeature.class, RenderPass.PIP, Collections.singleton("scopepip"), true, null);
		this.currentRenderPass = RenderPass.PIP;
	}

	public RenderType getRenderType() {
		return RenderTypeProvider.NO_RENDER_TYPE;
	}

	public RenderPass getRenderPass() {
		return this.currentRenderPass;
	}

	public void render(
		BakedGeoModel attachmentModel,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		GunItem animatable,
		RenderType renderType,
		VertexConsumer ignoredBuffer,
		float partialTick,
		int packedLight,
		int overlay,
		float red,
		float green,
		float blue,
		float alpha) {
		PipFeature pipFeature = PipFeature.getSelected(HierarchicalRenderContext.getRootItemStack());
		if (pipFeature != null) {
			this.currentRenderPass = RenderPass.PIP;
			ResourceLocation maskTexture = pipFeature.getMaskTexture();
			ResourceLocation overlayTexture = pipFeature.getOverlayTexture();
			boolean isParallaxEnabled = pipFeature.isParallaxEnabled();
			if (maskTexture != null) {
				RenderPass.push(RenderPass.PIP_MASK);
				this.currentRenderPass = RenderPass.PIP_MASK;

				try {
					RenderType maskRenderType = RenderTypeProvider.getInstance().getPipMaskRenderType(maskTexture);
					VertexConsumer maskBuffer = bufferSource.getBuffer(maskRenderType);
					super.render(
						attachmentModel,
						poseStack,
						bufferSource,
						animatable,
						maskRenderType,
						maskBuffer,
						partialTick,
						packedLight,
						overlay,
						red,
						green,
						blue,
						alpha);
					if (bufferSource instanceof Flushable flushable) {
						flushable.flush();
					}
				} finally {
					RenderPass.pop();
					this.currentRenderPass = RenderPass.PIP;
				}
			}

			RenderType pipRenderType = RenderTypeProvider.getInstance().getPipRenderType(maskTexture != null);
			super.render(
				attachmentModel,
				poseStack,
				bufferSource,
				animatable,
				pipRenderType,
				bufferSource.getBuffer(pipRenderType),
				partialTick,
				packedLight,
				overlay,
				red,
				green,
				blue,
				alpha);
			if (bufferSource instanceof Flushable flushable) {
				flushable.flush();
			}

			if (overlayTexture != null) {
				try (HierarchicalRenderContext subHrc = HierarchicalRenderContext.push()) {
					subHrc.setAttribute("is_parallax_enabled", isParallaxEnabled);
					RenderPass.push(RenderPass.PIP_OVERLAY);
					this.currentRenderPass = RenderPass.PIP_OVERLAY;

					try {
						RenderType overlayRenderType = RenderTypeProvider.getInstance().getPipOverlayRenderType(
							overlayTexture, maskTexture != null);
						VertexConsumer overlayBuffer = bufferSource.getBuffer(overlayRenderType);
						super.render(
							attachmentModel,
							poseStack,
							bufferSource,
							animatable,
							overlayRenderType,
							overlayBuffer,
							partialTick,
							packedLight,
							overlay,
							red,
							green,
							blue,
							alpha);
						if (bufferSource instanceof Flushable flushable) {
							flushable.flush();
						}
					} finally {
						RenderPass.pop();
						this.currentRenderPass = RenderPass.PIP;
					}
				}
			}
		}
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
