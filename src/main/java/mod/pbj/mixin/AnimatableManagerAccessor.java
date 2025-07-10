package mod.pbj.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.core.animation.AnimatableManager;

@Mixin({AnimatableManager.class})
public interface AnimatableManagerAccessor {
	@Mutable @Accessor("isFirstTick") void setIsFirstTick(boolean var1);
}
