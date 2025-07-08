package mod.pbj.feature;

import mod.pbj.client.GunClientState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public record ConditionContext(LivingEntity player, ItemStack rootStack, ItemStack currentItemStack, GunClientState gunClientState, ItemDisplayContext itemDisplayContext, int randomSample1, int randomSample2) {
   public ConditionContext(LivingEntity player, ItemStack rootStack, ItemStack currentItemStack, GunClientState gunClientState, ItemDisplayContext itemDisplayContext) {
      this(player, rootStack, currentItemStack, gunClientState, itemDisplayContext, Integer.MAX_VALUE, 0);
   }

   public ConditionContext(LivingEntity player, ItemStack rootStack, GunClientState gunClientState, ItemDisplayContext itemDisplayContext) {
      this(player, rootStack, rootStack, gunClientState, itemDisplayContext, Integer.MAX_VALUE, 0);
   }

   public ConditionContext(LivingEntity player, GunClientState gunClientState) {
      this(player, null, gunClientState, null);
   }

   public ConditionContext(LivingEntity player, ItemStack stack, GunClientState gunClientState) {
      this(player, stack, gunClientState, null);
   }

   public ConditionContext(ItemStack itemStack) {
      this(null, itemStack, itemStack, null, null);
   }

   public ConditionContext(ItemStack itemStack, GunClientState gunClientState) {
      this(null, itemStack, itemStack, gunClientState, null);
   }

    public LivingEntity player() {
      return this.player;
   }

   public ItemStack rootStack() {
      return this.rootStack;
   }

   public ItemStack currentItemStack() {
      return this.currentItemStack;
   }

   public GunClientState gunClientState() {
      return this.gunClientState;
   }

   public ItemDisplayContext itemDisplayContext() {
      return this.itemDisplayContext;
   }

   public int randomSample1() {
      return this.randomSample1;
   }

   public int randomSample2() {
      return this.randomSample2;
   }
}
