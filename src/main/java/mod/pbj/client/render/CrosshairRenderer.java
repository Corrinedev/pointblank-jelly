package mod.pbj.client.render;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class CrosshairRenderer {
	public static void renderCrosshairOverlay3(
		GuiGraphics guiGraphics,
		float partialTick,
		ResourceLocation targetLockonOverlay,
		float expansionRatio,
		float posX,
		float posY,
		int renderWidth,
		int renderHeight) {
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		float halfLockWidth = 8.0F;
		float halfLockHeight = 8.0F;
		float centerX = posX + (float)renderWidth * 0.5F;
		float centerY = posY + (float)renderHeight * 0.5F;
		float xOffset = halfLockWidth * 0.4F * expansionRatio;
		float yOffset = halfLockHeight * 0.4F * expansionRatio;
		float ratio = 0.17948718F;
		float uvOffset = (1.0F - ratio) * 0.5F;
		float sq = 30.0F;
		float innerSide = sq * ratio;
		float halfSq = sq * 0.5F;
		float halfInnerSide = innerSide * 0.5F;
		float hairLength = (sq - innerSide) * 0.5F;
		float hairWidth = 0.5F;
		float maxU = 1.0F - uvOffset;
		float maxV = 1.0F - uvOffset;
		float posXStart = centerX - halfInnerSide;
		float posXEnd = centerX + halfInnerSide;
		float posYStart = centerY - halfInnerSide;
		float posYEnd = centerY + halfInnerSide;
		RenderUtil.blit(
			guiGraphics,
			targetLockonOverlay,
			posXStart,
			posXEnd,
			posYStart,
			posYEnd,
			-90.0F,
			uvOffset,
			maxU,
			uvOffset,
			maxV);
		RenderSystem.blendFuncSeparate(
			SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO);
		float minU = 0.0F;
		float minV = 0.0F;
		maxV = 1.0F - minV;
		posXStart = centerX - halfInnerSide - hairLength - xOffset;
		posXEnd = centerX - halfInnerSide + hairWidth - xOffset;
		posYStart = centerY - halfSq;
		posYEnd = centerY + halfSq;
		RenderUtil.blit(
			guiGraphics,
			targetLockonOverlay,
			posXStart,
			posXEnd,
			posYStart,
			posYEnd,
			-90.0F,
			minU,
			uvOffset,
			minV,
			maxV);
		minU = 1.0F - uvOffset;
		maxU = 1.0F;
		minV = 0.0F;
		maxV = 1.0F - minV;
		posXStart = centerX + halfInnerSide - hairWidth + xOffset;
		posXEnd = centerX + halfInnerSide + hairLength - hairWidth + xOffset;
		posYStart = centerY - halfSq;
		posYEnd = centerY + halfSq;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
		minU = 0.0F;
		maxU = 1.0F;
		minV = 1.0F - uvOffset;
		maxV = 1.0F;
		posXStart = centerX - halfSq;
		posXEnd = centerX + halfSq;
		posYStart = centerY + halfInnerSide - hairWidth + yOffset;
		posYEnd = posYStart + hairLength - hairWidth;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
	}

	private void renderCrosshairOverlay(
		GuiGraphics guiGraphics,
		float partialTick,
		ResourceLocation targetLockonOverlay,
		float expansionRatio,
		float posX,
		float posY,
		int renderWidth,
		int renderHeight) {
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.blendFuncSeparate(
			SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO);
		float lockRatio = 0.2F;
		int halfLockWidth = 4;
		int halfLockHeight = 4;
		float centerX = posX + (float)renderWidth * 0.5F;
		float centerY = posY + (float)renderHeight * 0.5F;
		float xOffset = (float)halfLockWidth * 0.4F * expansionRatio;
		float yOffset = (float)halfLockHeight * 0.4F * expansionRatio;
		float minU = 0.0F;
		float minV = 0.0F;
		float maxU = 1.0F;
		float maxV = 0.4F;
		float posXStart = centerX - (float)halfLockWidth;
		float posXEnd = centerX + (float)halfLockWidth;
		float posYStart = centerY - (float)halfLockHeight - yOffset;
		float posYEnd = centerY - yOffset;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
		minU = 0.6F;
		minV = 0.0F;
		maxU = 1.0F;
		maxV = 1.0F;
		posXStart = centerX + xOffset;
		posXEnd = centerX + (float)halfLockWidth + xOffset;
		posYStart = centerY - (float)halfLockHeight;
		posYEnd = centerY + (float)halfLockHeight;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
		minU = 0.0F;
		minV = 0.0F;
		maxU = 1.0F;
		maxV = 0.4F;
		posXStart = centerX - (float)halfLockWidth;
		posXEnd = centerX + (float)halfLockWidth;
		posYStart = centerY + yOffset;
		posYEnd = centerY + (float)halfLockHeight + yOffset;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
		minU = 0.0F;
		minV = 0.0F;
		maxU = 0.4F;
		maxV = 1.0F;
		posXStart = centerX - (float)halfLockWidth - xOffset;
		posXEnd = centerX - xOffset;
		posYStart = centerY - (float)halfLockHeight;
		posYEnd = centerY + (float)halfLockHeight;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
	}

	private void renderCrosshairOverlay2(
		GuiGraphics guiGraphics,
		float partialTick,
		ResourceLocation targetLockonOverlay,
		float expansionRatio,
		float posX,
		float posY,
		int renderWidth,
		int renderHeight) {
		expansionRatio += 0.2F;
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.blendFuncSeparate(
			SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO);
		float halfLockWidth = 5.0F;
		float halfLockHeight = 5.0F;
		float centerX = posX + (float)renderWidth * 0.5F;
		float centerY = posY + (float)renderHeight * 0.5F;
		float xOffset = (float)((int)halfLockWidth) * 0.4F * expansionRatio;
		float yOffset = (float)((int)halfLockHeight) * 0.4F * expansionRatio;
		float posXStart = 0.0F;
		float posXEnd = 0.0F;
		float posYStart = 0.5F;
		float posYEnd = 0.5F;
		float minU = centerX - halfLockWidth - xOffset;
		float minV = centerX - xOffset;
		float maxU = centerY - halfLockHeight - yOffset;
		float maxV = centerY - yOffset;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, minU, minV, maxU, maxV, -90.0F, posXStart, posYStart, posXEnd, posYEnd);
		posXStart = centerX + xOffset;
		posXEnd = centerX + halfLockWidth + xOffset;
		posYStart = centerY - halfLockHeight - yOffset;
		posYEnd = centerY - yOffset;
		minU = 0.5F;
		minV = 0.0F;
		maxU = 1.0F;
		maxV = 0.5F;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
		posXStart = centerX + xOffset;
		posXEnd = centerX + halfLockWidth + xOffset;
		posYStart = centerY + yOffset;
		posYEnd = centerY + halfLockHeight + yOffset;
		minU = 0.5F;
		minV = 0.5F;
		maxU = 1.0F;
		maxV = 1.0F;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
		posXStart = centerX - halfLockWidth - xOffset;
		posXEnd = centerX - xOffset;
		posYStart = centerY + yOffset;
		posYEnd = centerY + halfLockHeight + yOffset;
		minU = 0.0F;
		minV = 0.5F;
		maxU = 0.5F;
		maxV = 1.0F;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
	}
}
