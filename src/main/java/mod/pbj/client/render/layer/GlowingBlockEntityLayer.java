package mod.pbj.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Predicate;
import mod.pbj.client.model.BaseBlockModel;
import mod.pbj.client.render.BaseModelBlockRenderer;
import mod.pbj.client.render.RenderTypeProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

public class GlowingBlockEntityLayer<T extends BlockEntity & GeoAnimatable> extends BaseModelBlockLayer<T> {
	private final BaseBlockModel<T> model;
	private final ResourceLocation texture;

	public GlowingBlockEntityLayer(BaseModelBlockRenderer<T> renderer) {
		super(renderer);
		this.renderer = renderer;
		this.model = renderer.getModel();
		this.texture = renderer.getTextureLocation(renderer.getAnimatable());
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
		RenderTypeProvider renderTypeProvider = RenderTypeProvider.getInstance();
		RenderType glowRenderType = renderTypeProvider.getGlowBlockEntityRenderType(this.texture);
		super.render(
			poseStack, animatable, bakedModel, glowRenderType, bufferSource, buffer, partialTick, 240, packedOverlay);
	}

	public boolean shouldRender(String boneName, BlockEntity blockEntity) {
		Predicate<BlockEntity> predicate = this.model.getGlowingParts().get(boneName);
		return !this.isRendering || predicate != null && predicate.test(blockEntity);
	}
}
