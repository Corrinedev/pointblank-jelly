package mod.pbj.feature;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Predicate;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DamageFeature extends ConditionalFeature {
	private static final float MIN_DAMAGE_MODIFIER = 0.01F;
	private static final float MAX_DAMAGE_MODIFIER = 10.0F;
	private final float hitScanDamageModifier;
	private final Component description;
	@Nullable private final Script script;

	private DamageFeature(
		FeatureProvider owner, Predicate<ConditionContext> predicate, float hitScanDamageModifier, Script script) {
		super(owner, predicate);
		this.hitScanDamageModifier = hitScanDamageModifier;
		this.script = script;
		if (hitScanDamageModifier < 1.0F) {
			this.description =
				Component.translatable("description.pointblank.reducesDamage")
					.append(Component.literal(String.format(" %.0f%%", 100.0F * (1.0F - hitScanDamageModifier))));
		} else {
			this.description =
				Component.translatable("description.pointblank.increasesDamage")
					.append(Component.literal(String.format(" %.0f%%", 100.0F * (hitScanDamageModifier - 1.0F))));
		}
	}

	public Component getDescription() {
		return this.description;
	}

	public float getHitScanDamageModifier() {
		return this.hitScanDamageModifier;
	}

	public static float getHitScanDamageModifier(ItemStack itemStack) {
		List<Features.EnabledFeature> enabledDamageFeatures =
			Features.getEnabledFeatures(itemStack, DamageFeature.class);
		float hitScanDamageModifier = 1.0F;

		for (Features.EnabledFeature enabledFeature : enabledDamageFeatures) {
			DamageFeature damageFeature = (DamageFeature)enabledFeature.feature();
			// Adds more damage modification
			if (damageFeature.hasScript() && damageFeature.hasFunction("addDamageModifier"))
				hitScanDamageModifier *=
					(float)damageFeature.invokeFunction("addDamageModifier", itemStack, damageFeature);
			// Replaces the base damage modifier
			if (damageFeature.hasScript() && damageFeature.hasFunction("getDamageModifier"))
				hitScanDamageModifier *=
					(float)damageFeature.invokeFunction("getDamageModifier", itemStack, damageFeature);
			else
				hitScanDamageModifier *= damageFeature.getHitScanDamageModifier();
		}

		return Mth.clamp(hitScanDamageModifier, 0.01F, 10.0F);
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public static class Builder implements FeatureBuilder<Builder, DamageFeature> {
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private float hitScanDamageModifier;
		private Script script;

		public Builder() {}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withHitScanDamageModifier(double damageModifier) {
			this.hitScanDamageModifier = (float)damageModifier;
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

			this.withHitScanDamageModifier(JsonUtil.getJsonFloat(obj, "hitScanDamageModifier"));
			this.withScript(JsonUtil.getJsonScript(obj));
			return this;
		}

		public DamageFeature build(FeatureProvider featureProvider) {
			return new DamageFeature(featureProvider, this.condition, this.hitScanDamageModifier, script);
		}
	}
}
