package mod.pbj.feature;

import com.google.gson.JsonObject;
import java.util.function.Predicate;
import mod.pbj.item.GunItem;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

;

public class ReloadFeature extends ConditionalFeature {
	private static final int MIN_AMMO_PER_RELOAD_ITERATION = 1;
	private static final int MAX_AMMO_PER_RELOAD_ITERATION = Integer.MAX_VALUE;
	private int maxAmmoPerReloadIteration;
	private @Nullable Script script;

	private ReloadFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, int maxAmmoPerReloadIteration) {
		super(owner, predicate);
		this.maxAmmoPerReloadIteration = maxAmmoPerReloadIteration;
	}

	public static int getMaxAmmoPerReloadIteration(ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof GunItem)) {
			return 0;
		} else {
			Features.EnabledFeature enabledReloadFeature =
				Features.getFirstEnabledFeature(itemStack, ReloadFeature.class);
			return enabledReloadFeature != null
				? ((ReloadFeature)enabledReloadFeature.feature()).maxAmmoPerReloadIteration
				: Integer.MAX_VALUE;
		}
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public static class Builder implements FeatureBuilder<Builder, ReloadFeature> {
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private int maxAmmoPerReloadIteration;

		public Builder() {}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withMaxAmmoPerReloadIteration(int maxAmmoPerReloadIteration) {
			this.maxAmmoPerReloadIteration = Mth.clamp(maxAmmoPerReloadIteration, 1, Integer.MAX_VALUE);
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition")) {
				this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
			}

			if (obj.has("maxAmmoPerReloadIteration")) {
				this.withMaxAmmoPerReloadIteration(JsonUtil.getJsonInt(obj, "maxAmmoPerReloadIteration"));
			}

			return this;
		}

		public ReloadFeature build(FeatureProvider featureProvider) {
			return new ReloadFeature(featureProvider, this.condition, this.maxAmmoPerReloadIteration);
		}
	}
}
