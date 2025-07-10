package mod.pbj.util;

import java.util.Random;
import mod.pbj.client.effect.EffectBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import software.bernie.geckolib.util.ClientUtils;

public class ParticleValueProviders {
	public ParticleValueProviders() {}

	public static class ConstantParticleCountProvider implements ParticleCountProvider {
		private final int count;

		public ConstantParticleCountProvider(int count) {
			this.count = count;
		}

		public int getCount(EffectBuilder.Context effectContext) {
			return this.count;
		}
	}

	public static class RandomParticleCountProvider implements ParticleCountProvider {
		private final Random random = new Random();
		private final int origin;
		private final int bound;

		public RandomParticleCountProvider(int origin, int bound) {
			this.origin = origin;
			this.bound = bound;
		}

		public int getCount(EffectBuilder.Context effectContext) {
			return this.random.nextInt(this.origin, this.bound);
		}
	}

	public static class DamageBasedParticleCountProvider implements ParticleCountProvider {
		private final int maxCount;
		private final float maxDamage;

		public DamageBasedParticleCountProvider(int maxCount, float maxDamage) {
			this.maxCount = maxCount;
			this.maxDamage = maxDamage;
		}

		public int getCount(EffectBuilder.Context effectContext) {
			float damage = effectContext.getDamage();
			return Math.round(Mth.clamp((float)this.maxCount * damage / this.maxDamage, 0.0F, (float)this.maxCount));
		}
	}

	public static class BoundingBoxBasedParticleWidthProvider implements ParticleWidthProvider {
		private final double maxBoundBoxSize;
		private final double maxWidth;

		public BoundingBoxBasedParticleWidthProvider(double maxBoundBoxSize, double maxWidth) {
			this.maxBoundBoxSize = maxBoundBoxSize;
			this.maxWidth = maxWidth;
		}

		public float getWidth(EffectBuilder.Context effectContext) {
			float result = (float)this.maxWidth;
			HitResult hitResult = effectContext.getHitResult();
			if (hitResult instanceof SimpleHitResult simpleHitResult) {
				int entityId = simpleHitResult.getEntityId();
				Entity entity = ClientUtils.getLevel().getEntity(entityId);
				if (entity != null) {
					AABB bb = entity.getBoundingBox();
					if (bb != null) {
						result =
							(float)Mth.clamp(this.maxWidth * bb.getSize() / this.maxBoundBoxSize, 0.0F, this.maxWidth);
					}
				}
			}

			return result;
		}
	}

	public interface ParticleCountProvider {
		int getCount(EffectBuilder.Context var1);
	}

	public interface ParticleWidthProvider {
		float getWidth(EffectBuilder.Context var1);
	}
}
