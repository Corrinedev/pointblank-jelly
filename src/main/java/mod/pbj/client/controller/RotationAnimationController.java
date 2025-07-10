package mod.pbj.client.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.pbj.client.GunClientState;
import mod.pbj.client.render.GunItemRenderer;
import mod.pbj.item.GunItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;

public class RotationAnimationController extends AbstractProceduralAnimationController {
	private static final float MODEL_SCALE = 0.0625F;
	private Phase phase;
	private long phaseStartTime;
	private double phaseStartDistance;
	private double rotationsPerSecond;
	private double decelerationRate;
	private double accelerationRate;
	private PhaseMapper phaseMapper;
	private DistanceFunction phaseDistanceFunction;

	private RotationAnimationController() {
		super(0L);
		this.phase = RotationAnimationController.Phase.IDLE;
		this.rotationsPerSecond = 5.0F;
		this.decelerationRate = 5.0F;
		this.accelerationRate = 1.0F;
		this.phaseDistanceFunction = RotationAnimationController.ZeroDistance.INSTANCE;
	}

	public void onStateTick(LivingEntity player, GunClientState state) {
		GunClientState.FireState fireState = state.getFireState();
		Phase newPhase = this.phaseMapper.getPhase(fireState);
		if (newPhase != this.phase) {
			double elapsedPhaseTime = (double)(System.nanoTime() - this.phaseStartTime) / (double)1.0E9F;
			double previousVelocity = this.phaseDistanceFunction.getVelocity(elapsedPhaseTime);
			double previousPhaseDurationSeconds = (double)(System.nanoTime() - this.phaseStartTime) / (double)1.0E9F;
			double previousPhaseDistance = this.phaseDistanceFunction.getDistance(previousPhaseDurationSeconds);
			this.phaseStartDistance += previousPhaseDistance;
			this.phaseStartTime = System.nanoTime();
			switch (newPhase) {
				case IDLE ->
					this.phaseDistanceFunction = new DecelerationDistance(previousVelocity, this.decelerationRate);
				case PREPARING ->
					this.phaseDistanceFunction = new AccelerationDistance(previousVelocity, this.accelerationRate);
				case RUNNING ->
					this.phaseDistanceFunction = new AccelerationDistance(previousVelocity, this.accelerationRate);
				case COMPLETING ->
					this.phaseDistanceFunction = new DecelerationDistance(previousVelocity, this.decelerationRate);
			}

			this.phase = newPhase;
		}
	}

	public void render(
		GunItemRenderer renderer,
		GunClientState gunClientState,
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		poseStack.pushPose();
		float xPivot = bone.getPivotX() * MODEL_SCALE;
		float yPivot = bone.getPivotY() * MODEL_SCALE;
		float zPivot = bone.getPivotZ() * MODEL_SCALE;
		double elapsedPhaseTime = (double)(System.nanoTime() - this.phaseStartTime) / (double)1.0E9F;
		double elapsedPhaseDistance = this.phaseDistanceFunction.getDistance(elapsedPhaseTime);
		double distance = this.phaseStartDistance + elapsedPhaseDistance;
		double currentDistanceInDegress = this.rotationsPerSecond * distance * (double)360.0F;
		poseStack.translate(xPivot, yPivot, zPivot);
		poseStack.mulPose((new Quaternionf())
							  .rotationXYZ(
								  ((float)Math.PI / 180F) * (float)((double)0.0F + (double)0.0F * this.progress),
								  ((float)Math.PI / 180F) * (float)((double)0.0F + (double)0.0F * this.progress),
								  ((float)Math.PI / 180F) * (float)((double)0.0F - currentDistanceInDegress)));
		poseStack.translate(-xPivot, -yPivot, -zPivot);
		renderer.renderCubesOfBoneParent(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		poseStack.popPose();
	}

	public void renderRecursively(
		GunItemRenderer renderer,
		PoseStack poseStack,
		GunItem animatable,
		GeoBone bone,
		RenderType renderType,
		MultiBufferSource bufferSource,
		VertexConsumer buffer,
		boolean isReRender,
		float partialTick,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		poseStack.pushPose();
		float xPivot = bone.getPivotX() * MODEL_SCALE;
		float yPivot = bone.getPivotY() * MODEL_SCALE;
		float zPivot = bone.getPivotZ() * MODEL_SCALE;
		double elapsedPhaseTime = (double)(System.nanoTime() - this.phaseStartTime) / (double)1.0E9F;
		double elapsedPhaseDistance = this.phaseDistanceFunction.getDistance(elapsedPhaseTime);
		double distance = this.phaseStartDistance + elapsedPhaseDistance;
		double currentDistanceInDegress = this.rotationsPerSecond * distance * (double)360.0F;
		poseStack.translate(xPivot, yPivot, zPivot);
		poseStack.mulPose((new Quaternionf())
							  .rotationXYZ(
								  ((float)Math.PI / 180F) * (float)((double)0.0F + (double)0.0F * this.progress),
								  ((float)Math.PI / 180F) * (float)((double)0.0F + (double)0.0F * this.progress),
								  ((float)Math.PI / 180F) * (float)((double)0.0F - currentDistanceInDegress)));
		poseStack.translate(-xPivot, -yPivot, -zPivot);
		renderer.renderRecursivelySuper(
			poseStack,
			animatable,
			bone,
			renderType,
			bufferSource,
			buffer,
			isReRender,
			partialTick,
			packedLight,
			packedOverlay,
			red,
			green,
			blue,
			alpha);
		poseStack.popPose();
	}

	public enum Phase {
		IDLE,
		PREPARING,
		RUNNING,
		COMPLETING;

		Phase() {}
	}

	private static class FirePhaseMapper implements PhaseMapper {
		static final FirePhaseMapper INSTANCE = new FirePhaseMapper();

		private FirePhaseMapper() {}

		public Phase getPhase(GunClientState.FireState fireState) {
			Phase phase;
			switch (fireState) {
				case PREPARE_FIRE_SINGLE:
				case PREPARE_FIRE_COOLDOWN_SINGLE:
				case PREPARE_FIRE_AUTO:
				case PREPARE_FIRE_COOLDOWN_AUTO:
				case PREPARE_FIRE_BURST:
				case PREPARE_FIRE_COOLDOWN_BURST:
					phase = RotationAnimationController.Phase.PREPARING;
					break;
				case FIRE_SINGLE:
				case FIRE_COOLDOWN_SINGLE:
				case FIRE_AUTO:
				case FIRE_COOLDOWN_AUTO:
				case FIRE_BURST:
				case FIRE_COOLDOWN_BURST:
					phase = RotationAnimationController.Phase.RUNNING;
					break;
				case COMPLETE_FIRE:
				case COMPLETE_FIRE_COOLDOWN:
					phase = RotationAnimationController.Phase.COMPLETING;
					break;
				default:
					phase = RotationAnimationController.Phase.IDLE;
			}

			return phase;
		}
	}

	private static class ReloadPhaseMapper implements PhaseMapper {
		static final ReloadPhaseMapper INSTANCE = new ReloadPhaseMapper();

		private ReloadPhaseMapper() {}

		public Phase getPhase(GunClientState.FireState fireState) {
			Phase phase;
			switch (fireState) {
				case PREPARE_RELOAD:
				case PREPARE_RELOAD_COOLDOWN:
				case PREPARE_RELOAD_ITER:
				case PREPARE_RELOAD_COOLDOWN_ITER:
					phase = RotationAnimationController.Phase.PREPARING;
					break;
				case RELOAD:
				case RELOAD_COOLDOWN:
				case RELOAD_ITER:
				case RELOAD_COOLDOWN_ITER:
					phase = RotationAnimationController.Phase.RUNNING;
					break;
				case COMPLETE_RELOAD:
				case COMPLETE_RELOAD_COOLDOWN:
					phase = RotationAnimationController.Phase.COMPLETING;
					break;
				default:
					phase = RotationAnimationController.Phase.IDLE;
			}

			return phase;
		}
	}

	private record AccelerationDistance(double initialVelocity, double acceleration) implements DistanceFunction {
		public double getDistance(double t) {
			double distanceWithConstantSpeed = 0.0F;
			double finalVelocity = this.initialVelocity + this.acceleration * t;
			double tMax;
			if (finalVelocity > (double)1.0F) {
				tMax = Math.max(((double)1.0F - this.initialVelocity) / this.acceleration, 0.0F);
				distanceWithConstantSpeed = t - tMax;
			} else {
				tMax = t;
			}

			return this.initialVelocity * tMax + (double)0.5F * this.acceleration * tMax * tMax +
				distanceWithConstantSpeed;
		}

		public double getVelocity(double t) {
			double finalVelocity = this.initialVelocity + this.acceleration * t;
			return Math.min(finalVelocity, 1.0F);
		}
	}

	private record DecelerationDistance(double initialVelocity, double deceleration) implements DistanceFunction {
		public double getDistance(double t) {
			double finalVelocity = this.initialVelocity - t * this.deceleration;
			if (finalVelocity < (double)0.0F) {
				double tStop = this.initialVelocity / this.deceleration;
				return this.initialVelocity * tStop - (double)0.5F * this.deceleration * tStop * tStop;
			} else {
				return this.initialVelocity * t - (double)0.5F * this.deceleration * t * t;
			}
		}

		public double getVelocity(double t) {
			double finalVelocity = this.initialVelocity - t * this.deceleration;
			return Math.max(finalVelocity, 0.0F);
		}
	}

	private static class ZeroDistance implements DistanceFunction {
		static final ZeroDistance INSTANCE = new ZeroDistance();

		private ZeroDistance() {}

		public double getDistance(double t) {
			return 0.0F;
		}

		public double getVelocity(double t) {
			return 0.0F;
		}
	}

	private static class ConstantDistance implements DistanceFunction {
		static final ConstantDistance INSTANCE = new ConstantDistance();

		private ConstantDistance() {}

		public double getDistance(double t) {
			return t;
		}

		public double getVelocity(double t) {
			return t;
		}
	}

	public static class Builder {
		private String modelPartName;
		private double rotationsPerSecond = 5.0F;
		private double decelerationRate = 5.0F;
		private double accelerationRate = 1.0F;
		private PhaseMapper phaseMapper;

		public Builder() {
			this.phaseMapper = RotationAnimationController.FirePhaseMapper.INSTANCE;
		}

		public String getModelPartName() {
			return this.modelPartName;
		}

		public Builder withPhase(String phase) {
			switch (phase) {
				case "fire" -> this.phaseMapper = RotationAnimationController.FirePhaseMapper.INSTANCE;
				case "reload" -> this.phaseMapper = RotationAnimationController.ReloadPhaseMapper.INSTANCE;
				default -> throw new IllegalArgumentException("Invalid phase name: " + phase);
			}

			return this;
		}

		public Builder withPhaseMapper(PhaseMapper phaseMapper) {
			this.phaseMapper = phaseMapper;
			return this;
		}

		public Builder withModelPart(String modelPartName) {
			this.modelPartName = modelPartName;
			return this;
		}

		public Builder withDecelerationRate(double decelerationRate) {
			this.decelerationRate = decelerationRate;
			return this;
		}

		public Builder withAccelerationRate(double accelerationRate) {
			this.accelerationRate = accelerationRate;
			return this;
		}

		public Builder withRotationsPerMinute(double rotationsPerMinute) {
			this.rotationsPerSecond = rotationsPerMinute / (double)60.0F;
			return this;
		}

		public RotationAnimationController build() {
			if (this.modelPartName == null) {
				throw new IllegalStateException("Invalid rotation configutaion, missing model part name");
			} else {
				RotationAnimationController controller = new RotationAnimationController();
				controller.accelerationRate = this.accelerationRate;
				controller.decelerationRate = this.decelerationRate;
				controller.rotationsPerSecond = this.rotationsPerSecond;
				controller.phaseMapper = this.phaseMapper;
				return controller;
			}
		}
	}

	private interface DistanceFunction {
		double getDistance(double var1);

		double getVelocity(double var1);
	}

	public interface PhaseMapper {
		Phase getPhase(GunClientState.FireState var1);
	}
}
