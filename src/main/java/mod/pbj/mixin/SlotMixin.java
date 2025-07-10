package mod.pbj.mixin;

import mod.pbj.item.GunItem;
import mod.pbj.item.ThrowableItem;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {Slot.class})
public class SlotMixin {
	@Shadow @Final private int slot;

	public SlotMixin() {}

	@Inject(method = {"mayPlace"}, at = { @At("HEAD") }, cancellable = true)
	private void onMayPlace(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
		if (this.slot == 40 &&
			(itemStack.getItem() instanceof GunItem || itemStack.getItem() instanceof ThrowableItem)) {
			cir.setReturnValue(false);
			cir.cancel();
		}
	}
}
