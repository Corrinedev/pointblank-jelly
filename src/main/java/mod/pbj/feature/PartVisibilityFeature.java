package mod.pbj.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.world.level.ItemLike;

public class PartVisibilityFeature implements Feature {
	private final FeatureProvider owner;
	private final Map<String, Predicate<ConditionContext>> predicates;

	private PartVisibilityFeature(FeatureProvider owner, Map<String, Predicate<ConditionContext>> partPredicates) {
		this.owner = owner;
		this.predicates = Collections.unmodifiableMap(partPredicates);
	}

	public FeatureProvider getOwner() {
		return this.owner;
	}

	public boolean isPartVisible(ItemLike partOwner, String partName, ConditionContext conditionContext) {
		if (partOwner != this.owner) {
			return true;
		} else {
			Predicate<ConditionContext> bonePredicate = this.predicates.get(partName);
			if (bonePredicate == null) {
				return true;
			} else {
				return bonePredicate.test(conditionContext);
			}
		}
	}

	public static class Builder implements FeatureBuilder<Builder, PartVisibilityFeature> {
		private final Map<String, Predicate<ConditionContext>> partPredicates = new HashMap<>();

		public Builder withShownPart(String partName, Predicate<ConditionContext> condition) {
			if (this.partPredicates.put(partName, condition) != null) {
				throw new IllegalArgumentException("Duplicate part: " + partName);
			} else {
				return this;
			}
		}

		public Builder withHiddenPart(String partName, Predicate<ConditionContext> condition) {
			if (this.partPredicates.put(partName, condition.negate()) != null) {
				throw new IllegalArgumentException("Duplicate part: " + partName);
			} else {
				return this;
			}
		}

		public Builder withJsonObject(JsonObject obj) {
			for (JsonObject partObj : JsonUtil.getJsonObjects(obj, "parts")) {
				String partName = JsonUtil.getJsonString(partObj, "name");
				boolean isVisible = JsonUtil.getJsonBoolean(partObj, "visible", true);
				JsonElement conditionObj = partObj.get("condition");
				Predicate<ConditionContext> condition = Conditions.fromJson(conditionObj);
				this.withShownPart(partName, isVisible ? condition : condition.negate());
			}

			return this;
		}

		public PartVisibilityFeature build(FeatureProvider featureProvider) {
			return new PartVisibilityFeature(featureProvider, this.partPredicates);
		}
	}
}
