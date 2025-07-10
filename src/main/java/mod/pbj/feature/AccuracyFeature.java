package mod.pbj.feature;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Predicate;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AccuracyFeature extends ConditionalFeature {
	private static final float MIN_ACCURACY_MODIFIER = 0.1F;
	private static final float MAX_ACCURACY_MODIFIER = 10.0F;
	private float accuracyModifier;
	@Nullable private Script script;

	private AccuracyFeature(
		FeatureProvider owner, Predicate<ConditionContext> predicate, float accuracyModifier, Script script) {
		super(owner, predicate);
		this.accuracyModifier = accuracyModifier;
		this.script = script;
	}

	public MutableComponent getDescription() {
		return this.accuracyModifier < 1.0F
			? Component.translatable("description.pointblank.reducesAccuracy")
				  .append(Component.literal(String.format(" %.0f%%", 100.0F * (1.0F - this.accuracyModifier))))
			: Component.translatable("description.pointblank.increasesAccuracy")
				  .append(Component.literal(String.format(" %.0f%%", 100.0F * (this.accuracyModifier - 1.0F))));
	}

	public float getAccuracyModifier() {
		return this.accuracyModifier;
	}

	public static float getAccuracyModifier(ItemStack itemStack) {
		List<Features.EnabledFeature> enabledAccuracyFeatures =
			Features.getEnabledFeatures(itemStack, AccuracyFeature.class);
		float accuracyModifier = 1.0F;

		for (Features.EnabledFeature enabledFeature : enabledAccuracyFeatures) {
			AccuracyFeature accuracyFeature = (AccuracyFeature)enabledFeature.feature();
			// Adds more accuracy modification
			if (accuracyFeature.hasScript() && accuracyFeature.hasFunction("addAccuracyModifier"))
				accuracyModifier *=
					(float)accuracyFeature.invokeFunction("addAccuracyModifier", itemStack, accuracyFeature);
			// Replaces the base accuracy modifier
			if (accuracyFeature.hasScript() && accuracyFeature.hasFunction("getAccuracyModifier"))
				accuracyModifier *=
					(float)accuracyFeature.invokeFunction("getAccuracyModifier", itemStack, accuracyFeature);
			else
				accuracyModifier *= accuracyFeature.getAccuracyModifier();
		}

		return Mth.clamp(accuracyModifier, 0.1F, 10.0F);
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public static class Builder implements FeatureBuilder<Builder, AccuracyFeature> {
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private float accuracyModifier;
		private Script script;

		public Builder() {}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withAccuracyModifier(double accuracyModifier) {
			this.accuracyModifier = (float)accuracyModifier;
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition")) {
				this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
			}

			this.withAccuracyModifier(JsonUtil.getJsonFloat(obj, "accuracyModifier"));
			return this;
		}

		public Builder withScript(Script script) {
			this.script = script;
			return this;
		}

		public AccuracyFeature build(FeatureProvider featureProvider) {
			return new AccuracyFeature(featureProvider, this.condition, this.accuracyModifier, script);
		}
	}
}
