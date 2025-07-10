package mod.pbj.util;

import software.bernie.geckolib.core.animation.EasingType;
import software.bernie.geckolib.core.keyframe.AnimationPoint;

public class AnimationPointInfo2 {
	private AnimationPoint rotXPoint;
	private AnimationPoint rotYPoint;
	private AnimationPoint rotZPoint;
	private AnimationPoint posXPoint;
	private AnimationPoint posYPoint;
	private AnimationPoint posZPoint;
	private AnimationPoint scaleXPoint;
	private AnimationPoint scaleYPoint;
	private AnimationPoint scaleZPoint;
	private boolean rotationChanged;
	private boolean positionChanged;
	private boolean scaleChanged;
	private final EasingType easingType;

	public AnimationPointInfo2(EasingType easingType) {
		this.easingType = easingType;
	}

	public EasingType getEasingType() {
		return this.easingType;
	}

	public AnimationPoint getRotXPoint() {
		return this.rotXPoint;
	}

	public void setRotXPoint(AnimationPoint rotXPoint) {
		this.rotationChanged = true;
		this.rotXPoint = rotXPoint;
	}

	public AnimationPoint getRotYPoint() {
		return this.rotYPoint;
	}

	public void setRotYPoint(AnimationPoint rotYPoint) {
		this.rotationChanged = true;
		this.rotYPoint = rotYPoint;
	}

	public AnimationPoint getRotZPoint() {
		return this.rotZPoint;
	}

	public void setRotZPoint(AnimationPoint rotZPoint) {
		this.rotationChanged = true;
		this.rotZPoint = rotZPoint;
	}

	public AnimationPoint getPosXPoint() {
		return this.posXPoint;
	}

	public void setPosXPoint(AnimationPoint posXPoint) {
		this.positionChanged = true;
		this.posXPoint = posXPoint;
	}

	public AnimationPoint getPosYPoint() {
		return this.posYPoint;
	}

	public void setPosYPoint(AnimationPoint posYPoint) {
		this.positionChanged = true;
		this.posYPoint = posYPoint;
	}

	public AnimationPoint getPosZPoint() {
		return this.posZPoint;
	}

	public void setPosZPoint(AnimationPoint posZPoint) {
		this.positionChanged = true;
		this.posZPoint = posZPoint;
	}

	public AnimationPoint getScaleXPoint() {
		return this.scaleXPoint;
	}

	public void setScaleXPoint(AnimationPoint scaleXPoint) {
		this.scaleChanged = true;
		this.scaleXPoint = scaleXPoint;
	}

	public AnimationPoint getScaleYPoint() {
		return this.scaleYPoint;
	}

	public void setScaleYPoint(AnimationPoint scaleYPoint) {
		this.scaleChanged = true;
		this.scaleYPoint = scaleYPoint;
	}

	public AnimationPoint getScaleZPoint() {
		return this.scaleZPoint;
	}

	public void setScaleZPoint(AnimationPoint scaleZPoint) {
		this.scaleChanged = true;
		this.scaleZPoint = scaleZPoint;
	}

	public boolean isRotationChanged() {
		return this.rotationChanged;
	}

	public boolean isPositionChanged() {
		return this.positionChanged;
	}

	public boolean isScaleChanged() {
		return this.scaleChanged;
	}
}
