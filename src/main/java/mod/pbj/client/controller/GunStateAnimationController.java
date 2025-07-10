package mod.pbj.client.controller;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import mod.pbj.client.GunClientState;
import mod.pbj.client.GunStateListener;
import mod.pbj.feature.ConditionContext;
import mod.pbj.item.GunItem;
import mod.pbj.util.ClientUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.Animation.LoopType;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class GunStateAnimationController extends AnimationController<GunItem> implements GunStateListener {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private final RawAnimation animation;
	private final Predicate<ConditionContext> shouldAnimate;
	private final Deque<Action> pendingActions = new ArrayDeque<>();
	private final String animationName;
	private final Map<String, RawAnimation> animations = new HashMap<>();

	public GunStateAnimationController(
		GunItem animatable, String controllerName, String animationName, Predicate<ConditionContext> shouldAnimate) {
		super(animatable, controllerName, new StateHandler());
		this.animation = RawAnimation.begin().then(animationName, LoopType.PLAY_ONCE);
		this.animations.put(animationName, this.animation);
		this.animationName = animationName;
		this.shouldAnimate = shouldAnimate;
	}

	RawAnimation getAnimation(String animationName) {
		return this.animations.computeIfAbsent(
			animationName, (n) -> RawAnimation.begin().then(animationName, LoopType.PLAY_ONCE));
	}

	public void scheduleReset(LivingEntity player, GunClientState state, ItemStack itemStack, String animationName) {
		this.pendingActions.addLast(new ResetAction(animationName != null ? animationName : this.animationName));
	}

	public void scheduleReset(LivingEntity player, GunClientState state, ItemStack itemStack) {
		if (ClientUtil.isFirstPerson()) {
			this.scheduleReset(player, state, itemStack, this.animationName);
		}
	}

	private static class StateHandler implements AnimationController.AnimationStateHandler<GunItem> {
		private PlayState currentPlayState;

		private StateHandler() {}

		public PlayState handle(AnimationState<GunItem> state) {
			GunStateAnimationController controller = this.getThisController(state);

			while (!controller.pendingActions.isEmpty()) {
				Action action = controller.pendingActions.removeFirst();
				action.execute(state);
			}

			ItemDisplayContext perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
			if (perspective != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
				return PlayState.STOP;
			} else {
				ItemStack itemStack = state.getData(DataTickets.ITEMSTACK);
				Player player = ClientUtil.getClientPlayer();
				GunClientState gunClientState = null;
				UUID itemStackId = GunItem.getItemStackId(itemStack);
				boolean isMainHand = itemStackId != null &&
									 Objects.equals(GunItem.getItemStackId(player.getMainHandItem()), itemStackId);
				if (player != null && isMainHand && itemStack != null && itemStack.getItem() instanceof GunItem) {
					gunClientState = GunClientState.getState(player, itemStack, -1, false);
					boolean shouldAnimate = false;

					if (gunClientState != null && (shouldAnimate = controller.shouldAnimate.test(new ConditionContext(
													   player, itemStack, gunClientState, perspective)))) {
						return this.setPlayState(PlayState.CONTINUE, controller);
					}

					if (gunClientState != null && this.currentPlayState == null) {
						GunStateAnimationController.LOGGER.debug(
							"{} {} cannot continue playing. Should animate: {}, iter: {}",
							System.currentTimeMillis() % 100000L,
							controller.getName(),
							shouldAnimate,
							gunClientState.getReloadIterationIndex());
					}
				}

				return this.setPlayState(PlayState.STOP, controller);
			}
		}

		private PlayState setPlayState(PlayState playState, GunStateAnimationController controller) {
			if (playState != this.currentPlayState) {
				// Spams logs on idle animation, reenable if neccessary for debugging
				// GunStateAnimationController.LOGGER.debug("{} {} updated play state from {} to {}",
				// System.currentTimeMillis() % 100000L, controller.getName(), this.currentPlayState, playState);
			}

			this.currentPlayState = playState;
			return this.currentPlayState;
		}

		private GunStateAnimationController getThisController(AnimationState<GunItem> state) {
			return (GunStateAnimationController)state.getController();
		}
	}

	private class ResetAction implements Action {
		private final String animationName;

		ResetAction(String animationName) {
			this.animationName = animationName;
		}

		public void execute(AnimationState<GunItem> state) {
			CoreGeoModel<GunItem> model = GunStateAnimationController.this.lastModel;
			if (model != null) {
				Animation a = model.getAnimation(GunStateAnimationController.this.animatable, this.animationName);
				if (a != null) {
					// Causes logs to spam, reenable if neccessary for debugging
					// GunStateAnimationController.LOGGER.debug("{} {} resetting animation {}",
					// System.currentTimeMillis() % 100000L, GunStateAnimationController.this.getName(),
					// this.animationName);
					((StateHandler)GunStateAnimationController.this.stateHandler).currentPlayState = null;
					state.resetCurrentAnimation();
					GunStateAnimationController.this.setAnimation(
						GunStateAnimationController.this.getAnimation(this.animationName));
				} else {
					GunStateAnimationController.LOGGER.debug("Animation not found: {}", this.animationName);
				}
			}
		}
	}

	private interface Action {
		void execute(AnimationState<GunItem> var1);
	}

	public enum DefaultControllers {

	}
}
