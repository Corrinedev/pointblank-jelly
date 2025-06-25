package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.Script;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.util.ClientUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class SkinFeature extends ConditionalFeature {
   private ResourceLocation texture;
   private final @Nullable Script script;
   private Map<String, Pair<ResourceLocation, Predicate<ConditionContext>>> conditions;

   private SkinFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, ResourceLocation texture, Script script, Map<String, Pair<ResourceLocation, Predicate<ConditionContext>>> conditions) {
      super(owner, predicate);
      this.texture = texture;
      this.script = script;
      this.conditions = conditions;
   }

   public MutableComponent getDescription() {
      return Component.literal("Changes skin");
   }

   public static ResourceLocation getTexture(ItemStack itemStack) {
      Features.EnabledFeature enabledSkinTexture = Features.getFirstEnabledFeature(itemStack, SkinFeature.class);
      if(enabledSkinTexture != null && enabledSkinTexture.feature() instanceof SkinFeature feature) {
         if (feature.conditions != null && !feature.conditions.isEmpty()) {
            Predicate<ConditionContext> condition = (ctx) -> true;
            for (var entry : feature.conditions.entrySet()) {
               GunClientState gunState = GunClientState.getMainHeldState(ClientUtils.getClientPlayer());
               ConditionContext testCondition = new ConditionContext(itemStack, gunState);
               if (gunState != null && itemStack.getItem().toString().equals(entry.getKey()) && !entry.getValue().getSecond().test(testCondition)) {
                  return null;
               } else if (gunState != null && itemStack.getItem().toString().equals(entry.getKey())) {
                  if(feature.hasFunction("getSkinTexture"))
                     return (ResourceLocation) feature.invokeFunction("getSkinTexture", itemStack, feature);
                  if(feature.conditions != null) {
                     String gunId = itemStack.getItem().toString();
                     if (feature.conditions.containsKey(gunId)) {
                        return feature.conditions.get(gunId).getFirst();
                     }
                  }
               }
            }
         }
         return feature.texture;
      }
      return null;
   }

   @Override
   public @Nullable Script getScript() {
      return script;
   }

   public static class Builder implements FeatureBuilder<Builder, SkinFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> true;
      private ResourceLocation skinResource;
      private Script script;
      private Map<String, Pair<ResourceLocation, Predicate<ConditionContext>>> conditions;

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withTexture(String texture) {
         this.skinResource = new ResourceLocation("pointblank", texture);
         return this;
      }

      public Builder withScript(Script script) {
         this.script = script;
         return this;
      }

      public Builder withTextures(JsonArray tArr) { //Thank you so much for the help, CorrineDuck!
         this.conditions = new HashMap<>();
         for (int i = 0; i < tArr.size(); i++) {
            JsonObject obj = tArr.get(i).getAsJsonObject();
            String gunId = obj.get("gunId").getAsString();
            String texture = obj.get("texture").getAsString();
            Predicate<ConditionContext> skinCondition = (ctx) -> true;
            if (obj.has("condition") && obj.get("condition") != null) {
               skinCondition = Conditions.fromJson(obj.get("condition"));
            }
            this.conditions.put(gunId, Pair.of(new ResourceLocation("pointblank", texture), skinCondition));
         }
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("texture")) {
            this.withTexture(JsonUtil.getJsonString(obj, "texture"));
         }

         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         if(obj.has("skins")) {
            this.withTextures(obj.getAsJsonArray("skins"));
         }

         this.withScript(JsonUtil.getJsonScript(obj));

         return this;
      }

      public SkinFeature build(FeatureProvider featureProvider) {
         return new SkinFeature(featureProvider, this.condition, this.skinResource, this.script, this.conditions);
      }
   }
}