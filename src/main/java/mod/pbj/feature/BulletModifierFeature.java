package mod.pbj.feature;

import com.google.gson.JsonObject;
import java.util.function.Predicate;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import org.jetbrains.annotations.Nullable;

public class BulletModifierFeature extends ConditionalFeature {
	private final float velocity;
	private final float gravity;
	private final float inaccuracy;
	private final float speedOffset;
	private final float maxSpeedOffset;
	private final Script script;

	public BulletModifierFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> predicate,
		float velocity,
		float gravity,
		float inaccuracy,
		float speedOffset,
		float maxSpeedOffset,
		Script script) {
		super(owner, predicate);
		this.velocity = velocity;
		this.gravity = gravity;
		this.inaccuracy = inaccuracy;
		this.speedOffset = speedOffset;
		this.maxSpeedOffset = maxSpeedOffset;
		this.script = script;
	}

	public float getVelocityModifier() {
		return velocity;
	}

	public float getGravityModifier() {
		return gravity;
	}

	public float getInaccuracyModifier() {
		return inaccuracy;
	}

	public float getSpeedOffsetModifier() {
		return speedOffset;
	}

	public float getMaxSpeedOffsetModifier() {
		return maxSpeedOffset;
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public static class Builder implements FeatureBuilder<Builder, BulletModifierFeature> {
		private Predicate<ConditionContext> predicate = (ctx) -> true;
		private float velocity = 0f;
		private float gravity = 0f;
		private float inaccuracy = 0f;
		private float speedOffset = 0f;
		private float maxSpeedOffset = 0f;
		private Script script = null;

		public Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition"))
				this.predicate = Conditions.fromJson(obj.getAsJsonObject("condition"));

			this.velocity = JsonUtil.getJsonFloat(obj, "velocity", 0f);
			this.gravity = JsonUtil.getJsonFloat(obj, "gravity", 0f);
			this.inaccuracy = JsonUtil.getJsonFloat(obj, "inaccuracy", 0f);
			this.speedOffset = JsonUtil.getJsonFloat(obj, "speedOffset", 0f);
			this.maxSpeedOffset = JsonUtil.getJsonFloat(obj, "maxSpeedOffset", 0f);
			this.script = JsonUtil.getJsonScript(obj);

			return this;
		}

		@Override
		public BulletModifierFeature build(FeatureProvider owner) {
			return new BulletModifierFeature(
				owner, predicate, velocity, gravity, inaccuracy, speedOffset, maxSpeedOffset, script);
		}
	}
}
