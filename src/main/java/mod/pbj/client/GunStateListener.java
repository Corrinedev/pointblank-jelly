package mod.pbj.client;

import mod.pbj.client.render.RenderListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

public interface GunStateListener extends RenderListener {
   default void onStateTick(LivingEntity player, GunClientState state) {
   }

   default void onUpdateState(LivingEntity player, GunClientState state) {
   }

   default void onRenderTick(LivingEntity player, GunClientState state, ItemStack itemStack, ItemDisplayContext context, float partialTicks) {
   }

   default void onGameTick(LivingEntity player, GunClientState gunClientState) {
   }

   default void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
   }

   default void onStopFiring() {
   }

   default void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
   }

   default void onPrepareReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
   }

   default void onCompleteReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
   }

   default void onToggleAiming(boolean isAiming, Player player) {
   }

   default void onAttachmentAdded(LivingEntity player, GunClientState state, ItemStack itemStack, ItemStack attachmentStack) {

   }

   default void onDrawing(LivingEntity player, GunClientState state, ItemStack itemStack) {
   }

   default void onInspecting(LivingEntity player, GunClientState state, ItemStack itemStack) {
   }

   default void onJumping(LivingEntity player, GunClientState state, ItemStack itemStack) {
   }

   default void onStartAutoFiring(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
   }

   default void onStopAutoFiring(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
   }

   default void onPrepareFiring(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
   }

   default void onCompleteFiring(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
   }

   default void onHitScanTargetAcquired(LivingEntity player, GunClientState gunClientState, ItemStack itemStack, HitResult hitResult) {
   }

   default void onHitScanTargetConfirmed(LivingEntity player, GunClientState gunClientState, ItemStack itemStack, HitResult hitResult, float damage) {
   }

   default void onPrepareIdle(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
   }

   default void onIdle(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
   }

   default void onEnablingFireMode(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
   }
}
