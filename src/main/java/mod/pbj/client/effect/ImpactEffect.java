package mod.pbj.client.effect;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collection;
import java.util.Set;
import mod.pbj.client.particle.EffectParticles;
import mod.pbj.item.GunItem;
import mod.pbj.item.GunItem.FirePhase;
import mod.pbj.util.Interpolators;
import mod.pbj.util.ParticleValueProviders;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ImpactEffect extends AbstractEffect {
	private final float textureMinU = 0.0F;
	private final float textureMaxU = 1.0F;
	private int count;

	private ImpactEffect() {}

	public boolean hasInfiniteBounds() {
		return false;
	}

	public void render(EffectRenderContext effectRenderContext) {
		Vec3 pos = effectRenderContext.getPosition();
		if (pos == null) {
			throw new IllegalArgumentException("Incorrect render context, missing position");
		} else {
			Camera camera = effectRenderContext.getCamera();
			if (camera == null) {
				throw new IllegalArgumentException("Incorrect render context, missing camera");
			} else {
				float progress = effectRenderContext.getProgress();
				if (!(progress < 0.0F)) {
					float[] uv = effectRenderContext.getSpriteUVProvider().getSpriteUV(progress);
					if (uv != null) {
						VertexConsumer vertexConsumer = effectRenderContext.getVertexBuffer();
						int lightColor = this.isGlowEnabled ? 240 : effectRenderContext.getLightColor();
						float size = this.widthProvider.getValue(progress);
						float alpha = this.alphaProvider.getValue(progress);
						float spriteWidth = uv[2] - uv[0];
						float minU = uv[0] + spriteWidth * this.textureMinU;
						float minV = uv[1];
						float maxU = uv[0] + spriteWidth * this.textureMaxU;
						float maxV = uv[3];
						float u0 = minU;
						float u1 = maxU;
						float v0 = minV;
						float v1 = maxV;
						Vec3 cameraPosition = camera.getPosition();
						float dx = (float)(pos.x - cameraPosition.x());
						float dy = (float)(pos.y - cameraPosition.y());
						float dz = (float)(pos.z - cameraPosition.z());
						Quaternionf rotation = new Quaternionf();
						float roll = -(effectRenderContext.getInitialAngle() + this.numRotations * 360.0F * progress);
						if (effectRenderContext.getRotation() != null) {
							rotation.mul(effectRenderContext.getRotation());
							rotation.rotateZ(((float)Math.PI / 180F) * roll);
						} else {
							rotation.mul(camera.rotation());
							rotation.rotateZ(((float)Math.PI / 180F) * roll);
						}

						Vector3f[] avector3f = new Vector3f[] {
							new Vector3f(-1.0F, -1.0F, 0.0F),
							new Vector3f(-1.0F, 1.0F, 0.0F),
							new Vector3f(1.0F, 1.0F, 0.0F),
							new Vector3f(1.0F, -1.0F, 0.0F)};

						for (int i = 0; i < 4; ++i) {
							Vector3f vector3f = avector3f[i];
							vector3f.rotate(rotation);
							vector3f.mul(size);
							vector3f.add(dx, dy, dz);
						}

						int argbColor = (int)(alpha * 255.0F) << 24 | this.color;

						for (int k = 0; k < this.brightness; ++k) {
							vertexConsumer.vertex(avector3f[0].x(), avector3f[0].y(), avector3f[0].z())
								.uv(u1, v1)
								.color(argbColor)
								.uv2(lightColor)
								.endVertex();
							vertexConsumer.vertex(avector3f[1].x(), avector3f[1].y(), avector3f[1].z())
								.uv(u1, v0)
								.color(argbColor)
								.uv2(lightColor)
								.endVertex();
							vertexConsumer.vertex(avector3f[2].x(), avector3f[2].y(), avector3f[2].z())
								.uv(u0, v0)
								.color(argbColor)
								.uv2(lightColor)
								.endVertex();
							vertexConsumer.vertex(avector3f[3].x(), avector3f[3].y(), avector3f[3].z())
								.uv(u0, v1)
								.color(argbColor)
								.uv2(lightColor)
								.endVertex();
						}
					}
				}
			}
		}
	}

	public void launch(Entity player) {
		Minecraft mc = Minecraft.getInstance();

		for (int i = 0; i < this.count; ++i) {
			Particle particle = new EffectParticles.EffectParticle(player, this);
			mc.particleEngine.add(particle);
		}
	}

	public static class Builder extends AbstractEffect.AbstractEffectBuilder<Builder, ImpactEffect> {
		private static final Set<GunItem.FirePhase> COMPATIBLE_PHASES;
		private ParticleValueProviders.ParticleCountProvider countProvider =
			new ParticleValueProviders.ConstantParticleCountProvider(1);
		private ParticleValueProviders.ParticleWidthProvider impactWidthProvider;

		public Builder() {}

		public Builder withCount(int count) {
			this.countProvider = new ParticleValueProviders.ConstantParticleCountProvider(count);
			return this;
		}

		public Builder withCount(ParticleValueProviders.ParticleCountProvider countProvider) {
			this.countProvider = countProvider;
			return this;
		}

		public Builder withWidth(ParticleValueProviders.ParticleWidthProvider widthProvider) {
			this.impactWidthProvider = widthProvider;
			return this;
		}

		public boolean isEffectAttached() {
			return false;
		}

		public ImpactEffect build(EffectBuilder.Context effectContext) {
			ImpactEffect effect = new ImpactEffect();
			super.apply(effect, effectContext);
			effect.count = this.countProvider.getCount(effectContext);
			if (this.impactWidthProvider != null) {
				float width = this.impactWidthProvider.getWidth(effectContext);
				effect.widthProvider = new Interpolators.ConstantFloatProvider(width);
			}

			return effect;
		}

		public Collection<GunItem.FirePhase> getCompatiblePhases() {
			return COMPATIBLE_PHASES;
		}

		static {
			COMPATIBLE_PHASES = Set.of(FirePhase.HIT_TARGET);
		}
	}
}
