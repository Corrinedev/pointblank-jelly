package com.vicmatskiv.pointblank.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.feature.ConditionContext;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import java.util.Iterator;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class Conditions {
   public static final Predicate<ConditionContext> RANDOM_PICK = (ctx) -> {
      return ctx.randomSample2() == ctx.randomSample1();
   };

   public static final Predicate<ConditionContext> isGunOnGround() {
      return (ctx) -> {
         return ctx.itemDisplayContext() == ItemDisplayContext.GROUND;
      };
   }

   public static final Predicate<ConditionContext> isGunInHands() {
      return (ctx) -> {
         return ctx.itemDisplayContext() != ItemDisplayContext.GROUND;
      };
   }

   public static final Predicate<ConditionContext> selectedFireMode(String fireModeName) {
      return (ctx) -> {
         if (ctx.rootStack() == null) {
            return false;
         } else {
            FireModeInstance fireModeInstance = GunItem.getFireModeInstance(ctx.rootStack());
            return fireModeInstance != null ? Objects.equals(fireModeInstance.getName().toUpperCase(Locale.ROOT), fireModeName.toUpperCase(Locale.ROOT)) : false;
         }
      };
   }

   public static final Predicate<ConditionContext> unselectedFireMode(String fireModeName) {
      return selectedFireMode(fireModeName).negate();
   }

   public static final Predicate<ConditionContext> isUsingDefaultMuzzle() {
      return (ctx) -> {
         if (ctx.rootStack() == null) {
            return false;
         } else {
            FireModeInstance fireModeInstance = GunItem.getFireModeInstance(ctx.rootStack());
            return fireModeInstance != null ? fireModeInstance.isUsingDefaultMuzzle() : true;
         }
      };
   }

   public static final Predicate<ConditionContext> hasAmmoCount(int ammoCount) {
      return (ctx) -> {
         return ammoCount == ctx.gunClientState().getAmmoCount(GunItem.getFireModeInstance(ctx.rootStack()));
      };
   }

   public static final Predicate<ConditionContext> onReloadIteration(int index) {
      return (ctx) -> {
         return index == ctx.gunClientState().getReloadIterationIndex();
      };
   }

   public static final Predicate<ConditionContext> onEmptyReload() {
      return (ctx) -> {
         return ctx.gunClientState().getReloadIterationIndex() == 0;
      };
   }

   public static final Predicate<ConditionContext> onNonEmptyReload() {
      return (ctx) -> {
         return ctx.gunClientState().getReloadIterationIndex() > 0;
      };
   }

   public static final Predicate<ConditionContext> beforePreparingReload() {
      return onReloadIteration(-1);
   }

   public static final Predicate<ConditionContext> afterPreparingReload() {
      return (ctx) -> {
         return ctx.gunClientState().getReloadIterationIndex() >= 0;
      };
   }

   public static final Predicate<ConditionContext> hasAttachment(Supplier<? extends Attachment> attachmentSupplier) {
      return (ctx) -> {
         NavigableMap<String, ItemStack> attachments = Attachments.getAttachments(ctx.currentItemStack(), false);
         String var10001 = ((Attachment)attachmentSupplier.get()).getName();
         return attachments.get("//" + var10001) != null;
      };
   }

   public static final Predicate<ConditionContext> doesNotHaveAttachment(Supplier<? extends Attachment> attachmentSupplier) {
      return hasAttachment(attachmentSupplier).negate();
   }

   public static final Predicate<ConditionContext> hasAttachment(String attachmentName) {
      return (ctx) -> {
         NavigableMap<String, ItemStack> attachments = Attachments.getAttachments(ctx.currentItemStack(), false);
         return attachments.get("//" + attachmentName) != null;
      };
   }

   public static final Predicate<ConditionContext> doesNotHaveAttachment(String attachmentName) {
      return hasAttachment(attachmentName).negate();
   }

   public static final Predicate<ConditionContext> hasAttachmentGroup(String attachmentGroup) {
      return (ctx) -> {
         return Attachments.getAttachmentGroups(ctx.currentItemStack()).containsKey(attachmentGroup);
      };
   }

   public static final Predicate<ConditionContext> doesNotHaveAttachmentGroup(String attachmentGroup) {
      return hasAttachmentGroup(attachmentGroup).negate();
   }

   public static final Predicate<ConditionContext> hasAttachmentAtPathPrefix(String prefix) {
      return (ctx) -> {
         NavigableMap<String, ItemStack> attachments = Attachments.getAttachments(ctx.currentItemStack(), true);
         String key = (String)attachments.ceilingKey(prefix);
         return key != null && key.startsWith(prefix);
      };
   }

   public static final Predicate<ConditionContext> doesNotHaveAttachmentAtPathPrefix(String prefix) {
      return hasAttachmentAtPathPrefix(prefix).negate();
   }

   public static final Predicate<ConditionContext> hasAttachmentInCategory(AttachmentCategory category) {
      return (ctx) -> {
         NavigableMap<String, ItemStack> attachments = Attachments.getAttachmentsForCategory(ctx.currentItemStack(), category);
         return !attachments.isEmpty();
      };
   }

   public static final Predicate<ConditionContext> doesNotHaveAttachmentInCategory(AttachmentCategory category) {
      return hasAttachmentInCategory(category).negate();
   }

   public static final Predicate<ConditionContext> hasAttachmentInCategoryAtPathPrefix(AttachmentCategory category, String prefix) {
      return (ctx) -> {
         NavigableMap<String, ItemStack> attachments = Attachments.getAttachments(ctx.currentItemStack(), true);
         SortedMap<String, ItemStack> tm = attachments.tailMap(prefix);
         boolean result = false;
         Iterator var6 = tm.entrySet().iterator();

         while(var6.hasNext()) {
            Entry<String, ItemStack> e = (Entry)var6.next();
            if (((String)e.getKey()).startsWith(prefix)) {
               Item patt6536$temp = ((ItemStack)e.getValue()).m_41720_();
               if (patt6536$temp instanceof Attachment) {
                  Attachment a = (Attachment)patt6536$temp;
                  if (a.getCategory() == category) {
                     result = true;
                  }
               }
            }
         }

         return result;
      };
   }

   public static final Predicate<ConditionContext> doesNotHaveAttachmentInCategoryAtPathPrefix(AttachmentCategory category, String prefix) {
      return hasAttachmentInCategoryAtPathPrefix(category, prefix).negate();
   }

   public static Predicate<ConditionContext> fromJson(JsonElement element) {
      if (element == null) {
         return (ctx) -> {
            return true;
         };
      } else if (element.isJsonPrimitive()) {
         String expr = element.getAsString();
         return expr.trim().equalsIgnoreCase("random") ? RANDOM_PICK : Expression.compile(expr);
      } else {
         JsonObject obj = element.getAsJsonObject();
         JsonArray conditions;
         Predicate predicate;
         Iterator var4;
         JsonElement condition;
         if (obj.has("anyOf")) {
            conditions = obj.getAsJsonArray("anyOf");
            predicate = (ctx) -> {
               return false;
            };

            for(var4 = conditions.iterator(); var4.hasNext(); predicate = predicate.or(fromJson(condition.getAsJsonObject()))) {
               condition = (JsonElement)var4.next();
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
               return doesNotHaveAttachmentAtPathPrefix(JsonUtil.getJsonString(obj, "doesNotHaveAttachmentAtPathPrefix"));
            } else if (obj.has("hasAttachmentGroup")) {
               return hasAttachmentGroup(JsonUtil.getJsonString(obj, "hasAttachmentGroup"));
            } else if (obj.has("doesNotHaveAttachmentGroup")) {
               return doesNotHaveAttachmentGroup(JsonUtil.getJsonString(obj, "doesNotHaveAttachmentGroup"));
            } else if (obj.has("hasAttachmentInCategory")) {
               return hasAttachmentInCategory(AttachmentCategory.fromString(JsonUtil.getJsonString(obj, "hasAttachmentInCategory")));
            } else if (obj.has("doesNotHaveAttachmentInCategory")) {
               return doesNotHaveAttachmentInCategory(AttachmentCategory.fromString(JsonUtil.getJsonString(obj, "doesNotHaveAttachmentInCategory")));
            } else if (obj.has("selectedFireMode")) {
               return selectedFireMode(JsonUtil.getJsonString(obj, "selectedFireMode"));
            } else if (obj.has("unselectedFireMode")) {
               return unselectedFireMode(JsonUtil.getJsonString(obj, "unselectedFireMode"));
            } else {
               boolean value;
               if (obj.has("isGunInHands")) {
                  value = JsonUtil.getJsonBoolean(obj, "isGunInHands", true);
                  return value ? isGunInHands() : isGunOnGround();
               } else if (obj.has("isUsingDefaultMuzzle")) {
                  value = JsonUtil.getJsonBoolean(obj, "isUsingDefaultMuzzle", true);
                  return value ? isUsingDefaultMuzzle() : isUsingDefaultMuzzle().negate();
               } else if (obj.has("onEmptyReload")) {
                  value = JsonUtil.getJsonBoolean(obj, "onEmptyReload", true);
                  return value ? onEmptyReload() : onNonEmptyReload();
               } else if (obj.has("ammoCount")) {
                  int value = JsonUtil.getJsonInt(obj, "ammoCount");
                  return hasAmmoCount(value);
               } else {
                  throw new IllegalArgumentException("Unknown condition in JSON: " + obj);
               }
            }
         } else {
            conditions = obj.getAsJsonArray("allOf");
            predicate = (ctx) -> {
               return true;
            };

            for(var4 = conditions.iterator(); var4.hasNext(); predicate = predicate.and(fromJson(condition))) {
               condition = (JsonElement)var4.next();
            }

            return predicate;
         }
      }
   }
}
