package mod.pbj.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.pbj.client.render.layer.AttachmentLayer;
import mod.pbj.item.AttachmentItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

public class AttachmentModelRenderer
	extends GeoItemRenderer<AttachmentItem> implements RenderPassGeoRenderer<AttachmentItem> {
	public AttachmentModelRenderer(String resourceName) {
		super(new DefaultedItemGeoModel<>(new ResourceLocation("pointblank", resourceName)));
		this.addRenderLayer(new AttachmentLayer<>(this));
	}

	public void renderByItem(
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int packedLight,
		int packedOverlay) {
		try (HierarchicalRenderContext hrc = HierarchicalRenderContext.push(itemStack, itemDisplayContext)) {
			super.renderByItem(itemStack, itemDisplayContext, poseStack, bufferSource, packedLight, packedOverlay);
		}
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
		if (bone.getName().charAt(0) != '_' && !bone.getName().equals("scope") && !bone.getName().equals("scopepip") &&
			!bone.getName().equals("muzzleflash") && !bone.getName().equals("muzzle")) {
			super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		}
	}

	public GeoRenderer<AttachmentItem> getRenderer() {
		return this;
	}

	public boolean isEffectLayer() {
		return false;
	}

	public RenderType getRenderType() {
		return null;
	}

	public boolean isSupportedItemDisplayContext(ItemDisplayContext context) {
		return false;
	}

	public RenderPass getRenderPass() {
		return null;
	}
}
