package mod.pbj.item;

import java.util.function.Predicate;
import mod.pbj.client.GunClientState;
import mod.pbj.feature.ConditionContext;
import mod.pbj.util.TimeUnit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface AnimationProvider {
	Descriptor getDescriptor(LivingEntity var1, ItemStack var2, GunClientState var3);

	class Simple implements AnimationProvider {
		public Descriptor descriptor;

		public Simple(String animationName) {
			this.descriptor = new Descriptor((ctx) -> true, 0L, TimeUnit.MILLISECOND, animationName);
		}

		public Descriptor getDescriptor(LivingEntity player, ItemStack itemStack, GunClientState gunClientState) {
			return this.descriptor;
		}
	}

	record Descriptor(Predicate<ConditionContext> predicate, long duration, TimeUnit timeUnit, String animationName) {
		public Predicate<ConditionContext> predicate() {
			return this.predicate;
		}

		public long duration() {
			return this.duration;
		}

		public TimeUnit timeUnit() {
			return this.timeUnit;
		}

		public String animationName() {
			return this.animationName;
		}
	}
}
