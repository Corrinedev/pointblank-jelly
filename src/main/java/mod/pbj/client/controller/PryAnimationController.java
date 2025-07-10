package mod.pbj.client.controller;

import mod.pbj.client.GunClientState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public abstract class PryAnimationController extends AbstractProceduralAnimationController {
	public PryAnimationController(long ticksPerTransition) {
		super(ticksPerTransition);
	}

	public void onRenderTick(
		LivingEntity player,
		GunClientState gunClientState,
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		float partialTicks) {
		super.onRenderTick(player, gunClientState, itemStack, itemDisplayContext, partialTicks);
		if (!this.isDone) {
			this.updatePitch(this.progress);
			this.updateYaw(this.progress);
			this.updateRoll(this.progress);
		}
	}

	public void
	reset(double startPitch, double startYaw, double startRoll, double endPitch, double endYaw, double endRoll) {
		this.startPitch = startPitch;
		this.startYaw = startYaw;
		this.startRoll = startRoll;
		this.endPitch = endPitch;
		this.endYaw = endYaw;
		this.endRoll = endRoll;
		super.reset();
	}

	public void reset() {
		this.reset(this.pitch, this.yaw, this.roll);
	}

	public void reset(double endPitch, double endYaw, double endRoll) {
		this.reset(this.pitch, this.yaw, this.roll, endPitch, endYaw, endRoll);
	}

	protected double updatePitch(double progress) {
		return this.pitch;
	}

	protected double updateYaw(double progress) {
		return this.yaw;
	}

	protected double updateRoll(double progress) {
		return this.roll;
	}

	public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
		this.reset();
	}
}
