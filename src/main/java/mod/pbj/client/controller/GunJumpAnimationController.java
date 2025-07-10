package mod.pbj.client.controller;

import java.util.Random;
import mod.pbj.client.GunClientState;
import mod.pbj.client.GunStateListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class GunJumpAnimationController extends AbstractProceduralAnimationController implements GunStateListener {
	private double roll;
	private final double initialAmplitude;
	private double initialAmplitudeAdj;
	private final double rateOfAmplitudeDecay;
	private double rateOfAmplitudeDecayAdj;
	private final double initialAngularFrequency;
	private double initialAngularFrequencyAdj;
	private final double rateOfFrequencyIncrease;
	private double rateOfFrequencyIncreaseAdj;
	private final Random random = new Random();
	private boolean rollDirection;
	private boolean yawDirection;

	public GunJumpAnimationController(
		double initialAmplitude,
		double rateOfAmplitudeDecay,
		double initialAngularFrequency,
		double rateOfFrequencyIncrease,
		long ticksPerTransition) {
		super(ticksPerTransition);
		this.initialAmplitude = initialAmplitude;
		this.rateOfAmplitudeDecay = rateOfAmplitudeDecay;
		this.initialAngularFrequency = initialAngularFrequency;
		this.rateOfFrequencyIncrease = rateOfFrequencyIncrease;
		this.reset();
	}

	static double oscillatingFunction(double t, double a0, double alpha, double omega0, double beta) {
		double aT = a0 * Math.exp(-alpha * t);
		double omegaT = omega0 + beta * t;
		return aT * Math.sin(omegaT * t);
	}

	static double derivativeOscillatingFunction(double t, double a0, double alpha, double omega0, double beta) {
		double aT = a0 * Math.exp(-alpha * t);
		double dAT = -alpha * aT;
		double omega_t = omega0 + beta * t;
		double sinTerm = Math.sin(omega_t * t);
		double cosTerm = Math.cos(omega_t * t);
		return dAT * sinTerm + aT * (beta * t + omega_t) * cosTerm;
	}

	public void onRenderTick(
		LivingEntity player,
		GunClientState gunClientState,
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		float partialTicks) {
		super.onRenderTick(player, gunClientState, itemStack, itemDisplayContext, partialTicks);
		if (!this.isDone) {
			double oscillation = oscillatingFunction(
				this.progress * 10.0D,
				this.initialAmplitudeAdj,
				this.rateOfAmplitudeDecayAdj,
				this.initialAngularFrequencyAdj,
				this.rateOfFrequencyIncreaseAdj);
			this.posX = -oscillation * 0.05D;
			this.posY = -oscillation * 0.3D;
			this.posZ = oscillation * 0.01D;
			this.roll = oscillation * 0.08D * (double)(this.rollDirection ? 1 : -1);
			this.pitch = -oscillation * 0.12D;
			this.yaw = oscillation * 0.05D * (double)(this.yawDirection ? 1 : -1);
		}
	}

	public void reset() {
		double randomness = 0.8D;
		this.rollDirection = this.random.nextBoolean();
		this.yawDirection = this.random.nextBoolean();
		this.initialAmplitudeAdj =
			this.initialAmplitude + (this.random.nextDouble() - 0.5D) * this.initialAmplitude * randomness;
		this.rateOfAmplitudeDecayAdj =
			this.rateOfAmplitudeDecay + (this.random.nextDouble() - 0.5D) * this.rateOfAmplitudeDecay * randomness;
		this.initialAngularFrequencyAdj = this.initialAngularFrequency +
										  (this.random.nextDouble() - 0.5D) * this.initialAngularFrequency * randomness;
		this.rateOfFrequencyIncreaseAdj =
			this.rateOfFrequencyIncrease + (this.random.nextDouble() - 0.5D) * this.rateOfFrequencyIncrease * 0.01D;
		super.reset();
	}

	public void onJumping(LivingEntity player, GunClientState state, ItemStack itemStack) {
		this.reset();
		if (state != null && state.isAiming()) {
			this.initialAmplitudeAdj *= 0.5D;
		}
	}

	public double getRoll() {
		return this.roll;
	}
}
