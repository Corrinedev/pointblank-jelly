package mod.pbj.util;

import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class VelocityProviders {
	private static Random random = new Random();

	public VelocityProviders() {}

	public static Supplier<Vec3> randomVelocityProvider(double magnitude) {
		return ()
				   -> new Vec3(
					   (random.nextDouble() - (double)0.5F) * (double)2.0F * magnitude,
					   (random.nextDouble() - (double)0.5F) * (double)2.0F * magnitude,
					   (random.nextDouble() - (double)0.5F) * (double)2.0F * magnitude);
	}

	public static Supplier<Vec3> sphereVelocityProvider(float radius, Distribution radialDistribution) {
		return () -> {
			double adjustedRadius = radialDistribution.transform((double)radius);
			float theta = ((float)Math.PI * 2F) * random.nextFloat();
			float phi = (float)Math.acos((double)(2.0F * random.nextFloat() - 1.0F));
			float sinPhi = Mth.sin(phi);
			double x = adjustedRadius * (double)sinPhi * (double)Mth.cos(theta);
			double z = adjustedRadius * (double)sinPhi * (double)Mth.sin(theta);
			double y = adjustedRadius * Math.cos((double)phi);
			return new Vec3(x, y, z);
		};
	}

	public static Supplier<Vec3> hemisphereVelocityProvider(double radius, Distribution radialDistribution) {
		return () -> {
			double adjustedRadius = radialDistribution.transform(radius);
			float theta = ((float)Math.PI * 2F) * random.nextFloat();
			float phi = (float)Math.acos((double)random.nextFloat());
			float sinPhi = Mth.sin(phi);
			double x = adjustedRadius * (double)sinPhi * (double)Mth.cos(theta);
			double z = adjustedRadius * (double)sinPhi * (double)Mth.sin(theta);
			double y = adjustedRadius * Math.cos((double)phi);
			return new Vec3(x, y, z);
		};
	}

	public static enum Distribution {
		CONSTANT,
		UNIFORM,
		NORMAL(0.0F, 1.0F, 0.0F, Float.POSITIVE_INFINITY),
		TIGHT(0.5F, 0.25F, 0.25F, 2.0F);

		private float mean;
		private float standardDeviation;
		private float lowerBound;
		private float upperBound;

		private Distribution(float mean, float standardDeviation, float lowerBound, float upperBound) {
			this.mean = mean;
			this.standardDeviation = standardDeviation;
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		private Distribution() {}

		private double transform(double value) {
			double adjustedValue;
			switch (this) {
				case CONSTANT -> adjustedValue = value;
				case UNIFORM -> adjustedValue = VelocityProviders.random.nextDouble() * value;
				default ->
					adjustedValue = Mth.clamp(
						(VelocityProviders.random.nextGaussian() * (double)this.mean + (double)this.standardDeviation) *
							value,
						(double)this.lowerBound,
						(double)this.upperBound);
			}

			return adjustedValue;
		}
	}
}
