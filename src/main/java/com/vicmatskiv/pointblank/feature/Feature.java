package com.vicmatskiv.pointblank.feature;

import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.item.GunItem;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface Feature {
   FeatureProvider getOwner();

   default Component getDescription() {
      return Component.empty();
   }

   default List<Component> getDescriptions() {
      return List.of(Component.empty());
   }

   default boolean isEnabledForAttachment(ItemStack rootStack, ItemStack attachmentStack) {
      if (rootStack == null) {
         return false;
      } else {
         Item item = attachmentStack.getItem();
          if (item instanceof Attachment attachment) {
              if (!attachment.getCategory().requiresAttachmentSelection(this.getClass())) {
                return attachment.getFeature(this.getClass()) == this && this.isEnabled(rootStack);
             } else {
                Pair<String, ItemStack> selectedAttachment = Attachments.getSelectedAttachment(rootStack, attachment.getCategory());
                if (selectedAttachment != null && selectedAttachment.getSecond() == attachmentStack) {
                   return attachment.getFeature(this.getClass()) == this && this.isEnabled(rootStack);
                } else {
                   return false;
                }
             }
          } else {
             return false;
          }
      }
   }

   default boolean isEnabled(ItemStack value) {
      return true;
   }

   default Map<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> getEffectBuilders() {
      return Collections.emptyMap();
   }


}
