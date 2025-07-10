package mod.pbj.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.pbj.client.render.BaseModelBlockRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public abstract class BaseModelBlockLayer<T extends BlockEntity & GeoAnimatable> extends GeoRenderLayer<T> {
	protected BaseModelBlockRenderer<T> renderer;
	protected boolean isRendering;

	public BaseModelBlockLayer(BaseModelBlockRenderer<T> renderer) {
		super(renderer);
		this.renderer = renderer;
	}

	public void render(
		PoseStack poseStack,
		T animatable,
		BakedGeoModel bakedModel,
		RenderType renderType,
		MultiBufferSource bufferSource,
		VertexConsumer buffer,
		float partialTick,
		int packedLight,
		int packedOverlay) {
		this.isRendering = true;

		try {
			this.renderer.reRender(
				bakedModel,
				poseStack,
				bufferSource,
				animatable,
				renderType,
				bufferSource.getBuffer(renderType),
				partialTick,
				packedLight,
				OverlayTexture.NO_OVERLAY,
				1.0F,
				1.0F,
				1.0F,
				1.0F);
		} finally {
			this.isRendering = false;
		}
	}

	public abstract boolean shouldRender(String var1, BlockEntity var2);
}
