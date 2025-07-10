package mod.pbj.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraftforge.eventbus.api.Event;

public class RenderHandEvent extends Event {
	private PoseStack poseStack;
	private Camera camera;
	private float partialTick;

	public RenderHandEvent(PoseStack poseStack, Camera camera, float partialTick) {
		this.poseStack = poseStack;
		this.camera = camera;
		this.partialTick = partialTick;
	}

	public PoseStack getPoseStack() {
		return this.poseStack;
	}

	public Camera getCamera() {
		return this.camera;
	}

	public float getPartialTick() {
		return this.partialTick;
	}

	public static class Pre extends RenderHandEvent {
		public Pre(PoseStack poseStack, Camera camera, float partialTick) {
			super(poseStack, camera, partialTick);
		}
	}
}
