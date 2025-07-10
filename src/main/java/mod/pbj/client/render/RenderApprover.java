package mod.pbj.client.render;

import mod.pbj.feature.Feature;
import mod.pbj.feature.Features;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public interface RenderApprover {
	boolean canRenderPart(String var1);

	Class<? extends Feature> getFeatureType();

	default boolean approveRendering(
		RenderPass renderPass,
		String partName,
		ItemStack rootStack,
		ItemStack currentStack,
		String path,
		ItemDisplayContext itemDisplayContext) {
		if (renderPass != RenderPass.current()) {
			return true;
		} else if (!this.canRenderPart(partName)) {
			return false;
		} else {
			Class<? extends Feature> approvedFeatureType = this.getFeatureType();
			if (approvedFeatureType == null) {
				return true;
			} else {
				boolean isApproved = false;

				for (Features.EnabledFeature enabledFeature :
					 Features.getEnabledFeatures(rootStack, approvedFeatureType)) {
					if (enabledFeature.ownerPath().equals(path)) {
						isApproved = true;
						break;
					}
				}

				return isApproved;
			}
		}
	}
}
