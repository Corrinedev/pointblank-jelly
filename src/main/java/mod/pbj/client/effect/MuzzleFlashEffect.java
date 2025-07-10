//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.client.effect;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collection;
import java.util.Set;
import mod.pbj.client.GunClientState;
import mod.pbj.client.uv.SpriteUVProvider;
import mod.pbj.item.GunItem;
import mod.pbj.item.GunItem.FirePhase;
import mod.pbj.util.TimeUnit;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class MuzzleFlashEffect extends AbstractEffect {
	private GunClientState gunState;
	private long startTime;
	private long nanoDuration;
	private SpriteUVProvider spriteUVProvider;

	public MuzzleFlashEffect() {}

	public boolean isExpired() {
		return System.nanoTime() - this.startTime > this.nanoDuration;
	}

	private float getProgress() {
		float progress = (float)((double)(System.nanoTime() - this.startTime) / (double)this.nanoDuration);
		return Mth.clamp(progress, 0.0F, 1.0F);
	}

	public void render(EffectRenderContext effectRenderContext) {
		PoseStack poseStack = effectRenderContext.getPoseStack();
		if (poseStack != null) {
			poseStack.pushPose();
			this.renderQuad(
				poseStack,
				effectRenderContext.getPosition(),
				effectRenderContext.getVertexBuffer(),
				effectRenderContext.getLightColor(),
				1.0F,
				1.0F,
				1.0F,
				1.0F,
				this.getProgress());
			poseStack.popPose();
		}
	}

	protected void renderQuad(
		PoseStack poseStack,
		Vec3 position,
		VertexConsumer buffer,
		int packedLight,
		float red,
		float green,
		float blue,
		float alpha,
		float progress) {
		Matrix4f poseState = poseStack.last().pose();
		this.createVerticesOfQuad(position, poseState, buffer, packedLight, red, green, blue, progress);
	}

	protected void createVerticesOfQuad(
		Vec3 position,
		Matrix4f poseState,
		VertexConsumer buffer,
		int packedLight,
		float red,
		float green,
		float blue,
		float progress) {
		float[] uv = this.spriteUVProvider.getSpriteUV(progress);
		float alpha = this.alphaProvider.getValue(progress);
		if (uv != null) {
			float minU = uv[0];
			float minV = uv[1];
			float maxU = uv[2];
			float maxV = uv[3];
			float[][] texUV = new float[][] {{maxU, maxV}, {minU, maxV}, {minU, minV}, {maxU, minV}};
			float expand = this.widthProvider.getValue(progress);
			float zOffset = 0.0F;
			float[][] positionOffsets = new float[][] {
				{expand, -expand, zOffset},
				{-expand, -expand, zOffset},
				{-expand, expand, zOffset},
				{expand, expand, zOffset}};
			if (this.isGlowEnabled) {
				packedLight = 240;
			}

			for (int i = 0; i < 4; ++i) {
				buffer
					.vertex(
						poseState,
						(float)position.x() + positionOffsets[i][0],
						(float)position.y() + positionOffsets[i][1],
						(float)position.z() + positionOffsets[i][2])
					.color(red, green, blue, alpha)
					.uv(texUV[i][0], texUV[i][1])
					.overlayCoords(0)
					.uv2(packedLight)
					.normal(0.0F, 1.0F, 0.0F)
					.endVertex();
			}
		}
	}

	public void launch(Entity player) {
		this.gunState.addMuzzleEffect(this);
	}

	public boolean hasInfiniteBounds() {
		return false;
	}

	public static class Builder extends AbstractEffect.AbstractEffectBuilder<Builder, MuzzleFlashEffect> {
		private static final Set<GunItem.FirePhase> COMPATIBLE_PHASES;

		public Builder() {}

		public Collection<GunItem.FirePhase> getCompatiblePhases() {
			return COMPATIBLE_PHASES;
		}

		public boolean isEffectAttached() {
			return true;
		}

		public Builder withJsonObject(JsonObject obj) {
			return super.withJsonObject(obj);
		}

		public MuzzleFlashEffect build(EffectBuilder.Context context) {
			MuzzleFlashEffect effect = new MuzzleFlashEffect();
			super.apply(effect, context);
			effect.gunState = context.getGunClientState();
			effect.startTime = System.nanoTime();
			effect.spriteUVProvider = effect.getSpriteUVProvider();
			effect.nanoDuration = TimeUnit.MILLISECOND.toNanos(effect.duration);
			return effect;
		}

		static {
			COMPATIBLE_PHASES = Set.of(FirePhase.FIRING);
		}
	}
}
