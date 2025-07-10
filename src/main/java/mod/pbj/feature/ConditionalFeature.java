package mod.pbj.feature;

import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.item.GunItem;
import mod.pbj.item.ScriptHolder;
import net.minecraft.world.item.ItemStack;

public abstract class ConditionalFeature implements Feature, ScriptHolder {
	protected FeatureProvider owner;
	protected Predicate<ConditionContext> predicate;
	protected Map<
		GunItem.FirePhase,
		List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
		effectBuilders;

	public ConditionalFeature(FeatureProvider owner, Predicate<ConditionContext> predicate) {
		this(owner, predicate, Collections.emptyMap());
	}

	public ConditionalFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> predicate,
		Map<GunItem.FirePhase,
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
			effectBuilders) {
		this.effectBuilders = new HashMap<>();
		this.owner = owner;
		this.predicate = predicate;
		this.effectBuilders = Collections.unmodifiableMap(effectBuilders);
	}

	public FeatureProvider getOwner() {
		return this.owner;
	}

	public boolean isEnabled(ItemStack itemStack) {
		return this.predicate.test(new ConditionContext(itemStack));
	}

	public Map<
		GunItem.FirePhase,
		List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
	getEffectBuilders() {
		return this.effectBuilders;
	}

	public String toString() {
		return this.getClass().getSimpleName();
	}
}
