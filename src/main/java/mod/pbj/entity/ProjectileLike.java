package mod.pbj.entity;

import java.util.List;
import java.util.function.Predicate;
import mod.pbj.client.effect.Effect;
import mod.pbj.util.Trajectory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

public interface ProjectileLike {
	ItemStack getItem();

	List<Effect> getActiveAttachedEffects();

	float getProgress(float var1);

	long getElapsedTimeMillis();

	void launchAtTargetEntity(LivingEntity var1, HitResult var2, Entity var3);

	void launchAtLookTarget(LivingEntity var1, double var2, long var4);

	double getInitialVelocityBlocksPerTick();

	default void setTrailEffects(List<EffectInfo> trailEffects) {}

	default void setAttachedEffects(List<EffectInfo> attachedEffects) {}

	default Trajectory<?> getTrajectory() {
		return null;
	}

	record EffectInfo(Effect effect, Predicate<ProjectileLike> predicate) {
		public Effect effect() {
			return this.effect;
		}

		public Predicate<ProjectileLike> predicate() {
			return this.predicate;
		}
	}
}
