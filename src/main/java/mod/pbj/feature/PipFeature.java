package mod.pbj.feature;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import java.util.function.Predicate;
import mod.pbj.attachment.AttachmentCategory;
import mod.pbj.attachment.Attachments;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class PipFeature extends ConditionalFeature {
	private final float zoom;
	private final ResourceLocation overlayTexture;
	private final ResourceLocation maskTexture;
	private final boolean isParallaxEnabled;
	private @Nullable Script script;

	private PipFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> condition,
		float zoom,
		boolean isParallaxEnabled,
		ResourceLocation overlayTexture,
		ResourceLocation maskTexture,
		Script script) {
		super(owner, condition);
		this.zoom = zoom;
		this.script = script;
		this.isParallaxEnabled = isParallaxEnabled;
		this.overlayTexture = overlayTexture;
		this.maskTexture = maskTexture;
	}

	public MutableComponent getDescription() {
		return Component.translatable("description.pointblank.enablesPipWithZoom")
			.append(Component.literal(String.format(" %.0f%%", this.zoom * 100.0F)));
	}

	public float getZoom() {
		return this.zoom;
	}

	public ResourceLocation getMaskTexture() {
		return this.maskTexture;
	}

	public boolean isParallaxEnabled() {
		return this.isParallaxEnabled;
	}

	public ResourceLocation getOverlayTexture() {
		return this.overlayTexture;
	}

	public static Optional<Float> getZoom(ItemStack itemStack) {
		Pair<String, ItemStack> selected = Attachments.getSelectedAttachment(itemStack, AttachmentCategory.SCOPE);
		ItemStack selectedStack;
		if (selected != null) {
			selectedStack = selected.getSecond();
		} else {
			selectedStack = itemStack;
		}

		Item item = selectedStack.getItem();
		if (item instanceof FeatureProvider fp) {
			PipFeature feature = fp.getFeature(PipFeature.class);
			if (feature != null) {
				if (feature.hasFunction("getPipZoom"))
					return Optional.of((float)feature.invokeFunction("getPipZoom", itemStack, feature));

				return Optional.of(feature.getZoom());
			}
		}

		return Optional.empty();
	}

	public static ResourceLocation getMaskTexture(ItemStack itemStack) {
		Pair<String, ItemStack> selected = Attachments.getSelectedAttachment(itemStack, AttachmentCategory.SCOPE);
		ItemStack selectedStack;
		if (selected != null) {
			selectedStack = selected.getSecond();
		} else {
			selectedStack = itemStack;
		}

		Item item = selectedStack.getItem();
		if (item instanceof FeatureProvider fp) {
			PipFeature feature = fp.getFeature(PipFeature.class);
			if (feature != null) {
				if (feature.hasFunction("getMaskTexture"))
					return (ResourceLocation)feature.invokeFunction("getMaskTexture", itemStack, feature);
				return feature.getMaskTexture();
			}
		}

		return null;
	}

	public static PipFeature getSelected(ItemStack itemStack) {
		Pair<String, ItemStack> selected = Attachments.getSelectedAttachment(itemStack, AttachmentCategory.SCOPE);
		ItemStack selectedStack;
		if (selected != null) {
			selectedStack = selected.getSecond();
			Item var4 = selectedStack.getItem();
			if (var4 instanceof FeatureProvider fp) {
				return fp.getFeature(PipFeature.class);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public static class Builder implements FeatureBuilder<Builder, PipFeature> {
		private static final float DEFAULT_ZOOM = 0.9F;
		private static final String DEFAULT_MASK_TEXTURE = "textures/gui/pip_mask_solid_rect.png";
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private ResourceLocation overlayTexture;
		private ResourceLocation maskTexture;
		private float zoom;
		private boolean isParallaxEnabled;
		private Script script;

		public Builder() {}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withZoom(double zoom) {
			this.zoom = (float)zoom;
			return this;
		}

		public Builder withOverlayTexture(String texture) {
			this.overlayTexture = new ResourceLocation("pointblank", texture);
			return this;
		}

		public Builder withMaskTexture(String texture) {
			this.maskTexture = new ResourceLocation("pointblank", texture);
			return this;
		}

		public Builder withParallaxEnabled(boolean isParallaxEnabled) {
			this.isParallaxEnabled = isParallaxEnabled;
			return this;
		}

		public Builder withScript(Script script) {
			this.script = script;
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition")) {
				this.withCondition(Conditions.fromJson(obj.get("condition")));
			}
			this.withScript(JsonUtil.getJsonScript(obj));
			this.isParallaxEnabled = JsonUtil.getJsonBoolean(obj, "parallax", false);
			this.isParallaxEnabled = JsonUtil.getJsonBoolean(obj, "parallax", false);
			String overlayTextureName = JsonUtil.getJsonString(obj, "overlayTexture", null);
			if (overlayTextureName != null) {
				this.withOverlayTexture(overlayTextureName);
			}

			String maskTextureName = JsonUtil.getJsonString(obj, "maskTexture", null);
			if (maskTextureName == null && this.isParallaxEnabled) {
				maskTextureName = "textures/gui/pip_mask_solid_rect.png";
			}

			if (maskTextureName != null) {
				this.withMaskTexture(maskTextureName);
			}

			this.withZoom(JsonUtil.getJsonFloat(obj, "zoom", 0.9F));
			return this;
		}

		public PipFeature build(FeatureProvider featureProvider) {
			ResourceLocation maskTexture = this.maskTexture;
			if (maskTexture == null && this.isParallaxEnabled) {
				maskTexture = new ResourceLocation("pointblank", "textures/gui/pip_mask_solid_rect.png");
			}

			return new PipFeature(
				featureProvider,
				this.condition,
				this.zoom,
				this.isParallaxEnabled,
				this.overlayTexture,
				maskTexture,
				this.script);
		}
	}
}
