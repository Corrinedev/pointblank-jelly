package mod.pbj.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;

public interface RenderPassRenderer<T extends GeoAnimatable> extends RenderPassProvider {
	GeoRenderer<T> getRenderer();

	boolean isEffectLayer();

	RenderType getRenderType();

	boolean isSupportedItemDisplayContext(ItemDisplayContext var1);

	default void renderPass(Runnable runnable) {
		ItemDisplayContext itemDisplayContext = HierarchicalRenderContext.current().getItemDisplayContext();
		if (this.isSupportedItemDisplayContext(itemDisplayContext)) {
			RenderPass.push(this.getRenderPass());

			try {
				runnable.run();
			} finally {
				RenderPass.pop();
			}
		}
	}

	default void render(
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
		this.getRenderer().reRender(
			attachmentModel,
			poseStack,
			bufferSource,
			animatable,
			renderType,
			buffer,
			partialTick,
			packedLight,
			overlay,
			red,
			green,
			blue,
			alpha);
	}
}
