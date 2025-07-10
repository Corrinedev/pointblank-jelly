package mod.pbj.mixin;

import mod.pbj.client.ClientSystem;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = {MouseHandler.class}, remap = false)
public class MouseHandlerMixin {
	@ModifyConstant(method = {"turnPlayer()V"}, constant = { @Constant(doubleValue = 0.6000000238418579D) }, expect = 0)
	private double modifySensitivity(double originalValue) {
		return ClientSystem.modifyMouseSensitivity(originalValue);
	}
}
