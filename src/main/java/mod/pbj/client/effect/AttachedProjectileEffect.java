package mod.pbj.client.effect;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import mod.pbj.client.ClientSystem;
import mod.pbj.client.PoseProvider;
import mod.pbj.client.SegmentsProviders;
import mod.pbj.client.VertexConsumers;
import mod.pbj.client.particle.EffectParticles;
import mod.pbj.client.render.RenderTypeProvider;
import mod.pbj.client.render.RenderUtil;
import mod.pbj.client.uv.SpriteUVProvider;
import mod.pbj.item.GunItem;
import mod.pbj.item.GunItem.FirePhase;
import mod.pbj.util.Interpolators;
import mod.pbj.util.JsonUtil;
import mod.pbj.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class AttachedProjectileEffect extends AbstractEffect {
	private static final Random random = new Random();
	private PoseProvider poseProvider;
	private Interpolators.FloatProvider distanceProvider;
	private int numBlades;
	private boolean isFaceEnabled;
	private float randomization;
	private final double randomPitch;
	private final double randomYaw;
	private int bladeBrightness;
	private int faceBrightness;
	private float bladeTextureMinU;
	private float bladeTextureMaxU;
	private float faceTextureMinU;
	private float faceTextureMaxU;
	private SegmentsProviders.SegmentsProvider segmentsProvider;
	private Interpolators.FloatInterpolator faceWidthProvider;
	private Interpolators.FloatInterpolator bladeWidthProvider;
	private Function<VertexConsumer, VertexConsumer> vertexConsumerTransformer;
	private RenderType renderType;

	private AttachedProjectileEffect() {
		this.randomPitch = (random.nextDouble() - (double)0.5F) * (double)2.0F;
		this.randomYaw = (random.nextDouble() - (double)0.5F) * (double)2.0F;
	}

	public boolean hasInfiniteBounds() {
		return true;
	}

	private VertexConsumer getVertextBuffer(EffectRenderContext effectRenderContext) {
		VertexConsumer vertexBuffer = effectRenderContext.getVertexBuffer();
		if (vertexBuffer == null) {
			MultiBufferSource bufferSource = effectRenderContext.getBufferSource();
			if (this.renderType == null) {
				this.renderType = RenderTypeProvider.getInstance().getMuzzleFlashRenderType(this.texture);
			}

			vertexBuffer = bufferSource.getBuffer(this.renderType);
			vertexBuffer = VertexConsumers.ENTITY.apply(vertexBuffer);
		}

		return vertexBuffer;
	}

	public void render(EffectRenderContext effectRenderContext) {
		if (!ClientSystem.getInstance().getAuxLevelRenderer().isRendering()) {
			PoseStack poseStack = effectRenderContext.getPoseStack();
			if (poseStack != null) {
				this.renderWithPose(effectRenderContext);
			} else {
				this.renderWithoutPose(effectRenderContext);
			}
		}
	}

	private void renderWithoutPose(EffectRenderContext effectRenderContext) {
		float progress = effectRenderContext.getProgress();
		if (!(progress < 0.0F)) {
			if (this.poseProvider != null) {
				PoseStack poseStack = new PoseStack();
				poseStack.pushPose();
				this.adjustPose(poseStack);
				this.render(
					effectRenderContext,
					poseStack,
					this.getVertextBuffer(effectRenderContext),
					progress,
					effectRenderContext.getLightColor());
				poseStack.popPose();
			}
		}
	}

	private void renderWithPose(EffectRenderContext effectRenderContext) {
		float progress = effectRenderContext.getProgress();
		if (!(progress < 0.0F)) {
			this.render(
				effectRenderContext,
				effectRenderContext.getPoseStack(),
				this.getVertextBuffer(effectRenderContext),
				progress,
				effectRenderContext.getLightColor());
		}
	}

	private void render(
		EffectRenderContext effectRenderContext,
		PoseStack poseStack,
		VertexConsumer vertexBuffer,
		float progress,
		int lightColor) {
		float numRotations = 0.0F;
		float distance = this.distanceProvider.getValue();
		float[][] beamSegments = this.segmentsProvider.getSegments(distance, progress, this.lifetimeNanos);
		float distanceSoFar = beamSegments[0][0] + (beamSegments[0][1] - beamSegments[0][0]) * 0.5F;
		if (!(distanceSoFar >= distance)) {
			vertexBuffer = this.vertexConsumerTransformer.apply(vertexBuffer);
			float roll = -(effectRenderContext.getInitialAngle() + numRotations * 360.0F * progress);
			if (this.isGlowEnabled) {
				lightColor = 240;
			}

			SpriteUVProvider spriteUVProvider = effectRenderContext.getSpriteUVProvider();
			this.renderBladesWithRotations(
				poseStack, beamSegments, roll, vertexBuffer, progress, lightColor, spriteUVProvider);
			this.renderFaceWithRotations(
				poseStack, beamSegments, roll, vertexBuffer, progress, lightColor, spriteUVProvider);
		}
	}

	private boolean adjustPose(PoseStack poseStack) {
		PoseStack.Pose currentItemPose = this.poseProvider.getPose();
		if (currentItemPose != null) {
			poseStack.mulPose(Axis.XP.rotationDegrees((float)this.randomPitch * this.randomization));
			poseStack.mulPose(Axis.YP.rotationDegrees((float)this.randomYaw * this.randomization));
			Matrix4f modelMatrixAdjusted = (new Matrix4f())
											   .mul(new Matrix4f(RenderUtil.getModelViewMatrixInverted()))
											   .mul(new Matrix4f(RenderUtil.getProjectionMatrixInverted()))
											   .mul(RenderUtil.getProjectionMatrixNormalFov())
											   .mul(currentItemPose.pose());
			poseStack.mulPoseMatrix(modelMatrixAdjusted);
			return true;
		} else {
			return false;
		}
	}

	private void renderFaceWithRotations(
		PoseStack poseStack,
		float[][] beamSegments,
		float rot,
		VertexConsumer vertexBuffer,
		float progress,
		int lightColor,
		SpriteUVProvider spriteUVProvider) {
		poseStack.pushPose();
		Quaternionf yQuaternion = Axis.YP.rotationDegrees(0.0F);
		poseStack.mulPose(yQuaternion);
		Quaternionf rollQ = Axis.XP.rotationDegrees(0.0F);
		poseStack.mulPose(rollQ);
		Quaternionf zQuaternion = Axis.ZP.rotationDegrees(-rot);
		poseStack.mulPose(zQuaternion);
		Matrix4f poseState = poseStack.last().pose();
		if (this.isFaceEnabled) {
			float faceZOffset = beamSegments[0][0] + (beamSegments[0][1] - beamSegments[0][0]) * 0.5F;
			this.renderFace(poseState, faceZOffset, vertexBuffer, progress, lightColor, spriteUVProvider);
		}

		poseStack.popPose();
	}

	private void renderBladesWithRotations(
		PoseStack poseStack,
		float[][] beamSegments,
		float rot,
		VertexConsumer vertexBuffer,
		float progress,
		int lightColor,
		SpriteUVProvider spriteUVProvider) {
		float bladeStep = 180.0F / (float)this.numBlades;

		for (int k = 0; k < this.numBlades; ++k) {
			poseStack.pushPose();
			Quaternionf yQuaternion = Axis.YP.rotationDegrees(90.0F);
			poseStack.mulPose(yQuaternion);
			float xRot = (float)k * bladeStep + rot;
			Quaternionf rollQ = Axis.XP.rotationDegrees(xRot);
			poseStack.mulPose(rollQ);
			Quaternionf zQuaternion = Axis.ZP.rotationDegrees(0.0F);
			poseStack.mulPose(zQuaternion);
			Matrix4f poseState = poseStack.last().pose();
			this.renderBlade(poseState, beamSegments, vertexBuffer, progress, lightColor, spriteUVProvider);
			poseStack.popPose();
		}
	}

	private void renderFace(
		Matrix4f transform,
		float zOffset,
		VertexConsumer vertexBuffer,
		float progress,
		int lightColor,
		SpriteUVProvider spriteUVProvider) {
		float[] uv = spriteUVProvider.getSpriteUV(progress);
		if (uv != null) {
			float offset = 0.5F * this.faceWidthProvider.getValue(progress);
			float dynamicAlpha = this.alphaProvider.getValue(progress);
			float spriteWidth = uv[2] - uv[0];
			float minU = uv[0] + spriteWidth * this.faceTextureMinU;
			float minV = uv[1];
			float maxU = uv[0] + spriteWidth * this.faceTextureMaxU;
			float maxV = uv[3];
			float[][] texUV = new float[][] {{maxU, maxV}, {minU, maxV}, {minU, minV}, {maxU, minV}};
			float[][] positionOffsets = new float[][] {
				{offset, -offset, -zOffset},
				{-offset, -offset, -zOffset},
				{-offset, offset, -zOffset},
				{offset, offset, -zOffset}};

			for (int k = 0; k < this.faceBrightness; ++k) {
				for (int i = 0; i < 4; ++i) {
					vertexBuffer.vertex(transform, positionOffsets[i][0], positionOffsets[i][1], positionOffsets[i][2])
						.uv(texUV[i][0], texUV[i][1])
						.color(1.0F, 1.0F, 1.0F, dynamicAlpha)
						.uv2(lightColor)
						.endVertex();
				}
			}
		}
	}

	private void renderBlade(
		Matrix4f transform,
		float[][] beamSegments,
		VertexConsumer vertexBuffer,
		float progress,
		int lightColor,
		SpriteUVProvider spriteUVProvider) {
		float beamSegmentWidth = 0.5F * this.bladeWidthProvider.getValue(progress);
		float dynamicAlpha = this.alphaProvider.getValue(progress);
		int numBeamSpriteSegments = beamSegments.length;
		float[] uv = spriteUVProvider.getSpriteUV(progress * (float)this.lifetimeNanos);
		float spriteWidth = uv[2] - uv[0];
		float spriteSegmentWidth =
			spriteWidth * (this.bladeTextureMaxU - this.bladeTextureMinU) / (float)numBeamSpriteSegments;

		for (int beamSegmentIndex = 0; beamSegmentIndex < numBeamSpriteSegments; ++beamSegmentIndex) {
			float minU = uv[0] + spriteWidth * this.bladeTextureMinU + spriteSegmentWidth * (float)beamSegmentIndex;
			float minV = uv[1];
			float maxU = minU + spriteSegmentWidth;
			float maxV = uv[3];
			float[][] texUV = new float[][] {{maxU, maxV}, {minU, maxV}, {minU, minV}, {maxU, minV}};
			float beamSegmentStart = beamSegments[beamSegmentIndex][0];
			float beamSegmentEnd = beamSegments[beamSegmentIndex][1];
			float[][] positionOffsets = new float[][] {
				{beamSegmentEnd, beamSegmentWidth, 0.0F},
				{beamSegmentStart, beamSegmentWidth, 0.0F},
				{beamSegmentStart, -beamSegmentWidth, 0.0F},
				{beamSegmentEnd, -beamSegmentWidth, 0.0F}};

			for (int k = 0; k < this.bladeBrightness; ++k) {
				for (int i = 0; i < 4; ++i) {
					vertexBuffer.vertex(transform, positionOffsets[i][0], positionOffsets[i][1], positionOffsets[i][2])
						.uv(texUV[i][0], texUV[i][1])
						.color(1.0F, 1.0F, 1.0F, dynamicAlpha)
						.uv2(lightColor)
						.endVertex();
				}
			}
		}
	}

	public void launch(Entity player) {
		EffectParticles.EffectParticle particle = new EffectParticles.EffectParticle(player, this);
		Minecraft mc = Minecraft.getInstance();
		mc.particleEngine.add(particle);
	}

	public static class Builder extends AbstractEffect.AbstractEffectBuilder<Builder, AttachedProjectileEffect> {
		private static final Set<GunItem.FirePhase> COMPATIBLE_PHASES;
		public static final long DEFAULT_DURATION = 250L;
		public static final float MAX_DISTANCE = 200.0F;
		public static final int DEFAULT_NUM_BLADES = 0;
		public static final int DEFAULT_NUM_SPRITES = 1;
		public static final int DEFAULT_FACE_BRIGHTNESS = 2;
		public static final int DEFAULT_BLADE_BRIGHTNESS = 2;
		public static final int MAX_SPRITES = 200;
		public static final int MAX_BRIGHTNESS = 5;
		public static final int DEFAULT_SHOTS_PER_TRACE = 1;
		public static final SegmentsProviders.SegmentsProvider DEFAULT_SEGMENTS_PROVIDER;
		private Interpolators.FloatInterpolator bladeWidthProvider;
		private Interpolators.FloatInterpolator faceWidthProvider;
		private Interpolators.FloatProvider bladeLengthProvider;
		private int bladeBrightness;
		private int faceBrightness;
		private SegmentsProviders.SegmentsProvider segmentsProvider;
		private int numBlades;
		private boolean isFaceEnabled;
		private float faceTextureMinU;
		private float faceTextureMaxU;
		private float bladeTextureMinU;
		private float bladeTextureMaxU;

		public Builder() {
			this.duration = 250L;
			this.numBlades = 0;
			this.faceBrightness = 2;
			this.bladeBrightness = 2;
			this.segmentsProvider = DEFAULT_SEGMENTS_PROVIDER;
		}

		public Builder withDuration(long durationMillis) {
			this.duration = durationMillis;
			return this;
		}

		public Builder withWidth(double width) {
			this.bladeWidthProvider = new Interpolators.EaseInEaseOutFloatProvider((float)width);
			this.faceWidthProvider = new Interpolators.EaseInEaseOutFloatProvider((float)width);
			return this;
		}

		public Builder withBladeWidthProvider(Interpolators.FloatInterpolator widthProvider) {
			this.bladeWidthProvider = widthProvider;
			return this;
		}

		public Builder withFaceWidthProvider(Interpolators.FloatInterpolator widthProvider) {
			this.faceWidthProvider = widthProvider;
			return this;
		}

		public Builder withBladeLengthProvider(Interpolators.FloatProvider bladeLengthProvider) {
			this.bladeLengthProvider = bladeLengthProvider;
			return this;
		}

		public Builder withBlades(int numBlades, float bladeTextureMinU, float bladeTextureMaxU) {
			this.numBlades = numBlades;
			this.bladeTextureMinU = bladeTextureMinU;
			this.bladeTextureMaxU = bladeTextureMaxU;
			return this;
		}

		public Builder withBlade(float bladeTextureMinU, float bladeTextureMaxU) {
			return this.withBlades(1, bladeTextureMinU, bladeTextureMaxU);
		}

		public Builder withBlades(int numBlades) {
			return this.withBlades(numBlades, 0.0F, 1.0F);
		}

		public Builder withFace(float faceTextureMinU, float faceTextureMaxU) {
			this.faceTextureMinU = faceTextureMinU;
			this.faceTextureMaxU = faceTextureMaxU;
			this.isFaceEnabled = true;
			return this;
		}

		public Builder withBrightness(int brightness) {
			this.faceBrightness = brightness;
			this.bladeBrightness = brightness;
			return this;
		}

		public Builder withFaceBrightness(int brightness) {
			this.faceBrightness = Mth.clamp(brightness, 0, 5);
			return this;
		}

		public Builder withBladeBrightness(int brightness) {
			this.bladeBrightness = Mth.clamp(brightness, 0, 5);
			return this;
		}

		public Builder withSegmentsProvider(SegmentsProviders.SegmentsProvider segmentsProvider) {
			this.segmentsProvider = segmentsProvider;
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			super.withJsonObject(obj);
			JsonObject faceObj = obj.getAsJsonObject("face");
			if (faceObj != null) {
				float minU = JsonUtil.getJsonFloat(faceObj, "minU", 0.0F);
				float maxU = JsonUtil.getJsonFloat(faceObj, "maxU", 1.0F);
				this.withFace(minU, maxU);
				int brightness = JsonUtil.getJsonInt(faceObj, "brightness", 1);
				this.withFaceBrightness(brightness);
				Interpolators.FloatInterpolator widthInt = JsonUtil.getJsonInterpolator(faceObj, "width");
				if (widthInt != null) {
					this.withFaceWidthProvider(widthInt);
				}
			}

			JsonObject bladeObj = obj.getAsJsonObject("blade");
			if (bladeObj != null) {
				int count = JsonUtil.getJsonInt(bladeObj, "count", 1);
				float minU = JsonUtil.getJsonFloat(bladeObj, "minU", 0.0F);
				float maxU = JsonUtil.getJsonFloat(bladeObj, "maxU", 1.0F);
				this.withBlades(count, minU, maxU);
				int brightness = JsonUtil.getJsonInt(bladeObj, "brightness", 1);
				this.withBladeBrightness(brightness);
				Interpolators.FloatInterpolator widthInt = JsonUtil.getJsonInterpolator(bladeObj, "width");
				if (widthInt != null) {
					this.withBladeWidthProvider(widthInt);
				}
			}

			this.withRotations(JsonUtil.getJsonFloat(obj, "numRotations", 0.0F));
			float initialSpeed = JsonUtil.getJsonFloat(obj, "initialSpeed", 0.0F);
			float acceleration = JsonUtil.getJsonFloat(obj, "acceleration", 0.0F);
			if (this.numBlades > 0) {
				if (MiscUtil.isNearlyZero(initialSpeed) && MiscUtil.isNearlyZero(acceleration)) {
					this.withSegmentsProvider(new SegmentsProviders.StaticBeamSegmentsProvider());
				} else {
					this.withSegmentsProvider(new SegmentsProviders.MovingSegmentsProvider(initialSpeed, acceleration));
				}
			} else if (MiscUtil.isNearlyZero(initialSpeed) && MiscUtil.isNearlyZero(acceleration)) {
				this.withSegmentsProvider(SegmentsProviders.ZERO_PROVIDER);
			} else {
				this.withSegmentsProvider(new SegmentsProviders.MovingPointProvider(initialSpeed, acceleration));
			}

			return this;
		}

		public boolean isEffectAttached() {
			return true;
		}

		@OnlyIn(Dist.CLIENT)
		public AttachedProjectileEffect build(EffectBuilder.Context context) {
			AttachedProjectileEffect effect = new AttachedProjectileEffect();
			super.apply(effect, context);
			effect.randomization = 0.0F;
			effect.faceWidthProvider = this.faceWidthProvider;
			effect.bladeWidthProvider = this.bladeWidthProvider;
			if (!MiscUtil.isNearlyZero(context.getDistance())) {
				effect.distanceProvider = () -> context.getDistance();
			} else {
				effect.distanceProvider = this.bladeLengthProvider;
			}

			effect.isFaceEnabled = this.isFaceEnabled;
			effect.numBlades = this.numBlades;
			effect.bladeTextureMinU = this.bladeTextureMinU;
			effect.bladeTextureMaxU = this.bladeTextureMaxU;
			effect.faceTextureMinU = this.faceTextureMinU;
			effect.faceTextureMaxU = this.faceTextureMaxU;
			effect.faceBrightness = this.faceBrightness;
			effect.bladeBrightness = this.bladeBrightness;
			effect.segmentsProvider = this.segmentsProvider;
			Function<VertexConsumer, VertexConsumer> contextVertexConsumerTransformer =
				context.getVertexConsumerTransformer();
			effect.vertexConsumerTransformer =
				contextVertexConsumerTransformer != null ? contextVertexConsumerTransformer : VertexConsumers.PARTICLE;
			PoseProvider poseProvider = context.getPoseProvider();
			if (poseProvider != null && effect.poseProvider == null) {
				effect.poseProvider = poseProvider;
			}

			return effect;
		}

		public Collection<GunItem.FirePhase> getCompatiblePhases() {
			return COMPATIBLE_PHASES;
		}

		static {
			COMPATIBLE_PHASES =
				Set.of(FirePhase.PREPARING, FirePhase.FIRING, FirePhase.HIT_SCAN_ACQUIRED, FirePhase.COMPLETETING);
			DEFAULT_SEGMENTS_PROVIDER = new SegmentsProviders.MovingSegmentsProvider(200.0F, -10.0F);
		}
	}
}
