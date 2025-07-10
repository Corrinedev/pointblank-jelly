package mod.pbj.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import mod.pbj.client.GunClientState;
import mod.pbj.feature.ConditionContext;
import mod.pbj.util.Conditions;
import mod.pbj.util.TimeUnit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ConditionalAnimationProvider implements AnimationProvider {
	private List<Descriptor> conditionalAnimations;
	private int randomUpperBound;
	private static final Random random = new Random();

	private ConditionalAnimationProvider(List<Descriptor> conditionalAnimations) {
		this.conditionalAnimations = Collections.unmodifiableList(conditionalAnimations);
		this.randomUpperBound =
			(int)conditionalAnimations.stream().filter((p) -> p.predicate() == Conditions.RANDOM_PICK).count();
	}

	public Descriptor getDescriptor(LivingEntity player, ItemStack itemStack, GunClientState gunClientState) {
		Descriptor result = null;
		int randomValue = this.randomUpperBound > 0 ? random.nextInt(this.randomUpperBound) : 0;
		int i = 0;

		for (Descriptor descriptor : this.conditionalAnimations) {
			ConditionContext ctx = new ConditionContext(
				player,
				itemStack,
				itemStack,
				gunClientState,
				ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
				randomValue,
				i);
			if (descriptor.predicate() == Conditions.RANDOM_PICK) {
				++i;
			}

			if (descriptor.predicate().test(ctx)) {
				result = descriptor;
				break;
			}
		}

		return result;
	}

	public static class Builder {
		private List<Descriptor> conditionalAnimations = new ArrayList<>();

		public Builder
		withAnimation(String animation, Predicate<ConditionContext> condition, long duration, TimeUnit timeUnit) {
			this.conditionalAnimations.add(new Descriptor(condition, duration, timeUnit, animation));
			return this;
		}

		public Builder withAnimation(String animation, Predicate<ConditionContext> condition) {
			return this.withAnimation(animation, condition, 0L, TimeUnit.MILLISECOND);
		}

		public List<Descriptor> getAnimations() {
			return this.conditionalAnimations;
		}

		public AnimationProvider build() {
			return new ConditionalAnimationProvider(this.conditionalAnimations);
		}
	}
}
