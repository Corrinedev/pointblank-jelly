package mod.pbj.client.controller;

import java.util.Random;
import mod.pbj.client.GunClientState;
import mod.pbj.client.GunStateListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class GunRandomizingAnimationController
	extends AbstractProceduralAnimationController implements GunStateListener {
	private final double amplitude;
	private double currentAmplitude;
	private double theta;
	private final long idleNanoTicksPerTransition;
	private final long fireNanoTicksPerTransition;
	private boolean isIdle;
	private boolean isForward;
	private static final double middleProgress = 1.0D - Math.sqrt(2.0D) / 2.0D;
	private final Random random;

	public GunRandomizingAnimationController(double amplitude, long idleDuration, long fireDuration) {
		super(idleDuration);
		this.amplitude = amplitude;
		this.random = new Random();
		this.idleNanoTicksPerTransition = idleDuration * 1000000L;
		this.fireNanoTicksPerTransition = fireDuration * 1000000L;
		this.reset(true, true, idleDuration, amplitude);
	}

	public void onRenderTick(
		LivingEntity player,
		GunClientState gunClientState,
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		float partialTicks) {
		super.onRenderTick(player, gunClientState, itemStack, itemDisplayContext, partialTicks);
		double adjustedProgress;
		if (this.isDone) {
			adjustedProgress = gunClientState.isAiming() ? this.currentAmplitude : this.amplitude;
			this.reset(true, true, this.idleNanoTicksPerTransition, adjustedProgress);
		} else {
			if (!this.isIdle && this.isForward && this.progress > middleProgress) {
				this.reset(false, false, this.nanoDuration, this.currentAmplitude);
			}

			if (this.isIdle) {
				adjustedProgress = this.progress;
			} else if (this.isForward) {
				adjustedProgress = 1.0D - (middleProgress - this.progress) / middleProgress;
			} else {
				adjustedProgress = 1.0D - (1.0D - this.progress) / (1.0D - middleProgress);
			}

			adjustedProgress = 0.5D - 0.5D * Math.cos(3.141592653589793D * this.progress);
			this.roll = this.startRoll + (this.endRoll - this.startRoll) * adjustedProgress;
			this.pitch = this.startPitch + (this.endPitch - this.startPitch) * adjustedProgress;
			this.yaw = this.startYaw + (this.endYaw - this.startYaw) * adjustedProgress;
			this.posX = this.startPosX + (this.endPosX - this.startPosX) * adjustedProgress;
			this.posY = this.startPosY + (this.endPosY - this.startPosY) * adjustedProgress;
			this.posZ = this.startPosZ + (this.endPosZ - this.startPosZ) * adjustedProgress;
		}
	}

	private void reset(boolean isIdle, boolean isForward, long nanoSecPerTransition, double radius) {
		this.isIdle = isIdle;
		this.isForward = isForward;
		this.nanoDuration = nanoSecPerTransition;
		double newTheta = this.theta + 1.5707963267948966D + this.random.nextDouble() * 3.141592653589793D;
		this.startPosX = this.posX;
		this.startPosY = this.posY;
		this.startPosZ = this.posZ;
		this.startRoll = this.roll;
		this.startPitch = this.pitch;
		this.startYaw = this.yaw;
		this.endRoll = -Math.signum(this.roll != 0.0D ? this.roll : 1.0D) * this.random.nextDouble() *
					   this.currentAmplitude * 1.0D;
		this.endPitch = -Math.signum(this.pitch != 0.0D ? this.pitch : 1.0D) * this.random.nextDouble() *
						this.currentAmplitude * 0.5D;
		this.endYaw =
			-Math.signum(this.yaw != 0.0D ? this.yaw : 1.0D) * this.random.nextDouble() * this.currentAmplitude * 0.5D;
		this.endPosX = this.currentAmplitude * Math.cos(newTheta) * 1.0D;
		this.endPosY = this.currentAmplitude * Math.sin(newTheta) * 1.0D;
		this.endPosZ = this.random.nextDouble() * this.currentAmplitude * 1.0D;
		this.theta = newTheta;
		if (isIdle) {
			this.currentAmplitude = radius * 0.5D;
		} else {
			this.currentAmplitude = radius;
		}

		if (isForward) {
			super.reset();
		}
	}

	public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
		double randomness = (double)state.getTotalUninterruptedShots() * 0.001D;
		if (randomness > 0.01D) {
			randomness = 0.01D;
		}

		double a = state.isAiming() ? 0.5D : 1.0D;
		this.reset(false, true, this.fireNanoTicksPerTransition, a * this.amplitude + randomness);
	}

	public void onUpdateState(LivingEntity player, GunClientState state) {
		if (state.isAiming()) {
			this.currentAmplitude *= 0.99D;
		}
	}
}
