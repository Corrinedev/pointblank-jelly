package mod.pbj.feature;

import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ActiveMuzzleFeature extends ConditionalFeature {
	private final Map<String, Predicate<ConditionContext>> muzzleParts;

	private ActiveMuzzleFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> predicate,
		Map<String, Predicate<ConditionContext>> muzzleParts) {
		super(owner, predicate);
		this.muzzleParts = muzzleParts;
	}

	public MutableComponent getDescription() {
		return Component.empty();
	}

	public static boolean isActiveMuzzle(
		ItemStack rootStack, ItemStack currentStack, ItemDisplayContext itemDisplayContext, String partName) {
		Item item = currentStack.getItem();
		if (item instanceof FeatureProvider featureProvider) {
			ActiveMuzzleFeature feature = featureProvider.getFeature(ActiveMuzzleFeature.class);
			if (feature == null) {
				return rootStack == currentStack;
			} else {
				Predicate<ConditionContext> predicate = feature.muzzleParts.get(partName);
				if (predicate == null) {
					predicate = feature.predicate;
				}

				return feature.predicate.test(
					new ConditionContext(null, rootStack, currentStack, null, itemDisplayContext));
			}
		} else {
			return rootStack == currentStack;
		}
	}

	@Override
	public @Nullable Script getScript() {
		return null;
	}

	public static class Builder implements FeatureBuilder<Builder, ActiveMuzzleFeature> {
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private final Map<String, Predicate<ConditionContext>> muzzleParts = new HashMap<>();

		public Builder() {}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withPart(String partName, Predicate<ConditionContext> condition) {
			this.muzzleParts.put(partName, condition);
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition")) {
				this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
			}

			for (JsonObject partObj : JsonUtil.getJsonObjects(obj, "parts")) {
				String partName = JsonUtil.getJsonString(partObj, "name");
				Predicate<ConditionContext> condition;
				if (partObj.has("condition")) {
					JsonObject conditionObj = partObj.getAsJsonObject("condition");
					condition = Conditions.fromJson(conditionObj);
				} else {
					condition = (ctx) -> true;
				}

				this.withPart(partName, condition);
			}

			return this;
		}

		public ActiveMuzzleFeature build(FeatureProvider featureProvider) {
			Map<String, Predicate<ConditionContext>> muzzleParts = new HashMap<>(this.muzzleParts);
			if (muzzleParts.isEmpty()) {
				muzzleParts.put("muzzleflash", this.condition);
				muzzleParts.put("muzzle", this.condition);
			}

			return new ActiveMuzzleFeature(featureProvider, this.condition, Collections.unmodifiableMap(muzzleParts));
		}
	}
}
