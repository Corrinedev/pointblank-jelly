//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.mixin;

import mod.pbj.Config;
import mod.pbj.item.GunItem;
import mod.pbj.util.MiscUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {LivingEntity.class}, remap = false)
public class LivingEntityMixin {
	@Unique private final ThreadLocal<DamageSource> hurtDamageSource = new ThreadLocal<>();

	public LivingEntityMixin() {}

	@Inject(
		method = {"hurt"},
		at = { @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V") })
	private void
	beforeHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callbackInfo) {
		this.hurtDamageSource.set(source);
	}

	@Inject(
		method = {"hurt"},
		at =
		{
			@At(value = "INVOKE",
				target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V",
				shift = Shift.AFTER)
		})
	private void
	afterHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callbackInfo) {
		this.hurtDamageSource.remove();
	}

	@ModifyArg(
		method = {"hurt"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"),
		index = 0)
	private double
	onKnockback(double knockback) {
		DamageSource source = this.hurtDamageSource.get();
		if (source != null) {
			Entity entity = source.getEntity();
			if (entity instanceof LivingEntity hurtByEntity) {
				GunItem gunItem = MiscUtil.getMainHeldGun(hurtByEntity).orElse(null);
				if (gunItem != null) {
					knockback = Mth.clamp(knockback * Config.knockback, 0.0F, 100.0F);
				}
			}
		}

		return knockback;
	}
}
