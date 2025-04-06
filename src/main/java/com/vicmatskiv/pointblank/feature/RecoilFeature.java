package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class RecoilFeature extends ConditionalFeature {
   private static final float MIN_RECOIL_MODIFIER = 0.01F;
   private static final float MAX_RECOIL_MODIFIER = 10.0F;
   private float recoilModifier;
   private Component description;

   private RecoilFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, float recoilModifier) {
      super(owner, predicate);
      this.recoilModifier = recoilModifier;
      if (recoilModifier < 1.0F) {
         this.description = Component.translatable("description.pointblank.reducesRecoil").append(Component.literal(String.format(" %.0f%%", 100.0F * (1.0F - recoilModifier))));
      } else {
         this.description = Component.translatable("description.pointblank.increasesRecoil").append(Component.literal(String.format(" %.0f%%", 100.0F * (recoilModifier - 1.0F))));
      }

   }

   public Component getDescription() {
      return this.description;
   }

   public float getRecoilModifier() {
      return this.recoilModifier;
   }

   public static float getRecoilModifier(ItemStack itemStack) {
      List<Features.EnabledFeature> enabledRecoilFeatures = Features.getEnabledFeatures(itemStack, RecoilFeature.class);
      float recoilModifier = 1.0F;

      for(Features.EnabledFeature enabledFeature : enabledRecoilFeatures) {
         RecoilFeature recoilFeature = (RecoilFeature)enabledFeature.feature();
         recoilModifier *= recoilFeature.getRecoilModifier();
      }

      return Mth.clamp(recoilModifier, 0.01F, 10.0F);
   }

   public static class Builder implements FeatureBuilder<Builder, RecoilFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> true;
      private float recoilModifier;

      public Builder() {
      }

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withRecoilModifier(double recoilModifier) {
         this.recoilModifier = (float)recoilModifier;
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         this.withRecoilModifier(JsonUtil.getJsonFloat(obj, "recoilModifier"));
         return this;
      }

      public RecoilFeature build(FeatureProvider featureProvider) {
         return new RecoilFeature(featureProvider, this.condition, this.recoilModifier);
      }
   }
}
