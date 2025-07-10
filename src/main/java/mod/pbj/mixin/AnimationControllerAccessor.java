package mod.pbj.mixin;

import java.util.function.Function;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.EasingType;

@Mixin({AnimationController.class})
public interface AnimationControllerAccessor {
	@Mutable @Accessor("isJustStarting") void setIsJustStarting(boolean var1);

	@Accessor("overrideEasingTypeFunction")
	<T extends GeoAnimatable> Function<T, EasingType> getOverrideEasingTypeFunction();

	@Accessor("justStartedTransition") boolean justStartedTransition();
}
