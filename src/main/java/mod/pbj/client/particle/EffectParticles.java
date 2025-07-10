//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.client.particle;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import mod.pbj.client.effect.Effect;
import mod.pbj.client.effect.EffectRenderContext;
import mod.pbj.client.uv.SpriteUVProvider;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.TimeUnit;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class EffectParticles {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private static final Function<EffectRenderKey, ParticleRenderType> effectRenderTypes =
		Util.memoize((key) -> new EffectParticleRenderType(key.texture, key.blendMode, key.isDepthTestEnabled));

	public EffectParticles() {}

	private record
	EffectParticleRenderType(ResourceLocation texture, Effect.BlendMode blendMode, boolean isDepthTestEnabled)
		implements ParticleRenderType {
		public void begin(BufferBuilder bufferBuilder, TextureManager textureManager) {
			RenderSystem.setShaderTexture(0, this.texture);
			RenderSystem.enableBlend();
			if (this.isDepthTestEnabled) {
				RenderSystem.depthMask(true);
				RenderSystem.enableDepthTest();
			} else {
				RenderSystem.depthMask(false);
				RenderSystem.disableDepthTest();
			}

			RenderSystem.disableCull();
			if (Objects.requireNonNull(this.blendMode) == Effect.BlendMode.ADDITIVE) {
				RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
			} else {
				RenderSystem.defaultBlendFunc();
			}

			Minecraft mc = Minecraft.getInstance();
			mc.gameRenderer.lightTexture().turnOnLightLayer();
			bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.PARTICLE);
		}

		public void end(Tesselator tesselator) {
			tesselator.end();
		}
	}

	private static class EffectRenderKey {
		ResourceLocation texture;
		Effect.BlendMode blendMode;
		boolean isDepthTestEnabled;

		EffectRenderKey(ResourceLocation texture, Effect.BlendMode blendMode, boolean isDepthTestEnabled) {
			this.texture = texture;
			this.blendMode = blendMode;
			this.isDepthTestEnabled = isDepthTestEnabled;
		}

		public int hashCode() {
			return Objects.hash(this.blendMode, this.isDepthTestEnabled, this.texture);
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj == null) {
				return false;
			} else if (this.getClass() != obj.getClass()) {
				return false;
			} else {
				EffectRenderKey other = (EffectRenderKey)obj;
				return this.blendMode == other.blendMode && this.isDepthTestEnabled == other.isDepthTestEnabled &&
					Objects.equals(this.texture, other.texture);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static class EffectParticle extends Particle {
		private ParticleRenderType renderType;
		private Entity owner;
		private float initialRoll;
		protected Effect effect;
		private boolean hasInfiniteBounds;
		protected SpriteUVProvider spriteUVProvider;
		private int delay;

		protected EffectParticle(ClientLevel level, double x, double y, double z) {
			super(level, x, y, z);
		}

		public EffectParticle(Entity owner, Effect effect) {
			super((ClientLevel)MiscUtil.getLevel(owner), 0.0F, 0.0F, 0.0F);
			this.effect = effect;
			this.hasInfiniteBounds = effect.hasInfiniteBounds();
			Vec3 startPosition = effect.getStartPositionProvider().get();
			Random random = new Random();
			this.setPos(
				startPosition.x + (double)(random.nextFloat() * 0.01F),
				startPosition.y + (double)(random.nextFloat() * 0.01F),
				startPosition.z + (double)(random.nextFloat() * 0.01F));
			this.xo = this.x;
			this.yo = this.y;
			this.zo = this.z;
			this.xd *= 0.1F;
			this.yd *= 0.1F;
			this.zd *= 0.1F;
			Vec3 velocity = effect.getVelocityProvider().get();
			float c = 1.0F;
			this.xd += velocity.x * (double)c;
			this.yd += velocity.y * (double)c;
			this.zd += velocity.z * (double)c;
			this.hasPhysics = effect.hasPhysics();
			this.owner = owner;
			this.delay = (int)TimeUnit.MILLISECOND.toTicks(effect.getDelay());
			this.lifetime = (int)TimeUnit.MILLISECOND.toTicks(effect.getDuration()) + this.delay;
			this.renderType = EffectParticles.effectRenderTypes.apply(
				new EffectRenderKey(effect.getTexture(), effect.getBlendMode(), effect.isDepthTestEnabled()));
			this.initialRoll = effect.getInitialRoll();
			this.friction = effect.getFriction();
			this.gravity = effect.getGravity();
			this.speedUpWhenYMotionIsBlocked = true;
			this.spriteUVProvider = effect.getSpriteUVProvider();
		}

		protected float getProgress(float partialTick) {
			int adjustedAge = this.age - this.delay;
			float elapsedTimeTicks = (float)adjustedAge + partialTick;
			float progress;
			if (adjustedAge < 0) {
				progress = elapsedTimeTicks / (float)this.delay;
			} else {
				progress = elapsedTimeTicks / (float)(this.lifetime - this.delay);
			}

			return Mth.clamp(progress, -1.0F, 1.0F);
		}

		public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
			try {
				if (camera.getEntity() != this.owner) {
				}

				int lightColor = this.getLightColor(partialTick);
				double posX = Mth.lerp(partialTick, this.xo, this.x);
				double posY = Mth.lerp(partialTick, this.yo, this.y);
				double posZ = Mth.lerp(partialTick, this.zo, this.z);
				float progress = this.getProgress(partialTick);
				EffectRenderContext effectRenderContext = (new EffectRenderContext())
															  .withCamera(camera)
															  .withRotation(this.effect.getRotation())
															  .withPosition(new Vec3(posX, posY, posZ))
															  .withInitialAngle(this.initialRoll)
															  .withVertexBuffer(vertexConsumer)
															  .withProgress(progress)
															  .withLightColor(lightColor)
															  .withSpriteUVProvider(this.spriteUVProvider);
				this.effect.render(effectRenderContext);
			} catch (Exception e) {
				EffectParticles.LOGGER.error("Failed to render effect particle: {}", e);
			}
		}

		public ParticleRenderType getRenderType() {
			return this.renderType;
		}

		public AABB getBoundingBox() {
			return this.hasInfiniteBounds ? IForgeBlockEntity.INFINITE_EXTENT_AABB : super.getBoundingBox();
		}

		public void tick() {
			super.tick();
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static class EffectParticleProvider implements ParticleProvider<SimpleParticleType> {
		public EffectParticleProvider(SpriteSet spriteSet) {}

		public Particle createParticle(
			SimpleParticleType particleType,
			ClientLevel level,
			double posX,
			double posY,
			double posZ,
			double xd,
			double yd,
			double zd) {
			EffectParticle particle = new EffectParticle(level, posX, posY, posZ);
			return particle;
		}
	}
}
