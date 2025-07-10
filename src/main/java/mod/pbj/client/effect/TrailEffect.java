package mod.pbj.client.effect;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collection;
import java.util.Collections;
import mod.pbj.client.particle.EffectParticles;
import mod.pbj.client.uv.SpriteUVProvider;
import mod.pbj.item.GunItem;
import mod.pbj.item.GunItem.FirePhase;
import mod.pbj.util.JsonUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class TrailEffect extends AbstractEffect {
	private Vec3 previousPosition;
	private Vec3 previousVelocity;
	private double longitudeOffset;

	public TrailEffect() {}

	public boolean hasInfiniteBounds() {
		return false;
	}

	public void render(EffectRenderContext effectRenderContext) {
		TrailRenderContext trailRenderContext = (TrailRenderContext)effectRenderContext;
		float progress = effectRenderContext.getProgress();
		float partialTick = effectRenderContext.getPartialTick();
		Camera camera = effectRenderContext.getCamera();
		Vec3 cameraPos = camera.getPosition();
		Vector3f viewDirection3f = camera.getLookVector();
		Vec3 viewDirection = new Vec3(viewDirection3f.x, viewDirection3f.y, viewDirection3f.z);
		Vec3 cameraUpVector = new Vec3(0.0F, 1.0F, 0.0F);
		Vec3 cameraRightVector = viewDirection.cross(cameraUpVector);
		Vec3 position = effectRenderContext.getPosition();
		Vec3 velocity = effectRenderContext.getVelocity();
		Vec3 r2 = viewDirection.cross(velocity).normalize();
		Vec3 up2 = r2;
		if (r2.lengthSqr() < 1.0E-5) {
			up2 = cameraRightVector;
		}

		float halfWidth = this.widthProvider.getValue(progress) * 0.5F;
		Vec3 bottomLeft;
		Vec3 topLeft;
		if (trailRenderContext.prevVelocity != null && trailRenderContext.prevPos != null) {
			Vec3 prevR2 = viewDirection.cross(trailRenderContext.prevVelocity).normalize();
			Vec3 prevUp2 = prevR2;
			if (prevR2.lengthSqr() < 1.0E-5) {
				prevUp2 = cameraRightVector;
			}

			bottomLeft = this.getVerticePos(
				trailRenderContext.prevPos, trailRenderContext.prevVelocity, prevUp2, 1.0F, -halfWidth);
			topLeft = this.getVerticePos(
				trailRenderContext.prevPos, trailRenderContext.prevVelocity, prevUp2, 1.0F, halfWidth);
		} else {
			bottomLeft = this.getVerticePos(position, velocity, up2, 0.0F, -halfWidth);
			topLeft = this.getVerticePos(position, velocity, up2, 0.0F, halfWidth);
		}

		Vec3 bottomRight = this.getVerticePos(position, velocity, up2, partialTick, -halfWidth);
		Vec3 topRight = this.getVerticePos(position, velocity, up2, partialTick, halfWidth);
		bottomLeft = bottomLeft.subtract(cameraPos);
		bottomRight = bottomRight.subtract(cameraPos);
		topLeft = topLeft.subtract(cameraPos);
		topRight = topRight.subtract(cameraPos);
		SpriteUVProvider spriteUVProvider = effectRenderContext.getSpriteUVProvider();
		float[] uv = spriteUVProvider.getSpriteUV(progress);
		float u0 = uv[0];
		float v0 = uv[1];
		float u1 = uv[2];
		float v1 = uv[3];
		int lightColor = effectRenderContext.getLightColor();
		float alpha = this.alphaProvider.getValue(progress);
		float rCol = 1.0F;
		float gCol = 1.0F;
		float bCol = 1.0F;
		VertexConsumer vertexConsumer = effectRenderContext.getVertexBuffer();
		vertexConsumer.vertex(topRight.x, topRight.y, topRight.z)
			.uv(u1, v1)
			.color(rCol, gCol, bCol, alpha)
			.uv2(lightColor)
			.endVertex();
		vertexConsumer.vertex(bottomRight.x, bottomRight.y, bottomRight.z)
			.uv(u1, v0)
			.color(rCol, gCol, bCol, alpha)
			.uv2(lightColor)
			.endVertex();
		vertexConsumer.vertex(bottomLeft.x, bottomLeft.y, bottomLeft.z)
			.uv(u0, v0)
			.color(rCol, gCol, bCol, alpha)
			.uv2(lightColor)
			.endVertex();
		vertexConsumer.vertex(topLeft.x, topLeft.y, topLeft.z)
			.uv(u0, v1)
			.color(rCol, gCol, bCol, alpha)
			.uv2(lightColor)
			.endVertex();
	}

	private Vec3 getVerticePos(Vec3 position, Vec3 velocity, Vec3 side, float segmentProgress, float sideOffset) {
		Vec3 vertixPos = position.add(velocity.scale(segmentProgress)).add(side.scale(sideOffset));
		return vertixPos;
	}

	public void launch(Entity player) {
		Minecraft mc = Minecraft.getInstance();
		Particle particle = new EffectParticles.EffectParticle(player, this);
		mc.particleEngine.add(particle);
	}

	public void launchNext(Entity owner, Vec3 position, Vec3 velocity) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 positionWithOffset = position.subtract(velocity.normalize().scale(this.longitudeOffset));
		if (this.previousPosition != null) {
			Particle particle = new TrailParticle(
				owner, this, positionWithOffset, velocity, this.previousPosition, this.previousVelocity);
			mc.particleEngine.add(particle);
		}

		this.previousVelocity = velocity;
		this.previousPosition = positionWithOffset;
	}

	public static class Builder extends AbstractEffect.AbstractEffectBuilder<Builder, TrailEffect> {
		private final Collection<GunItem.FirePhase> compatiblePhases;
		private double longitudeOffset;

		public Builder() {
			this.compatiblePhases = Collections.singletonList(FirePhase.FLYING);
		}

		public Builder withLongitudeOffset(double longitudeOffset) {
			this.longitudeOffset = longitudeOffset;
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			super.withJsonObject(obj);
			this.withLongitudeOffset(JsonUtil.getJsonFloat(obj, "longitudeOffset", 0.0F));
			return this;
		}

		public Collection<GunItem.FirePhase> getCompatiblePhases() {
			return this.compatiblePhases;
		}

		public boolean isEffectAttached() {
			return false;
		}

		public TrailEffect build(EffectBuilder.Context effectContext) {
			TrailEffect effect = new TrailEffect();
			this.apply(effect, effectContext);
			effect.longitudeOffset = this.longitudeOffset;
			return effect;
		}
	}

	public static class TrailRenderContext extends EffectRenderContext {
		public Vec3 prevPos;
		public Vec3 prevVelocity;

		public TrailRenderContext() {}
	}

	private static class TrailParticle extends EffectParticles.EffectParticle {
		private Vec3 position;
		private Vec3 velocity;
		private Vec3 prevVelocity;
		private Vec3 prevPos;
		private float segmentProgress;

		TrailParticle(ClientLevel level, double x, double y, double z) {
			super(level, x, y, z);
		}

		public TrailParticle(
			Entity owner, TrailEffect effect, Vec3 position, Vec3 velocity, Vec3 prevPos, Vec3 prevVelocity) {
			super(owner, effect);
			this.setPos(position.x, position.y, position.z);
			this.xo = this.x;
			this.yo = this.y;
			this.zo = this.z;
			this.position = position;
			this.velocity = velocity;
			this.prevPos = prevPos;
			this.prevVelocity = prevVelocity;
		}

		public boolean shouldCull() {
			return false;
		}

		public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
			int lightColor = this.getLightColor(partialTick);
			float progress = this.getProgress(partialTick);
			if (this.segmentProgress < partialTick) {
				this.segmentProgress = partialTick;
			} else {
				this.segmentProgress = 1.0F;
			}

			TrailRenderContext effectRenderContext = (TrailRenderContext)(new TrailRenderContext())
														 .withCamera(camera)
														 .withPosition(this.position)
														 .withVelocity(this.velocity)
														 .withVertexBuffer(vertexConsumer)
														 .withProgress(progress)
														 .withPartialTick(this.segmentProgress)
														 .withLightColor(lightColor)
														 .withSpriteUVProvider(this.spriteUVProvider);
			effectRenderContext.prevPos = this.prevPos;
			effectRenderContext.prevVelocity = this.prevVelocity;
			this.effect.render(effectRenderContext);
		}
	}
}
