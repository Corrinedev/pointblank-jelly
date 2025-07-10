package mod.pbj.util;

import java.util.Random;
import net.minecraft.util.Mth;

public class Interpolators {
	public Interpolators() {}

	public static class RealTimeProgressProvider implements FloatProvider {
		private long startTime;
		private long lifetimeNanos;

		public RealTimeProgressProvider(long lifetimeMillis) {
			this(lifetimeMillis, 0L);
		}

		public RealTimeProgressProvider(long lifetimeMillis, long delayMillis) {
			this.startTime = System.nanoTime() + TimeUnit.MILLISECOND.toNanos(delayMillis);
			this.lifetimeNanos = TimeUnit.MILLISECOND.toNanos(lifetimeMillis);
		}

		public float getValue() {
			if (this.startTime > System.nanoTime()) {
				return Float.NEGATIVE_INFINITY;
			} else {
				float progress =
					Mth.clamp((float)(System.nanoTime() - this.startTime) / (float)this.lifetimeNanos, 0.0F, 1.0F);
				return progress;
			}
		}
	}

	public static class RandomFloatProvider implements FloatProvider {
		private static Random random = new Random();
		private float value;

		public RandomFloatProvider(float maxValue) {
			this.value = maxValue;
		}

		public float getValue() {
			return this.value * random.nextFloat();
		}
	}

	public static class ConstantFloatProvider implements FloatInterpolator {
		private float value;

		public ConstantFloatProvider(float value) {
			this.value = value;
		}

		public float getValue(float progress) {
			return this.value;
		}
	}

	public static class LinearInterpolatorFloatProvider implements FloatInterpolator {
		private float startValue;
		private float endValue;

		public LinearInterpolatorFloatProvider(float startValue, float endValue) {
			this.startValue = startValue;
			this.endValue = endValue;
		}

		public float getValue(float progress) {
			return Mth.lerp(progress, this.startValue, this.endValue);
		}
	}

	public static class EaseInEaseOutFloatProvider implements FloatInterpolator {
		private float value;

		public EaseInEaseOutFloatProvider(float value) {
			this.value = value;
		}

		public float getValue(float progress) {
			return this.value * Mth.sin(Mth.sqrt(progress) * (float)Math.PI);
		}
	}

	public static class EaseInEaseOutFloatProvider2 implements FloatInterpolator {
		private float value;

		public EaseInEaseOutFloatProvider2(float value) {
			this.value = value;
		}

		public float getValue(float progress) {
			return this.value * Mth.sin(progress * (float)Math.PI);
		}
	}

	public static class AnotherEaseInEaseOutFloatProvider implements FloatInterpolator {
		private float value;
		private float fadeIn;
		private float fadeOut;

		public AnotherEaseInEaseOutFloatProvider(float value, float fadeIn, float fadeOut) {
			this.value = value;
			this.fadeIn = fadeIn;
			this.fadeOut = fadeOut;
		}

		public float getValue(float progress) {
			float factor;
			if (progress < this.fadeIn) {
				float ap = progress / this.fadeIn;
				factor = Mth.sin(((float)Math.PI / 2F) * ap);
			} else if (progress > this.fadeOut) {
				float ap = (progress - this.fadeOut) / (1.0F - this.fadeOut);
				factor = Mth.cos(((float)Math.PI / 2F) * ap);
			} else {
				factor = 1.0F;
			}

			factor = Mth.clamp(factor, 0.0F, 1.0F);
			return factor * this.value;
		}
	}

	public static class EaseInFloatProvider implements FloatInterpolator {
		private float value;

		public EaseInFloatProvider(float value) {
			this.value = value;
		}

		public float getValue(float progress) {
			return this.value * Mth.sin(progress * (float)Math.PI * 0.5F);
		}
	}

	public static class EaseOutFloatProvider implements FloatInterpolator {
		private float value;

		public EaseOutFloatProvider(float value) {
			this.value = value;
		}

		public float getValue(float progress) {
			return this.value * Mth.cos(progress * (float)Math.PI * 0.5F);
		}
	}

	@FunctionalInterface
	public interface FloatInterpolator {
		float getValue(float var1);
	}

	public interface FloatProvider {
		float getValue();
	}
}
