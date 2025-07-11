package mod.pbj.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import java.util.NavigableMap;
import java.util.function.Predicate;
import mod.pbj.attachment.AttachmentCategory;
import mod.pbj.attachment.AttachmentModelInfo;
import mod.pbj.attachment.Attachments;
import mod.pbj.client.GunStateListener;
import mod.pbj.feature.math.FeatureVariables;
import mod.pbj.item.GunItem;
import mod.pbj.registry.ExtensionRegistry;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class AimingFeature extends ConditionalFeature implements GunStateListener {
	private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();
	private Matrix4f aimMatrix;
	private final float zoom;
	private final float viewBobbing;
	private final Script script;
	@Nullable
	private String zoomExpression;

	@OnlyIn(Dist.CLIENT)
	public static void
	applyAimingPosition(ItemStack itemStack, PoseStack poseStack, float rescale, float aimingProgress) {
		if (itemStack.getItem() instanceof GunItem) {
			if (aimingProgress > 0.0F) {
				NavigableMap<String, Pair<ItemStack, Matrix4f>> poseMatrices =
					AttachmentModelInfo.findInverseBoneMatrices(itemStack, "scope", rescale);
				if (!poseMatrices.isEmpty()) {
					Features.EnabledFeature aimingFeature =
						Features.getFirstEnabledFeature(itemStack, AimingFeature.class);
					Pair<ItemStack, Matrix4f> attachmentPos = null;
					if (aimingFeature != null) {
						if (aimingFeature.feature() instanceof AimingFeature feature && feature.aimMatrix != null)
							attachmentPos = Pair.of(itemStack, feature.aimMatrix);
						else
							attachmentPos = poseMatrices.get(aimingFeature.ownerPath());
					}

					if (attachmentPos == null) {
						attachmentPos = poseMatrices.firstEntry().getValue();
					}

					if (attachmentPos == null) {
						return;
					}

					if (aimingProgress < 1.0F) {
						poseStack.mulPoseMatrix(AimingFeature.AimSwitchAnimation.INSTANCE.update(
							IDENTITY_MATRIX, attachmentPos.getSecond(), aimingProgress));
					} else {
						poseStack.mulPoseMatrix(
							AimingFeature.AimSwitchAnimation.INSTANCE.update(attachmentPos.getSecond()));
					}
				}

				poseStack.translate(0.0F, aimingProgress * -0.6095F * rescale, aimingProgress * -0.7F * rescale);
			}
		}
	}

	private AimingFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> predicate,
		float zoom,
		float viewBobbing,
		Matrix4f aimMatrix,
		@Nullable String zoomExpression,
		Script script) {
		super(owner, predicate, FeatureVariables.createDefaults(
				"health",
				"maxhealth",
				"speed",
				"damage"
		));
		this.zoom = zoom;
		this.viewBobbing = viewBobbing;
		this.script = script;
		this.aimMatrix = aimMatrix;
		this.zoomExpression = zoomExpression;
	}

	public MutableComponent getDescription() {
		return Component.translatable("description.pointblank.enablesAimingWithZoom")
			.append(Component.literal(String.format(" %.0f%%", this.zoom * 100.0F)));
	}

	public float getZoom() {
		return (float) getMathValue(zoom, zoomExpression);
	}

	public float getViewBobbing() {
		return this.viewBobbing;
	}

	public static float getZoom(ItemStack itemStack, Player player) {
		Pair<String, ItemStack> selected = Attachments.getSelectedAttachment(itemStack, AttachmentCategory.SCOPE);
		ItemStack selectedStack = null;
		if (selected != null) {
			selectedStack = selected.getSecond();
		} else {
			selectedStack = itemStack;
		}

		Item item = selectedStack.getItem();
		if (item instanceof FeatureProvider fp) {
			AimingFeature feature = fp.getFeature(AimingFeature.class);
			if (feature != null) {
				feature.mathParser.set("health", player.getHealth());
				feature.mathParser.set("maxhealth", player.getMaxHealth());
				feature.mathParser.set("speed", player.getDeltaMovement().normalize().horizontalDistance());
				feature.mathParser.set("damage", GunItem.getFireModeInstance(itemStack).getDamage());

				if (feature.hasScript() && feature.hasFunction("getZoom"))
					return (float)feature.invokeFunction("getZoom", itemStack, feature);
				return feature.getZoom();
			}
		} else {
			item = selectedStack.getItem();
			if (item instanceof GunItem gunItem) {
				return (float)gunItem.getAimingZoom();
			}
		}

		return 0.0F;
	}

	public static float getViewBobbing(ItemStack itemStack) {
		Pair<String, ItemStack> selected = Attachments.getSelectedAttachment(itemStack, AttachmentCategory.SCOPE);
		ItemStack selectedStack = null;
		if (selected != null) {
			selectedStack = selected.getSecond();
		} else {
			selectedStack = itemStack;
		}

		Item item = selectedStack.getItem();
		if (item instanceof FeatureProvider fp) {
			AimingFeature feature = fp.getFeature(AimingFeature.class);
			if (feature != null) {
				return feature.getViewBobbing();
			}
		}

		return 1.0F;
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	@OnlyIn(Dist.CLIENT)
	private static class AimSwitchAnimation {
		private static final AimSwitchAnimation INSTANCE = new AimSwitchAnimation(200L);
		protected long startTime;
		protected long nanoDuration;
		protected boolean isDone;
		protected Matrix4f fromMatrix;
		protected Matrix4f toMatrix;

		protected AimSwitchAnimation(long durationMillis) {
			this.nanoDuration = durationMillis * 1000000L;
			this.fromMatrix = AimingFeature.IDENTITY_MATRIX;
			this.toMatrix = AimingFeature.IDENTITY_MATRIX;
		}

		protected float getProgress() {
			double progress = (double)(System.nanoTime() - this.startTime) / (double)this.nanoDuration;
			if (progress > (double)1.0F) {
				progress = 1.0F;
			}

			return Mth.clamp((float)progress, 0.0F, 1.0F);
		}

		public void reset() {
			this.isDone = false;
			this.startTime = System.nanoTime();
		}

		public Matrix4f update(Matrix4f matrix) {
			if (this.toMatrix != matrix) {
				this.fromMatrix = this.toMatrix;
				this.toMatrix = matrix;
				this.reset();
			}

			float progress = this.getProgress();
			if (progress >= 1.0F) {
				this.isDone = true;
			}

			return this.isDone ? this.toMatrix : (new Matrix4f(this.fromMatrix)).lerp(this.toMatrix, progress);
		}

		public Matrix4f update(Matrix4f fromMatrix, Matrix4f toMatrix, float progress) {
			Matrix4f resultMatrix = (new Matrix4f(fromMatrix)).lerp(toMatrix, progress);
			INSTANCE.fromMatrix = fromMatrix;
			INSTANCE.toMatrix = toMatrix;
			return resultMatrix;
		}
	}

	public static class Builder implements FeatureBuilder<Builder, AimingFeature> {
		private static final float DEFAULT_ZOOM = 0.1F;
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private float zoom;
		private float viewBobbing = 1.0F;
		public ExtensionRegistry.Extension extension;
		private Script script;
		private Matrix4f aimMatrix;
		@Nullable
		private String zoomExpression;

		public Builder() {}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withZoom(JsonElement zoom) {
			if(zoom == null) {
				this.zoom = 0.1f;
				return this;
			}
			if(zoom.isJsonPrimitive()) {
				JsonPrimitive primitive = (JsonPrimitive) zoom;
				if(primitive.isNumber())
					this.zoom = primitive.getAsFloat();
				else if(primitive.isString())
					this.zoomExpression = primitive.getAsString();
			}
			return this;
		}

		public Builder withAimMatrix(Matrix4f aimMatrix) {
			this.aimMatrix = aimMatrix;
			return this;
		}

		public Builder withViewBobbing(double viewBobbing) {
			this.viewBobbing = Mth.clamp((float)viewBobbing, 0.0F, 1.0F);
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

			this.withZoom(obj.get("zoom"));
			this.withViewBobbing(JsonUtil.getJsonFloat(obj, "viewBobbing", 1.0F));
			this.withScript(JsonUtil.getJsonScript(obj));
			return this;
		}

		public AimingFeature build(FeatureProvider featureProvider) {
			return new AimingFeature(
				featureProvider, this.condition, this.zoom, this.viewBobbing, this.aimMatrix, this.zoomExpression, script);
		}

		public Builder withPrimitiveZoom(float aimingZoom) {
			this.zoom = aimingZoom;
			return this;
		}
	}
}
