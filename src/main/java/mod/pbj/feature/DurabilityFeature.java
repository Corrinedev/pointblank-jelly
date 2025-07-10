package mod.pbj.feature;

import com.google.gson.JsonObject;
import java.util.function.Predicate;
import mod.pbj.item.GunItem;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DurabilityFeature extends ConditionalFeature {
	private static final double MIN_DEGRADATION_RATE = 1.0E-7;
	private static final double MAX_DEGRADATION_RATE = 1.0F;
	private static final int MIN_DURABILITY = 1;
	private static final int MAX_DURABILITY = Integer.MAX_VALUE;
	private final double degradationMultiplier;
	private final int durability;

	private DurabilityFeature(
		FeatureProvider owner, Predicate<ConditionContext> predicate, int durability, double hitScanDamageModifier) {
		super(owner, predicate);
		this.durability = durability;
		this.degradationMultiplier = hitScanDamageModifier;
	}

	public MutableComponent getDescription() {
		return this.degradationMultiplier < (double)1.0F
			? Component.translatable("description.pointblank.reducesDegradation")
				  .append(Component.literal(
					  String.format(" %.0f%%", (double)100.0F * ((double)1.0F - this.degradationMultiplier))))
			: Component.translatable("description.pointblank.increasesDegradation")
				  .append(Component.literal(
					  String.format(" %.0f%%", (double)100.0F * (this.degradationMultiplier - (double)1.0F))));
	}

	public static void ensureDurability(ItemStack itemStack) {
		if (itemStack.getItem() instanceof GunItem) {
			int baseMaxDurability = 0;
			double baseDegradationRate = 1.0F;
			int combinedMaxDurability = baseMaxDurability;
			double combinedDegradationRate = baseDegradationRate;

			for (Features.EnabledFeature enabledFeature :
				 Features.getEnabledFeatures(itemStack, DurabilityFeature.class)) {
				DurabilityFeature durabilityFeature = (DurabilityFeature)enabledFeature.feature();
				combinedMaxDurability += durabilityFeature.durability;
				combinedDegradationRate *= durabilityFeature.degradationMultiplier;
			}

			CompoundTag tag = itemStack.getTag();
			if (tag != null) {
				int currentMaxDurability = tag.getInt("mdu");
				if (currentMaxDurability == 0) {
					currentMaxDurability = combinedMaxDurability;
					tag.putInt("mdu", combinedMaxDurability);
				}

				if (!tag.contains("du")) {
					tag.putInt("du", combinedMaxDurability);
				}

				int currentDurability = tag.getInt("du");
				if (currentMaxDurability != combinedMaxDurability) {
					tag.putInt("mdu", Mth.clamp(combinedMaxDurability, 1, Integer.MAX_VALUE));
					int durabilityDiff = currentMaxDurability - combinedMaxDurability;
					float diffRate = (float)durabilityDiff / (float)currentMaxDurability;
					currentDurability =
						(int)((float)currentDurability * Mth.clamp(diffRate, 0.0F, (float)Integer.MAX_VALUE));
					tag.putInt("du", currentDurability);
				}

				tag.putDouble("dr", combinedDegradationRate);
			}
		}
	}

	public static void degradeDurability(ItemStack itemStack) {
		CompoundTag tag = itemStack.getTag();
		if (tag != null) {
			int currentMaxDurability = tag.getInt("mdu");
			int durability = tag.getInt("du");
			double degradationRate = tag.getDouble("dr");
			durability =
				(int)Mth.clamp((double)durability * ((double)1.0F - degradationRate), 0.0F, currentMaxDurability);
			tag.putInt("du", durability);
		}
	}

	public static float getRelativeDurability(ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof GunItem)) {
			return 0.0F;
		} else {
			CompoundTag tag = itemStack.getTag();
			if (tag == null) {
				return 0.0F;
			} else {
				int currentMaxDurability = tag.getInt("mdu");
				if (currentMaxDurability == 0) {
					return 0.0F;
				} else {
					float durability = (float)tag.getInt("du");
					return durability / (float)currentMaxDurability;
				}
			}
		}
	}

	@Override
	public @Nullable Script getScript() {
		return null;
	}

	public static class Builder implements FeatureBuilder<Builder, DurabilityFeature> {
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private double degradationMultiplier;
		private int durability;

		public Builder() {}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withDurability(int durability) {
			this.durability = durability;
			return this;
		}

		public Builder withDegradationMultiplier(double degradationMultiplier) {
			this.degradationMultiplier = degradationMultiplier;
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition")) {
				this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
			}

			this.withDegradationMultiplier(JsonUtil.getJsonFloat(obj, "degradationMultiplier"));
			return this;
		}

		public DurabilityFeature build(FeatureProvider featureProvider) {
			return new DurabilityFeature(
				featureProvider,
				this.condition,
				Mth.clamp(this.durability, 1, Integer.MAX_VALUE),
				Mth.clamp(this.degradationMultiplier, 1.0E-7, 1.0F));
		}
	}
}
