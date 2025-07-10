package mod.pbj.mixin;

import mod.pbj.client.gui.AttachmentManagerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {AbstractContainerScreen.class}, remap = false)
public abstract class AbstractContainerScreenMixin {
	@Inject(
		method = {"renderSlot"},
		at =
		{
			@At(value = "INVOKE",
				target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V")
		})
	public void
	beforeRenderingSlotItem(GuiGraphics guiGraphics, Slot slot, CallbackInfo callbackInfo) {
		if ((AbstractContainerScreen<?>)(Object)this instanceof AttachmentManagerScreen ams) {
			ams.beforeRenderingSlot(guiGraphics, slot);
		}
	}
}
