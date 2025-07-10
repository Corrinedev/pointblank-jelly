package mod.pbj.client.gui;

import mod.pbj.util.DoubleBiPredicate;
import net.minecraft.util.Mth;

public class MouseInteractionHandler {
	private final DoubleBiPredicate mouseInAreaPredicate;
	private float zoom;
	private final float minZoom;
	private final float maxZoom;
	private final float zoomStep;
	private int xOffset;
	private int yOffset;
	private double rotationYaw;
	private double rotationPitch;
	private boolean isInteracting;
	private int interactionButton;
	private final int rotatingButton;
	private final int translatingButton;
	private double clickedX;
	private double clickedY;

	public MouseInteractionHandler(
		DoubleBiPredicate mouseInAreaPredicate, float minZoom, float maxZoom, float zoomStep) {
		this.mouseInAreaPredicate = mouseInAreaPredicate;
		this.zoom = 1.0F;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		this.zoomStep = zoomStep;
		this.rotatingButton = 0;
		this.translatingButton = 1;
	}

	public boolean onMouseScrolled(double mouseX, double mouseY, double mouseScroll) {
		if (this.mouseInAreaPredicate.test(mouseX, mouseY)) {
			this.zoom =
				Mth.clamp(this.zoom + this.zoomStep * Math.signum((float)mouseScroll), this.minZoom, this.maxZoom);
			return true;
		} else {
			return false;
		}
	}

	public boolean onMouseButtonClicked(double mouseX, double mouseY, int mouseButton) {
		if (!this.mouseInAreaPredicate.test(mouseX, mouseY) || this.isInteracting ||
			mouseButton != this.rotatingButton && mouseButton != this.translatingButton) {
			return false;
		} else {
			this.isInteracting = true;
			this.interactionButton = mouseButton;
			this.clickedX = mouseX;
			this.clickedY = mouseY;
			return true;
		}
	}

	public boolean onMouseButtonReleased(double mouseX, double mouseY, int mouseButton) {
		if (!this.isInteracting) {
			return false;
		} else {
			if (this.interactionButton == this.translatingButton && mouseButton == this.translatingButton) {
				this.isInteracting = false;
				this.xOffset = (int)((double)this.xOffset + (mouseX - this.clickedX));
				this.yOffset = (int)((double)this.yOffset + (mouseY - this.clickedY));
			} else if (this.interactionButton == this.rotatingButton && mouseButton == this.rotatingButton) {
				this.isInteracting = false;
				this.rotationYaw = this.rotationYaw + mouseX - this.clickedX;
				this.rotationPitch = this.rotationPitch - mouseY + this.clickedY;
			}

			return true;
		}
	}

	public float getZoom() {
		return this.zoom;
	}

	public int getX() {
		return this.xOffset;
	}

	public int getY() {
		return this.yOffset;
	}

	public float getRotationPitch() {
		return (float)this.rotationPitch;
	}

	public float getRotationYaw() {
		return (float)this.rotationYaw;
	}

	public boolean isRotating() {
		return this.interactionButton == this.rotatingButton;
	}

	public boolean isTranslating() {
		return this.interactionButton == this.translatingButton;
	}

	public boolean isInteracting() {
		return this.isInteracting;
	}

	public float getMouseClickedX() {
		return (float)this.clickedX;
	}

	public float getMouseClickedY() {
		return (float)this.clickedY;
	}
}
