package mod.pbj.client.controller;

import mod.pbj.Config;
import mod.pbj.client.GunClientState;
import mod.pbj.client.GunStateListener;
import mod.pbj.feature.RecoilFeature;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PlayerRecoilController implements GunStateListener {
	private static final double middleProgress = (double)1.0F - Math.sqrt(2.0F) / (double)2.0F;
	private int elapsedTime;
	private final double ticksPerTransition;
	private double progress;
	private double prevProgress;
	private boolean isForward;
	private double actualAmplitude;
	private final double amplitude;
	private double dPitch;
	private double dPitchResetRatio = 1.0F;
	private double startPitch;
	private final double maxPitch;
	private double initialPitch;
	private long startTime;
	private long nanoSecPerTransition;
	private final long resetDurationNano;
	private State currentState;

	public PlayerRecoilController(double amplitude, double maxPitch, double duration) {
		this.currentState = PlayerRecoilController.State.IDLE;
		this.nanoSecPerTransition = (long)(duration * (double)1000000.0F);
		this.amplitude = amplitude;
		this.actualAmplitude = amplitude;
		this.ticksPerTransition = duration;
		this.maxPitch = -Math.abs(maxPitch);
		this.resetDurationNano = 200000000L;
	}

	public void onUpdateState(LivingEntity player, GunClientState state) {
		if ((double)this.elapsedTime < this.ticksPerTransition) {
			++this.elapsedTime;
		}
	}

	public void onRenderTick(
		LivingEntity player,
		GunClientState state,
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		float partialTicks) {
		if (this.currentState != PlayerRecoilController.State.IDLE) {
			this.prevProgress = this.progress;
			this.progress = this.getProgress(state, partialTicks);
			if (this.currentState == PlayerRecoilController.State.RECOILING && this.progress >= (double)1.0F) {
				if (Config.resetAutoFirePitchEnabled && this.resetDurationNano > 0L) {
					this.currentState = PlayerRecoilController.State.RESETTING;
					this.startTime = System.nanoTime();
					this.startPitch = player.getXRot();
					this.progress = this.prevProgress = 0.0F;
					this.nanoSecPerTransition = this.resetDurationNano;
				} else {
					this.currentState = PlayerRecoilController.State.IDLE;
				}
			}

			if (this.currentState == PlayerRecoilController.State.RESETTING) {
				this.progress = (double)0.5F - (double)0.5F * Math.cos(Math.PI * this.progress);
				if (this.progress >= (double)1.0F) {
					this.currentState = PlayerRecoilController.State.IDLE;
				}
			}

			if (this.currentState == PlayerRecoilController.State.RESETTING) {
				double dProgress = this.progress - this.prevProgress;
				float prevPitch = player.getXRot();
				this.dPitch = (this.initialPitch - this.startPitch) * dProgress;
				float newXRot = (float)((double)prevPitch + this.dPitch);
				player.setXRot(newXRot);
			} else {
				if (this.isForward && this.progress > middleProgress) {
					this.isForward = false;
				}

				double dProgress = this.progress - this.prevProgress;
				if (this.isForward) {
					this.dPitch = -this.actualAmplitude * dProgress / middleProgress;
				} else {
					this.dPitch =
						this.dPitchResetRatio * this.actualAmplitude * dProgress / ((double)1.0F - middleProgress);
				}

				float prevPitch = player.getXRot();
				float newXRot = (float)((double)prevPitch + this.dPitch);
				if (!this.isForward) {
					if ((double)newXRot < this.startPitch) {
						player.setXRot(newXRot);
					}
				} else {
					player.setXRot(newXRot);
				}
			}
		}
	}

	public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
		this.isForward = true;
		this.currentState = PlayerRecoilController.State.RECOILING;
		this.elapsedTime = -1;
		this.startTime = System.nanoTime();
		this.startPitch = player.getXRot();
		this.progress = this.prevProgress = 0.0F;
		this.actualAmplitude = state.isAiming() ? this.amplitude * (double)0.5F : this.amplitude;
		this.actualAmplitude *= RecoilFeature.getRecoilModifier(itemStack);
		if (state.getTotalUninterruptedShots() <= 1 || this.startPitch > this.initialPitch) {
			this.initialPitch = this.startPitch;
		}

		if (state.getTotalUninterruptedShots() > 1 &&
			!(Math.abs(this.startPitch - this.initialPitch) >= Math.abs(this.maxPitch))) {
			this.dPitchResetRatio = 0.1;
		} else {
			this.dPitchResetRatio = 1.0F;
		}
	}

	protected double getProgress(GunClientState gunClientState, float partialTicks) {
		double progress = (double)(System.nanoTime() - this.startTime) / (double)this.nanoSecPerTransition;
		if (progress > (double)1.0F) {
			progress = 1.0F;
		}

		return progress;
	}

	private enum State {
		IDLE,
		RECOILING,
		RESETTING;

		State() {}
	}
}
