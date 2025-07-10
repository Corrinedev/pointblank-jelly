package mod.pbj.client;

import java.util.*;
import java.util.function.Predicate;
import mod.pbj.client.effect.MuzzleFlashEffect;
import mod.pbj.feature.FireModeFeature;
import mod.pbj.feature.ReloadFeature;
import mod.pbj.item.*;
import mod.pbj.item.GunItem.ReloadPhase;
import mod.pbj.network.MainHeldSimplifiedStateSyncRequest;
import mod.pbj.network.Network;
import mod.pbj.util.ClientUtil;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.StateMachine;
import mod.pbj.util.StateMachine.TransitionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.util.ClientUtils;

public class GunClientState {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private static final Map<PlayerSlot, GunClientState> localSlotStates = new HashMap<>();
	private static final Map<UUID, GunClientState> noSlotStates = new WeakHashMap<>();
	private final UUID id;
	private final GunItem gunItem;
	protected int totalUninterruptedFireTime;
	private long reloadCooldownStartTime;
	private long reloadCooldownDuration;
	private long prepareFireCooldownStartTime;
	private long completeFireCooldownStartTime;
	private long drawCooldownDuration;
	private long drawCooldownStartTime;
	private long inspectCooldownStartTime;
	private long inspectCooldownDuration;
	private long prepareIdleCooldownStartTime;
	private long prepareIdleCooldownDuration = 0L;
	private long idleCooldownStartTime;
	private long idleCooldownDuration;
	private long enableFireModeCooldownStartTime;
	protected long fireCooldownStartTime;
	protected long lastSyncTime;
	protected long lastShotFiredTime;
	protected AmmoCount ammoCount = new AmmoCount();
	protected boolean isTriggerOn;
	protected int totalUninterruptedShots;
	private final Map<String, GunStateListener> animationControllers = new HashMap<>();
	private final List<GunStateListener> stateListeners = new ArrayList<>();
	private final List<MuzzleFlashEffect> muzzleFlashEffects = new ArrayList<>();
	private final long clientSyncTimeoutTicks = 20L;
	private boolean isAiming;
	private int remainingAmmoToReload;
	private final AmmoCount reloadAmmoCount = new AmmoCount();
	private int reloadIterationIndex;
	private Component temporaryMessage;
	private long messageEndTime;
	private Predicate<GunClientState> messagePredicate;
	private final Predicate<GunClientStateContext> hasAmmo =
		(context) -> this.ammoCount.getAmmoCount(context.getFireModeInstance()) > 0;
	private final Predicate<GunClientStateContext> isValidGameMode =
		(context) -> context.player != null && !context.player.isSpectator();
	private FireState simplifiedFireState;
	private final StateMachine<FireState, GunClientStateContext> stateMachine;
	private static final Map<UUID, GunClientState> statesById = new HashMap<>();

	public GunClientState(UUID id, GunItem item) {
		this.id = id;
		this.gunItem = item;
		this.stateMachine = this.createStateMachine();
		this.prepareIdleCooldownDuration = 1000000L * this.gunItem.getPrepareIdleCooldownDuration();
		statesById.put(id, this);
	}

	private StateMachine<FireState, GunClientStateContext> createStateMachine() {
		StateMachine.Builder<FireState, GunClientStateContext> builder = new StateMachine.Builder<>();
		builder.withTransition(
			List.of(
				GunClientState.FireState.PREPARE_IDLE,
				GunClientState.FireState.IDLE,
				GunClientState.FireState.IDLE_COOLDOWN),
			GunClientState.FireState.DRAW,
			this.isValidGameMode,
			TransitionMode.EVENT,
			null,
			this::actionDraw);
		builder.withTransition(
			GunClientState.FireState.DRAW,
			GunClientState.FireState.DRAW_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.drawCooldownStartTime = System.nanoTime();
				this.drawCooldownDuration =
					this.gunItem.getDrawCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
			});
		builder.withTransition(
			GunClientState.FireState.DRAW_COOLDOWN,
			GunClientState.FireState.PREPARE_IDLE,
			Predicate.not(this::isDrawCooldownInProgress));
		builder.withTransition(
			GunClientState.FireState.PREPARE_IDLE,
			GunClientState.FireState.IDLE,
			(ctx)
				-> !this.isPrepareIdleCooldownInProgress(ctx) && this.gunItem.hasIdleAnimations(),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			GunClientState.FireState.IDLE,
			GunClientState.FireState.IDLE_COOLDOWN,
			(ctx)
				-> this.gunItem.hasIdleAnimations(),
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.idleCooldownStartTime = System.nanoTime();
				this.idleCooldownDuration =
					this.gunItem.getIdleCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
			});
		builder.withTransition(
			GunClientState.FireState.IDLE_COOLDOWN,
			GunClientState.FireState.IDLE,
			Predicate.not(this::isIdleCooldownInProgress));
		builder.withTransition(
			List.of(GunClientState.FireState.IDLE, GunClientState.FireState.IDLE_COOLDOWN),
			GunClientState.FireState.PREPARE_IDLE,
			(ctx)
				-> true,
			TransitionMode.EVENT,
			null,
			null);
		builder.withTransition(
			List.of(
				GunClientState.FireState.PREPARE_IDLE,
				GunClientState.FireState.IDLE,
				GunClientState.FireState.IDLE_COOLDOWN),
			GunClientState.FireState.INSPECT,
			this.isValidGameMode,
			TransitionMode.EVENT,
			null,
			this::actionInspect);
		builder.withTransition(
			GunClientState.FireState.INSPECT,
			GunClientState.FireState.INSPECT_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.inspectCooldownStartTime = System.nanoTime();
				this.inspectCooldownDuration =
					this.gunItem.getInspectCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
			});
		builder.withTransition(
			GunClientState.FireState.INSPECT_COOLDOWN,
			GunClientState.FireState.PREPARE_IDLE,
			Predicate.not(this::inspectCooldownInProgress));
		builder.withTransition(
			GunClientState.FireState.PREPARE_IDLE,
			GunClientState.FireState.CHANGE_FIRE_MODE,
			this.isValidGameMode,
			TransitionMode.EVENT,
			null,
			this::actionEnableFireMode);
		builder.withTransition(
			List.of(
				GunClientState.FireState.PREPARE_IDLE,
				GunClientState.FireState.IDLE,
				GunClientState.FireState.IDLE_COOLDOWN),
			GunClientState.FireState.CHANGE_FIRE_MODE,
			this.isValidGameMode,
			TransitionMode.EVENT,
			null,
			this::actionEnableFireMode);
		builder.withTransition(
			GunClientState.FireState.CHANGE_FIRE_MODE,
			GunClientState.FireState.CHANGE_FIRE_MODE_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.enableFireModeCooldownStartTime = System.nanoTime());
		builder.withTransition(
			GunClientState.FireState.CHANGE_FIRE_MODE_COOLDOWN,
			GunClientState.FireState.PREPARE_IDLE,
			Predicate.not(this::isEnableFireModeCooldownInProgress));
		builder.withTransition(
			GunClientState.FireState.PREPARE_IDLE,
			GunClientState.FireState.PREPARE_RELOAD,
			this.isValidGameMode.and(Predicate.not(this::requiresPhasedReload)),
			TransitionMode.EVENT,
			null,
			this::actionPrepareReload);
		builder.withTransition(
			List.of(
				GunClientState.FireState.PREPARE_IDLE,
				GunClientState.FireState.IDLE,
				GunClientState.FireState.IDLE_COOLDOWN),
			GunClientState.FireState.PREPARE_RELOAD,
			this.isValidGameMode.and(Predicate.not(this::requiresPhasedReload)),
			TransitionMode.EVENT,
			null,
			this::actionPrepareReload);
		builder.withTransition(
			GunClientState.FireState.PREPARE_RELOAD,
			GunClientState.FireState.PREPARE_RELOAD_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.reloadCooldownStartTime = System.nanoTime();
				this.reloadCooldownDuration =
					this.gunItem.getReloadingCooldownTime(ReloadPhase.PREPARING, ctx.player, this, ctx.itemStack) *
					1000000L;
			});
		builder.withTransition(
			GunClientState.FireState.PREPARE_RELOAD_COOLDOWN,
			GunClientState.FireState.RELOAD,
			Predicate.not(this::isReloadCooldownInProgress),
			TransitionMode.AUTO,
			null,
			this::actionReload);
		builder.withTransition(
			GunClientState.FireState.RELOAD,
			GunClientState.FireState.RELOAD_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.reloadCooldownStartTime = System.nanoTime();
				this.reloadCooldownDuration =
					this.gunItem.getReloadingCooldownTime(ReloadPhase.RELOADING, ctx.player, this, ctx.itemStack) *
					1000000L;
			});
		builder.withTransition(
			GunClientState.FireState.RELOAD_COOLDOWN,
			GunClientState.FireState.COMPLETE_RELOAD,
			Predicate.not(this::isReloadCooldownInProgress),
			TransitionMode.AUTO,
			null,
			this::actionCompleteReload);
		builder.withTransition(
			GunClientState.FireState.PREPARE_IDLE,
			GunClientState.FireState.PREPARE_RELOAD_ITER,
			(context)
				-> this.isValidGameMode.test(context) && this.canReload(context) && this.requiresPhasedReload(context),
			TransitionMode.EVENT,
			null,
			this::actionPrepareReload);
		builder.withTransition(
			List.of(
				GunClientState.FireState.PREPARE_IDLE,
				GunClientState.FireState.IDLE,
				GunClientState.FireState.IDLE_COOLDOWN),
			GunClientState.FireState.PREPARE_RELOAD_ITER,
			(context)
				-> this.isValidGameMode.test(context) && this.canReload(context) && this.requiresPhasedReload(context),
			TransitionMode.EVENT,
			null,
			this::actionPrepareReload);
		builder.withTransition(
			GunClientState.FireState.PREPARE_RELOAD_ITER,
			GunClientState.FireState.PREPARE_RELOAD_COOLDOWN_ITER,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.reloadCooldownStartTime = System.nanoTime();
				this.reloadCooldownDuration =
					this.gunItem.getReloadingCooldownTime(ReloadPhase.PREPARING, ctx.player, this, ctx.itemStack) *
					1000000L;
			});
		builder.withTransition(
			GunClientState.FireState.PREPARE_RELOAD_COOLDOWN_ITER,
			GunClientState.FireState.RELOAD_ITER,
			Predicate.not(this::isReloadCooldownInProgress),
			TransitionMode.AUTO,
			null,
			this::actionReload);
		builder.withTransition(
			GunClientState.FireState.RELOAD_ITER,
			GunClientState.FireState.RELOAD_COOLDOWN_ITER,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.reloadCooldownStartTime = System.nanoTime();
				this.reloadCooldownDuration =
					this.gunItem.getReloadingCooldownTime(ReloadPhase.RELOADING, ctx.player, this, ctx.itemStack) *
					1000000L;
			});
		builder.withTransition(
			GunClientState.FireState.RELOAD_COOLDOWN_ITER,
			GunClientState.FireState.RELOAD_ITER,
			(context)
				-> !this.isReloadCooldownInProgress(context) && this.remainingAmmoToReload > 0,
			TransitionMode.AUTO,
			null,
			this::actionReload);
		builder.withTransition(
			GunClientState.FireState.RELOAD_COOLDOWN_ITER,
			GunClientState.FireState.COMPLETE_RELOAD,
			(context)
				-> !this.isReloadCooldownInProgress(context),
			TransitionMode.AUTO,
			null,
			this::actionCompleteReload);
		builder.withTransition(
			GunClientState.FireState.COMPLETE_RELOAD,
			GunClientState.FireState.COMPLETE_RELOAD_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> {
				this.reloadCooldownStartTime = System.nanoTime();
				this.reloadCooldownDuration =
					this.gunItem.getReloadingCooldownTime(ReloadPhase.COMPLETETING, ctx.player, this, ctx.itemStack) *
					1000000L;
			});
		builder.withTransition(
			GunClientState.FireState.COMPLETE_RELOAD_COOLDOWN,
			GunClientState.FireState.PREPARE_IDLE,
			Predicate.not(this::isReloadCooldownInProgress).and(this::canCompleteReload),
			TransitionMode.AUTO,
			this::actionApplyReloadAmmo,
			null);
		builder.withTransition(
			List.of(
				GunClientState.FireState.PREPARE_IDLE,
				GunClientState.FireState.IDLE,
				GunClientState.FireState.IDLE_COOLDOWN,
				GunClientState.FireState.INSPECT_COOLDOWN),
			GunClientState.FireState.PREPARE_FIRE_SINGLE,
			this.isValidGameMode.and(
				this.hasAmmo
					.and(
						(context)
							-> GunItem.getSelectedFireModeType(context.itemStack) == FireMode.SINGLE ||
								   GunItem.getSelectedFireModeType(context.itemStack) == FireMode.MELEE)
					.and(
						(context)
							-> this.isTriggerOn && context.itemStack == context.player.getMainHandItem() &&
								   !context.player.isSprinting() &&
								   !FirstPersonWalkingAnimationHandler.isPlayingBlendingAnimation(
									   Set.of(
										   GunItem.RAW_ANIMATION_PREPARE_RUNNING,
										   GunItem.RAW_ANIMATION_COMPLETE_RUNNING,
										   GunItem.RAW_ANIMATION_RUNNING),
									   context.itemStack,
									   0.0F,
									   0.25F))),
			TransitionMode.EVENT,
			null,
			this::actionPrepareFire);
		builder.withTransition(
			GunClientState.FireState.PREPARE_FIRE_SINGLE,
			GunClientState.FireState.PREPARE_FIRE_COOLDOWN_SINGLE,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.prepareFireCooldownStartTime = System.nanoTime());
		builder.withTransition(
			GunClientState.FireState.PREPARE_FIRE_COOLDOWN_SINGLE,
			GunClientState.FireState.PREPARE_IDLE,
			(context)
				-> !this.isTriggerOn,
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			GunClientState.FireState.PREPARE_FIRE_COOLDOWN_SINGLE,
			GunClientState.FireState.FIRE_SINGLE,
			(context)
				-> this.isTriggerOn && !this.isPrepareFireCooldownInProgress(context),
			TransitionMode.AUTO,
			null,
			this::actionFire);
		builder.withTransition(
			GunClientState.FireState.FIRE_SINGLE,
			GunClientState.FireState.FIRE_COOLDOWN_SINGLE,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.fireCooldownStartTime = System.nanoTime());
		builder.withTransition(
			GunClientState.FireState.FIRE_COOLDOWN_SINGLE,
			GunClientState.FireState.COMPLETE_FIRE,
			Predicate.not(this::isFireCooldownInProgress),
			TransitionMode.AUTO,
			null,
			this::actionCompleteFire);
		builder.withTransition(
			List.of(
				GunClientState.FireState.PREPARE_IDLE,
				GunClientState.FireState.IDLE,
				GunClientState.FireState.IDLE_COOLDOWN,
				GunClientState.FireState.INSPECT_COOLDOWN),
			GunClientState.FireState.PREPARE_FIRE_BURST,
			this.isValidGameMode.and(
				this.hasAmmo.and((context) -> GunItem.getSelectedFireModeType(context.itemStack) == FireMode.BURST)
					.and(
						(context)
							-> this.isTriggerOn && context.itemStack == context.player.getMainHandItem() &&
								   !context.player.isSprinting() &&
								   !FirstPersonWalkingAnimationHandler.isPlayingBlendingAnimation(
									   Set.of(
										   GunItem.RAW_ANIMATION_PREPARE_RUNNING,
										   GunItem.RAW_ANIMATION_COMPLETE_RUNNING,
										   GunItem.RAW_ANIMATION_RUNNING),
									   context.itemStack,
									   0.0F,
									   0.25F))),
			TransitionMode.EVENT,
			null,
			this::actionPrepareFire);
		builder.withTransition(
			GunClientState.FireState.PREPARE_FIRE_BURST,
			GunClientState.FireState.PREPARE_FIRE_COOLDOWN_BURST,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.prepareFireCooldownStartTime = System.nanoTime());
		builder.withTransition(
			GunClientState.FireState.PREPARE_FIRE_COOLDOWN_BURST,
			GunClientState.FireState.PREPARE_IDLE,
			(context)
				-> !this.isTriggerOn,
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			GunClientState.FireState.PREPARE_FIRE_COOLDOWN_BURST,
			GunClientState.FireState.FIRE_BURST,
			(context)
				-> this.isTriggerOn && !this.isPrepareFireCooldownInProgress(context),
			TransitionMode.AUTO,
			null,
			this::actionFire);
		builder.withTransition(
			GunClientState.FireState.FIRE_BURST,
			GunClientState.FireState.FIRE_COOLDOWN_BURST,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.fireCooldownStartTime = System.nanoTime());
		builder.withTransition(
			GunClientState.FireState.FIRE_COOLDOWN_BURST,
			GunClientState.FireState.FIRE_BURST,
			Predicate.not(this::isFireCooldownInProgress)
				.and(this.hasAmmo)
				.and((context) -> GunItem.getSelectedFireModeType(context.itemStack) == FireMode.BURST)
				.and((context) -> this.isTriggerOn && context.itemStack == context.player.getMainHandItem())
				.and(
					(context)
						-> this.totalUninterruptedShots <
							   this.gunItem.getBurstShots(context.itemStack, context.getFireModeInstance())),
			TransitionMode.AUTO,
			null,
			this::actionFire);
		builder.withTransition(
			GunClientState.FireState.FIRE_COOLDOWN_BURST,
			GunClientState.FireState.COMPLETE_FIRE,
			Predicate.not(this::isFireCooldownInProgress)
				.and(this.hasAmmo.negate()
						 .or((context) -> !this.isTriggerOn)
						 .or((context) -> context.itemStack != context.player.getMainHandItem())
						 .or((context)
								 -> this.totalUninterruptedShots >=
										this.gunItem.getBurstShots(context.itemStack, context.getFireModeInstance()))),
			TransitionMode.AUTO,
			null,
			this::actionCompleteFire);
		builder.withTransition(
			List.of(
				GunClientState.FireState.PREPARE_IDLE,
				GunClientState.FireState.IDLE,
				GunClientState.FireState.IDLE_COOLDOWN,
				GunClientState.FireState.INSPECT_COOLDOWN),
			GunClientState.FireState.PREPARE_FIRE_AUTO,
			this.isValidGameMode.and(
				this.hasAmmo.and((context) -> GunItem.getSelectedFireModeType(context.itemStack) == FireMode.AUTOMATIC)
					.and(
						(context)
							-> this.isTriggerOn && context.itemStack == context.player.getMainHandItem() &&
								   !context.player.isSprinting() &&
								   !FirstPersonWalkingAnimationHandler.isPlayingBlendingAnimation(
									   Set.of(
										   GunItem.RAW_ANIMATION_PREPARE_RUNNING,
										   GunItem.RAW_ANIMATION_COMPLETE_RUNNING,
										   GunItem.RAW_ANIMATION_RUNNING),
									   context.itemStack,
									   0.0F,
									   0.25F))),
			TransitionMode.EVENT,
			null,
			this::actionPrepareFire);
		builder.withTransition(
			GunClientState.FireState.PREPARE_FIRE_AUTO,
			GunClientState.FireState.PREPARE_FIRE_COOLDOWN_AUTO,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.prepareFireCooldownStartTime = System.nanoTime());
		builder.withTransition(
			GunClientState.FireState.PREPARE_FIRE_COOLDOWN_AUTO,
			GunClientState.FireState.PREPARE_IDLE,
			(context)
				-> !this.isTriggerOn,
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			GunClientState.FireState.PREPARE_FIRE_COOLDOWN_AUTO,
			GunClientState.FireState.FIRE_AUTO,
			(context)
				-> this.isTriggerOn && !this.isPrepareFireCooldownInProgress(context),
			TransitionMode.AUTO,
			null,
			this::actionFire);
		builder.withTransition(
			GunClientState.FireState.FIRE_AUTO,
			GunClientState.FireState.FIRE_COOLDOWN_AUTO,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.fireCooldownStartTime = System.nanoTime());
		builder.withTransition(
			GunClientState.FireState.FIRE_COOLDOWN_AUTO,
			GunClientState.FireState.FIRE_AUTO,
			Predicate.not(this::isFireCooldownInProgress)
				.and(this.hasAmmo)
				.and((context) -> GunItem.getSelectedFireModeType(context.itemStack) == FireMode.AUTOMATIC)
				.and((context) -> this.isTriggerOn && context.itemStack == context.player.getMainHandItem()),
			TransitionMode.AUTO,
			null,
			this::actionFire);
		builder.withTransition(
			GunClientState.FireState.FIRE_COOLDOWN_AUTO,
			GunClientState.FireState.COMPLETE_FIRE,
			Predicate.not(this::isFireCooldownInProgress)
				.and(this.hasAmmo.negate()
						 .or((context) -> !this.isTriggerOn)
						 .or((context) -> context.itemStack != context.player.getMainHandItem())),
			TransitionMode.AUTO,
			null,
			this::actionCompleteFire);
		builder.withTransition(
			GunClientState.FireState.COMPLETE_FIRE,
			GunClientState.FireState.COMPLETE_FIRE_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.completeFireCooldownStartTime = System.nanoTime());
		builder.withTransition(
			GunClientState.FireState.COMPLETE_FIRE_COOLDOWN,
			GunClientState.FireState.PREPARE_IDLE,
			Predicate.not(this::isCompleteFireCooldownInProgress),
			TransitionMode.AUTO,
			null,
			null);
		builder.withOnSetStateAction(GunClientState.FireState.PREPARE_IDLE, (ctx, f, t) -> this.actionPrepareIdle(ctx));
		builder.withOnSetStateAction(GunClientState.FireState.IDLE, (ctx, f, t) -> this.actionFiddle(ctx));
		builder.withOnChangeStateAction((ctx, f, t) -> {
			boolean wasFiring = isFiring(f);
			boolean isFiring = isFiring(t);
			if (isFiring && !wasFiring) {
				this.actionStartAutoFiring(ctx);
			} else if (!isFiring && wasFiring) {
				this.actionStopAutoFiring(ctx);
			}
		});
		return builder.build(GunClientState.FireState.PREPARE_IDLE);
	}

	private boolean isPrepareIdleCooldownInProgress(GunClientStateContext context) {
		return System.nanoTime() - this.prepareIdleCooldownStartTime <= this.prepareIdleCooldownDuration;
	}

	private boolean isIdleCooldownInProgress(GunClientStateContext context) {
		return System.nanoTime() - this.idleCooldownStartTime <= this.idleCooldownDuration;
	}

	private boolean isDrawCooldownInProgress(GunClientStateContext context) {
		return System.nanoTime() - this.drawCooldownStartTime <= this.drawCooldownDuration;
	}

	private boolean inspectCooldownInProgress(GunClientStateContext context) {
		return System.nanoTime() - this.inspectCooldownStartTime <= this.inspectCooldownDuration;
	}

	private boolean isEnableFireModeCooldownInProgress(GunClientStateContext context) {
		return (double)(System.nanoTime() - this.enableFireModeCooldownStartTime) <=
			(double)1000000.0F *
				(double)FireModeFeature.getEnableFireModeCooldownDuration(context.player, this, context.itemStack);
	}

	private boolean isCompleteFireCooldownInProgress(GunClientStateContext context) {
		return (double)(System.nanoTime() - this.completeFireCooldownStartTime) <=
			(double)1000000.0F *
				(double)FireModeFeature.getCompleteFireCooldownDuration(context.player, this, context.itemStack);
	}

	private boolean isFireCooldownInProgress(GunClientStateContext context) {
		return System.nanoTime() - this.fireCooldownStartTime <= this.getFireCooldownDuration(context);
	}

	private long getFireCooldownDuration(GunClientStateContext context) {
		return (long)(6.0E10 / (double)FireModeFeature.getRpm(context.itemStack));
	}

	private boolean isPrepareFireCooldownInProgress(GunClientStateContext context) {
		return (double)(System.nanoTime() - this.prepareFireCooldownStartTime) <=
			(double)1000000.0F *
				(double)FireModeFeature.getPrepareFireCooldownDuration(context.player, this, context.itemStack);
	}

	private boolean isReloadCooldownInProgress(GunClientStateContext context) {
		return System.nanoTime() - this.reloadCooldownStartTime <= this.reloadCooldownDuration;
	}

	private boolean requiresPhasedReload(GunClientStateContext context) {
		return this.gunItem.requiresPhasedReload();
	}

	private boolean canReload(GunClientStateContext context) {
		int ammoToReload =
			this.gunItem.canReloadGun(context.itemStack, (Player)context.player, context.getFireModeInstance());
		return ammoToReload > 0;
	}

	private boolean canCompleteReload(GunClientStateContext context) {
		return this.reloadAmmoCount.getAmmoCount(context.getFireModeInstance()) > 0 ||
			this.isClientSyncTimeoutExpired(MiscUtil.getLevel(context.player));
	}

	private void actionPrepareReload(GunClientStateContext context, FireState fromState, FireState toState) {
		this.reloadIterationIndex = this.ammoCount.getAmmoCount(context.getFireModeInstance()) - 1;
		this.remainingAmmoToReload =
			this.gunItem.canReloadGun(context.itemStack, (Player)context.player, context.getFireModeInstance());
		LOGGER.debug("Preparing to reload ammo: {}", this.remainingAmmoToReload);
		this.publishMessage(
			Component.translatable("message.pointblank.reloading").append("..."), 10000L, GunClientState::isReloading);
		LOGGER.debug(
			"{} Set reload iteration index to {}", System.currentTimeMillis() % 100000L, this.reloadIterationIndex);

		for (GunStateListener listener : this.stateListeners) {
			listener.onPrepareReloading(context.player, this, context.itemStack);
		}
	}

	private void actionReload(GunClientStateContext context, FireState fromState, FireState toState) {
		++this.reloadIterationIndex;
		int ammoToReloadPerIteration =
			Math.min(this.remainingAmmoToReload, ReloadFeature.getMaxAmmoPerReloadIteration(context.itemStack));
		this.remainingAmmoToReload -= ammoToReloadPerIteration;
		this.ammoCount.incrementAmmoCount(context.getFireModeInstance(), ammoToReloadPerIteration);
		LOGGER.debug(
			"Ammo to reload per iteration: {}, remaining ammo to reload: {}, current ammo: {}, reload iteration "
				+ "index: {}",
			ammoToReloadPerIteration,
			this.remainingAmmoToReload,
			this.ammoCount,
			this.reloadIterationIndex);
		this.gunItem.requestReloadFromServer((Player)context.player, context.itemStack);

		for (GunStateListener listener : this.stateListeners) {
			listener.onStartReloading(context.player, this, context.itemStack);
		}
	}

	private void actionCompleteReload(GunClientStateContext context, FireState fromState, FireState toState) {
		this.reloadIterationIndex = 0;
		LOGGER.debug("Completing reload");
		// this.gunItem.requestReloadFromServer((Player) context.player, context.itemStack);
		for (GunStateListener listener : this.stateListeners) {
			listener.onCompleteReloading(context.player, this, context.itemStack);
		}
	}

	private void actionPrepareFire(GunClientStateContext context, FireState fromState, FireState toState) {
		this.publishMessage(
			Component.translatable("message.pointblank.preparing").append("..."),
			3500L,
			GunClientState::isPreparingFiring);

		for (GunStateListener listener : this.stateListeners) {
			listener.onPrepareFiring(context.player, this, context.itemStack);
		}
	}

	private void actionFire(GunClientStateContext context, FireState fromState, FireState toState) {
		boolean scriptFlag = true;

		if (context.itemStack.getItem() instanceof ScriptHolder scriptHolder) {
			if (scriptHolder.hasFunction("preFire"))
				scriptFlag = (boolean)scriptHolder.invokeFunction("preFire", context, this);
		}
		if (scriptFlag) {
			this.gunItem.requestFireFromServer(this, (Player)context.player, context.itemStack, context.targetEntity);
			if (this.gunItem.getMaxAmmoCapacity(context.itemStack, context.getFireModeInstance()) < Integer.MAX_VALUE) {
				this.ammoCount.incrementAmmoCount(context.getFireModeInstance(), -1);
			}

			++this.totalUninterruptedShots;
			this.lastShotFiredTime = MiscUtil.getLevel(context.player).getGameTime();

			for (GunStateListener listener : this.stateListeners) {
				listener.onStartFiring(context.player, this, context.itemStack);
			}
		}
	}

	private void actionDraw(GunClientStateContext context, FireState fromState, FireState toState) {
		for (GunStateListener listener : this.stateListeners) {
			listener.onDrawing(context.player, this, context.itemStack);
		}
	}

	private void actionInspect(GunClientStateContext context, FireState fromState, FireState toState) {
		for (GunStateListener listener : this.stateListeners) {
			listener.onInspecting(context.player, this, context.itemStack);
		}
	}

	private void actionEnableFireMode(GunClientStateContext context, FireState fromState, FireState toState) {
		((GunItem)context.itemStack.getItem()).initiateClientSideFireMode((Player)context.player, context.itemStack);

		for (GunStateListener listener : this.stateListeners) {
			listener.onEnablingFireMode(context.player, this, context.itemStack);
		}
	}

	private void actionCompleteFire(GunClientStateContext context, FireState fromState, FireState toState) {
		this.publishMessage(
			Component.translatable("message.pointblank.completing").append("..."),
			3500L,
			GunClientState::isCompletingFiring);
		LOGGER.debug("{} Completing firing in state {}", System.currentTimeMillis() % 100000L, toState);

		for (GunStateListener listener : this.stateListeners) {
			listener.onCompleteFiring(context.player, this, context.itemStack);
		}
	}

	private void actionPrepareIdle(GunClientStateContext context) {
		this.prepareIdleCooldownStartTime = System.nanoTime();
		this.totalUninterruptedShots = 0;

		for (GunStateListener listener : this.stateListeners) {
			listener.onPrepareIdle(context.player, this, context.itemStack);
		}
	}

	private void actionFiddle(GunClientStateContext context) {
		this.totalUninterruptedShots = 0;

		for (GunStateListener listener : this.stateListeners) {
			listener.onIdle(context.player, this, context.itemStack);
		}
	}

	private void actionStartAutoFiring(GunClientStateContext context) {
		for (GunStateListener listener : this.stateListeners) {
			listener.onStartAutoFiring(context.player, this, context.itemStack);
		}
	}

	private void actionStopAutoFiring(GunClientStateContext context) {
		for (GunStateListener listener : this.stateListeners) {
			listener.onStopAutoFiring(context.player, this, context.itemStack);
		}
	}

	private void actionApplyReloadAmmo(GunClientStateContext context, FireState fromState, FireState toState) {
		FireModeInstance fireModeInstance = context.getFireModeInstance();
		if (this.reloadAmmoCount.getAmmoCount(fireModeInstance) > 0) {
			LOGGER.debug("Applying reload ammo count: {}", this.reloadAmmoCount);
			this.ammoCount.setAmmoCount(
				context.getFireModeInstance(), this.reloadAmmoCount.getAmmoCount(fireModeInstance));
			this.reloadAmmoCount.setAmmoCount(fireModeInstance, 0);
		}
	}

	public boolean tryReload(LivingEntity player, ItemStack itemStack) {
		GunClientStateContext context = new GunClientStateContext(player, itemStack);
		return this.stateMachine.setStateToAnyOf(
				   context,
				   List.of(GunClientState.FireState.PREPARE_RELOAD, GunClientState.FireState.PREPARE_RELOAD_ITER)) !=
			null;
	}

	public boolean tryFire(LivingEntity player, ItemStack itemStack, Entity targetEntity) {
		GunClientStateContext context = new GunClientStateContext(player, itemStack, targetEntity);
		return this.stateMachine.setStateToAnyOf(
				   context,
				   List.of(
					   GunClientState.FireState.PREPARE_FIRE_SINGLE,
					   GunClientState.FireState.PREPARE_FIRE_BURST,
					   GunClientState.FireState.PREPARE_FIRE_AUTO)) != null;
	}

	public boolean tryDraw(LivingEntity player, ItemStack itemStack) {
		GunClientStateContext context = new GunClientStateContext(player, itemStack);
		return this.stateMachine.setState(context, GunClientState.FireState.DRAW) != null;
	}

	public boolean tryDeactivate(LivingEntity player, ItemStack itemStack) {
		GunClientStateContext context = new GunClientStateContext(player, itemStack);
		return this.stateMachine.setState(context, GunClientState.FireState.PREPARE_IDLE) != null;
	}

	public boolean tryInspect(LivingEntity player, ItemStack itemStack) {
		GunClientStateContext context = new GunClientStateContext(player, itemStack);
		return this.stateMachine.setState(context, GunClientState.FireState.INSPECT) != null;
	}

	public boolean tryChangeFireMode(LivingEntity player, ItemStack itemStack) {
		GunClientStateContext context = new GunClientStateContext(player, itemStack);
		return this.stateMachine.setState(context, GunClientState.FireState.CHANGE_FIRE_MODE) != null;
	}

	public UUID getId() {
		return this.id;
	}

	public GunItem getGunItem() {
		return this.gunItem;
	}

	public void setTrigger(boolean isTriggerOn) {
		this.isTriggerOn = isTriggerOn;
	}

	public boolean isIdle() {
		FireState fireState = this.stateMachine.getCurrentState();
		return this.simplifiedFireState == GunClientState.FireState.IDLE ||
			fireState == GunClientState.FireState.PREPARE_IDLE || fireState == GunClientState.FireState.IDLE ||
			fireState == GunClientState.FireState.IDLE_COOLDOWN;
	}

	public boolean isPreparingReload() {
		FireState fireState = this.stateMachine.getCurrentState();
		return fireState == GunClientState.FireState.PREPARE_RELOAD ||
			fireState == GunClientState.FireState.PREPARE_RELOAD_ITER ||
			fireState == GunClientState.FireState.PREPARE_RELOAD_COOLDOWN ||
			fireState == GunClientState.FireState.PREPARE_RELOAD_COOLDOWN_ITER;
	}

	public boolean isReloading() {
		FireState fireState = this.stateMachine.getCurrentState();
		return this.simplifiedFireState == GunClientState.FireState.RELOAD ||
			fireState == GunClientState.FireState.PREPARE_RELOAD ||
			fireState == GunClientState.FireState.PREPARE_RELOAD_COOLDOWN ||
			fireState == GunClientState.FireState.PREPARE_RELOAD_ITER ||
			fireState == GunClientState.FireState.PREPARE_RELOAD_COOLDOWN_ITER ||
			fireState == GunClientState.FireState.RELOAD || fireState == GunClientState.FireState.RELOAD_ITER ||
			fireState == GunClientState.FireState.RELOAD_COOLDOWN ||
			fireState == GunClientState.FireState.RELOAD_COOLDOWN_ITER ||
			fireState == GunClientState.FireState.COMPLETE_RELOAD ||
			fireState == GunClientState.FireState.COMPLETE_RELOAD_COOLDOWN;
	}

	public boolean isFiring() {
		FireState fireState = this.stateMachine.getCurrentState();
		return isFiring(fireState) || this.simplifiedFireState == GunClientState.FireState.FIRE_SINGLE;
	}

	private static boolean isFiring(FireState fireState) {
		return fireState == GunClientState.FireState.PREPARE_FIRE_SINGLE ||
			fireState == GunClientState.FireState.PREPARE_FIRE_COOLDOWN_SINGLE ||
			fireState == GunClientState.FireState.PREPARE_FIRE_AUTO ||
			fireState == GunClientState.FireState.PREPARE_FIRE_COOLDOWN_AUTO ||
			fireState == GunClientState.FireState.PREPARE_FIRE_BURST ||
			fireState == GunClientState.FireState.PREPARE_FIRE_COOLDOWN_BURST ||
			fireState == GunClientState.FireState.FIRE_SINGLE || fireState == GunClientState.FireState.FIRE_BURST ||
			fireState == GunClientState.FireState.FIRE_AUTO ||
			fireState == GunClientState.FireState.FIRE_COOLDOWN_SINGLE ||
			fireState == GunClientState.FireState.FIRE_COOLDOWN_BURST ||
			fireState == GunClientState.FireState.FIRE_COOLDOWN_AUTO ||
			fireState == GunClientState.FireState.COMPLETE_FIRE ||
			fireState == GunClientState.FireState.COMPLETE_FIRE_COOLDOWN;
	}

	public boolean isPreparingFiring() {
		FireState fireState = this.stateMachine.getCurrentState();
		return fireState == GunClientState.FireState.PREPARE_FIRE_SINGLE ||
			fireState == GunClientState.FireState.PREPARE_FIRE_COOLDOWN_SINGLE ||
			fireState == GunClientState.FireState.PREPARE_FIRE_AUTO ||
			fireState == GunClientState.FireState.PREPARE_FIRE_COOLDOWN_AUTO ||
			fireState == GunClientState.FireState.PREPARE_FIRE_BURST ||
			fireState == GunClientState.FireState.PREPARE_FIRE_COOLDOWN_BURST;
	}

	public boolean isCompletingFiring() {
		FireState fireState = this.stateMachine.getCurrentState();
		return fireState == GunClientState.FireState.COMPLETE_FIRE ||
			fireState == GunClientState.FireState.COMPLETE_FIRE_COOLDOWN;
	}

	public boolean isDrawing() {
		FireState fireState = this.stateMachine.getCurrentState();
		return this.simplifiedFireState == GunClientState.FireState.DRAW ||
			fireState == GunClientState.FireState.DRAW || fireState == GunClientState.FireState.DRAW_COOLDOWN;
	}

	public boolean isInspecting() {
		FireState fireState = this.stateMachine.getCurrentState();
		return this.simplifiedFireState == GunClientState.FireState.INSPECT ||
			fireState == GunClientState.FireState.INSPECT || fireState == GunClientState.FireState.INSPECT_COOLDOWN;
	}

	public boolean isChangingFireMode() {
		FireState fireState = this.stateMachine.getCurrentState();
		return fireState == GunClientState.FireState.CHANGE_FIRE_MODE ||
			fireState == GunClientState.FireState.CHANGE_FIRE_MODE_COOLDOWN;
	}

	public void inventoryTick(LivingEntity player, ItemStack itemStack, boolean isSelected) {
		this.updateState(player, itemStack, isSelected);

		for (GunStateListener listener : this.stateListeners) {
			listener.onGameTick(player, this);
			if (gunItem.hasFunction("onClientTick"))
				gunItem.invokeFunction("onClientTick", (Player)player, itemStack, isSelected, this);
		}
	}

	public void stateTick(LivingEntity player, ItemStack itemStack, boolean isSelected) {
		this.updateState(player, itemStack, isSelected);

		for (GunStateListener listener : this.stateListeners) {
			listener.onStateTick(player, this);
		}

		Iterator<MuzzleFlashEffect> it = this.muzzleFlashEffects.iterator();

		while (it.hasNext()) {
			MuzzleFlashEffect effect = it.next();
			if (effect.isExpired()) {
				LOGGER.debug("Effect {} expired, removing", effect);
				it.remove();
			}
		}
	}

	private boolean isClientSyncTimeoutExpired(Level level) {
		return level.getGameTime() - Math.max(this.lastSyncTime, this.lastShotFiredTime) > this.clientSyncTimeoutTicks;
	}

	public void updateState(LivingEntity player, ItemStack itemStack, boolean isSelected) {
		if (player == ClientUtil.getClientPlayer()) {
			if (!isSelected && this.isAiming) {
				this.setAiming(false);
				LOGGER.debug("Turning the aiming off");
			}

			GunClientStateContext context = new GunClientStateContext(player, itemStack);
			this.stateMachine.update(context);
			Level level = MiscUtil.getLevel(player);
			if (!this.isFiring() && !this.isReloading() && this.isClientSyncTimeoutExpired(level)) {
				this.syncAmmo(level, itemStack);
			}

			for (GunStateListener listener : this.stateListeners) {
				listener.onUpdateState(player, this);
			}

			FireState updatedSimplifiedFireState =
				GunClientState.FireState.getSimplifiedFireState(this.stateMachine.getCurrentState());
			if (updatedSimplifiedFireState != this.simplifiedFireState) {
				this.simplifiedFireState = updatedSimplifiedFireState;
				Network.networkChannel.sendToServer(
					new MainHeldSimplifiedStateSyncRequest(this.id, updatedSimplifiedFireState));
			}
		}
	}

	private void syncAmmo(Level level, ItemStack itemStack) {
		for (FireModeInstance fireModeInstance : GunItem.getFireModes(itemStack)) {
			int stackAmmo = GunItem.getAmmo(itemStack, fireModeInstance);
			if (stackAmmo != this.ammoCount.getAmmoCount(fireModeInstance)) {
				LOGGER.debug("Current ammo {} is out of sync with server count {} ", this.ammoCount, stackAmmo);
			}

			this.ammoCount.setAmmoCount(fireModeInstance, stackAmmo);
		}

		this.lastSyncTime = level.getGameTime();
	}

	public int getTotalUninterruptedShots() {
		return this.totalUninterruptedShots;
	}

	public void jump(LivingEntity player, ItemStack itemStack) {
		for (GunStateListener listener : this.stateListeners) {
			listener.onJumping(player, this, itemStack);
		}
	}

	public void confirmHitScanTarget(LivingEntity player, ItemStack itemStack, HitResult hitResult, float damage) {
		for (GunStateListener listener : this.stateListeners) {
			listener.onHitScanTargetConfirmed(player, this, itemStack, hitResult, damage);
		}
	}

	public void acquireHitScan(LivingEntity player, ItemStack itemStack, HitResult hitResult) {
		for (GunStateListener listener : this.stateListeners) {
			listener.onHitScanTargetAcquired(player, this, itemStack, hitResult);
		}
	}

	public void renderTick(LivingEntity player, ItemStack itemStack, float partialTicks) {
		for (GunStateListener listener : this.stateListeners) {
			listener.onRenderTick(player, this, itemStack, null, partialTicks);
		}
	}

	public GunStateListener getAnimationController(String controllerId) {
		return this.animationControllers.get(controllerId);
	}

	public void setAnimationController(String controllerId, GunStateListener controller) {
		this.animationControllers.put(controllerId, controller);
		this.addListener(controller);
	}

	public void addListener(GunStateListener listener) {
		this.stateListeners.add(listener);
	}

	public int getAmmoCount(FireModeInstance fireModeInstance) {
		return this.ammoCount.getAmmoCount(fireModeInstance);
	}

	public FireState getFireState() {
		return this.stateMachine.getCurrentState();
	}

	public boolean isAiming() {
		return this.isAiming;
	}

	public void setAiming(boolean isAiming) {
		if (isAiming != this.isAiming) {
			this.isAiming = isAiming;
			this.toggleAiming(isAiming);
		}
	}

	private void toggleAiming(boolean isAiming) {
		for (GunStateListener listener : this.stateListeners) {
			listener.onToggleAiming(isAiming, Minecraft.getInstance().player);
		}
	}

	public void callAttachmentListener(ItemStack gunStack, ItemStack attachmentStack) {
		for (GunStateListener listener : this.stateListeners) {
			listener.onAttachmentAdded(Minecraft.getInstance().player, this, gunStack, attachmentStack);
		}
	}

	public void reloadAmmo(Level level, FireModeInstance fireModeInstance, int reloadAmmoCount) {
		this.lastSyncTime = level.getGameTime();
		this.reloadAmmoCount.setAmmoCount(fireModeInstance, reloadAmmoCount);
	}

	public int getReloadIterationIndex() {
		return this.reloadIterationIndex;
	}

	public GunItem.ReloadPhase getReloadPhase() {
		return switch (this.stateMachine.getCurrentState()) {
			case PREPARE_RELOAD, PREPARE_RELOAD_COOLDOWN, PREPARE_RELOAD_ITER, PREPARE_RELOAD_COOLDOWN_ITER ->
				ReloadPhase.PREPARING;
			case RELOAD, RELOAD_COOLDOWN, RELOAD_ITER, RELOAD_COOLDOWN_ITER -> ReloadPhase.RELOADING;
			case COMPLETE_RELOAD, COMPLETE_RELOAD_COOLDOWN -> ReloadPhase.COMPLETETING;
			default -> null;
		};
	}

	public void
	publishMessage(Component message, long displayDurationMillis, Predicate<GunClientState> messagePredicate) {
		this.temporaryMessage = message;
		this.messageEndTime = System.currentTimeMillis() + displayDurationMillis;
		this.messagePredicate = messagePredicate;
	}

	public Component getCurrentMessage() {
		if (this.temporaryMessage != null && System.currentTimeMillis() < this.messageEndTime) {
			return this.messagePredicate != null && !this.messagePredicate.test(this) ? null : this.temporaryMessage;
		} else {
			return null;
		}
	}

	public String toString() {
		return String.format("GunClientState[sid=%s,id=%s]", System.identityHashCode(this), this.id);
	}

	public static GunClientState getMainHeldState() {
		Player player = ClientUtils.getClientPlayer();
		return getMainHeldState(player);
	}

	public static GunClientState getMainHeldState(Player player) {
		if (player == null) {
			return null;
		} else {
			ItemStack itemStack = player.getMainHandItem();
			int activeSlot = player.getInventory().selected;
			return itemStack != null && itemStack.getItem() instanceof GunItem
				? getState(player, itemStack, activeSlot, false)
				: null;
		}
	}

	public static GunClientState getState(Player player, ItemStack itemStack, int slotIndex, boolean isOffhand) {
		Level level = MiscUtil.getLevel(player);
		UUID stackId = GunItem.getItemStackId(itemStack);
		if (stackId == null) {
			return null;
		} else if (slotIndex == -1) {
			GunClientState state = null;

			for (Map.Entry<PlayerSlot, GunClientState> entry : localSlotStates.entrySet()) {
				if (entry.getKey().isClientSide == level.isClientSide &&
					Objects.equals(entry.getValue().getId(), stackId)) {
					PlayerSlot playerSlot = entry.getKey();
					slotIndex = playerSlot.slotId;
					if (slotIndex >= 0 && playerSlot.playerEntityId == player.getId()) {
						ItemStack stackAtSlotIndex = player.getInventory().getItem(slotIndex);
						if (stackAtSlotIndex != null && stackAtSlotIndex.getItem() instanceof GunItem) {
							state = entry.getValue();
							break;
						}
					}
				}
			}

			if (state == null) {
				GunItem var10002 = (GunItem)itemStack.getItem();
				Objects.requireNonNull(var10002);
				state = noSlotStates.computeIfAbsent(stackId, var10002::createState);
			}

			return state;
		} else {
			int adjustedSlotIndex = isOffhand ? -5 : slotIndex;
			PlayerSlot playerSlot = new PlayerSlot(player.getId(), adjustedSlotIndex, level.isClientSide);
			GunClientState slotState = localSlotStates.get(playerSlot);
			if (slotState == null || !Objects.equals(slotState.getId(), stackId)) {
				slotState = ((GunItem)itemStack.getItem()).createState(stackId);
				localSlotStates.put(playerSlot, slotState);
			}

			return slotState;
		}
	}

	public static GunClientState getState(UUID stateId) {
		return statesById.get(stateId);
	}

	public int getTotalUninterruptedFireTime() {
		return totalUninterruptedFireTime;
	}

	public void increaseTotalUninterruptedFireTime() {
		++totalUninterruptedFireTime;
	}

	public void decreaseTotalUninterruptedFireTime() {
		if (totalUninterruptedFireTime < 0)
			totalUninterruptedFireTime = 0;
		if (totalUninterruptedFireTime == 0)
			return;
		--totalUninterruptedFireTime;
	}

	public void resetTotalUninterruptedFireTime() {
		totalUninterruptedFireTime = 0;
	}

	public void addMuzzleEffect(MuzzleFlashEffect muzzleFlashEffect) {
		this.muzzleFlashEffects.add(muzzleFlashEffect);
	}

	public List<MuzzleFlashEffect> getMuzzleFlashEffects() {
		return Collections.unmodifiableList(this.muzzleFlashEffects);
	}

	public void setSimplifiedState(FireState simplifiedFireState) {
		this.simplifiedFireState = simplifiedFireState;
	}

	public enum FireState {
		PREPARE_IDLE,
		IDLE,
		IDLE_COOLDOWN,
		DRAW,
		DRAW_COOLDOWN,
		CHANGE_FIRE_MODE,
		CHANGE_FIRE_MODE_COOLDOWN,
		PREPARE_FIRE_SINGLE,
		PREPARE_FIRE_COOLDOWN_SINGLE,
		PREPARE_FIRE_AUTO,
		PREPARE_FIRE_COOLDOWN_AUTO,
		PREPARE_FIRE_BURST,
		PREPARE_FIRE_COOLDOWN_BURST,
		FIRE_SINGLE,
		FIRE_COOLDOWN_SINGLE,
		FIRE_AUTO,
		FIRE_COOLDOWN_AUTO,
		FIRE_BURST,
		FIRE_COOLDOWN_BURST,
		COMPLETE_FIRE,
		COMPLETE_FIRE_COOLDOWN,
		PREPARE_RELOAD,
		PREPARE_RELOAD_COOLDOWN,
		PREPARE_RELOAD_ITER,
		PREPARE_RELOAD_COOLDOWN_ITER,
		RELOAD,
		RELOAD_COOLDOWN,
		RELOAD_ITER,
		RELOAD_COOLDOWN_ITER,
		COMPLETE_RELOAD,
		COMPLETE_RELOAD_COOLDOWN,
		INSPECT,
		INSPECT_COOLDOWN;

		FireState() {}

		public static FireState getSimplifiedFireState(FireState fireState) {
			return switch (fireState) {
				case DRAW, DRAW_COOLDOWN -> DRAW;
				case PREPARE_FIRE_SINGLE, PREPARE_FIRE_COOLDOWN_SINGLE, PREPARE_FIRE_AUTO, PREPARE_FIRE_COOLDOWN_AUTO,
					PREPARE_FIRE_BURST, PREPARE_FIRE_COOLDOWN_BURST, FIRE_SINGLE, FIRE_COOLDOWN_SINGLE, FIRE_AUTO,
					FIRE_COOLDOWN_AUTO, FIRE_BURST, FIRE_COOLDOWN_BURST, COMPLETE_FIRE, COMPLETE_FIRE_COOLDOWN ->
					FIRE_SINGLE;
				case PREPARE_RELOAD, PREPARE_RELOAD_COOLDOWN, PREPARE_RELOAD_ITER, PREPARE_RELOAD_COOLDOWN_ITER, RELOAD,
					RELOAD_COOLDOWN, RELOAD_ITER, RELOAD_COOLDOWN_ITER, COMPLETE_RELOAD, COMPLETE_RELOAD_COOLDOWN ->
					RELOAD;
				case INSPECT, INSPECT_COOLDOWN -> INSPECT;
				default -> IDLE;
			};
		}
	}

	public static class GunClientStateContext {
		public LivingEntity player;
		public ItemStack itemStack;
		public Entity targetEntity;

		public GunClientStateContext(LivingEntity player, ItemStack itemStack) {
			this.player = player;
			this.itemStack = itemStack;
		}

		public GunClientStateContext(LivingEntity player, ItemStack itemStack, Entity targetEntity) {
			this.player = player;
			this.itemStack = itemStack;
			this.targetEntity = targetEntity;
		}

		public FireModeInstance getFireModeInstance() {
			return GunItem.getFireModeInstance(this.itemStack);
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
