//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import mod.pbj.item.Drawable;
import mod.pbj.item.GunItem;
import mod.pbj.item.ThrowableItem;
import mod.pbj.item.ThrowableLike;
import mod.pbj.util.ClientUtil;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.StateMachine;
import mod.pbj.util.StateMachine.TransitionMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThrowableClientState {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private static final Map<PlayerSlot, ThrowableClientState> localSlotStates = new HashMap<>();
	private static final Map<UUID, ThrowableClientState> noSlotStates = new WeakHashMap<>();
	private final UUID id;
	private final ThrowableLike item;
	private long prepareThrowCooldownStartTime;
	private long completeThrowCooldownStartTime;
	private long drawCooldownDuration;
	private long drawCooldownStartTime;
	private long inspectCooldownStartTime;
	private long inspectCooldownDuration;
	private long throwCooldownDuration;
	private long prepareIdleCooldownDuration;
	private long prepareIdleCooldownStartTime;
	private long idleCooldownStartTime;
	private long idleCooldownDuration;
	protected long throwCooldownStartTime;
	protected long lastThrowTime;
	protected boolean isTriggerOn;
	private final Predicate<Context> isValidGameMode =
		(context) -> context.player != null && !context.player.isSpectator();
	private State simplifiedThrowState;
	private final StateMachine<State, Context> stateMachine;
	private static final Map<UUID, ThrowableClientState> statesById = new HashMap<>();

	public ThrowableClientState(UUID id, ThrowableLike item) {
		this.id = id;
		this.item = item;
		this.stateMachine = this.createStateMachine();
		statesById.put(id, this);
	}

	private StateMachine<State, Context> createStateMachine() {
		StateMachine.Builder<State, Context> builder = new StateMachine.Builder<>();
		builder.withTransition(
			List.of(
				ThrowableClientState.State.PREPARE_IDLE,
				ThrowableClientState.State.IDLE,
				ThrowableClientState.State.IDLE_COOLDOWN),
			ThrowableClientState.State.DRAW,
			this.isValidGameMode,
			TransitionMode.EVENT,
			null,
			this::actionDraw);
		builder.withTransition(
			ThrowableClientState.State.DRAW,
			ThrowableClientState.State.DRAW_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.drawCooldownStartTime = System.nanoTime();
				this.drawCooldownDuration =
					this.item.getDrawCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
			});
		builder.withTransition(
			ThrowableClientState.State.DRAW_COOLDOWN,
			ThrowableClientState.State.PREPARE_IDLE,
			Predicate.not(this::isDrawCooldownInProgress));
		builder.withTransition(
			ThrowableClientState.State.PREPARE_IDLE,
			ThrowableClientState.State.IDLE,
			(ctx)
				-> !this.isPrepareIdleCooldownInProgress(ctx) && this.item.hasIdleAnimations(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			ThrowableClientState.State.IDLE,
			ThrowableClientState.State.IDLE_COOLDOWN,
			(ctx)
				-> this.item.hasIdleAnimations(),
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.idleCooldownStartTime = System.nanoTime();
				this.idleCooldownDuration =
					this.item.getIdleCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
			});
		builder.withTransition(
			ThrowableClientState.State.IDLE_COOLDOWN,
			ThrowableClientState.State.IDLE,
			Predicate.not(this::isIdleCooldownInProgress));
		builder.withTransition(
			List.of(ThrowableClientState.State.IDLE, ThrowableClientState.State.IDLE_COOLDOWN),
			ThrowableClientState.State.PREPARE_IDLE,
			(ctx)
				-> true,
			TransitionMode.EVENT,
			null,
			null);
		builder.withTransition(
			List.of(
				ThrowableClientState.State.PREPARE_IDLE,
				ThrowableClientState.State.IDLE,
				ThrowableClientState.State.IDLE_COOLDOWN),
			ThrowableClientState.State.INSPECT,
			this.isValidGameMode,
			TransitionMode.EVENT,
			null,
			this::actionInspect);
		builder.withTransition(
			ThrowableClientState.State.INSPECT,
			ThrowableClientState.State.INSPECT_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.inspectCooldownStartTime = System.nanoTime();
				this.inspectCooldownDuration =
					this.item.getInspectCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
			});
		builder.withTransition(
			ThrowableClientState.State.INSPECT_COOLDOWN,
			ThrowableClientState.State.PREPARE_IDLE,
			Predicate.not(this::inspectCooldownInProgress));
		builder.withTransition(
			ThrowableClientState.State.PREPARE_IDLE,
			ThrowableClientState.State.PREPARE_THROW,
			this.isValidGameMode.and(
				(context) -> this.isTriggerOn && context.itemStack == context.player.getMainHandItem()),
			TransitionMode.EVENT,
			null,
			this::actionPrepareThrow);
		builder.withTransition(
			List.of(
				ThrowableClientState.State.PREPARE_IDLE,
				ThrowableClientState.State.IDLE,
				ThrowableClientState.State.IDLE_COOLDOWN),
			ThrowableClientState.State.PREPARE_THROW,
			this.isValidGameMode.and(
				(context) -> this.isTriggerOn && context.itemStack == context.player.getMainHandItem()),
			TransitionMode.EVENT,
			null,
			this::actionPrepareThrow);
		builder.withTransition(
			List.of(
				ThrowableClientState.State.PREPARE_IDLE,
				ThrowableClientState.State.IDLE,
				ThrowableClientState.State.IDLE_COOLDOWN),
			ThrowableClientState.State.PREPARE_THROW,
			this.isValidGameMode.and(
				(context) -> this.isTriggerOn && context.itemStack == context.player.getMainHandItem()),
			TransitionMode.EVENT,
			null,
			this::actionPrepareThrow);
		builder.withTransition(
			ThrowableClientState.State.INSPECT_COOLDOWN,
			ThrowableClientState.State.PREPARE_THROW,
			(context)
				-> this.isTriggerOn && context.itemStack == context.player.getMainHandItem(),
			TransitionMode.EVENT,
			null,
			this::actionPrepareThrow);
		builder.withTransition(
			ThrowableClientState.State.PREPARE_THROW,
			ThrowableClientState.State.PREPARE_THROW_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.prepareThrowCooldownStartTime = System.nanoTime());
		builder.withTransition(
			ThrowableClientState.State.PREPARE_THROW_COOLDOWN,
			ThrowableClientState.State.THROW,
			(context)
				-> !this.isPrepareThrowCooldownInProgress(context),
			TransitionMode.AUTO,
			null,
			this::actionThrow);
		builder.withTransition(
			ThrowableClientState.State.THROW,
			ThrowableClientState.State.THROW_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.throwCooldownStartTime = System.nanoTime();
				this.throwCooldownDuration =
					this.item.getThrowCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
			});
		builder.withTransition(
			ThrowableClientState.State.THROW_COOLDOWN,
			ThrowableClientState.State.COMPLETE_THROW,
			Predicate.not(this::isThrowCooldownInProgress),
			TransitionMode.AUTO,
			null,
			this::actionCompleteThrow);
		builder.withTransition(
			ThrowableClientState.State.COMPLETE_THROW,
			ThrowableClientState.State.COMPLETE_THROW_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.completeThrowCooldownStartTime = System.nanoTime());
		builder.withTransition(
			ThrowableClientState.State.COMPLETE_THROW_COOLDOWN,
			ThrowableClientState.State.PREPARE_IDLE,
			Predicate.not(this::isCompleteFireCooldownInProgress),
			TransitionMode.AUTO,
			null,
			null);
		builder.withOnSetStateAction(
			ThrowableClientState.State.PREPARE_IDLE, (ctx, f, t) -> this.actionPrepareIdle(ctx));
		builder.withOnSetStateAction(ThrowableClientState.State.IDLE, (ctx, f, t) -> this.actionFiddle(ctx));
		builder.withOnChangeStateAction((ctx, f, t) -> LOGGER.debug("Throwable state changed from {} to {}", f, t));
		return builder.build(ThrowableClientState.State.PREPARE_IDLE);
	}

	private boolean isPrepareIdleCooldownInProgress(Context context) {
		return System.nanoTime() - this.prepareIdleCooldownStartTime <= this.prepareIdleCooldownDuration;
	}

	private boolean isIdleCooldownInProgress(Context context) {
		return System.nanoTime() - this.idleCooldownStartTime <= this.idleCooldownDuration;
	}

	private boolean isDrawCooldownInProgress(Context context) {
		return System.nanoTime() - this.drawCooldownStartTime <= this.drawCooldownDuration;
	}

	private boolean inspectCooldownInProgress(Context context) {
		return System.nanoTime() - this.inspectCooldownStartTime <= this.inspectCooldownDuration;
	}

	private boolean isCompleteFireCooldownInProgress(Context context) {
		return (double)(System.nanoTime() - this.completeThrowCooldownStartTime) <=
			(double)1000000.0F *
				(double)this.item.getCompleteThrowCooldownDuration(context.player, this, context.itemStack);
	}

	private boolean isThrowCooldownInProgress(Context context) {
		return System.nanoTime() - this.throwCooldownStartTime <= this.throwCooldownDuration;
	}

	private boolean isPrepareThrowCooldownInProgress(Context context) {
		return (double)(System.nanoTime() - this.prepareThrowCooldownStartTime) <=
			(double)1000000.0F *
				(double)this.item.getPrepareThrowCooldownDuration(context.player, this, context.itemStack);
	}

	private void actionPrepareThrow(Context context, State fromState, State toState) {
		this.item.prepareThrow(this, (Player)context.player, context.itemStack, context.targetEntity);
	}

	private void actionThrow(Context context, State fromState, State toState) {
		this.item.requestThrowFromServer(this, (Player)context.player, context.itemStack, context.targetEntity);
		this.lastThrowTime = MiscUtil.getLevel(context.player).getGameTime();
	}

	private void actionDraw(Context context, State fromState, State toState) {
		if (this.item instanceof Drawable drawable) {
			drawable.draw((Player)context.player, context.itemStack);
		}
	}

	private void actionInspect(Context context, State fromState, State toState) {}

	private void actionCompleteThrow(Context context, State fromState, State toState) {
		LOGGER.debug("{} Completing firing in state {}", System.currentTimeMillis() % 100000L, toState);
	}

	private void actionPrepareIdle(Context context) {
		this.prepareIdleCooldownStartTime = System.nanoTime();
		this.prepareIdleCooldownDuration = this.item.getPrepareIdleCooldownDuration();
	}

	private void actionFiddle(Context context) {}

	public boolean tryThrow(LivingEntity player, ItemStack itemStack, Entity targetEntity) {
		Context context = new Context(player, itemStack, targetEntity);
		return this.stateMachine.setStateToAnyOf(context, List.of(ThrowableClientState.State.PREPARE_THROW)) != null;
	}

	public boolean tryDraw(LivingEntity player, ItemStack itemStack) {
		Context context = new Context(player, itemStack);
		return this.stateMachine.setState(context, ThrowableClientState.State.DRAW) != null;
	}

	public boolean tryDeactivate(LivingEntity player, ItemStack itemStack) {
		Context context = new Context(player, itemStack);
		return this.stateMachine.setState(context, ThrowableClientState.State.PREPARE_IDLE) != null;
	}

	public boolean tryInspect(LivingEntity player, ItemStack itemStack) {
		Context context = new Context(player, itemStack);
		return this.stateMachine.setState(context, ThrowableClientState.State.INSPECT) != null;
	}

	public UUID getId() {
		return this.id;
	}

	public void setTrigger(boolean isTriggerOn) {
		this.isTriggerOn = isTriggerOn;
	}

	public boolean isIdle() {
		State fireState = this.stateMachine.getCurrentState();
		return this.simplifiedThrowState == ThrowableClientState.State.IDLE ||
			fireState == ThrowableClientState.State.PREPARE_IDLE || fireState == ThrowableClientState.State.IDLE ||
			fireState == ThrowableClientState.State.IDLE_COOLDOWN;
	}

	public boolean isThrowing() {
		State fireState = this.stateMachine.getCurrentState();
		return isThrowing(fireState) || this.simplifiedThrowState == ThrowableClientState.State.THROW;
	}

	private static boolean isThrowing(State fireState) {
		return fireState == ThrowableClientState.State.PREPARE_THROW ||
			fireState == ThrowableClientState.State.PREPARE_THROW_COOLDOWN ||
			fireState == ThrowableClientState.State.THROW || fireState == ThrowableClientState.State.THROW_COOLDOWN ||
			fireState == ThrowableClientState.State.COMPLETE_THROW ||
			fireState == ThrowableClientState.State.COMPLETE_THROW_COOLDOWN;
	}

	public boolean isPreparingThrowing() {
		State fireState = this.stateMachine.getCurrentState();
		return fireState == ThrowableClientState.State.PREPARE_THROW ||
			fireState == ThrowableClientState.State.PREPARE_THROW_COOLDOWN;
	}

	public boolean isCompletingThrowing() {
		State state = this.stateMachine.getCurrentState();
		return state == ThrowableClientState.State.COMPLETE_THROW ||
			state == ThrowableClientState.State.COMPLETE_THROW_COOLDOWN;
	}

	public boolean isDrawing() {
		State state = this.stateMachine.getCurrentState();
		return state == ThrowableClientState.State.DRAW || state == ThrowableClientState.State.DRAW_COOLDOWN;
	}

	public boolean isInspecting() {
		State state = this.stateMachine.getCurrentState();
		return state == ThrowableClientState.State.INSPECT || state == ThrowableClientState.State.INSPECT_COOLDOWN;
	}

	public void updateState(LivingEntity player, ItemStack itemStack, boolean isSelected) {
		if (player == ClientUtil.getClientPlayer()) {
			Context context = new Context(player, itemStack);
			this.stateMachine.update(context);
			State updatedSimplifiedFireState =
				ThrowableClientState.State.getSimplifiedState(this.stateMachine.getCurrentState());
			if (updatedSimplifiedFireState != this.simplifiedThrowState) {
				this.simplifiedThrowState = updatedSimplifiedFireState;
			}
		}
	}

	public String toString() {
		return String.format("ThrowableClientState[sid=%s,id=%s]", System.identityHashCode(this), this.id);
	}

	public static ThrowableClientState getMainHeldState() {
		Player player = ClientUtil.getClientPlayer();
		return getMainHeldState(player);
	}

	public static ThrowableClientState getMainHeldState(Player player) {
		if (player == null) {
			return null;
		} else {
			ItemStack itemStack = player.getMainHandItem();
			int activeSlot = player.getInventory().selected;
			return itemStack != null && itemStack.getItem() instanceof ThrowableItem
				? getState(player, itemStack, activeSlot, false)
				: null;
		}
	}

	public static ThrowableClientState getState(Player player, ItemStack itemStack, int slotIndex, boolean isOffhand) {
		Level level = MiscUtil.getLevel(player);
		UUID stackId = GunItem.getItemStackId(itemStack);
		if (stackId == null) {
			return null;
		} else if (slotIndex == -1) {
			ThrowableClientState state = null;

			for (Map.Entry<PlayerSlot, ThrowableClientState> entry : localSlotStates.entrySet()) {
				if (entry.getKey().isClientSide == level.isClientSide &&
					Objects.equals(entry.getValue().getId(), stackId)) {
					PlayerSlot playerSlot = entry.getKey();
					slotIndex = playerSlot.slotId;
					if (slotIndex >= 0 && playerSlot.playerEntityId == player.getId()) {
						ItemStack stackAtSlotIndex = player.getInventory().getItem(slotIndex);
						if (stackAtSlotIndex != null && stackAtSlotIndex.getItem() instanceof ThrowableLike) {
							state = entry.getValue();
							break;
						}
					}
				}
			}

			if (state == null) {
				ThrowableLike var10002 = (ThrowableLike)itemStack.getItem();
				Objects.requireNonNull(var10002);
				state = noSlotStates.computeIfAbsent(stackId, var10002::createState);
			}

			return state;
		} else {
			int adjustedSlotIndex = isOffhand ? -5 : slotIndex;
			PlayerSlot playerSlot = new PlayerSlot(player.getId(), adjustedSlotIndex, level.isClientSide);
			ThrowableClientState slotState = localSlotStates.get(playerSlot);
			if (slotState == null || !Objects.equals(slotState.getId(), stackId)) {
				slotState = ((ThrowableLike)itemStack.getItem()).createState(stackId);
				localSlotStates.put(playerSlot, slotState);
			}

			return slotState;
		}
	}

	public static ThrowableClientState getState(UUID stateId) {
		return statesById.get(stateId);
	}

	public void setSimplifiedState(State state) {
		this.simplifiedThrowState = state;
	}

	public enum State {
		PREPARE_IDLE,
		IDLE,
		IDLE_COOLDOWN,
		DRAW,
		DRAW_COOLDOWN,
		PREPARE_THROW,
		PREPARE_THROW_COOLDOWN,
		THROW,
		THROW_COOLDOWN,
		COMPLETE_THROW,
		COMPLETE_THROW_COOLDOWN,
		INSPECT,
		INSPECT_COOLDOWN;

		State() {}

		public static State getSimplifiedState(State state) {
			return switch (state) {
				case DRAW, DRAW_COOLDOWN -> DRAW;
				case PREPARE_THROW, PREPARE_THROW_COOLDOWN, THROW, THROW_COOLDOWN, COMPLETE_THROW,
					COMPLETE_THROW_COOLDOWN ->
					THROW;
				case INSPECT, INSPECT_COOLDOWN -> INSPECT;
				default -> IDLE;
			};
		}
	}

	private static class Context {
		LivingEntity player;
		ItemStack itemStack;
		Entity targetEntity;

		public Context(LivingEntity player, ItemStack itemStack) {
			this.player = player;
			this.itemStack = itemStack;
		}

		public Context(LivingEntity player, ItemStack itemStack, Entity targetEntity) {
			this.player = player;
			this.itemStack = itemStack;
			this.targetEntity = targetEntity;
		}
	}

	private static class PlayerSlot {
		int playerEntityId;
		int slotId;
		boolean isClientSide;

		PlayerSlot(int playerEntityId, int slotId, boolean isClientSide) {
			this.playerEntityId = playerEntityId;
			this.slotId = slotId;
			this.isClientSide = isClientSide;
		}

		public int hashCode() {
			return Objects.hash(this.isClientSide, this.playerEntityId, this.slotId);
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj == null) {
				return false;
			} else if (this.getClass() != obj.getClass()) {
				return false;
			} else {
				PlayerSlot other = (PlayerSlot)obj;
				return this.isClientSide == other.isClientSide && this.playerEntityId == other.playerEntityId &&
					this.slotId == other.slotId;
			}
		}
	}
}
