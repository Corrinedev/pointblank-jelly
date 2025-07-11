package mod.pbj.feature;

import com.eliotlash.mclib.math.Variable;
import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import mod.pbj.PointBlankJelly;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.feature.math.FeatureVariables;
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
	protected FeatureVariables mathParser;

	public ConditionalFeature(FeatureProvider owner, Predicate<ConditionContext> predicate) {
		this(owner, predicate, Collections.emptyMap());
	}

	public ConditionalFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, Variable... variables) {
		this(owner, predicate, Collections.emptyMap(), variables);
	}

	public ConditionalFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> predicate,
		Map<GunItem.FirePhase,
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
			effectBuilders,
		Variable... variables
	) {
        this.owner = owner;
		this.predicate = predicate;
		this.effectBuilders = Collections.unmodifiableMap(effectBuilders);
		this.mathParser = new FeatureVariables(variables);
	}

	public FeatureProvider getOwner() {
		return this.owner;
	}

	public boolean isEnabled(ItemStack itemStack) {
		return this.predicate.test(new ConditionContext(itemStack));
	}

	/* Unsafe, must be a primitive, int, double, float, long etc. (String is NOT a primitive) */
	public Object getMathValue(Object original, String expression) {
		if(expression != null && !expression.isBlank()) {
			try {
				return mathParser.parse(expression).get();
			} catch (Exception e) {
				PointBlankJelly.LOGGER.warn("Parsing of math expression failed! Reason = {}", e.getMessage());
			}
		}
		return original;
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
