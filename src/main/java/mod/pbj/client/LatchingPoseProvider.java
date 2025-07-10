package mod.pbj.client;

import com.mojang.blaze3d.vertex.PoseStack.Pose;

public class LatchingPoseProvider implements PoseProvider {
	private final PoseProvider actualPoseProvider;
	private Pose pose;

	public LatchingPoseProvider(PoseProvider actualPoseProvider) {
		this.actualPoseProvider = actualPoseProvider;
		this.pose = actualPoseProvider.getPose();
	}

	public Pose getPose() {
		if (this.pose == null) {
			this.pose = this.actualPoseProvider.getPose();
		}

		return this.pose;
	}
}
