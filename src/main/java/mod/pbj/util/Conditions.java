package mod.pbj.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.attachment.Attachment;
import mod.pbj.attachment.AttachmentCategory;
import mod.pbj.attachment.Attachments;
import mod.pbj.feature.ConditionContext;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class Conditions {
	public static final Predicate<ConditionContext> RANDOM_PICK = (ctx) -> ctx.randomSample2() == ctx.randomSample1();

	public Conditions() {}

	public static Predicate<ConditionContext> isGunOnGround() {
		return (ctx) -> ctx.itemDisplayContext() == ItemDisplayContext.GROUND;
	}

	public static Predicate<ConditionContext> aiming() {
		return (ctx) -> ctx.rootStack().getOrCreateTag().getBoolean("aim");
	}

	public static Predicate<ConditionContext> isGunInHands() {
		return (ctx) -> ctx.itemDisplayContext() != ItemDisplayContext.GROUND;
	}

	public static Predicate<ConditionContext> selectedFireMode(String fireModeName) {
		return (ctx) -> {
			if (ctx.rootStack() == null) {
				return false;
			} else {
				FireModeInstance fireModeInstance = GunItem.getFireModeInstance(ctx.rootStack());
				return fireModeInstance != null &&
					Objects.equals(
						fireModeInstance.getName().toUpperCase(Locale.ROOT), fireModeName.toUpperCase(Locale.ROOT));
			}
		};
	}

	public static Predicate<ConditionContext> unselectedFireMode(String fireModeName) {
		return selectedFireMode(fireModeName).negate();
	}

	public static Predicate<ConditionContext> isUsingDefaultMuzzle() {
		return (ctx) -> {
			if (ctx.rootStack() == null) {
				return false;
			} else {
				FireModeInstance fireModeInstance = GunItem.getFireModeInstance(ctx.rootStack());
				return fireModeInstance == null || fireModeInstance.isUsingDefaultMuzzle();
			}
		};
	}

	public static Predicate<ConditionContext> hasAmmoCount(int ammoCount) {
		return (ctx) -> ammoCount == ctx.gunClientState().getAmmoCount(GunItem.getFireModeInstance(ctx.rootStack()));
	}

	public static Predicate<ConditionContext> onReloadIteration(int index) {
		return (ctx) -> index == ctx.gunClientState().getReloadIterationIndex();
	}

	public static Predicate<ConditionContext> onEmptyReload() {
		return (ctx) -> ctx.gunClientState().getReloadIterationIndex() == 0;
	}

	public static Predicate<ConditionContext> onNonEmptyReload() {
		return (ctx) -> ctx.gunClientState().getReloadIterationIndex() > 0;
	}

	public static Predicate<ConditionContext> beforePreparingReload() {
		return onReloadIteration(-1);
	}

	public static Predicate<ConditionContext> afterPreparingReload() {
		return (ctx) -> ctx.gunClientState().getReloadIterationIndex() >= 0;
	}

	public static Predicate<ConditionContext> hasAttachment(Supplier<? extends Attachment> attachmentSupplier) {
		return (ctx) -> {
			NavigableMap<String, ItemStack> attachments = Attachments.getAttachments(ctx.currentItemStack(), false);
			String var10001 = attachmentSupplier.get().getName();
			return attachments.get("//" + var10001) != null;
		};
	}

	public static Predicate<ConditionContext> doesNotHaveAttachment(Supplier<? extends Attachment> attachmentSupplier) {
		return hasAttachment(attachmentSupplier).negate();
	}

	public static Predicate<ConditionContext> hasAttachment(String attachmentName) {
		return (ctx) -> {
			NavigableMap<String, ItemStack> attachments = Attachments.getAttachments(ctx.currentItemStack(), false);
			return attachments.get("//" + attachmentName) != null;
		};
	}

	public static Predicate<ConditionContext> doesNotHaveAttachment(String attachmentName) {
		return hasAttachment(attachmentName).negate();
	}

	public static Predicate<ConditionContext> hasAttachmentGroup(String attachmentGroup) {
		return (ctx) -> Attachments.getAttachmentGroups(ctx.currentItemStack()).containsKey(attachmentGroup);
	}

	public static Predicate<ConditionContext> doesNotHaveAttachmentGroup(String attachmentGroup) {
		return hasAttachmentGroup(attachmentGroup).negate();
	}

	public static Predicate<ConditionContext> hasAttachmentAtPathPrefix(String prefix) {
		return (ctx) -> {
			NavigableMap<String, ItemStack> attachments = Attachments.getAttachments(ctx.currentItemStack(), true);
			String key = attachments.ceilingKey(prefix);
			return key != null && key.startsWith(prefix);
		};
	}

	public static Predicate<ConditionContext> doesNotHaveAttachmentAtPathPrefix(String prefix) {
		return hasAttachmentAtPathPrefix(prefix).negate();
	}

	public static Predicate<ConditionContext> hasAttachmentInCategory(AttachmentCategory category) {
		return (ctx) -> {
			NavigableMap<String, ItemStack> attachments =
				Attachments.getAttachmentsForCategory(ctx.currentItemStack(), category);
			return !attachments.isEmpty();
		};
	}

	public static Predicate<ConditionContext> doesNotHaveAttachmentInCategory(AttachmentCategory category) {
		return hasAttachmentInCategory(category).negate();
	}

	public static Predicate<ConditionContext>
	hasAttachmentInCategoryAtPathPrefix(AttachmentCategory category, String prefix) {
		return (ctx) -> {
			NavigableMap<String, ItemStack> attachments = Attachments.getAttachments(ctx.currentItemStack(), true);
			SortedMap<String, ItemStack> tm = attachments.tailMap(prefix);
			boolean result = false;

			for (Map.Entry<String, ItemStack> e : tm.entrySet()) {
				if (e.getKey().startsWith(prefix)) {
					Item patt6536$temp = e.getValue().getItem();
					if (patt6536$temp instanceof Attachment a) {
						if (a.getCategory() == category) {
							result = true;
						}
					}
				}
			}

			return result;
		};
	}

	public static Predicate<ConditionContext>
	doesNotHaveAttachmentInCategoryAtPathPrefix(AttachmentCategory category, String prefix) {
		return hasAttachmentInCategoryAtPathPrefix(category, prefix).negate();
	}

	public static Predicate<ConditionContext> fromJson(JsonElement element) {
		if (element == null) {
			return (ctx) -> true;
		} else if (element.isJsonPrimitive()) {
			String expr = element.getAsString();
			return expr.trim().equalsIgnoreCase("random") ? RANDOM_PICK : Expression.compile(expr);
		} else {
			JsonObject obj = element.getAsJsonObject();
			if (obj.has("anyOf")) {
				JsonArray conditions = obj.getAsJsonArray("anyOf");
				Predicate<ConditionContext> predicate = (ctx) -> false;

				for (JsonElement condition : conditions) {
					predicate = predicate.or(fromJson(condition.getAsJsonObject()));
				}

				return predicate;
			} else if (!obj.has("allOf")) {
				if (obj.has("not")) {
					return fromJson(obj.getAsJsonObject("not")).negate();
				} else if (obj.has("hasAttachment")) {
					return hasAttachment(JsonUtil.getJsonString(obj, "hasAttachment"));
				} else if (obj.has("doesNotHaveAttachment")) {
					return doesNotHaveAttachment(JsonUtil.getJsonString(obj, "doesNotHaveAttachment"));
				} else if (obj.has("hasAttachmentAtPathPrefix")) {
					return hasAttachmentAtPathPrefix(JsonUtil.getJsonString(obj, "hasAttachmentAtPathPrefix"));
				} else if (obj.has("doesNotHaveAttachmentAtPathPrefix")) {
					return doesNotHaveAttachmentAtPathPrefix(
						JsonUtil.getJsonString(obj, "doesNotHaveAttachmentAtPathPrefix"));
				} else if (obj.has("hasAttachmentGroup")) {
					return hasAttachmentGroup(JsonUtil.getJsonString(obj, "hasAttachmentGroup"));
				} else if (obj.has("doesNotHaveAttachmentGroup")) {
					return doesNotHaveAttachmentGroup(JsonUtil.getJsonString(obj, "doesNotHaveAttachmentGroup"));
				} else if (obj.has("hasAttachmentInCategory")) {
					return hasAttachmentInCategory(
						AttachmentCategory.fromString(JsonUtil.getJsonString(obj, "hasAttachmentInCategory")));
				} else if (obj.has("doesNotHaveAttachmentInCategory")) {
					return doesNotHaveAttachmentInCategory(
						AttachmentCategory.fromString(JsonUtil.getJsonString(obj, "doesNotHaveAttachmentInCategory")));
				} else if (obj.has("selectedFireMode")) {
					return selectedFireMode(JsonUtil.getJsonString(obj, "selectedFireMode"));
				} else if (obj.has("unselectedFireMode")) {
					return unselectedFireMode(JsonUtil.getJsonString(obj, "unselectedFireMode"));
				} else if (obj.has("isGunInHands")) {
					boolean isGunInHands = JsonUtil.getJsonBoolean(obj, "isGunInHands", true);
					return isGunInHands ? isGunInHands() : isGunOnGround();
				} else if (obj.has("isGunOnGround")) {
					boolean isGunOnGround = JsonUtil.getJsonBoolean(obj, "isGunOnGround", false);
					return isGunOnGround ? isGunOnGround() : isGunInHands();
				} else if (obj.has("isUsingDefaultMuzzle")) {
					boolean isUsingDefaultMuzzle = JsonUtil.getJsonBoolean(obj, "isUsingDefaultMuzzle", true);
					return isUsingDefaultMuzzle ? isUsingDefaultMuzzle() : isUsingDefaultMuzzle().negate();
				} else if (obj.has("aiming")) {
					boolean isAiming = obj.get("aiming").getAsBoolean();
					return isAiming ? aiming() : Predicate.not(aiming());
				} else if (obj.has("onEmptyReload")) {
					boolean value = JsonUtil.getJsonBoolean(obj, "onEmptyReload", true);
					return value ? onEmptyReload() : onNonEmptyReload();
				} else if (obj.has("ammoCount")) {
					int value = JsonUtil.getJsonInt(obj, "ammoCount");
					return hasAmmoCount(value);
				} else {
					throw new IllegalArgumentException("Unknown condition in JSON: " + obj);
				}
			} else {
				JsonArray conditions = obj.getAsJsonArray("allOf");
				Predicate<ConditionContext> predicate = (ctx) -> true;

				for (JsonElement condition : conditions) {
					predicate = predicate.and(fromJson(condition));
				}

				return predicate;
			}
		}
	}
}
