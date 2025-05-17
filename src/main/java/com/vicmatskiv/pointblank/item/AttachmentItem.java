package com.vicmatskiv.pointblank.item;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.client.render.AttachmentModelRenderer;
import com.vicmatskiv.pointblank.crafting.Craftable;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.FeatureBuilder;
import com.vicmatskiv.pointblank.feature.FeatureProvider;
import com.vicmatskiv.pointblank.feature.Features;
import com.vicmatskiv.pointblank.registry.ExtensionRegistry;
import com.vicmatskiv.pointblank.registry.ItemRegistry;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.TimeUnit;
import com.vicmatskiv.pointblank.util.Tradeable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import groovy.lang.Script;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class AttachmentItem extends Item implements ScriptHolder, GeoItem, Attachment, AttachmentHost, FeatureProvider, Craftable, Tradeable {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private String name;
   private AttachmentCategory category;
   private List<String> compatibleAttachmentGroups;
   private List<Supplier<Attachment>> compatibleAttachmentSuppliers;
   private List<Attachment> compatibleAttachments;
   private Set<String> groups;
   private Map<Class<? extends Feature>, Feature> features;
   private long craftingDuration;
   private float tradePrice;
   private int tradeBundleQuantity;
   private int tradeLevel;
   private List<Component> descriptionLines;
   public List<Supplier<Attachment>> defaultAttachmentSuppliers;
   private @org.jetbrains.annotations.Nullable Script script;
   private JsonObject bulletModifiers = new JsonObject();

   public JsonObject getBulletModifiers() {
      return this.bulletModifiers;
   }
   public AttachmentItem() {
      super(new Item.Properties());
      SingletonGeoAnimatable.registerSyncedAnimatable(this);
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private AttachmentModelRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new AttachmentModelRenderer(AttachmentItem.this.name);
            }

            return this.renderer;
         }
      });
   }

   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "scriptAnimator", state -> PlayState.CONTINUE));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public String getName() {
      return this.name;
   }

   public Collection<Feature> getFeatures() {
      return Collections.unmodifiableCollection(this.features.values());
   }

   public List<Component> getDescriptionTooltipLines() {
      return this.descriptionLines;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
   }

   public AttachmentCategory getCategory() {
      return this.category;
   }

   public Set<String> getGroups() {
      return this.groups;
   }

   public int getMaxStackSize(ItemStack stack) {
      return 1;
   }

   public Collection<Attachment> getCompatibleAttachments() {
      if (this.compatibleAttachments == null) {
         this.compatibleAttachments = new ArrayList<>();

         for(Supplier<Attachment> cas : this.compatibleAttachmentSuppliers) {
            this.compatibleAttachments.add(cas.get());
         }

         for(String group : this.compatibleAttachmentGroups) {
            for(Supplier<? extends Item> ga : ItemRegistry.ITEMS.getAttachmentsForGroup(group)) {
               Item item = ga.get();
               if (item instanceof Attachment attachment) {
                   this.compatibleAttachments.add(attachment);
               }
            }
         }
      }

      return this.compatibleAttachments;
   }

   public Component getName(ItemStack itemStack) {
      return Component.translatable(this.getDescriptionId(itemStack));
   }

   public <T extends Feature> T getFeature(Class<T> featureClass) {
      return featureClass.cast(this.features.get(featureClass));
   }

   public long getCraftingDuration() {
      return this.craftingDuration;
   }

   public float getPrice() {
      return this.tradePrice;
   }

   public int getTradeLevel() {
      return this.tradeLevel;
   }

   public int getBundleQuantity() {
      return this.tradeBundleQuantity;
   }

   public List<Attachment> getDefaultAttachments() {
      return this.defaultAttachmentSuppliers.stream().map(Supplier::get).toList();
   }

   public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int itemSlot, boolean isSelected) {
      if (!level.isClientSide) {
         this.ensureItemStack(itemStack);
      }

   }

   public void ensureItemStack(ItemStack itemStack) {
      CompoundTag stateTag = itemStack.getTag();
      if (stateTag == null) {
         stateTag = new CompoundTag();
         itemStack.setTag(stateTag);
         Item defaultAttachments = itemStack.getItem();
         if (defaultAttachments instanceof AttachmentHost attachmentHost) {

             for(Attachment attachment : attachmentHost.getDefaultAttachments()) {
               Attachments.addAttachment(itemStack, new ItemStack(attachment), true);
            }
         }
      }

   }

   @Override
   public @org.jetbrains.annotations.Nullable Script getScript() {
      return script;
   }

   public static class Builder extends ItemBuilder<Builder> {
      private static final int DEFAULT_CRAFTING_DURATION = 750;
      private static final float DEFAULT_PRICE = Float.NaN;
      private static final int DEFAULT_TRADE_LEVEL = 0;
      private static final int DEFAULT_TRADE_BUNDLE_QUANTITY = 1;
      private String name;
      private AttachmentCategory category;
      private final List<Supplier<Attachment>> compatibleAttachmentSuppliers = new ArrayList<>();
      private final List<String> compatibleAttachmentGroups = new ArrayList<>();
      private final Set<String> groups = new HashSet<>();
      private final List<FeatureBuilder<?, ?>> featureBuilders = new ArrayList<>();
      private long craftingDuration = 750L;
      private float tradePrice = Float.NaN;
      private int tradeBundleQuantity = 1;
      private int tradeLevel = 0;
      private final List<Component> descriptionLines = new ArrayList<>();
      private final List<Supplier<Attachment>> defaultAttachments = new ArrayList<>();
      private @org.jetbrains.annotations.Nullable Script script;

      private JsonObject bulletModifiers = new JsonObject();

      public JsonObject getBulletModifiers() {
         return this.bulletModifiers;
      }

      public Builder(ExtensionRegistry.Extension extension) {
         this.extension = extension;
      }

      public Builder() {
         this.extension = new ExtensionRegistry.Extension("pointblank", Path.of("pointblank"), "pointblank");
      }

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withScript(Script script) {
         this.script = script;
         return this;
      }

      public Builder withCategory(String categoryName) {
         this.category = AttachmentCategory.fromString(categoryName);
         return this;
      }

      public Builder withCategory(AttachmentCategory category) {
         this.category = category;
         return this;
      }

      public Builder withDescription(Component description) {
         this.descriptionLines.add(description);
         return this;
      }

      public Builder withDescription(String description) {
         this.descriptionLines.add(Component.translatable(description).withStyle(ChatFormatting.RED).withStyle(ChatFormatting.ITALIC));
         return this;
      }

      public Builder withFeature(FeatureBuilder<?, ?> featureBuilder) {
         this.featureBuilders.add(featureBuilder);
         return this;
      }

      @SafeVarargs
      public final Builder withCompatibleAttachment(Supplier<? extends Attachment>... attachmentSuppliers) {
         for(Supplier<? extends Attachment> s : attachmentSuppliers) {
             Objects.requireNonNull(s);
            this.compatibleAttachmentSuppliers.add(s::get);
         }

         return this;
      }

      public Builder withCompatibleAttachmentGroup(String... groups) {
         this.compatibleAttachmentGroups.addAll(Set.of(groups));
         return this;
      }

      public Builder withGroup(String... groups) {
         this.groups.addAll(Set.of(groups));
         return this;
      }

      public Builder withCraftingDuration(int duration, TimeUnit timeUnit) {
         this.craftingDuration = timeUnit.toMillis(duration);
         return this;
      }

      public Builder withTradePrice(double price, int tradeLevel) {
         this.tradePrice = (float)price;
         this.tradeLevel = tradeLevel;
         this.tradeBundleQuantity = 1;
         return this;
      }

      public Set<String> getGroups() {
         return Collections.unmodifiableSet(this.groups);
      }

      @SafeVarargs
      public final Builder withDefaultAttachment(Supplier<? extends Attachment>... attachmentSuppliers) {
         for(Supplier<? extends Attachment> s : attachmentSuppliers) {
             Objects.requireNonNull(s);
            this.defaultAttachments.add(s::get);
         }

         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         this.withName(JsonUtil.getJsonString(obj, "name"));
         String description = JsonUtil.getJsonString(obj, "description", null);
         if (description != null) {
            this.withDescription(description);
         }

         this.withCategory(JsonUtil.getJsonString(obj, "category"));
         this.withTradePrice(JsonUtil.getJsonFloat(obj, "tradePrice", Float.NaN), JsonUtil.getJsonInt(obj, "tradeLevel", 0));
         this.withCraftingDuration(JsonUtil.getJsonInt(obj, "craftingDuration", 750), TimeUnit.MILLISECOND);
         this.withScript(JsonUtil.getJsonScript(obj));
         List<String> groups = JsonUtil.getStrings(obj, "groups");
         this.groups.addAll(groups);
         if (obj.has("bulletModifiers")) {
            this.bulletModifiers = obj.getAsJsonObject("bulletModifiers");
         }

         for(JsonObject featureObj : JsonUtil.getJsonObjects(obj, "features")) {
            FeatureBuilder<?, ?> featureBuilder = Features.fromJson(featureObj);
            this.withFeature(featureBuilder);
         }

         for(String compatibleAttachmentName : JsonUtil.getStrings(obj, "compatibleAttachments")) {
            Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(compatibleAttachmentName);
            if (ri != null) {
               this.withCompatibleAttachment(() -> (Attachment)ri.get());
            }
         }

         List<String> compatibleAttachmentGroups = JsonUtil.getStrings(obj, "compatibleAttachmentGroups");
         this.compatibleAttachmentGroups.addAll(compatibleAttachmentGroups);

         for(String defaultAttachmentName : JsonUtil.getStrings(obj, "defaultAttachments")) {
            Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(defaultAttachmentName);
            if (ri != null) {
               this.withDefaultAttachment(() -> (Attachment)ri.get());
            }
         }

         return this;
      }

      public AttachmentItem build() {
         if (this.name == null) {
            throw new IllegalStateException("Attachment name not set");
         } else if (this.category == null) {
            throw new IllegalStateException("Attachment category not set");
         } else {
            AttachmentItem attachment = new AttachmentItem();
            attachment.name = this.name;
            attachment.descriptionLines = Collections.unmodifiableList(this.descriptionLines);
            attachment.category = this.category;
            attachment.compatibleAttachmentSuppliers = Collections.unmodifiableList(this.compatibleAttachmentSuppliers);
            attachment.compatibleAttachmentGroups = Collections.unmodifiableList(this.compatibleAttachmentGroups);
            attachment.groups = Collections.unmodifiableSet(this.groups);
            attachment.script = this.script;
            Map<Class<? extends Feature>, Feature> features = new HashMap<>();

            for(FeatureBuilder<?, ?> featureBuilder : this.category.getDefaultFeatures()) {
               Feature feature = featureBuilder.build(attachment);
               features.put(feature.getClass(), feature);
            }

            for(FeatureBuilder<?, ?> featureBuilder : this.featureBuilders) {
               Feature feature = featureBuilder.build(attachment);
               features.put(feature.getClass(), feature);
            }

            attachment.features = Collections.unmodifiableMap(features);
            attachment.craftingDuration = this.craftingDuration;
            attachment.tradePrice = this.tradePrice;
            attachment.tradeBundleQuantity = this.tradeBundleQuantity;
            attachment.tradeLevel = this.tradeLevel;
            attachment.defaultAttachmentSuppliers = Collections.unmodifiableList(this.defaultAttachments);
            attachment.bulletModifiers = this.bulletModifiers;
            return attachment;
         }
      }

      public String getName() {
         return this.name;
      }
   }
}
