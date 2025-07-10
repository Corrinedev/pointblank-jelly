package mod.pbj.feature;

import com.google.gson.JsonObject;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AmmoCapacityFeature extends ConditionalFeature {
	private static final int MIN_AMMO = 1;
	private static final int MAX_AMMO = Integer.MAX_VALUE;
	private IntUnaryOperator ammoCapacityTransformer;
	private Component description;
	private @Nullable Script script;

	private AmmoCapacityFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> predicate,
		IntUnaryOperator ammoCapacityTransformer,
		Component description,
		Script script) {
		super(owner, predicate);
		this.description = description;
		this.ammoCapacityTransformer = ammoCapacityTransformer;
		this.script = script;
	}

	public Component getDescription() {
		return this.description;
	}

	public static int modifyAmmoCapacity(ItemStack itemStack, int ammoCapacity) {
		Features.EnabledFeature enabledExtendedAmmoFeature =
			Features.getFirstEnabledFeature(itemStack, AmmoCapacityFeature.class);
		if (enabledExtendedAmmoFeature != null &&
			enabledExtendedAmmoFeature.feature() instanceof AmmoCapacityFeature feature &&
			feature.hasFunction("modifyAmmoCapacity")) {
			return (int)feature.invokeFunction("modifyAmmoCapacity", ammoCapacity, itemStack, feature);
		}
		return enabledExtendedAmmoFeature != null ? ((AmmoCapacityFeature)enabledExtendedAmmoFeature.feature())
														.ammoCapacityTransformer.applyAsInt(ammoCapacity)
												  : ammoCapacity;
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public static class Builder implements FeatureBuilder<Builder, AmmoCapacityFeature> {
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private IntUnaryOperator ammoCapacityTransformer;
		private Component description;
		private Script script;

		public Builder() {}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withAmmoCapacityModifier(float ammoCapacityModifier) {
			this.ammoCapacityTransformer =
				(ammo) -> Mth.clamp(Math.round(ammo * ammoCapacityModifier), 1, Integer.MAX_VALUE);
			this.description = Component.translatable("description.pointblank.extendsAmmoCapacity")
								   .append(Component.literal(String.format(" %d%%", (ammoCapacityModifier - 1) * 100)));
			return this;
		}

		public Builder withAmmoCapacity(int ammoCapacity) {
			this.ammoCapacityTransformer = (ammo) -> Mth.clamp(ammoCapacity, 1, Integer.MAX_VALUE);
			this.description = Component.translatable("description.pointblank.changesAmmoCapacity")
								   .append(Component.literal(" " + ammoCapacity));
			return this;
		}

		public Builder withScript(Script script) {
			this.script = script;
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition")) {
				this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
			}

			if (obj.has("ammoCapacityModifier")) {
				this.withAmmoCapacityModifier(JsonUtil.getJsonFloat(obj, "ammoCapacityModifier"));
			} else if (obj.has("ammoCapacity")) {
				this.withAmmoCapacity(JsonUtil.getJsonInt(obj, "ammoCapacity"));
			}
			this.withScript(JsonUtil.getJsonScript(obj));
			return this;
		}

		public AmmoCapacityFeature build(FeatureProvider featureProvider) {
			if (this.ammoCapacityTransformer == null) {
				throw new IllegalStateException("Either ammoCapacity ammoCapacityModifier must be set");
			} else {
				return new AmmoCapacityFeature(
					featureProvider, this.condition, this.ammoCapacityTransformer, this.description, this.script);
			}
		}
	}
}
