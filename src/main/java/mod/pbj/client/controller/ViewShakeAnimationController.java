package mod.pbj.client.controller;

import mod.pbj.client.GunClientState;
import mod.pbj.item.FireModeInstance;
import net.minecraft.world.entity.LivingEntity;

public class ViewShakeAnimationController extends PryAnimationController {
	private final double speed;
	private final double amplitude;
	private double rollAmplitude = 0.0D;
	private double yawAmplitude = 0.0D;
	private double currentSpeed;
	private double currentAmplitude;
	private long currentNanoDuration;

	public ViewShakeAnimationController(double amplitude, double speed, double decay, long duration) {
		super(duration);
		this.amplitude = amplitude;
		this.speed = speed;
		this.currentSpeed = speed;
		this.currentAmplitude = amplitude;
		this.currentNanoDuration = this.nanoDuration;
	}

	protected double getProgress(GunClientState gunClientState, float partialTicks) {
		double progress = (double)(System.nanoTime() - this.startTime) / (double)this.currentNanoDuration;
		if (progress > 1.0D) {
			progress = 1.0D;
		}

		return progress;
	}

	public void onUpdateState(LivingEntity player, GunClientState state) {
		super.onUpdateState(player, state);
		double adjustedProgress = 1.0D - Math.pow(1.0D - this.progress, 0.1D);
		this.rollAmplitude = (1.0D - adjustedProgress) * this.rollAmplitude;
		this.yawAmplitude = (1.0D - adjustedProgress) * this.yawAmplitude;
	}

	public void reset(double endPitch, double endYaw, double endRoll) {
		this.rollAmplitude = this.currentAmplitude * (1.0D + Math.random());
		if (Math.random() > 0.5D) {
			this.rollAmplitude = -this.rollAmplitude;
		}

		this.yawAmplitude = this.currentAmplitude * (1.0D + Math.random());
		if (Math.random() > 0.5D) {
			this.yawAmplitude = -this.yawAmplitude;
		}

		super.reset(endPitch, endYaw, endRoll);
	}

	public void reset(FireModeInstance.ViewShakeDescriptor descriptor) {
		if (descriptor != null) {
			this.currentAmplitude = descriptor.amplitude();
			this.currentSpeed = descriptor.speed();
			this.currentNanoDuration = descriptor.duration() * 1000000L;
		} else {
			this.currentAmplitude = this.amplitude;
			this.currentSpeed = this.speed;
			this.currentNanoDuration = this.nanoDuration;
		}

		this.rollAmplitude = this.currentAmplitude * (1.0D + Math.random());
		if (Math.random() > 0.5D) {
			this.rollAmplitude = -this.rollAmplitude;
		}

		this.yawAmplitude = this.currentAmplitude * (1.0D + Math.random());
		if (Math.random() > 0.5D) {
			this.yawAmplitude = -this.yawAmplitude;
		}

		super.reset(this.pitch, this.yaw, this.roll);
	}

	protected double updateRoll(double progress) {
		double angle = progress * this.currentSpeed;
		this.roll = this.rollAmplitude * Math.sin(angle * 3.141592653589793D);
		return -this.roll;
	}

	protected double updateYaw(double progress) {
		double angle = progress * this.currentSpeed;
		this.yaw = this.yawAmplitude * Math.sin(angle * 3.141592653589793D);
		return -this.yaw;
	}
}
