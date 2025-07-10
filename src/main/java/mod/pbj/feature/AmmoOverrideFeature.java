package mod.pbj.feature;

import com.google.gson.JsonObject;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.registry.ItemRegistry;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public class AmmoOverrideFeature extends ConditionalFeature {
	private final String ammoId;
	private final boolean overrideOnly;
	private Item cachedItem = null;

	public AmmoOverrideFeature(
		FeatureProvider owner, Predicate<ConditionContext> condition, String ammoId, boolean overrideOnly) {
		super(owner, condition);
		this.ammoId = ammoId;
		this.overrideOnly = overrideOnly;
	}

	@Override
	public @Nullable Script getScript() {
		return null;
	}

	public Item getOverrideAmmo() {
		if (cachedItem == null) {
			Supplier<Item> supplier = ItemRegistry.ITEMS.getDeferredRegisteredObject(ammoId);
			if (supplier == null || supplier.get() == null) {
				throw new IllegalStateException(
					"AmmoOverrideFeature: Unable to resolve ammo '" + ammoId + "' at runtime");
			}
			cachedItem = supplier.get();
		}
		return cachedItem;
	}

	public boolean isOverrideOnly() {
		return overrideOnly;
	}

	public static class Builder implements FeatureBuilder<Builder, AmmoOverrideFeature> {
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private String ammoId;
		private boolean overrideOnly = false;

		@Override
		public Builder withJsonObject(JsonObject obj) {
			this.ammoId = JsonUtil.getJsonString(obj, "ammo");

			if (obj.has("overrideOnly")) {
				this.overrideOnly = obj.get("overrideOnly").getAsBoolean();
			}

			if (obj.has("condition")) {
				this.condition = Conditions.fromJson(obj.getAsJsonObject("condition"));
			}

			return this;
		}

		@Override
		public AmmoOverrideFeature build(FeatureProvider owner) {
			return new AmmoOverrideFeature(owner, condition, ammoId, overrideOnly);
		}
	}
}
