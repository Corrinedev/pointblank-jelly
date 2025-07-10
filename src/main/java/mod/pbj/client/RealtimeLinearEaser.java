package mod.pbj.client;

import net.minecraft.util.Mth;

public final class RealtimeLinearEaser {
	private long startTime;
	private final long durationNano;
	private float initialValue;
	private float currentValue = Float.NaN;
	private float targetValue = Float.NaN;

	public RealtimeLinearEaser(long duration) {
		this.durationNano = duration * 1000000L;
	}

	private void reset() {
		this.startTime = System.nanoTime();
		this.initialValue = this.currentValue;
	}

	public float update(float targetValue) {
		return this.update(targetValue, false);
	}

	public float update(float targetValue, boolean immediate) {
		if (targetValue != this.targetValue) {
			this.targetValue = targetValue;
			if (immediate || Float.isNaN(this.currentValue)) {
				this.currentValue = targetValue;
			}

			this.reset();
		}

		return this.currentValue = Mth.lerp(this.getProgress(), this.initialValue, targetValue);
	}

	private float getProgress() {
		return Mth.clamp((float)(System.nanoTime() - this.startTime) / (float)this.durationNano, 0.0F, 1.0F);
	}
}
