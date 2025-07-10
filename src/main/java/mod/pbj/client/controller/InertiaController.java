package mod.pbj.client.controller;

import mod.pbj.client.GunClientState;
import mod.pbj.client.GunStateListener;
import mod.pbj.client.RealtimeLinearEaser;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class InertiaController implements GunStateListener {
	private double previousYaw;
	private double targetYawTiltAngle;
	private double previousPitch;
	private double targetPitchTiltAngle;
	private double maxTiltAngle;
	private double tiltFactor;
	private double smoothingFactor;
	private final double dampingFactor = 0.01;
	private final float threshold = 0.01F;
	private long lastUpdateTime = System.nanoTime();
	private double roll;
	private double pitch;
	private double yaw;
	private float dynamicModifier;
	private final RealtimeLinearEaser dynamicModifierEaser = new RealtimeLinearEaser(1000L);

	public InertiaController(double tiltFactor, double smoothingFactor, double maxTiltAngle) {
		this.tiltFactor = tiltFactor;
		this.smoothingFactor = smoothingFactor;
		this.maxTiltAngle = maxTiltAngle;
		this.reset();
	}

	public void onUpdateState(LivingEntity player, GunClientState state) {
		long currentTime = System.nanoTime();
		double deltaTime = (double)(currentTime - this.lastUpdateTime) / (double)1.0E9F;
		this.lastUpdateTime = currentTime;
		float currentYaw = player.getYRot();
		double deltaYaw = (double)currentYaw - this.previousYaw;
		double yawTurnSpeed = deltaYaw / deltaTime;
		float currentPitch = player.getXRot();
		double deltaPitch = (double)currentPitch - this.previousPitch;
		double pitchTurnSpeed = deltaPitch / deltaTime;
		double scaledTiltFactor = this.tiltFactor * (double)this.dynamicModifierEaser.update(this.dynamicModifier);
		double yawInterpolationSpeed = this.smoothingFactor * deltaTime;
		if (Math.abs(yawTurnSpeed) >= (double)this.threshold) {
			this.targetYawTiltAngle = yawTurnSpeed * (state.isAiming() ? 0.2 : (double)1.0F) * scaledTiltFactor;
		} else {
			this.targetYawTiltAngle *= this.dampingFactor;
			yawInterpolationSpeed *= 30.0F;
		}

		double pitchInterpolationSpeed = this.smoothingFactor * deltaTime;
		if (Math.abs(pitchTurnSpeed) >= (double)this.threshold) {
			this.targetPitchTiltAngle = pitchTurnSpeed * (state.isAiming() ? 0.2 : (double)1.0F) * scaledTiltFactor;
		} else {
			this.targetPitchTiltAngle *= this.dampingFactor;
			pitchInterpolationSpeed *= 30.0F;
		}

		this.targetYawTiltAngle = Mth.clamp(this.targetYawTiltAngle, -this.maxTiltAngle, this.maxTiltAngle);
		this.targetPitchTiltAngle = Mth.clamp(this.targetPitchTiltAngle, -this.maxTiltAngle, this.maxTiltAngle);
		this.yaw += (this.targetYawTiltAngle - this.yaw) * yawInterpolationSpeed;
		this.pitch += (this.targetPitchTiltAngle - this.pitch) * pitchInterpolationSpeed;
		this.roll += (this.targetYawTiltAngle - this.roll) * yawInterpolationSpeed;
		this.previousYaw = currentYaw;
		this.previousPitch = currentPitch;
	}

	public void onRenderTick(
		LivingEntity player,
		GunClientState state,
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		float partialTicks) {}

	public void reset() {
		this.yaw = 0.0F;
		this.pitch = 0.0F;
		this.roll = 0.0F;
		this.previousYaw = 0.0F;
		this.previousPitch = 0.0F;
		this.dynamicModifier = 1.0F;
		this.lastUpdateTime = System.nanoTime();
	}

	public void reset(Player player) {
		this.yaw = 0.0F;
		this.pitch = 0.0F;
		this.roll = 0.0F;
		this.previousYaw = player.getYRot();
		this.previousPitch = player.getXRot();
		this.dynamicModifier = 1.0F;
		this.lastUpdateTime = System.nanoTime();
	}

	public double getRoll() {
		return this.roll;
	}

	public double getPitch() {
		return this.pitch;
	}

	public double getYaw() {
		return this.yaw;
	}

	void setTiltFactor(double tiltFactor) {
		this.tiltFactor = tiltFactor;
	}

	void setSmoothingFactor(double smoothingFactor) {
		this.smoothingFactor = smoothingFactor;
	}

	void setMaxTiltAngle(double maxTiltAngle) {
		this.maxTiltAngle = maxTiltAngle;
	}

	public void setDynamicModifier(float dynamicModifier) {
		if (dynamicModifier < this.dynamicModifier) {
			this.dynamicModifierEaser.update(dynamicModifier, true);
		}

		this.dynamicModifier = dynamicModifier;
	}
}
