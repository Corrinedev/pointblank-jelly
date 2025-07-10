package mod.pbj.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class TopDownAttackTrajectory implements Trajectory<TopDownAttackTrajectory.Phase> {
	private static final Logger LOGGER = LogManager.getFormatterLogger("pointblank");
	private static final double TIME_PER_TICK = 0.05;
	private static final double DEFAULT_CLIMB_ACCELERATION = 0.25F;
	private static final double DEFAULT_SOFT_LAUNCH_CURVATURE = Math.toRadians(20.0F);
	private static final double DEFAULT_SOFT_LAUNCH_SPEED = 10.0F;
	private static final double MIN_CURVATURE_ANGLE = Math.toRadians(15.0F);
	private static final double COS_45 = Math.cos((Math.PI / 4D));
	private static final double MIN_ATTACK_ANGLE = Math.toRadians(30.0F);
	private static final double SOFT_LAUNCH_DISTANCE = 5.0F;
	private static final double MIN_DISTANCE_TO_TARGET_FOR_TOP_DOWN_ATTACK = 25.0F;
	private static final Vec3 UP = new Vec3(0.0F, 1.0F, 0.0F);
	private static final Vec3 DOWN = new Vec3(0.0F, -1.0F, 0.0F);
	private Vec3 targetPosition;
	private Vec3 startToTargetNormalized;
	private double defaultSpeed;
	private double currentSpeed;
	private double remainingClimbSpeed;
	private double climbToDescendTurnSpeed;
	private int currentTick;
	private int climbTicks;
	private int descendTicks;
	private double climbToDescendCurvature;
	private Vec3 climbVector;
	private Vec3 endOfClimbPosition;
	private Vec3 deltaMovement;
	private Vec3 startOfTickPosition;
	private Vec3 endOfTickPosition;
	private Vec3 verticalRotationAxis;
	private Phase phase;
	private Runnable pendingCorrection;
	private double wouldBeLaunchOffset;
	private double startClimbHOffset;
	private double startClimbVOffset;
	private double softLaunchCurvature;
	private int softLaunchTicks;
	private int softLaunchTurnTicks;
	private int softLaunchTicksBeforeTurn;
	private double defaultSoftLaunchSpeed;
	private double softLaunchSpeedBeforeTurn;
	private double climbAcceleration = 0.25F;
	private final List<TrajectoryPhaseListener<Phase>> trajectoryPhaseListeners = new ArrayList<>();

	public static TopDownAttackTrajectory
	createTrajectory(Vec3 startPosition, Vec3 targetPosition, double defaultSpeedBlocksPerSecond) {
		LOGGER.trace("Initializing trajectory");
		Vec3 startToTarget = targetPosition.subtract(startPosition);
		Vec3 startToTargetNormalized = startToTarget.normalize();
		Vec3 wouldBeClimbStartPosition = startPosition.add(startToTargetNormalized.scale(5.0F));
		LOGGER.trace(
			"Start of soft launch position: (%.5f, %.5f, %.5f)", startPosition.x, startPosition.y, startPosition.z);
		LOGGER.trace(
			"End of soft launch position: (%.5f, %.5f, %.5f)",
			wouldBeClimbStartPosition.x,
			wouldBeClimbStartPosition.y,
			wouldBeClimbStartPosition.z);
		double climbToDescendCurvature = MIN_CURVATURE_ANGLE;
		double defaultSpeed = defaultSpeedBlocksPerSecond * 0.05;
		double climbToDescendTurnSpeed = defaultSpeed * (double)0.75F;
		double distanceToTarget = startToTarget.length();
		if (distanceToTarget < (double)25.0F) {
			return null;
		} else {
			ClimbInfo climbInfo = getClimbInfo(
				startToTarget,
				targetPosition,
				wouldBeClimbStartPosition,
				climbToDescendCurvature,
				climbToDescendTurnSpeed);
			if (climbInfo == null) {
				return null;
			} else {
				LOGGER.trace("Climb to descend curvature: %.3f°", Math.toDegrees(climbToDescendCurvature));
				LOGGER.trace("Target pos: (%.5f, %.5f, %.5f)", targetPosition.x, targetPosition.y, targetPosition.z);
				Vec3 normalizedClimbVector = climbInfo.climbVector.normalize();
				LOGGER.trace(
					"Original normalized climb vector: (%.3f, %.3f, %.3f)",
					normalizedClimbVector.x,
					normalizedClimbVector.y,
					normalizedClimbVector.z);
				TopDownAttackTrajectory topDownAttackTrajectory = new TopDownAttackTrajectory();
				initSoftLaunch(topDownAttackTrajectory, climbInfo);
				topDownAttackTrajectory.climbToDescendTurnSpeed = climbToDescendTurnSpeed;
				topDownAttackTrajectory.climbToDescendCurvature = climbToDescendCurvature;
				topDownAttackTrajectory.climbVector = climbInfo.climbVector;
				topDownAttackTrajectory.endOfClimbPosition = wouldBeClimbStartPosition.add(climbInfo.climbVector);
				LOGGER.trace(
					"Start of climb position: (%.5f, %.5f, %.5f)",
					wouldBeClimbStartPosition.x,
					wouldBeClimbStartPosition.y,
					wouldBeClimbStartPosition.z);
				LOGGER.trace(
					"End of climb position: (%.5f, %.5f, %.5f)",
					topDownAttackTrajectory.endOfClimbPosition.x,
					topDownAttackTrajectory.endOfClimbPosition.y,
					topDownAttackTrajectory.endOfClimbPosition.z);
				topDownAttackTrajectory.setPhase(TopDownAttackTrajectory.Phase.SOFT_LAUNCH);
				topDownAttackTrajectory.targetPosition = targetPosition;
				topDownAttackTrajectory.startToTargetNormalized = startToTargetNormalized;
				topDownAttackTrajectory.defaultSpeed = defaultSpeedBlocksPerSecond * 0.05;
				topDownAttackTrajectory.currentSpeed = defaultSpeed;
				topDownAttackTrajectory.startOfTickPosition = topDownAttackTrajectory.endOfTickPosition = startPosition;
				return topDownAttackTrajectory;
			}
		}
	}

	private static ClimbInfo getClimbInfo(
		Vec3 startToTarget,
		Vec3 targetPosition,
		Vec3 wouldBeClimbStartPosition,
		double climbToDescendCurvature,
		double climbToDescendTurnSpeed) {
		double downSimilarity = startToTarget.normalize().dot(DOWN);
		if (downSimilarity > Math.cos(Math.toDegrees(10.0F))) {
			return null;
		} else {
			double radius = getRadius(climbToDescendTurnSpeed, climbToDescendCurvature);
			LOGGER.trace("Radius: %.5f", radius);
			Vec3 pivot = getPivot(wouldBeClimbStartPosition, targetPosition, radius, 0.0F);
			LOGGER.trace("Pivot: (%.5f, %.5f, %.5f)", pivot.x, pivot.y, pivot.z);
			return getClimbInfo(wouldBeClimbStartPosition, pivot, radius);
		}
	}

	private static void initSoftLaunch(TopDownAttackTrajectory topDownAttackTrajectory, ClimbInfo climbInfo) {
		double wouldBeLaunchOffset = 5.0F;
		double defaultSoftLaunchCurvature = DEFAULT_SOFT_LAUNCH_CURVATURE;
		double softLaunchSpeed = 0.5F;
		topDownAttackTrajectory.defaultSoftLaunchSpeed = softLaunchSpeed;
		if (defaultSoftLaunchCurvature > climbInfo.launchAngle) {
			defaultSoftLaunchCurvature = climbInfo.launchAngle / (double)10.0F;
		}

		double adjustedCurvature = MiscUtil.adjustDivisor(climbInfo.launchAngle, defaultSoftLaunchCurvature);
		if (Math.abs(adjustedCurvature - defaultSoftLaunchCurvature) > 1.0E-6) {
			LOGGER.trace("Adjusted phi to: %.3f°", Math.toDegrees(adjustedCurvature));
		}

		double softLaunchRadius = softLaunchSpeed * (double)0.5F / Math.sin(adjustedCurvature * (double)0.5F);
		int n = (int)Math.round(climbInfo.launchAngle / adjustedCurvature);
		double dnm1 = (double)2.0F * softLaunchRadius * Math.sin(adjustedCurvature * (double)(n - 1) * (double)0.5F);
		double turnPointDistanceOffset = dnm1 * (double)0.5F / Math.cos(adjustedCurvature * (double)n * (double)0.5F);
		double distanceToTurn = wouldBeLaunchOffset - turnPointDistanceOffset;
		LOGGER.trace("phi: %.3f°, L: %.3f°", Math.toDegrees(adjustedCurvature), Math.toDegrees(climbInfo.launchAngle));
		double D = (double)2.0F * softLaunchRadius * Math.sin(adjustedCurvature * (double)n * (double)0.5F);
		topDownAttackTrajectory.startClimbHOffset =
			D * Math.cos(adjustedCurvature * (double)(n + 1) * (double)0.5F) + distanceToTurn;
		topDownAttackTrajectory.startClimbVOffset = D * Math.sin(adjustedCurvature * (double)(n + 1) * (double)0.5F);
		LOGGER.trace(
			"Start climb at hv offsets: (%.3f, %.3f)",
			topDownAttackTrajectory.startClimbHOffset,
			topDownAttackTrajectory.startClimbVOffset);
		topDownAttackTrajectory.softLaunchSpeedBeforeTurn = MiscUtil.adjustDivisor(distanceToTurn, softLaunchSpeed);
		topDownAttackTrajectory.softLaunchTicksBeforeTurn = (int)Math.round(distanceToTurn / softLaunchSpeed);
		LOGGER.trace(
			"Ticks before turn: %d, Ticks to turn: %d, R: %.3f, speed: %.3f, speed before turn: %.3f, ",
			topDownAttackTrajectory.softLaunchTicksBeforeTurn,
			n,
			softLaunchRadius,
			topDownAttackTrajectory.defaultSoftLaunchSpeed,
			topDownAttackTrajectory.softLaunchSpeedBeforeTurn);
		LOGGER.trace(
			"Would be launch offset: %.3f, Turn offset:  %.3f",
			topDownAttackTrajectory.wouldBeLaunchOffset,
			distanceToTurn);
		topDownAttackTrajectory.softLaunchTurnTicks = n;
		topDownAttackTrajectory.softLaunchCurvature = adjustedCurvature;
		topDownAttackTrajectory.softLaunchTicks =
			topDownAttackTrajectory.softLaunchTicksBeforeTurn + topDownAttackTrajectory.softLaunchTurnTicks;
		LOGGER.trace("Total soft launch ticks: %d", topDownAttackTrajectory.softLaunchTicks);
	}

	private TopDownAttackTrajectory() {}

	public void addListener(TrajectoryPhaseListener<Phase> listener) {
		this.trajectoryPhaseListeners.add(listener);
	}

	public Phase getPhase() {
		return this.phase;
	}

	public Vec3 getStartOfTickPosition() {
		return this.startOfTickPosition;
	}

	public Vec3 getEndOfTickPosition() {
		return this.endOfTickPosition;
	}

	public Vec3 getDeltaMovement() {
		return this.deltaMovement;
	}

	public boolean isCompleted() {
		return this.phase == TopDownAttackTrajectory.Phase.COMPLETED;
	}

	public void tick() {
		switch (this.phase) {
			case SOFT_LAUNCH -> this.tickSoftLaunch();
			case CLIMB -> this.tickClimb();
			case CLIMB_TO_DESCEND_TURN -> this.tickTurn();
			case DESCEND -> this.tickDescend();
		}

		this.startOfTickPosition = this.endOfTickPosition;
		this.endOfTickPosition = this.startOfTickPosition.add(this.deltaMovement);
		if (this.phase != TopDownAttackTrajectory.Phase.COMPLETED) {
			if (this.pendingCorrection != null) {
				this.pendingCorrection.run();
				this.pendingCorrection = null;
			}

			this.debugDeltaMovement();
		}

		LOGGER.trace(
			"Tick #%d, phase: %s, start: (%.5f, %.5f, %.5f), end: (%.5f, %.5f, %.5f), mv (%.5f, %.5f, %.5f)\n",
			this.currentTick,
			this.phase,
			this.startOfTickPosition.x,
			this.startOfTickPosition.y,
			this.startOfTickPosition.z,
			this.endOfTickPosition.x,
			this.endOfTickPosition.y,
			this.endOfTickPosition.z,
			this.deltaMovement.x,
			this.deltaMovement.y,
			this.deltaMovement.z);
		++this.currentTick;
	}

	private void setPhase(Phase phase) {
		this.phase = phase;
		LOGGER.trace("Started phase: %s", phase);

		for (TrajectoryPhaseListener<Phase> trajectoryPhaseListener : this.trajectoryPhaseListeners) {
			trajectoryPhaseListener.onStartPhase(phase, this.endOfTickPosition);
		}
	}

	private void tickDescend() {
		--this.descendTicks;
		if (this.descendTicks < -5) {
			this.setPhase(TopDownAttackTrajectory.Phase.COMPLETED);
		} else {
			if (this.currentSpeed < this.defaultSpeed) {
				this.currentSpeed += this.climbAcceleration;
			}

			if (this.checkTargetProximity()) {
				this.setPhase(TopDownAttackTrajectory.Phase.COMPLETED);
			} else {
				Vec3 currentToTarget = this.correctTrajectory();
				this.deltaMovement = currentToTarget.scale(this.currentSpeed);
			}
		}
	}

	private Vec3 correctTrajectory() {
		Vec3 currentToTarget = this.targetPosition.subtract(this.endOfTickPosition).normalize();
		double targetDirSimilarity = currentToTarget.normalize().dot(this.deltaMovement.normalize());
		LOGGER.trace("Trajectory similarity: %.3f", targetDirSimilarity);
		Vec3 newHorizontalRotationAxis = currentToTarget.cross(DOWN);
		if (this.verticalRotationAxis != null) {
			double rotationAxisSimilarity =
				newHorizontalRotationAxis.normalize().dot(this.verticalRotationAxis.normalize());
			if (rotationAxisSimilarity > (double)0.0F && targetDirSimilarity < COS_45) {
				this.verticalRotationAxis = newHorizontalRotationAxis;
			}
		} else {
			this.verticalRotationAxis = newHorizontalRotationAxis;
		}

		double angleToTarget = Math.acos(targetDirSimilarity);
		double rotationAngle = Math.min(angleToTarget, this.climbToDescendCurvature);
		double rotationAngleDegrees = Math.toDegrees(rotationAngle);
		LOGGER.trace("Descend rotation angle degrees: %.3f", rotationAngleDegrees);
		Quaterniond rotation = new Quaterniond();
		rotation.rotateTo(
			this.deltaMovement.x,
			this.deltaMovement.y,
			this.deltaMovement.z,
			currentToTarget.x,
			currentToTarget.y,
			currentToTarget.z);
		rotation.slerp(rotation, rotationAngle / this.climbToDescendCurvature);
		Vector3d dm = new Vector3d(this.deltaMovement.x, this.deltaMovement.y, this.deltaMovement.z);
		dm.rotate(rotation);
		return currentToTarget;
	}

	private boolean checkTargetProximity() {
		Vec3 startOfTickToTarget = this.targetPosition.subtract(this.startOfTickPosition);
		Vec3 endOfTickToTarget = this.targetPosition.subtract(this.endOfTickPosition);
		double magnitudeDeltaMovementSqr = this.deltaMovement.lengthSqr();
		if (startOfTickToTarget.lengthSqr() < magnitudeDeltaMovementSqr &&
			endOfTickToTarget.lengthSqr() < magnitudeDeltaMovementSqr) {
			Vec3 crossProduct = this.deltaMovement.cross(startOfTickToTarget);
			double distanceFromTargetToTrajectory = crossProduct.length() / Math.sqrt(magnitudeDeltaMovementSqr);
			LOGGER.trace("Distance from trajectory to target: %.3f", distanceFromTargetToTrajectory);
			return distanceFromTargetToTrajectory < (double)0.5F;
		} else {
			return false;
		}
	}

	private void tickSoftLaunch() {
		if (this.softLaunchTicksBeforeTurn > 0) {
			LOGGER.trace("Soft launch ticks before turn left: %d", this.softLaunchTicksBeforeTurn);
			this.deltaMovement = this.startToTargetNormalized.scale(this.softLaunchSpeedBeforeTurn);
			--this.softLaunchTicksBeforeTurn;
		} else if (this.softLaunchTurnTicks > 0) {
			LOGGER.trace("Soft launch turn ticks before climb: %d", this.softLaunchTurnTicks);
			Vec3 ndm = this.deltaMovement.normalize();
			this.deltaMovement = ndm.scale(this.defaultSoftLaunchSpeed);
			Vec3 right = this.startToTargetNormalized.cross(UP);
			Quaterniond rotation = new Quaterniond();
			LOGGER.trace("Rotating %.3f°", Math.toDegrees(this.softLaunchCurvature));
			rotation.rotateAxis(this.softLaunchCurvature, right.x, right.y, right.z);
			Vector3d dm = new Vector3d(this.deltaMovement.x, this.deltaMovement.y, this.deltaMovement.z);
			dm.rotate(rotation);
			this.deltaMovement = new Vec3(dm.x, dm.y, dm.z);
			--this.softLaunchTurnTicks;
		} else {
			this.prepareClimb();
			this.tickClimb();
		}
	}

	private void tickClimb() {
		LOGGER.trace("Climb tick #%d, speed: %.3f", this.climbTicks, this.currentSpeed);
		this.deltaMovement =
			(new Vec3(this.climbVector.x, this.climbVector.y, this.climbVector.z)).normalize().scale(this.currentSpeed);
		if (this.climbTicks-- <= 0) {
			this.prepareTurn();
			this.tickTurn();
		} else if (this.currentSpeed < this.defaultSpeed - this.climbAcceleration) {
			this.currentSpeed += this.climbAcceleration;
		} else {
			this.currentSpeed = this.remainingClimbSpeed;
		}
	}

	private void tickTurn() {
		this.currentSpeed = this.climbToDescendTurnSpeed;
		LOGGER.trace("Speed: %.3f", this.currentSpeed);
		Vec3 currentToTarget = this.targetPosition.subtract(this.startOfTickPosition);
		Vec3 normalizedDeltaMovement = this.deltaMovement.normalize();
		this.deltaMovement = normalizedDeltaMovement.scale(this.currentSpeed);
		double targetDirSimilarity = currentToTarget.normalize().dot(normalizedDeltaMovement);
		double targetDirOffset = Math.acos(targetDirSimilarity);
		double targetDirOffsetDegrees = Math.toDegrees(targetDirOffset);
		LOGGER.trace("Target direction similarity: %.5f, degrees: %.5f", targetDirSimilarity, targetDirOffsetDegrees);
		double rotationAngle = this.climbToDescendCurvature;
		Vec3 newHorizontalRotationAxis = currentToTarget.cross(DOWN);
		if (this.verticalRotationAxis != null) {
			double rotationAxisSimilarity =
				newHorizontalRotationAxis.normalize().dot(this.verticalRotationAxis.normalize());
			if (rotationAxisSimilarity > (double)0.0F && targetDirSimilarity < COS_45) {
				this.verticalRotationAxis = newHorizontalRotationAxis;
			}
		} else {
			this.verticalRotationAxis = newHorizontalRotationAxis;
		}

		LOGGER.trace(
			"Right: (%.5f, %.5f, %.5f)",
			this.verticalRotationAxis.x,
			this.verticalRotationAxis.y,
			this.verticalRotationAxis.z);
		if (targetDirOffsetDegrees < Math.toDegrees(this.climbToDescendCurvature)) {
			this.prepareDescend(this.verticalRotationAxis, targetDirOffset);
			this.tickDescend();
		} else {
			Quaterniond rotation = new Quaterniond();
			rotation.rotateAxis(
				rotationAngle, this.verticalRotationAxis.x, this.verticalRotationAxis.y, this.verticalRotationAxis.z);
			Vector3d dm = new Vector3d(this.deltaMovement.x, this.deltaMovement.y, this.deltaMovement.z);
			dm.rotate(rotation);
			this.deltaMovement = new Vec3(dm.x, dm.y, dm.z);
		}
	}

	private void debugDeltaMovement() {
		Vec3 directionToTarget = this.targetPosition.subtract(this.startOfTickPosition).normalize();
		LOGGER.trace(
			"Direction to target: (%.3f, %.3f, %.3f)", directionToTarget.x, directionToTarget.y, directionToTarget.z);
		Vec3 directionToTargetRight = directionToTarget.cross(UP).normalize();
		Vec3 updatedRight = this.deltaMovement.cross(UP).normalize();
		double horizontalAngleOffset = Math.toDegrees(Math.acos(updatedRight.dot(directionToTargetRight)));
		LOGGER.trace("Horizontal azimuth offset: %.3f°", horizontalAngleOffset);
	}

	public void setTargetPosition(Vec3 updatedTargetPosition) {
		if (!(updatedTargetPosition.distanceToSqr(this.targetPosition) < 0.001)) {
			if (this.phase != TopDownAttackTrajectory.Phase.SOFT_LAUNCH &&
				this.phase != TopDownAttackTrajectory.Phase.CLIMB_TO_DESCEND_TURN) {
				this.pendingCorrection = () -> {
					Vec3 currentRight = this.deltaMovement.cross(UP).normalize();
					Vec3 newDirection = updatedTargetPosition.subtract(this.endOfTickPosition);
					Vec3 newRight = newDirection.cross(UP).normalize();
					double rightSimilarity = currentRight.dot(newRight);
					double angle = Math.acos(rightSimilarity < (double)0.0F ? -rightSimilarity : rightSimilarity);
					LOGGER.trace(
						"Correction angle: %.3f, old right vector: (%.5f, %.5f, %.5f), new right vector: (%.5f, "
							+ "%.5f, %.5f)",
						Math.toDegrees(angle),
						currentRight.x,
						currentRight.y,
						currentRight.z,
						newRight.x,
						newRight.y,
						newRight.z);
					Quaterniond rotation = new Quaterniond();
					rotation.rotateAxis(angle, UP.x, UP.y, UP.z);
					Vector3d dm = new Vector3d(this.deltaMovement.x, this.deltaMovement.y, this.deltaMovement.z);
					dm.rotate(rotation);
					Vec3 updatedDeltaMovement = new Vec3(dm.x, dm.y, dm.z);
					LOGGER.trace(
						"Updated delta movement from (%.5f, %.5f, %.5f) to (%.5f, %.5f, %.5f), angle: %.3f°",
						this.deltaMovement.x,
						this.deltaMovement.y,
						this.deltaMovement.z,
						updatedDeltaMovement.x,
						updatedDeltaMovement.y,
						updatedDeltaMovement.z,
						Math.toDegrees(angle));
					this.deltaMovement = updatedDeltaMovement;
					this.targetPosition = updatedTargetPosition;
					this.debugDeltaMovement();
				};
			}
		}
	}

	private void prepareClimb() {
		this.climbVector = this.endOfClimbPosition.subtract(this.endOfTickPosition);
		Vec3 normalizedClimbVector = this.climbVector.normalize();
		LOGGER.trace(
			"Recomputed normalized climb vector: (%.3f, %.3f, %.3f)",
			normalizedClimbVector.x,
			normalizedClimbVector.y,
			normalizedClimbVector.z);
		double climbDistanceBeforeTurn = this.climbVector.length();
		this.climbAcceleration =
			MiscUtil.adjustDivisor(this.defaultSpeed - this.defaultSoftLaunchSpeed, this.climbAcceleration);
		int climbAccelerationTicks =
			(int)Math.round((this.defaultSpeed - this.defaultSoftLaunchSpeed) / this.climbAcceleration);
		double averageSpeed = (this.defaultSpeed + this.defaultSoftLaunchSpeed - this.climbAcceleration) * (double)0.5F;
		double climbAccelerationDistance = averageSpeed * (double)climbAccelerationTicks;
		if (climbAccelerationDistance > climbDistanceBeforeTurn) {
			double tickD =
				(-this.defaultSoftLaunchSpeed + Math.sqrt(
													this.defaultSoftLaunchSpeed * this.defaultSoftLaunchSpeed +
													(double)2.0F * this.climbAcceleration * climbDistanceBeforeTurn)) /
				this.climbAcceleration;
			climbAccelerationTicks = (int)Math.round(tickD);
		}

		Vec3 stopClimbAccelerationPos =
			this.endOfTickPosition.add(this.climbVector.normalize().scale(climbAccelerationDistance));
		LOGGER.trace(
			"Climb acceleration distance: %.3f. Stop acceleration at: (%.3f, %.3f, %.3f)",
			climbAccelerationDistance,
			stopClimbAccelerationPos.x,
			stopClimbAccelerationPos.y,
			stopClimbAccelerationPos.z);
		double remainingClimbDistance = climbDistanceBeforeTurn - climbAccelerationDistance;
		if (remainingClimbDistance > this.defaultSpeed) {
			this.remainingClimbSpeed = MiscUtil.adjustDivisor(remainingClimbDistance, this.defaultSpeed);
			int remainingClimbTicks = (int)Math.round(remainingClimbDistance / this.remainingClimbSpeed);
			this.climbTicks = climbAccelerationTicks + remainingClimbTicks;
		} else {
			this.climbTicks = climbAccelerationTicks;
			this.remainingClimbSpeed = this.defaultSpeed;
		}

		this.currentSpeed = this.defaultSoftLaunchSpeed;
		this.setPhase(TopDownAttackTrajectory.Phase.CLIMB);
	}

	private void prepareTurn() {
		this.setPhase(TopDownAttackTrajectory.Phase.CLIMB_TO_DESCEND_TURN);
	}

	private void prepareDescend(Vec3 right, double targetDirAngle) {
		this.setPhase(TopDownAttackTrajectory.Phase.DESCEND);
		Vec3 targetDir = this.targetPosition.subtract(this.endOfTickPosition);
		double distanceToTarget = targetDir.length();
		this.currentSpeed = MiscUtil.adjustDivisor(distanceToTarget, this.currentSpeed);
		this.descendTicks = (int)Math.round(distanceToTarget / this.currentSpeed);
		LOGGER.trace("Ticks to target: %d", this.descendTicks);
		this.deltaMovement = targetDir.normalize().scale(this.currentSpeed);
	}

	public static double getRadius(double segmentLength, double curvatureAngle) {
		return segmentLength * (double)0.5F / Math.sin(curvatureAngle * (double)0.5F);
	}

	public static Vec3 getHorizontalProjection(Vec3 source, Vec3 target) {
		Vec3 sourceTargetDirection = target.subtract(source);
		Vec3 right = sourceTargetDirection.cross(UP);
		return UP.cross(right).normalize();
	}

	public static Vec3 getPivot(Vec3 source, Vec3 target, double radius, double offset) {
		double s = radius / Math.tan(MIN_ATTACK_ANGLE);
		double normalizedOffset = s * Mth.clamp(offset, -1.0F, 1.0F);
		Vec3 hp = getHorizontalProjection(source, target);
		return target.add(hp.scale(-s + normalizedOffset)).add(UP.scale(radius));
	}

	public static ClimbInfo getClimbInfo(Vec3 source, Vec3 pivot, double radius) {
		Vec3 sourceToPivot = pivot.subtract(source);
		Vec3 right = sourceToPivot.cross(UP);
		double alpha = -Math.asin(radius / sourceToPivot.length());
		Vec3 horizontalProjection = getHorizontalProjection(source, pivot);
		double sourceToPivotAngle = Math.acos(sourceToPivot.normalize().dot(horizontalProjection.normalize()));
		double sourceToPivotAngleDeg = Math.toDegrees(sourceToPivotAngle);
		double launchAngle = Math.abs(sourceToPivotAngle) + Math.abs(alpha);
		if (launchAngle > (Math.PI / 2D)) {
			LOGGER.trace("Launch angle %.5f exceeds 90 degrees", Math.toDegrees(launchAngle));
			return null;
		} else {
			LOGGER.trace(
				"Angle to offset sourcePivot: %.5f, source-pivot angle: %.5f",
				Math.toDegrees(alpha),
				sourceToPivotAngleDeg);
			Quaterniond rotation = new Quaterniond();
			rotation.rotateAxis(-alpha, right.x, right.y, right.z);
			Vector3d v =
				(new Vector3d(sourceToPivot.x, sourceToPivot.y, sourceToPivot.z)).rotate(rotation).mul(Math.cos(alpha));
			return new ClimbInfo(launchAngle, new Vec3(v.x, v.y, v.z));
		}
	}

	public enum Phase {
		SOFT_LAUNCH,
		CLIMB,
		CLIMB_TO_DESCEND_TURN,
		DESCEND,
		COMPLETED;

		Phase() {}
	}

	public record ClimbInfo(double launchAngle, Vec3 climbVector) {
		public ClimbInfo(double launchAngle, Vec3 climbVector) {
			this.launchAngle = launchAngle;
			this.climbVector = climbVector;
		}

		public double launchAngle() {
			return this.launchAngle;
		}

		public Vec3 climbVector() {
			return this.climbVector;
		}
	}
}
