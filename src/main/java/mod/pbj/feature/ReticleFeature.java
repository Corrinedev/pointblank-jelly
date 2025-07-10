package mod.pbj.feature;

import com.google.gson.JsonObject;
import java.util.function.Predicate;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class ReticleFeature extends ConditionalFeature {
	public static final float DEFAULT_MAX_ANGULAR_OFFSET_DEGREES = 5.0F;
	public static final float DEFAULT_MAX_ANGULAR_OFFSET_COS = Mth.cos(0.08726646F);
	private final ResourceLocation texture;
	private final boolean isParallaxEnabled;
	private final float maxAngularOffsetCos;
	private @Nullable Script script;

	private ReticleFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> condition,
		ResourceLocation texture,
		boolean isParallaxEnabled,
		float maxAngularOffsetCos,
		Script script) {
		super(owner, condition);
		this.texture = texture;
		this.isParallaxEnabled = isParallaxEnabled;
		this.maxAngularOffsetCos = maxAngularOffsetCos;
	}

	/// Check <code>ReticleItemLayer</code> for more information on script usage within ReticleFeature,
	///  or follow this method's only usage
	public ResourceLocation getTexture() {
		return this.texture;
	}

	public boolean isParallaxEnabled() {
		return this.isParallaxEnabled;
	}

	public float getMaxAngularOffsetCos() {
		return this.maxAngularOffsetCos;
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public static class Builder implements FeatureBuilder<Builder, ReticleFeature> {
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private boolean isParallaxEnabled;
		private float maxAngularOffsetCos;
		private ResourceLocation texture;
		private Script script;

		public Builder() {
			this.maxAngularOffsetCos = ReticleFeature.DEFAULT_MAX_ANGULAR_OFFSET_COS;
		}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withTexture(String texture) {
			this.texture = new ResourceLocation("pointblank", texture);
			return this;
		}

		public Builder withParallaxEnabled(boolean isParallaxEnabled) {
			this.isParallaxEnabled = isParallaxEnabled;
			return this;
		}

		public Builder withMaxAngularOffset(float maxAngularOffsetDegrees) {
			this.maxAngularOffsetCos =
				Mth.cos(((float)Math.PI / 180F) * Mth.clamp(maxAngularOffsetDegrees, 0.0F, 45.0F));
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
			this.withScript(JsonUtil.getJsonScript(obj));
			this.isParallaxEnabled = JsonUtil.getJsonBoolean(obj, "parallax", false);
			if (obj.has("texture")) {
				this.withTexture(JsonUtil.getJsonString(obj, "texture"));
			} else if (this.isParallaxEnabled) {
				this.withTexture("textures/item/reticle4.png");
			}

			this.withMaxAngularOffset(JsonUtil.getJsonFloat(obj, "maxAngularOffset", 5.0F));
			return this;
		}

		public ReticleFeature build(FeatureProvider featureProvider) {
			ResourceLocation texture = this.texture;
			if (texture == null) {
				if (this.isParallaxEnabled) {
					texture = new ResourceLocation("pointblank", "textures/item/reticle4.png");
				} else {
					texture = new ResourceLocation("pointblank", "textures/item/reticle.png");
				}
			}

			return new ReticleFeature(
				featureProvider,
				this.condition,
				texture,
				this.isParallaxEnabled,
				this.maxAngularOffsetCos,
				this.script);
		}
	}
}
