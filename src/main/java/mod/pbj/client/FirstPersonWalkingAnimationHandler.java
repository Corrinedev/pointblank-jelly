package mod.pbj.client;

import java.util.Collection;
import java.util.List;
import mod.pbj.client.controller.BlendingAnimationController;
import mod.pbj.item.GunItem;
import mod.pbj.util.StateMachine;
import mod.pbj.util.StateMachine.TransitionMode;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationController.State;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.ClientUtils;

public class FirstPersonWalkingAnimationHandler {
	public static final String WALKING_CONTROLLER_NAME = "walking";
	private static final float MIN_SPEED_SQUARED = 1.0E-4F;
	private final StateMachine<PlayerWalkingState, PlayerWalkingStateContext> firstPersonAnimationStateMachine =
		this.createFirstPersonAnimationStateMachine();
	private long ts;

	public FirstPersonWalkingAnimationHandler() {}

	private StateMachine<PlayerWalkingState, PlayerWalkingStateContext> createFirstPersonAnimationStateMachine() {
		StateMachine.Builder<PlayerWalkingState, PlayerWalkingStateContext> builder = new StateMachine.Builder<>();
		Minecraft mc = Minecraft.getInstance();
		builder.withTransition(
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.NONE,
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
			(ctx)
				-> mc.options.getCameraType() == CameraType.FIRST_PERSON && !ctx.state.isDrawing() &&
					   System.currentTimeMillis() - this.ts > 100L,
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_BACKWARDS,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_LEFT,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_RIGHT,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.RUNNING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.PREPARE_RUNNING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.COMPLETE_RUNNING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND_SPRINTING),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.NONE,
			(ctx)
				-> mc.options.getCameraType() != CameraType.FIRST_PERSON,
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_BACKWARDS,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_LEFT,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_RIGHT),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND,
			(ctx)
				-> !ctx.player.onGround() && !ctx.player.isSprinting(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.COMPLETE_RUNNING,
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && !ctx.player.onGround() &&
					   (!ctx.player.isSprinting() || ctx.player.isMovingSlowly()) && ctx.isMovingForward() &&
					   !ctx.state.isAiming() &&
					   !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(FirstPersonWalkingAnimationHandler.PlayerWalkingState.RUNNING),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND_SPRINTING,
			(ctx)
				-> !ctx.player.onGround() && ctx.player.isSprinting(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.PREPARE_RUNNING,
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND_SPRINTING,
			(ctx)
				-> ctx.player.isSprinting() && !ctx.player.onGround() && !ctx.player.isMovingSlowly() &&
					   !isPlayingAnimation(GunItem.RAW_ANIMATION_PREPARE_RUNNING, ctx.itemStack),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_BACKWARDS,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_LEFT,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_RIGHT),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_AIMING,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && ctx.player.onGround() && !ctx.player.isSprinting() &&
					   ctx.state.isAiming(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.COMPLETE_RUNNING,
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_AIMING,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && ctx.player.onGround() && !ctx.player.isSprinting() &&
					   ctx.state.isAiming() &&
					   !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_AIMING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_BACKWARDS,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_LEFT,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_RIGHT),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && ctx.player.onGround() && !ctx.player.isSprinting() &&
					   ctx.isMovingForward() && !ctx.state.isAiming(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.COMPLETE_RUNNING,
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && ctx.player.onGround() &&
					   (!ctx.player.isSprinting() || ctx.player.isMovingSlowly()) && ctx.isMovingForward() &&
					   !ctx.state.isAiming() &&
					   !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_AIMING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_LEFT,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_RIGHT),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_BACKWARDS,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && ctx.player.onGround() &&
					   (!ctx.player.isSprinting() || ctx.player.isMovingSlowly()) && ctx.isMovingBackward() &&
					   !ctx.state.isAiming(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.COMPLETE_RUNNING,
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_BACKWARDS,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && (!ctx.player.isSprinting() || ctx.player.isMovingSlowly()) &&
					   ctx.isMovingBackward() && !ctx.state.isAiming() &&
					   !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_AIMING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_BACKWARDS,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_RIGHT),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_LEFT,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && ctx.player.onGround() &&
					   (!ctx.player.isSprinting() || ctx.player.isMovingSlowly()) && ctx.isMovingLeft() &&
					   !ctx.state.isAiming(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.COMPLETE_RUNNING,
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_LEFT,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && ctx.player.onGround() &&
					   (!ctx.player.isSprinting() || ctx.player.isMovingSlowly()) && ctx.isMovingLeft() &&
					   !ctx.state.isAiming() &&
					   !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_AIMING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_BACKWARDS,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_LEFT),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_RIGHT,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && ctx.player.onGround() && !ctx.player.isSprinting() &&
					   !ctx.state.isAiming() && ctx.isMovingRight(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.COMPLETE_RUNNING,
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_RIGHT,
			(ctx)
				-> ctx.speedSquared > 1.0E-4F && ctx.player.onGround() &&
					   (!ctx.player.isSprinting() || ctx.player.isMovingSlowly()) && ctx.isMovingRight() &&
					   !ctx.state.isAiming() &&
					   !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_AIMING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_BACKWARDS,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_LEFT,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_RIGHT),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
			(ctx)
				-> ctx.player.onGround() && !ctx.player.isSprinting() && ctx.speedSquared <= 1.0E-4F,
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.COMPLETE_RUNNING,
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
			(ctx)
				-> ctx.player.onGround() && !ctx.player.isSprinting() && ctx.speedSquared <= 1.0E-4F &&
					   !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_AIMING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_BACKWARDS,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_LEFT,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.WALKING_RIGHT,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.COMPLETE_RUNNING),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.PREPARE_RUNNING,
			(ctx)
				-> ctx.player.isSprinting() && !ctx.player.isMovingSlowly() &&
					   !isPlayingAnimation("animation.model.fire", ctx.itemStack, "fire_controller"),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND_SPRINTING),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.RUNNING,
			(ctx)
				-> ctx.player.onGround(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.PREPARE_RUNNING,
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.RUNNING,
			(ctx)
				-> ctx.player.isSprinting() && ctx.player.onGround() && !ctx.player.isMovingSlowly() &&
					   !isPlayingAnimation(GunItem.RAW_ANIMATION_PREPARE_RUNNING, ctx.itemStack),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			List.of(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.PREPARE_RUNNING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.RUNNING,
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.OFF_GROUND_SPRINTING),
			FirstPersonWalkingAnimationHandler.PlayerWalkingState.COMPLETE_RUNNING,
			(ctx)
				-> !ctx.player.isSprinting() || ctx.player.isMovingSlowly(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withOnChangeStateAction((ctx, f, t) -> {
			long id = GeoItem.getId(ctx.itemStack);
			GunItem gunItem = (GunItem)ctx.itemStack.getItem();
			switch (t) {
				case STANDING:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.standing");
					break;
				case WALKING_AIMING:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.walking_aiming");
					break;
				case WALKING:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.walking");
					break;
				case WALKING_BACKWARDS:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.walking_backwards");
					break;
				case WALKING_LEFT:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.walking_left");
					break;
				case WALKING_RIGHT:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.walking_right");
					break;
				case PREPARE_RUNNING:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.runningstart");
					break;
				case RUNNING:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.running");
					break;
				case COMPLETE_RUNNING:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.runningend");
					break;
				case OFF_GROUND:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.off_ground");
					break;
				case OFF_GROUND_SPRINTING:
					gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.off_ground_sprinting");
					break;
				case NONE:
					AnimationController<GeoAnimatable> controller =
						gunItem.getGeoAnimationController("walking", ctx.itemStack);
					controller.forceAnimationReset();
					controller.setAnimation(null);
					((BlendingAnimationController<GeoAnimatable>)controller).clearAll();
			}
		});
		return builder.build(FirstPersonWalkingAnimationHandler.PlayerWalkingState.STANDING);
	}

	public void handlePlayerFirstPersonMovement(Player player, ItemStack itemStack) {
		if (itemStack.getItem() instanceof GunItem) {
			Minecraft mc = Minecraft.getInstance();
			Player mainPlayer = ClientUtils.getClientPlayer();
			if (player == mainPlayer) {
				PlayerWalkingStateContext context = new PlayerWalkingStateContext((LocalPlayer)player, itemStack);
				if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
					this.firstPersonAnimationStateMachine.update(context);
				} else {
					this.firstPersonAnimationStateMachine.update(context);
				}
			}
		}
	}

	public void reset(Player player, ItemStack itemStack) {
		if (itemStack.getItem() instanceof GunItem) {
			this.ts = System.currentTimeMillis();
			this.firstPersonAnimationStateMachine.resetToState(
				FirstPersonWalkingAnimationHandler.PlayerWalkingState.NONE);
		}
	}

	public static boolean isPlayingAnimation(RawAnimation rawAnimation, ItemStack itemStack) {
		Item controller = itemStack.getItem();
		if (!(controller instanceof GunItem gunItem)) {
			return false;
		} else {
			boolean var10000;
		label22: {
			AnimationController<?> var5 = gunItem.getGeoAnimationController("walking", itemStack);
			if (rawAnimation != var5.getCurrentRawAnimation()) {
				if (!(var5 instanceof BlendingAnimationController<?> bac)) {
					break label22;
				}

				if (bac.getTriggeredAnimation() != rawAnimation) {
					break label22;
				}
			}

			if (var5.getAnimationState() != State.STOPPED) {
				var10000 = true;
				return var10000;
			}
		}

			var10000 = false;
			return var10000;
		}
	}

	public static boolean isPlayingBlendingAnimation(
		Collection<RawAnimation> rawAnimations, ItemStack itemStack, double minProgress, double maxProgress) {
		Item item = itemStack.getItem();
		if (item instanceof GunItem gunItem) {
			AnimationController<GeoAnimatable> controller = gunItem.getGeoAnimationController("walking", itemStack);
			if (!(controller instanceof BlendingAnimationController<GeoAnimatable> bac)) {
				return false;
			} else {
				boolean result = collectionContains(rawAnimations, bac.getTriggeredAnimation()) &&
								 bac.getAnimationState() != State.STOPPED;
				if (!result) {
					return false;
				} else {
					double progress = bac.getAnimationProgress();
					return minProgress <= progress && progress < maxProgress;
				}
			}
		} else {
			return false;
		}
	}

	private static <T> boolean collectionContains(Collection<T> items, T item) {
		return item != null && items.contains(item);
	}

	public static boolean isPlayingAnimation(String animationName, ItemStack itemStack, String controllerName) {
		Item item = itemStack.getItem();
		if (!(item instanceof GunItem gunItem)) {
			return false;
		} else {
			AnimationController<GeoAnimatable> controller =
				gunItem.getGeoAnimationController(controllerName, itemStack);
			AnimationProcessor.QueuedAnimation currentAnimation = controller.getCurrentAnimation();
			return currentAnimation != null && animationName.equals(currentAnimation.animation().name()) &&
				controller.getAnimationState() != State.STOPPED;
		}
	}

	private static class PlayerWalkingStateContext {
		final LocalPlayer player;
		final ItemStack itemStack;
		final float speedSquared;
		final GunClientState state;

		public PlayerWalkingStateContext(LocalPlayer player, ItemStack itemStack) {
			this.player = player;
			this.itemStack = itemStack;
			this.speedSquared = player.input.getMoveVector().lengthSquared();
			this.state = GunClientState.getMainHeldState();
		}

		boolean isMovingForward() {
			return this.player.input.up;
		}

		boolean isMovingBackward() {
			return this.player.input.down;
		}

		boolean isMovingLeft() {
			return this.player.input.left && !this.player.input.up && !this.player.input.down;
		}

		boolean isMovingRight() {
			return this.player.input.right && !this.player.input.up && !this.player.input.down;
		}
	}

	private enum PlayerWalkingState {
		NONE,
		OFF_GROUND,
		OFF_GROUND_SPRINTING,
		STANDING,
		WALKING,
		WALKING_BACKWARDS,
		WALKING_LEFT,
		WALKING_RIGHT,
		WALKING_AIMING,
		PREPARE_RUNNING,
		RUNNING,
		COMPLETE_RUNNING;

		PlayerWalkingState() {}
	}
}
