package mod.pbj.client;

import com.mojang.blaze3d.vertex.PoseStack.Pose;

@FunctionalInterface
public interface PoseProvider {
	Pose getPose();
}
