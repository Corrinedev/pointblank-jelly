package mod.pbj.client.controller;

import com.eliotlash.mclib.utils.Interpolations;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import mod.pbj.mixin.AnimatableManagerAccessor;
import mod.pbj.mixin.AnimationControllerAccessor;
import mod.pbj.util.AnimationPointInfo2;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreBakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.Animation.Keyframes;
import software.bernie.geckolib.core.animation.Animation.LoopType;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.EasingType;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.RawAnimation.Stage;
import software.bernie.geckolib.core.keyframe.AnimationPoint;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.core.keyframe.event.data.CustomInstructionKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.core.state.BoneSnapshot;

public class BlendingAnimationProcessor<T extends GeoAnimatable> extends AnimationProcessor<T> {
	private final Map<String, CoreGeoBone> bones = new Object2ObjectOpenHashMap<>();
	private final CoreGeoModel<T> model;
	public boolean reloadAnimations = false;
	static final String WAIT = "internal.wait";

	public BlendingAnimationProcessor(CoreGeoModel<T> model) {
		super(null);
		this.model = model;
	}

	public Queue<QueuedAnimation> buildAnimationQueue(T animatable, RawAnimation rawAnimation) {
		LinkedList<QueuedAnimation> animations = new LinkedList<>();
		boolean error = false;

		for (Stage stage : rawAnimation.getAnimationStages()) {
			Animation animation;
			if (stage.animationName().equals("internal.wait")) {
				animation = generateWaitAnimation(stage.additionalTicks());
			} else {
				animation = this.model.getAnimation(animatable, stage.animationName());
			}

			if (animation == null) {
				PrintStream var10000 = System.out;
				String var10001 = stage.animationName();
				var10000.println(
					"Unable to find animation: " + var10001 + " for " + animatable.getClass().getSimpleName());
				error = true;
			} else {
				animations.add(new QueuedAnimation(animation, stage.loopType()));
			}
		}

		return error ? null : animations;
	}

	static Animation generateWaitAnimation(double length) {
		return new Animation(
			"internal.wait",
			length,
			LoopType.PLAY_ONCE,
			new BoneAnimation[0],
			new Keyframes(new SoundKeyframeData[0], new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0]));
	}

	public void tickAnimation(
		T animatable,
		CoreGeoModel<T> model,
		AnimatableManager<T> animatableManager,
		double animTime,
		AnimationState<T> state,
		boolean crashWhenCantFindBone) {
		Map<CoreGeoBone, Map<String, AnimationPointInfo2>> animationPoints = new LinkedHashMap<>();
		Map<String, BoneSnapshot> globalBoneSnapshots =
			this.updateBoneSnapshots(animatableManager.getBoneSnapshotCollection());
		Set<Map<String, BoneSnapshot>> snapshotSets = Collections.newSetFromMap(new IdentityHashMap<>());
		snapshotSets.add(globalBoneSnapshots);
		var var11 = animatableManager.getAnimationControllers().values().iterator();

	label108:
		while (var11.hasNext()) {
			AnimationController<T> controller = var11.next();
			if (this.reloadAnimations) {
				controller.forceAnimationReset();
				controller.getBoneAnimationQueues().clear();
			}

			AnimationControllerAccessor controllerExt = (AnimationControllerAccessor)controller;
			controllerExt.setIsJustStarting(animatableManager.isFirstTick());
			state.withController(controller);
			controller.process(model, state, this.bones, globalBoneSnapshots, animTime, crashWhenCantFindBone);
			Iterator<BoneAnimationQueue> var14 = controller.getBoneAnimationQueues().values().iterator();

			while (true) {
				CoreGeoBone bone;
				AnimationPointInfo2 info;
				do {
					if (!var14.hasNext()) {
						continue label108;
					}

					BoneAnimationQueue boneAnimation = var14.next();
					bone = boneAnimation.bone();
					AnimationPoint rotXPoint = boneAnimation.rotationXQueue().poll();
					AnimationPoint rotYPoint = boneAnimation.rotationYQueue().poll();
					AnimationPoint rotZPoint = boneAnimation.rotationZQueue().poll();
					AnimationPoint posXPoint = boneAnimation.positionXQueue().poll();
					AnimationPoint posYPoint = boneAnimation.positionYQueue().poll();
					AnimationPoint posZPoint = boneAnimation.positionZQueue().poll();
					AnimationPoint scaleXPoint = boneAnimation.scaleXQueue().poll();
					AnimationPoint scaleYPoint = boneAnimation.scaleYQueue().poll();
					AnimationPoint scaleZPoint = boneAnimation.scaleZQueue().poll();
					EasingType easingType = controllerExt.getOverrideEasingTypeFunction().apply(animatable);
					info = new AnimationPointInfo2(easingType);
					if (rotXPoint != null && rotYPoint != null && rotZPoint != null) {
						info.setRotXPoint(rotXPoint);
						info.setRotYPoint(rotYPoint);
						info.setRotZPoint(rotZPoint);
					}

					if (posXPoint != null && posYPoint != null && posZPoint != null) {
						info.setPosXPoint(posXPoint);
						info.setPosYPoint(posYPoint);
						info.setPosZPoint(posZPoint);
					}

					if (scaleXPoint != null && scaleYPoint != null && scaleZPoint != null) {
						info.setScaleXPoint(scaleXPoint);
						info.setScaleYPoint(scaleYPoint);
						info.setScaleZPoint(scaleZPoint);
					}
				} while (!info.isPositionChanged() && !info.isRotationChanged() && !info.isScaleChanged());

				Map<String, AnimationPointInfo2> boneAnimationPoints =
					animationPoints.computeIfAbsent(bone, (b) -> new HashMap<>());
				String controllerName =
					controller instanceof BlendingAnimationController ? controller.getName() : "_default";
				boneAnimationPoints.put(controllerName, info);
			}
		}

		this.reloadAnimations = false;
		double resetTickLength = animatable.getBoneResetTime();
		Iterator<Entry<CoreGeoBone, Map<String, AnimationPointInfo2>>> var41 = animationPoints.entrySet().iterator();

		while (true) {
			Entry<CoreGeoBone, Map<String, AnimationPointInfo2>> e;
			Map<String, AnimationPointInfo2> pointInfoByControllers;
			do {
				if (!var41.hasNext()) {
					resetBones(this.getRegisteredBones(), globalBoneSnapshots, animTime, resetTickLength);
					((AnimatableManagerAccessor)animatableManager).setIsFirstTick(false);
					return;
				}

				e = var41.next();
				pointInfoByControllers = e.getValue();
			} while (pointInfoByControllers == null);

			double rotXPoint = 0.0D;
			double rotYPoint = 0.0D;
			double rotZPoint = 0.0D;
			double posXPoint = 0.0D;
			double posYPoint = 0.0D;
			double posZPoint = 0.0D;
			double scaleXPoint = 0.0D;
			double scaleYPoint = 0.0D;
			double scaleZPoint = 0.0D;
			boolean rotationChanged = false;
			boolean positionChanged = false;
			boolean scaleChanged = false;

			for (Entry<String, AnimationPointInfo2> stringAnimationPointInfo2Entry :
				 pointInfoByControllers.entrySet()) {
				AnimationPointInfo2 pointInfo = stringAnimationPointInfo2Entry.getValue();
				if (pointInfo.isRotationChanged()) {
					rotXPoint +=
						(float)EasingType.lerpWithOverride(pointInfo.getRotXPoint(), pointInfo.getEasingType());
					rotYPoint +=
						(float)EasingType.lerpWithOverride(pointInfo.getRotYPoint(), pointInfo.getEasingType());
					rotZPoint +=
						(float)EasingType.lerpWithOverride(pointInfo.getRotZPoint(), pointInfo.getEasingType());
					rotationChanged = true;
				}

				if (pointInfo.isPositionChanged()) {
					posXPoint +=
						(float)EasingType.lerpWithOverride(pointInfo.getPosXPoint(), pointInfo.getEasingType());
					posYPoint +=
						(float)EasingType.lerpWithOverride(pointInfo.getPosYPoint(), pointInfo.getEasingType());
					posZPoint +=
						(float)EasingType.lerpWithOverride(pointInfo.getPosZPoint(), pointInfo.getEasingType());
					positionChanged = true;
				}

				if (pointInfo.isScaleChanged()) {
					scaleXPoint +=
						(float)EasingType.lerpWithOverride(pointInfo.getScaleXPoint(), pointInfo.getEasingType());
					scaleYPoint +=
						(float)EasingType.lerpWithOverride(pointInfo.getScaleYPoint(), pointInfo.getEasingType());
					scaleZPoint +=
						(float)EasingType.lerpWithOverride(pointInfo.getScaleZPoint(), pointInfo.getEasingType());
					scaleChanged = true;
				}
			}

			CoreGeoBone bone = this.bones.get(e.getKey().getName());
			BoneSnapshot snapshot = globalBoneSnapshots.get(bone.getName());
			if (rotationChanged) {
				BoneSnapshot initialSnapshot = bone.getInitialSnapshot();
				bone.setRotX((float)rotXPoint + initialSnapshot.getRotX());
				bone.setRotY((float)rotYPoint + initialSnapshot.getRotY());
				bone.setRotZ((float)rotZPoint + initialSnapshot.getRotZ());
				snapshot.updateRotation(bone.getRotX(), bone.getRotY(), bone.getRotZ());
				snapshot.startRotAnim();
				bone.markRotationAsChanged();
			}

			if (positionChanged) {
				bone.setPosX((float)posXPoint);
				bone.setPosY((float)posYPoint);
				bone.setPosZ((float)posZPoint);
				snapshot.updateOffset(bone.getPosX(), bone.getPosY(), bone.getPosZ());
				snapshot.startPosAnim();
				bone.markPositionAsChanged();
			}

			if (scaleChanged) {
				bone.setScaleX((float)scaleXPoint);
				bone.setScaleY((float)scaleYPoint);
				bone.setScaleZ((float)scaleZPoint);
				snapshot.updateScale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
				snapshot.startScaleAnim();
				bone.markScaleAsChanged();
			}
		}
	}

	private static void resetBones(
		Collection<CoreGeoBone> bones,
		Map<String, BoneSnapshot> boneSnapshots,
		double animTime,
		double resetTickLength) {
		Iterator<CoreGeoBone> var6 = bones.iterator();

		while (true) {
			CoreGeoBone bone;
			BoneSnapshot initialSnapshot;
			BoneSnapshot saveSnapshot;
			double percentageReset;
			while (true) {
				if (!var6.hasNext()) {
					resetBoneTransformationMarkers(bones);
					return;
				}

				bone = var6.next();
				if (bone.hasRotationChanged()) {
					break;
				}

				initialSnapshot = bone.getInitialSnapshot();
				saveSnapshot = boneSnapshots.get(bone.getName());
				if (saveSnapshot != null) {
					if (saveSnapshot.isRotAnimInProgress()) {
						saveSnapshot.stopRotAnim(animTime);
					}

					percentageReset =
						Math.min((animTime - saveSnapshot.getLastResetRotationTick()) / resetTickLength, 1.0D);
					bone.setRotX(
						(float)Interpolations.lerp(saveSnapshot.getRotX(), initialSnapshot.getRotX(), percentageReset));
					bone.setRotY(
						(float)Interpolations.lerp(saveSnapshot.getRotY(), initialSnapshot.getRotY(), percentageReset));
					bone.setRotZ(
						(float)Interpolations.lerp(saveSnapshot.getRotZ(), initialSnapshot.getRotZ(), percentageReset));
					if (percentageReset >= 1.0D) {
						saveSnapshot.updateRotation(bone.getRotX(), bone.getRotY(), bone.getRotZ());
					}
					break;
				}
			}

			if (!bone.hasPositionChanged()) {
				initialSnapshot = bone.getInitialSnapshot();
				saveSnapshot = boneSnapshots.get(bone.getName());
				if (saveSnapshot.isPosAnimInProgress()) {
					saveSnapshot.stopPosAnim(animTime);
				}

				percentageReset =
					Math.min((animTime - saveSnapshot.getLastResetPositionTick()) / resetTickLength, 1.0D);
				bone.setPosX((float)Interpolations.lerp(
					saveSnapshot.getOffsetX(), initialSnapshot.getOffsetX(), percentageReset));
				bone.setPosY((float)Interpolations.lerp(
					saveSnapshot.getOffsetY(), initialSnapshot.getOffsetY(), percentageReset));
				bone.setPosZ((float)Interpolations.lerp(
					saveSnapshot.getOffsetZ(), initialSnapshot.getOffsetZ(), percentageReset));
				if (percentageReset >= 1.0D) {
					saveSnapshot.updateOffset(bone.getPosX(), bone.getPosY(), bone.getPosZ());
				}
			}

			if (!bone.hasScaleChanged()) {
				initialSnapshot = bone.getInitialSnapshot();
				saveSnapshot = boneSnapshots.get(bone.getName());
				if (saveSnapshot.isScaleAnimInProgress()) {
					saveSnapshot.stopScaleAnim(animTime);
				}

				percentageReset = Math.min((animTime - saveSnapshot.getLastResetScaleTick()) / resetTickLength, 1.0D);
				bone.setScaleX(
					(float)Interpolations.lerp(saveSnapshot.getScaleX(), initialSnapshot.getScaleX(), percentageReset));
				bone.setScaleY(
					(float)Interpolations.lerp(saveSnapshot.getScaleY(), initialSnapshot.getScaleY(), percentageReset));
				bone.setScaleZ(
					(float)Interpolations.lerp(saveSnapshot.getScaleZ(), initialSnapshot.getScaleZ(), percentageReset));
				if (percentageReset >= 1.0D) {
					saveSnapshot.updateScale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
				}
			}
		}
	}

	private static void resetBoneTransformationMarkers(Collection<CoreGeoBone> bones) {
		bones.forEach(CoreGeoBone::resetStateChanges);
	}

	private Map<String, BoneSnapshot> updateBoneSnapshots(Map<String, BoneSnapshot> snapshots) {
		for (CoreGeoBone bone : this.getRegisteredBones()) {
			if (!snapshots.containsKey(bone.getName())) {
				snapshots.put(bone.getName(), BoneSnapshot.copy(bone.getInitialSnapshot()));
			}
		}

		return snapshots;
	}

	public CoreGeoBone getBone(String boneName) {
		return this.bones.get(boneName);
	}

	public void registerGeoBone(CoreGeoBone bone) {
		bone.saveInitialSnapshot();
		this.bones.put(bone.getName(), bone);

		for (CoreGeoBone child : bone.getChildBones()) {
			this.registerGeoBone(child);
		}
	}

	public void setActiveModel(CoreBakedGeoModel model) {
		this.bones.clear();

		for (CoreGeoBone bone : model.getBones()) {
			this.registerGeoBone(bone);
		}
	}

	public Collection<CoreGeoBone> getRegisteredBones() {
		return this.bones.values();
	}

	public void preAnimationSetup(T animatable, double animTime) {
		this.model.applyMolangQueries(animatable, animTime);
	}
}
