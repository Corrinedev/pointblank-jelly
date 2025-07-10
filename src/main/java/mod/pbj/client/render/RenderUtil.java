package mod.pbj.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Objects;
import java.util.function.Supplier;
import mod.pbj.Config;
import mod.pbj.client.ClientSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

public class RenderUtil {
	private static final double FOV_NORMAL_DEGREES = 70.0F;
	private static Matrix4f projectionMatrix;
	private static Matrix4f projectionMatrixInverted;
	private static Matrix4f modelViewMatrix;
	private static Matrix4f modelViewMatrixInverted;

	public RenderUtil() {}

	public static void renderQuad(
		PoseStack poseStack,
		GeoQuad quad,
		VertexConsumer buffer,
		float dx,
		float dy,
		float red,
		float green,
		float blue,
		float alpha) {
		Matrix4f poseState = poseStack.last().pose();
		float[][] texUV = new float[][] {
			{1.0F + dx, 0.0F + dy}, {0.0F + dx, 0.0F + dy}, {0.0F + dx, 1.0F + dy}, {1.0F + dx, 1.0F + dy}};

		for (int i = 0; i < 4; ++i) {
			GeoVertex vertex = quad.vertices()[i];
			float u = Mth.clamp(texUV[i][0], 0.0F, 1.0F);
			float v = Mth.clamp(texUV[i][1], 0.0F, 1.0F);
			buffer.vertex(poseState, vertex.position().x, vertex.position().y, vertex.position().z)
				.uv(u, v)
				.color(red, green, blue, alpha)
				.endVertex();
		}
	}

	public static void renderQuadColorTex(
		PoseStack poseStack,
		GeoQuad quad,
		VertexConsumer buffer,
		float dx,
		float dy,
		float red,
		float green,
		float blue,
		float alpha) {
		Matrix4f poseState = poseStack.last().pose();
		float[][] texUV = new float[][] {
			{1.0F + dx, 0.0F + dy}, {0.0F + dx, 0.0F + dy}, {0.0F + dx, 1.0F + dy}, {1.0F + dx, 1.0F + dy}};

		for (int i = 0; i < 4; ++i) {
			GeoVertex vertex = quad.vertices()[i];
			float u = Mth.clamp(texUV[i][0], 0.0F, 1.0F);
			float v = Mth.clamp(texUV[i][1], 0.0F, 1.0F);
			buffer.vertex(poseState, vertex.position().x, vertex.position().y, vertex.position().z)
				.color(red, green, blue, alpha)
				.uv(u, v)
				.overlayCoords(655360)
				.uv2(15728640)
				.normal(1.0F, 1.0F, 1.0F)
				.endVertex();
		}
	}

	public static void renderColoredQuad(
		PoseStack poseStack, GeoQuad quad, VertexConsumer buffer, float red, float green, float blue, float alpha) {
		Matrix4f poseState = poseStack.last().pose();

		for (int i = 0; i < 4; ++i) {
			GeoVertex v = quad.vertices()[i];
			buffer.vertex(poseState, v.position().x, v.position().y, v.position().z)
				.color(red, green, blue, alpha)
				.endVertex();
		}
	}

	public static void blit(
		GuiGraphics guiGraphics,
		ResourceLocation textureResource,
		float posX,
		float posY,
		int zLevel,
		float minU,
		float minV,
		int width,
		int height,
		int actualWidth,
		int actualHeight) {
		blit(
			guiGraphics,
			textureResource,
			posX,
			posX + (float)width,
			posY,
			posY + (float)height,
			zLevel,
			width,
			height,
			minU,
			minV,
			actualWidth,
			actualHeight);
	}

	public static void blit(
		GuiGraphics guiGraphics,
		ResourceLocation textureResource,
		float posXStart,
		float posXEnd,
		float posYStart,
		float posYEnd,
		int zLevel,
		int width,
		int height,
		float minU,
		float minV,
		int actualWidth,
		int actualHeight) {
		blit(
			guiGraphics,
			textureResource,
			posXStart,
			posXEnd,
			posYStart,
			posYEnd,
			(float)zLevel,
			(minU + 0.0F) / (float)actualWidth,
			(minU + (float)width) / (float)actualWidth,
			(minV + 0.0F) / (float)actualHeight,
			(minV + (float)height) / (float)actualHeight);
	}

	public static void blit(
		GuiGraphics guiGraphics,
		ResourceLocation textureResource,
		float posXStart,
		float posXEnd,
		float posYStart,
		float posYEnd,
		float zLevel,
		float minU,
		float maxU,
		float minV,
		float maxV) {
		RenderSystem.setShaderTexture(0, textureResource);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		Matrix4f matrix4f = guiGraphics.pose().last().pose();
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(matrix4f, posXStart, posYStart, zLevel).uv(minU, minV).endVertex();
		bufferbuilder.vertex(matrix4f, posXStart, posYEnd, zLevel).uv(minU, maxV).endVertex();
		bufferbuilder.vertex(matrix4f, posXEnd, posYEnd, zLevel).uv(maxU, maxV).endVertex();
		bufferbuilder.vertex(matrix4f, posXEnd, posYStart, zLevel).uv(maxU, minV).endVertex();
		BufferUploader.drawWithShader(bufferbuilder.end());
	}

	public static void blit(
		GuiGraphics guiGraphics,
		ResourceLocation resourceLocation,
		int startX,
		int endX,
		int startY,
		int endY,
		int zLevel,
		float minU,
		float maxU,
		float minV,
		float maxV,
		float colorR,
		float colorG,
		float colorB,
		float alpha) {
		RenderSystem.setShaderTexture(0, resourceLocation);
		Supplier<ShaderInstance> var17;
		if (Config.customShadersEnabled) {
			ClientSystem var10000 = ClientSystem.getInstance();
			Objects.requireNonNull(var10000);
			var17 = var10000::getTexColorShaderInstance;
		} else {
			var17 = GameRenderer::getPositionTexColorShader;
		}

		RenderSystem.setShader(var17);
		RenderSystem.enableBlend();
		Matrix4f matrix4f = guiGraphics.pose().last().pose();
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		bufferbuilder.vertex(matrix4f, (float)startX, (float)startY, (float)zLevel)
			.uv(minU, minV)
			.color(colorR, colorG, colorB, alpha)
			.endVertex();
		bufferbuilder.vertex(matrix4f, (float)startX, (float)endY, (float)zLevel)
			.uv(minU, maxV)
			.color(colorR, colorG, colorB, alpha)
			.endVertex();
		bufferbuilder.vertex(matrix4f, (float)endX, (float)endY, (float)zLevel)
			.uv(maxU, maxV)
			.color(colorR, colorG, colorB, alpha)
			.endVertex();
		bufferbuilder.vertex(matrix4f, (float)endX, (float)startY, (float)zLevel)
			.uv(maxU, minV)
			.color(colorR, colorG, colorB, alpha)
			.endVertex();
		BufferUploader.drawWithShader(bufferbuilder.end());
		RenderSystem.disableBlend();
	}

	public static Matrix4f getProjectionMatrixInverted() {
		Matrix4f projectionMatrixNew = RenderSystem.getProjectionMatrix();
		if (projectionMatrixNew == null || projectionMatrixNew != projectionMatrix) {
			projectionMatrix = projectionMatrixNew;
			projectionMatrixInverted = (new Matrix4f(projectionMatrixNew)).invert();
		}

		return projectionMatrixInverted;
	}

	public static Matrix4f getModelViewMatrixInverted() {
		Matrix4f modelViewMatrixNew = RenderSystem.getModelViewMatrix();
		if (modelViewMatrixNew == null || modelViewMatrixNew != modelViewMatrix) {
			modelViewMatrix = modelViewMatrixNew;
			modelViewMatrixInverted = (new Matrix4f(modelViewMatrixNew)).invert();
		}

		return modelViewMatrixInverted;
	}

	public static Matrix4f getProjectionMatrixNormalFov() {
		Minecraft mc = Minecraft.getInstance();
		return mc.gameRenderer.getProjectionMatrix(70.0F);
	}
}
