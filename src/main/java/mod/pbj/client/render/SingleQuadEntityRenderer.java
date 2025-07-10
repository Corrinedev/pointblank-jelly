package mod.pbj.client.render;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import mod.pbj.entity.SlowProjectile;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class SingleQuadEntityRenderer extends EntityRenderer<SlowProjectile> {
	private final ResourceLocation texture = new ResourceLocation("pointblank:textures/effect/laser3.png");

	public SingleQuadEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public void render(
		SlowProjectile projectile,
		float p_114657_,
		float partialTick,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int lightColor) {
		this.renderOrig(projectile, p_114657_, partialTick, poseStack, bufferSource, lightColor);
	}

	public void renderOrig(
		SlowProjectile projectile,
		float p_114657_,
		float partialTick,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int lightColor) {
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.getMainCamera();
		poseStack.mulPose(camera.rotation());
		float width = 0.5F;
		float length = 0.0F;
		poseStack.pushPose();
		float u = 1.0F;
		float brightness = 1.0F;
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		RenderSystem.setShaderTexture(0, this.texture);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();
		Matrix4f matrix4f = poseStack.last().pose();
		Vector4f t1 = new Vector4f(width, width, 0.0F, 1.0F);
		matrix4f.transform(t1);
		bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		bufferbuilder.vertex(matrix4f, -width + length, width, 0.0F)
			.uv(0.0F, 0.0F)
			.color(1.0F, 1.0F, 1.0F, brightness)
			.endVertex();
		bufferbuilder.vertex(matrix4f, width, width, 0.0F).uv(u, 0.0F).color(1.0F, 1.0F, 1.0F, brightness).endVertex();
		bufferbuilder.vertex(matrix4f, width, -width, 0.0F).uv(u, 1.0F).color(1.0F, 1.0F, 1.0F, brightness).endVertex();
		bufferbuilder.vertex(matrix4f, -width + length, -width, 0.0F)
			.uv(0.0F, 1.0F)
			.color(1.0F, 1.0F, 1.0F, brightness)
			.endVertex();
		BufferUploader.drawWithShader(bufferbuilder.end());
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		poseStack.popPose();
	}

	public ResourceLocation getTextureLocation(SlowProjectile entity) {
		return this.texture;
	}
}
