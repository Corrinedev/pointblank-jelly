package mod.pbj.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BiDirectionalInterpolator implements GunStateListener {
	private long startTime;
	private long fullDuration;
	private long duration;
	private boolean isDone;
	private Position targetPosition;
	private double value;
	private double progress;

	public BiDirectionalInterpolator(long durationMillis) {
		this.fullDuration = durationMillis * 1000000L;
		this.isDone = true;
		this.set(Position.START, true);
	}

	public void setFullDuration(long msDuration) {
		this.fullDuration = msDuration * 1000000L;
	}

	public long getDurationMS() {
		return this.fullDuration / 1000000L;
	}

	public double getValue() {
		return this.value;
	}

	public boolean isDone() {
		return this.isDone;
	}

	private double calculateProgress() {
		long elapsedTime = System.nanoTime() - this.startTime;
		double t = (double)elapsedTime / (double)this.duration;
		t = t < 0.5D ? 4.0D * t * t * t : (t - 1.0D) * (2.0D * t - 2.0D) * (2.0D * t - 2.0D) + 1.0D;
		this.progress = Mth.clamp(t, 0.0D, 1.0D);
		return this.progress;
	}

	public void set(Position position) {
		this.set(position, true);
	}

	public void set(Position targetPosition, boolean immediate) {
		if (immediate) {
			this.completeTransition(targetPosition);
		} else if (targetPosition != this.targetPosition) {
			this.startTransition(targetPosition);
		}
	}

	private void startTransition(Position targetPosition) {
		this.targetPosition = targetPosition;
		if (this.isDone) {
			this.isDone = false;
			this.duration = this.fullDuration;
			this.value = targetPosition == Position.START ? 0.0D : 1.0D;
			this.startTime = System.nanoTime();
		} else {
			this.startTime = System.nanoTime() - (long)((double)this.fullDuration * (1.0D - this.progress));
		}
	}

	private void completeTransition(Position targetPosition) {
		this.isDone = true;
		this.value = targetPosition == Position.START ? 0.0D : 1.0D;
	}

	public void onRenderTick(
		LivingEntity player,
		GunClientState state,
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		float partialTicks) {
		this.update();
	}

	public void update() {
		if (!this.isDone) {
			this.calculateProgress();
			if (this.targetPosition == Position.END) {
				this.value = this.progress;
			} else {
				this.value = 1.0D - this.progress;
			}

			if (this.progress >= 1.0D) {
				this.completeTransition(this.targetPosition);
			}
		}
	}

	public enum Position { START, END }
}
