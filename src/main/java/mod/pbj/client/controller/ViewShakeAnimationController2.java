package mod.pbj.client.controller;

import java.util.Random;
import mod.pbj.client.GunClientState;
import mod.pbj.client.GunStateListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ViewShakeAnimationController2 extends AbstractProceduralAnimationController implements GunStateListener {
	private final double initialAmplitude;
	private double initialAmplitudeAdj;
	private final double rateOfAmplitudeDecay;
	private double rateOfAmplitudeDecayAdj;
	private final double initialAngularFrequency;
	private double initialAngularFrequencyAdj;
	private final double rateOfFrequencyIncrease;
	private double rateOfFrequencyIncreaseAdj;
	private final Random random = new Random();

	public ViewShakeAnimationController2(
		double initialAmplitude,
		double rateOfAmplitudeDecay,
		double initialAngularFrequency,
		double rateOfFrequencyIncrease,
		long duration) {
		super(duration);
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
			double recoilPosZ = oscillatingFunction(
				this.progress * 10.0D,
				this.initialAmplitudeAdj,
				this.rateOfAmplitudeDecayAdj,
				this.initialAngularFrequencyAdj,
				this.rateOfFrequencyIncreaseAdj);
			this.roll = recoilPosZ * 10.0D;
		}
	}

	public void reset() {
		double randomness = 0.1D;
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

	public void reset(
		double initialAmplitude,
		double rateOfAmplitudeDecay,
		double initialAngularFrequency,
		double rateOfFrequencyIncrease) {
		double randomness = 0.1D;
		this.initialAmplitudeAdj = initialAmplitude + (this.random.nextDouble() - 0.5D) * initialAmplitude * randomness;
		this.rateOfAmplitudeDecayAdj =
			rateOfAmplitudeDecay + (this.random.nextDouble() - 0.5D) * rateOfAmplitudeDecay * randomness;
		this.initialAngularFrequencyAdj =
			initialAngularFrequency + (this.random.nextDouble() - 0.5D) * initialAngularFrequency * randomness;
		this.rateOfFrequencyIncreaseAdj =
			rateOfFrequencyIncrease + (this.random.nextDouble() - 0.5D) * rateOfFrequencyIncrease * 0.01D;
		super.reset();
	}

	public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
		this.reset();
	}

	public double getRoll() {
		return this.roll;
	}
}
