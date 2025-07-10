package mod.pbj.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import mod.pbj.attachment.Attachment;
import mod.pbj.attachment.AttachmentCategory;
import mod.pbj.attachment.AttachmentHost;
import mod.pbj.attachment.AttachmentModelInfo;
import mod.pbj.attachment.Attachments;
import mod.pbj.client.GunClientState;
import mod.pbj.client.gui.AttachmentManagerScreen;
import mod.pbj.client.render.HierarchicalRenderContext;
import mod.pbj.client.render.RenderApprover;
import mod.pbj.client.render.RenderPass;
import mod.pbj.client.render.RenderPassGeoRenderer;
import mod.pbj.client.render.RenderPassRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class AttachmentLayer<T extends GeoAnimatable> extends FeaturePassLayer<T> {
	private static final Vector4f COLOR_GREEN = new Vector4f(0.0F, 1.0F, 0.0F, 1.0F);
	private static final Vector4f COLOR_NORMAL = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
	private RenderPassRenderer<T> currentRenderer;
	private final RenderPassGeoRenderer<T> mainRenderer;

	public AttachmentLayer(RenderPassGeoRenderer<T> renderer) {
		super(renderer, null, RenderPass.ATTACHMENTS, ALL_PARTS, true, null);
		this.mainRenderer = renderer;
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
		if (animatable instanceof AttachmentHost) {
			ItemStack rootStack = HierarchicalRenderContext.current().getItemStack();
			this.currentRenderer = this.mainRenderer;
			this.renderPass(() -> {
				this.renderAttachments(
					null,
					null,
					animatable,
					"/",
					rootStack,
					poseStack,
					bufferSource,
					buffer,
					partialTick,
					packedLight,
					packedOverlay);

				for (GeoRenderLayer<T> effectLayer : this.mainRenderer.getRenderLayers()) {
					if (effectLayer != this && effectLayer instanceof RenderPassRenderer renderPassRenderer) {
						if (renderPassRenderer.isEffectLayer()) {
							GunClientState state = GunClientState.getMainHeldState();
							if (state != null) {
								renderPassRenderer.renderPass(() -> {
									this.currentRenderer = renderPassRenderer;
									this.renderAttachments(
										state,
										null,
										animatable,
										"/",
										rootStack,
										poseStack,
										bufferSource,
										buffer,
										partialTick,
										packedLight,
										packedOverlay);
									this.currentRenderer = null;
								});
							}
						}
					}
				}
			});
		}
	}

	public void renderAttachments(
		GunClientState state,
		RenderType renderTypeOverride,
		T gunItem,
		String parentPath,
		ItemStack baseItemStack,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		VertexConsumer buffer,
		float partialTick,
		int packedLight,
		int packedOverlay) {
		Item attachmentItemStacks = baseItemStack.getItem();
		if (attachmentItemStacks instanceof AttachmentHost attachmentHost) {
			for (ItemStack attachmentItemStack : Attachments.getAttachments(baseItemStack)) {
				Item item = attachmentItemStack.getItem();
				if (item instanceof Attachment attachment) {
					AttachmentModelInfo.attachmentInfos.apply(attachmentHost, attachment)
						.ifPresent(
							attachmentInfo
							-> this.renderAttachment(
								state,
								renderTypeOverride,
								gunItem,
								parentPath,
								baseItemStack,
								attachmentInfo.baseModel(),
								attachmentInfo.baseBone(),
								attachmentItemStack,
								attachmentInfo.attachmentModel(),
								attachmentInfo.attachmentBone(),
								poseStack,
								bufferSource,
								buffer,
								partialTick,
								packedLight,
								packedOverlay));
				}
			}
		}
	}

	protected void renderAttachment(
		GunClientState state,
		RenderType renderTypeOverride,
		T gunItem,
		String parentPath,
		ItemStack baseItemStack,
		BakedGeoModel baseModel,
		CoreGeoBone baseBone,
		ItemStack attachmentItemStack,
		BakedGeoModel attachmentModel,
		CoreGeoBone attachmentBone,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		VertexConsumer buffer,
		float partialTick,
		int packedLight,
		int packedOverlay) {
		Item itemDisplayContext = attachmentItemStack.getItem();
		if (itemDisplayContext instanceof Attachment attachment) {
			ItemDisplayContext var28 = HierarchicalRenderContext.current().getItemDisplayContext();

			try (HierarchicalRenderContext hrc = HierarchicalRenderContext.push(attachmentItemStack, var28)) {
				String attachmentPath = parentPath + "/" + attachment.getName();
				Pair<Matrix4f, Matrix4f> basePos =
					AttachmentModelInfo.modelBonePositions
						.apply(new AttachmentModelInfo.ModelBoneKey(baseModel, baseBone.getName(), 1.0F))
						.orElse(null);
				Pair<Matrix4f, Matrix4f> attachmentPos =
					AttachmentModelInfo.modelBonePositions
						.apply(new AttachmentModelInfo.ModelBoneKey(attachmentModel, attachmentBone.getName(), 1.0F))
						.orElse(null);
				if (basePos != null && attachmentPos != null) {
					RenderType attachmentRenderType = null;
					Vector4f color = COLOR_NORMAL;
					if (renderTypeOverride == null) {
						Pair<RenderType, Vector4f> p = this.getRenderType(
							baseItemStack, attachmentItemStack, attachment.getName(), attachmentPath, var28);
						attachmentRenderType = p.getFirst();
						color = p.getSecond();
					} else {
						attachmentRenderType = renderTypeOverride;
						color = COLOR_GREEN;
					}

					if (attachmentRenderType != null) {
						poseStack.pushPose();
						AttachmentModelInfo.preparePoseStackForBoneInHierarchy(poseStack, baseBone);
						poseStack.mulPoseMatrix(basePos.getFirst());
						poseStack.mulPoseMatrix(attachmentPos.getSecond());
						this.currentRenderer.render(
							attachmentModel,
							poseStack,
							bufferSource,
							gunItem,
							attachmentRenderType,
							bufferSource.getBuffer(attachmentRenderType),
							partialTick,
							packedLight,
							OverlayTexture.NO_OVERLAY,
							color.x,
							color.y,
							color.z,
							color.w);
						this.renderAttachments(
							state,
							renderTypeOverride,
							gunItem,
							attachmentPath,
							attachmentItemStack,
							poseStack,
							bufferSource,
							buffer,
							partialTick,
							packedLight,
							packedOverlay);
						poseStack.popPose();
					}
				}
			}
		}
	}

	private Pair<RenderType, Vector4f> getRenderType(
		ItemStack baseItemStack,
		ItemStack attachmentItemStack,
		String attachmentName,
		String attachmentPath,
		ItemDisplayContext itemDisplayContext) {
		RenderType renderType = null;
		ResourceLocation texture = new ResourceLocation("pointblank", "textures/item/" + attachmentName + ".png");
		Vector4f color = null;
		Minecraft mc = Minecraft.getInstance();
		if (itemDisplayContext == ItemDisplayContext.GROUND) {
			Screen screen = mc.screen;
			if (screen instanceof AttachmentManagerScreen ams) {
				Pair<RenderType, Vector4f> p =
					ams.getRenderTypeOverride(baseItemStack, attachmentItemStack, attachmentName, attachmentPath);
				if (p != null) {
					renderType = p.getFirst();
					color = p.getSecond();
				}
			}
		}

		if (renderType == null) {
			renderType = this.currentRenderer.getRenderType();
		}

		if (renderType == null) {
			renderType = RenderType.entityCutoutNoCull(texture);
		}

		if (color == null) {
			color = COLOR_NORMAL;
		}

		return Pair.of(renderType, color);
	}

	public boolean approveRendering(
		RenderPass renderPass,
		String boneName,
		ItemStack rootStack,
		ItemStack currentStack,
		String path,
		ItemDisplayContext itemDisplayContext) {
		if (renderPass == RenderPass.ATTACHMENTS) {
			return true;
		} else {
			HierarchicalRenderContext hrc = HierarchicalRenderContext.current();
			Item category = hrc.getItemStack().getItem();
			if (category instanceof Attachment currentAttachment) {
				AttachmentCategory var13 = currentAttachment.getCategory();
				Pair<String, ItemStack> activeCategoryAttachment = Attachments.getSelectedAttachment(rootStack, var13);
				if (activeCategoryAttachment != null &&
					!Objects.equals(activeCategoryAttachment.getFirst(), hrc.getPath())) {
					return false;
				} else {
					RenderPassRenderer<T> var12 = this.currentRenderer;
					if (var12 instanceof RenderApprover renderApprover) {
						return renderApprover.approveRendering(
							renderPass, boneName, rootStack, currentStack, path, itemDisplayContext);
					} else {
						return true;
					}
				}
			} else {
				return true;
			}
		}
	}

	public RenderType getRenderType() {
		return null;
	}

	public boolean isSupportedItemDisplayContext(ItemDisplayContext context) {
		return context != ItemDisplayContext.GUI;
	}
}
