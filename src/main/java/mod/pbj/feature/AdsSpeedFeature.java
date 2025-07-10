package mod.pbj.feature;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Predicate;
import mod.pbj.client.GunClientState;
import mod.pbj.item.GunItem;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class AdsSpeedFeature extends ConditionalFeature {
	private float adsMultiplier;
	@Nullable private Script script;

	public AdsSpeedFeature(
		FeatureProvider owner, Predicate<ConditionContext> predicate, float adsMultiplier, Script script) {
		super(owner, predicate);
		this.adsMultiplier = adsMultiplier;
		this.script = script;
	}

	@OnlyIn(Dist.CLIENT)
	public static long getTotalAdsSpeed(ItemStack itemStack, Player player, GunClientState state) {
		if (!(itemStack.getItem() instanceof GunItem))
			return 0L;
		List<Features.EnabledFeature> enabledAccuracyFeatures =
			Features.getEnabledFeatures(itemStack, AdsSpeedFeature.class);
		float adsModifier = 1.0F;

		for (Features.EnabledFeature enabledFeature : enabledAccuracyFeatures) {
			AdsSpeedFeature adsFeature = (AdsSpeedFeature)enabledFeature.feature();
			if (adsFeature.predicate.test(new ConditionContext(player, itemStack, state))) {
				// Adds more accuracy modification
				if (adsFeature.hasScript() && adsFeature.hasFunction("addAdsSpeedModifier"))
					adsModifier *= (float)adsFeature.invokeFunction("addAdsSpeedModifier", itemStack, adsFeature);
				// Replaces the base accuracy modifier
				if (adsFeature.hasScript() && adsFeature.hasFunction("getAdsSpeedModifier"))
					adsModifier *= (float)adsFeature.invokeFunction("getAdsSpeedModifier", itemStack, adsFeature);
				else
					adsModifier *= adsFeature.getAdsMultiplier();
			}
		}
		return Math.round(((GunItem)itemStack.getItem()).adsSpeed * adsModifier);
	}

	public float getAdsMultiplier() {
		return adsMultiplier;
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public static class Builder implements FeatureBuilder<AdsSpeedFeature.Builder, AdsSpeedFeature> {
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private float adsSpeedMultiplier;
		private Script script;

		public Builder() {}

		public AdsSpeedFeature.Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public AdsSpeedFeature.Builder withAdsSpeedMultiplier(double adsSpeed) {
			this.adsSpeedMultiplier = (float)adsSpeed;
			return this;
		}

		public AdsSpeedFeature.Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition")) {
				this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
			}

			this.withAdsSpeedMultiplier(JsonUtil.getJsonFloat(obj, "adsModifier"));
			return this;
		}

		public AdsSpeedFeature.Builder withScript(Script script) {
			this.script = script;
			return this;
		}

		public AdsSpeedFeature build(FeatureProvider featureProvider) {
			return new AdsSpeedFeature(featureProvider, this.condition, this.adsSpeedMultiplier, script);
		}
	}
}
