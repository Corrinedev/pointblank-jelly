package mod.pbj.compat.iris.mixin;

import mod.pbj.compat.iris.RenderTargetsExt;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({RenderTargets.class})
public class RenderTargetsMixin implements RenderTargetsExt {
	@Shadow private boolean fullClearRequired;

	public void setPointblankRenderFullClearRequired(boolean fullClearRequired) {
		this.fullClearRequired = fullClearRequired;
	}
}
