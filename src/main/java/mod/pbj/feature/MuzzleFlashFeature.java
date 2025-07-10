package mod.pbj.feature;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.client.effect.MuzzleFlashEffect;
import mod.pbj.item.GunItem;
import mod.pbj.registry.EffectRegistry;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class MuzzleFlashFeature extends ConditionalFeature {
	private final List<Pair<MuzzleFlashEffect.Builder, Predicate<ConditionContext>>> muzzleEffectBuilders =
		new ArrayList<>();

	public MuzzleFlashFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> predicate,
		Map<GunItem.FirePhase,
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
			effectBuilders) {
		super(owner, predicate, effectBuilders);

		for (Entry<
				 GunItem.FirePhase,
				 List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
				 firePhaseListEntry : effectBuilders.entrySet()) {
			for (Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>> o :
				 firePhaseListEntry.getValue()) {
				EffectBuilder<? extends EffectBuilder<?, ?>, ?> eb = o.getFirst().get();
				if (eb instanceof MuzzleFlashEffect.Builder mfeb) {
					this.muzzleEffectBuilders.add(Pair.of(mfeb, o.getSecond()));
				}
			}
		}
	}

	public List<Pair<MuzzleFlashEffect.Builder, Predicate<ConditionContext>>> getMuzzleFlashEffectBuilders() {
		return this.muzzleEffectBuilders;
	}

	public boolean isEnabled(ItemStack itemStack) {
		return super.isEnabled(itemStack);
	}

	@Override
	public @Nullable Script getScript() {
		return null;
	}

	public static class Builder implements FeatureBuilder<Builder, MuzzleFlashFeature> {
		private final
			Map<GunItem.FirePhase,
				List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
				effectBuilders = new HashMap<>();
		private Predicate<ConditionContext> condition = (ctx) -> true;

		public Builder withEffect(
			GunItem.FirePhase firePhase, Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilder) {
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>
				builders = this.effectBuilders.computeIfAbsent(firePhase, (k) -> new ArrayList<>());
			builders.add(Pair.of(effectBuilder, (ctx) -> true));
			return this;
		}

		public Builder withEffect(
			GunItem.FirePhase firePhase,
			Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilder,
			Predicate<ConditionContext> condition) {
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>
				builders = this.effectBuilders.computeIfAbsent(firePhase, (k) -> new ArrayList<>());
			builders.add(Pair.of(effectBuilder, condition));
			return this;
		}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition")) {
				this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
			}

			GunItem.FirePhase firePhase;
			Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier;
			Predicate<ConditionContext> condition;
			for (Iterator<JsonObject> var2 = JsonUtil.getJsonObjects(obj, "effects").iterator(); var2.hasNext();
				 this.withEffect(firePhase, supplier, condition)) {
				JsonObject effect = var2.next();
				firePhase = (GunItem.FirePhase)JsonUtil.getEnum(effect, "phase", GunItem.FirePhase.class, null, true);
				String effectName = JsonUtil.getJsonString(effect, "name");
				supplier = () -> EffectRegistry.getEffectBuilderSupplier(effectName).get();
				if (effect.has("condition")) {
					JsonObject conditionObj = effect.getAsJsonObject("condition");
					condition = Conditions.fromJson(conditionObj);
				} else {
					condition = (ctx) -> true;
				}
			}

			return this;
		}

		public MuzzleFlashFeature build(FeatureProvider featureProvider) {
			return new MuzzleFlashFeature(featureProvider, this.condition, this.effectBuilders);
		}
	}
}
