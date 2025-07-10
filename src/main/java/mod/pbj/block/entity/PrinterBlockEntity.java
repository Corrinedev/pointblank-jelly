package mod.pbj.block.entity;

import java.util.List;
import mod.pbj.Enableable;
import mod.pbj.crafting.Craftable;
import mod.pbj.crafting.PointBlankRecipe;
import mod.pbj.inventory.CraftingContainerMenu;
import mod.pbj.registry.BlockEntityRegistry;
import mod.pbj.registry.SoundRegistry;
import mod.pbj.util.InventoryUtils;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.StateMachine;
import mod.pbj.util.StateMachine.TransitionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.ClientUtils;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PrinterBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {
	private static final int OPENING_DURATION = 655;
	private static final int CLOSING_DURATION = 577;
	protected final ContainerData dataAccess = new ContainerData() {
		public int get(int dataSlotIndex) {
			switch (dataSlotIndex) {
				case 0 -> {
					return PrinterBlockEntity.this.getState().ordinal();
				}
				case 1 -> {
					return PrinterBlockEntity.this.craftingPlayer != null
						? PrinterBlockEntity.this.craftingPlayer.getId()
						: -1;
				}
				default -> {
					return 0;
				}
			}
		}

		public void set(int dataSlotIndex, int value) {}

		public int getCount() {
			return 2;
		}
	};
	private static final Component CONTAINER_TITLE = Component.translatable("screen.pointblank.crafting");
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private List<Player> nearbyEntities;
	private long lastNearbyEntityUpdateTimestamp;
	private Player craftingPlayer;
	private PointBlankRecipe craftingRecipe;
	private long craftingStartTime;
	private long craftingDuration;
	private final long openingDuration = 655L;
	private long closingStartTime;
	private final long closingDuration = 577L;
	private long openingStartTime;
	private CraftingEventHandler craftingEventHandler;
	private StateMachine<State, Context> stateMachine;
	private State clientState;
	private static final RawAnimation ANIMATION_OPEN =
		RawAnimation.begin().thenPlay("animation.model.open").thenLoop("animation.model.idle");
	private static final RawAnimation ANIMATION_CLOSE = RawAnimation.begin().thenPlay("animation.model.close");
	private static final RawAnimation ANIMATION_IDLE = RawAnimation.begin().thenPlay("animation.model.idle");
	private static final RawAnimation ANIMATION_CRAFTING =
		RawAnimation.begin().thenPlay("animation.model.crafting").thenLoop("animation.model.idle");

	public PrinterBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityRegistry.PRINTER_BLOCK_ENTITY.get(), pos, state);
	}

	public void setLevel(Level level) {
		super.setLevel(level);
		if (level.isClientSide) {
			this.clientState = PrinterBlockEntity.State.CLOSED;
		} else {
			this.stateMachine = this.createStateMachine();
		}
	}

	public State getState() {
		if (this.level == null) {
			return null;
		} else {
			return this.level.isClientSide ? this.clientState : this.stateMachine.getCurrentState();
		}
	}

	private StateMachine<State, Context> createStateMachine() {
		StateMachine.Builder<State, Context> builder = new StateMachine.Builder<>();
		builder.withTransition(
			PrinterBlockEntity.State.CLOSED,
			PrinterBlockEntity.State.OPENING,
			this::predicateIsPlayerNearby,
			TransitionMode.AUTO,
			null,
			this::actionOpen);
		builder.withTransition(
			PrinterBlockEntity.State.OPENING,
			PrinterBlockEntity.State.IDLE,
			this::openingTimeoutExpired,
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			PrinterBlockEntity.State.IDLE,
			PrinterBlockEntity.State.CRAFTING,
			this::predicateIsPlayerNearby,
			TransitionMode.EVENT,
			null,
			this::actionStartCrafting);
		builder.withTransition(
			PrinterBlockEntity.State.CRAFTING,
			PrinterBlockEntity.State.IDLE,
			this::predicateIsPlayerNearby,
			TransitionMode.EVENT,
			null,
			null);
		builder.withTransition(
			PrinterBlockEntity.State.CRAFTING,
			PrinterBlockEntity.State.CRAFTING_COMPLETED,
			(ctx)
				-> this.predicateIsPlayerNearby(ctx) && this.craftingTimeoutExpired(ctx),
			TransitionMode.AUTO,
			null,
			this::actionCompleteCrafting);
		builder.withTransition(
			PrinterBlockEntity.State.CRAFTING,
			PrinterBlockEntity.State.CLOSING,
			(ctx)
				-> !this.predicateIsPlayerNearby(ctx),
			TransitionMode.AUTO,
			null,
			this::actionCancelCrafting);
		builder.withTransition(
			PrinterBlockEntity.State.CRAFTING_COMPLETED,
			PrinterBlockEntity.State.IDLE,
			(ctx)
				-> true,
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			PrinterBlockEntity.State.IDLE,
			PrinterBlockEntity.State.CLOSING,
			(ctx)
				-> !this.predicateIsPlayerNearby(ctx),
			TransitionMode.AUTO,
			null,
			null);
		builder.withTransition(
			PrinterBlockEntity.State.CLOSING,
			PrinterBlockEntity.State.CLOSED,
			this::closingTimeoutExpired,
			TransitionMode.AUTO,
			null,
			null);
		builder.withOnSetStateAction(PrinterBlockEntity.State.IDLE, this::actionIdle);
		builder.withOnChangeStateAction(this::actionOnChangeState);
		return builder.build(PrinterBlockEntity.State.CLOSED);
	}

	private boolean closingTimeoutExpired(Context ctx) {
		return System.currentTimeMillis() - this.closingStartTime >= this.closingDuration;
	}

	private boolean craftingTimeoutExpired(Context ctx) {
		return System.currentTimeMillis() - this.craftingStartTime >= this.craftingDuration;
	}

	private boolean openingTimeoutExpired(Context ctx) {
		return System.currentTimeMillis() - this.openingStartTime >= this.openingDuration;
	}

	private boolean predicateIsPlayerNearby(Context context) {
		return isPlayerNearby(this.getBlockPos(), this.nearbyEntities);
	}

	private void actionStartCrafting(Context context, State fromState, State toState) {
		this.craftingPlayer = context.craftingPlayer;
		this.craftingRecipe = context.craftingRecipe;
		this.craftingStartTime = System.currentTimeMillis();
		this.craftingDuration = ((Craftable)context.craftingRecipe.getResultItem(null).getItem()).getCraftingDuration();
		this.craftingEventHandler = context.craftingEventHandler;
	}

	private void actionIdle(Context context, State fromState, State toState) {
		this.resetCrafting();
	}

	private void actionOpen(Context context, State fromState, State toState) {
		this.openingStartTime = System.currentTimeMillis();
	}

	private void actionCompleteCrafting(Context context, State fromState, State toState) {
		this.createCraftingItem();
	}

	private void actionCancelCrafting(Context context, State fromState, State toState) {
		this.cancelCrafting(context.craftingPlayer, context.craftingRecipe.getId());
	}

	private void actionOnChangeState(Context context, State fromState, State toState) {
		this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
	}

	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add((new AnimationController<>(this, (state) -> {
							PlayState playState = null;
							switch (this.getState()) {
								case CLOSED, CLOSING -> playState = state.setAndContinue(ANIMATION_CLOSE);
								case OPENING -> playState = state.setAndContinue(ANIMATION_OPEN);
								case IDLE, CRAFTING_COMPLETED -> playState = state.setAndContinue(ANIMATION_IDLE);
								case CRAFTING -> playState = state.setAndContinue(ANIMATION_CRAFTING);
							}

							return playState;
						})).setSoundKeyframeHandler((event) -> {
			Player player = ClientUtils.getClientPlayer();
			if (player != null) {
				SoundKeyframeData soundKeyframeData = event.getKeyframeData();
				String soundName = soundKeyframeData.getSound();
				SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
				if (soundEvent != null) {
					BlockPos blockPos = this.getBlockPos();
					this.level.playLocalSound(
						blockPos.getX(),
						blockPos.getY(),
						blockPos.getZ(),
						soundEvent,
						SoundSource.BLOCKS,
						2.0F,
						(1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
						false);
				}
			}
		}));
	}

	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	public static void clientTick(Level level, BlockPos blockPos, BlockState blockState, PrinterBlockEntity entity) {}

	private void updateEntities() {
		BlockPos blockpos = this.getBlockPos();
		if (this.level.getGameTime() > this.lastNearbyEntityUpdateTimestamp + 50L || this.nearbyEntities == null) {
			this.lastNearbyEntityUpdateTimestamp = this.level.getGameTime();
			AABB aabb = (new AABB(blockpos)).inflate(15.0F);
			this.nearbyEntities = this.level.getEntitiesOfClass(Player.class, aabb);
		}
	}

	private static boolean isPlayerNearby(BlockPos blockPos, List<Player> entities) {
		if (entities != null) {
			for (LivingEntity entity : entities) {
				if (entity instanceof Player && entity.isAlive() && !entity.isRemoved() &&
					blockPos.closerToCenterThan(entity.position(), 6.0F)) {
					return true;
				}
			}
		}
		return false;
	}

	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new CraftingContainerMenu(containerId, inventory, this, this.dataAccess);
	}

	public Component getDisplayName() {
		return CONTAINER_TITLE;
	}

	public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, PrinterBlockEntity entity) {
		entity.serverTick();
	}

	private void serverTick() {
		this.updateEntities();
		Context context = new Context(this.craftingPlayer, this.craftingRecipe, this.craftingEventHandler);
		this.stateMachine.update(context);
	}

	private void createCraftingItem() {
		ItemStack craftedStack = null;
		boolean isCraftingSuccessful = false;
		boolean isAddedToInventory = false;
		Exception craftingException = null;

		try {
			if (this.craftingRecipe.canBeCrafted(this.craftingPlayer)) {
				craftedStack = this.craftingRecipe.getResultItem(null);
				if (craftedStack != null && !craftedStack.isEmpty()) {
					this.craftingRecipe.removeIngredients(this.craftingPlayer);
					int remaingCount =
						InventoryUtils.addItem(this.craftingPlayer, craftedStack.getItem(), craftedStack.getCount());
					if (remaingCount > 0) {
						BlockPos blockPos = this.getBlockPos();
						if (blockPos != null) {
							Containers.dropItemStack(
								MiscUtil.getLevel(this.craftingPlayer),
								blockPos.getX(),
								(float)blockPos.getY() + 1.25F,
								blockPos.getZ(),
								craftedStack.copy());
							isCraftingSuccessful = true;
						}
					} else {
						isAddedToInventory = true;
						isCraftingSuccessful = true;
					}
				}
			}
		} catch (Exception e) {
			craftingException = e;
			System.err.println("Caught exception during crafting " + e);
		}

		if (craftedStack == null) {
			craftedStack = ItemStack.EMPTY;
		}

		if (this.craftingEventHandler != null) {
			if (isCraftingSuccessful) {
				this.craftingEventHandler.onCraftingCompleted(this.craftingPlayer, craftedStack, isAddedToInventory);
			} else {
				this.craftingEventHandler.onCraftingFailed(this.craftingPlayer, craftedStack, craftingException);
			}
		}
	}

	private void resetCrafting() {
		this.craftingPlayer = null;
		this.craftingRecipe = null;
		this.craftingStartTime = 0L;
		this.craftingDuration = 0L;
		this.craftingEventHandler = null;
	}

	public boolean tryCrafting(Player player, ResourceLocation recipeId, CraftingEventHandler craftingEventHandler) {
		if (this.nearbyEntities != null && !this.nearbyEntities.contains(player)) {
			return false;
		} else {
			PointBlankRecipe craftingRecipe = PointBlankRecipe.getRecipe(MiscUtil.getLevel(player), recipeId);
			if (craftingRecipe == null) {
				return false;
			} else {
				ItemStack craftingItemStack = craftingRecipe.getResultItem(null);
				if (craftingItemStack != null && !craftingItemStack.isEmpty() &&
					craftingItemStack.getItem() instanceof Craftable) {
					Item var7 = craftingItemStack.getItem();
					if (var7 instanceof Enableable e) {
						if (!e.isEnabled()) {
							return false;
						}
					}

					return this.stateMachine.setState(
							   new Context(player, craftingRecipe, craftingEventHandler),
							   PrinterBlockEntity.State.CRAFTING) == PrinterBlockEntity.State.CRAFTING;
				} else {
					return false;
				}
			}
		}
	}

	public boolean cancelCrafting(Player player, ResourceLocation recipeId) {
		if (this.stateMachine.getCurrentState() != PrinterBlockEntity.State.CRAFTING) {
			return false;
		} else if (player != this.craftingPlayer) {
			return false;
		} else if (!recipeId.equals(this.craftingRecipe.getId())) {
			return false;
		} else {
			this.stateMachine.setState(
				new Context(player, this.craftingRecipe, this.craftingEventHandler), PrinterBlockEntity.State.IDLE);
			if (this.craftingEventHandler != null) {
				this.craftingEventHandler.onCraftingCancelled(
					this.craftingPlayer, this.craftingRecipe.getResultItem(null));
			}

			return true;
		}
	}

	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("clientState", this.stateMachine.getCurrentState().ordinal());
		return ClientboundBlockEntityDataPacket.create(this, (e) -> tag);
	}

	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
		CompoundTag tag = packet.getTag();
		if (tag != null) {
			int ordinal = tag.getInt("clientState");
			this.clientState = PrinterBlockEntity.State.values()[ordinal];
		}
	}

	public interface CraftingEventHandler {
		default void onCraftingCompleted(Player player, ItemStack craftingItemStack, boolean isAddedToInventory) {}

		default void onCraftingCancelled(Player player, ItemStack craftingItemStack) {}

		default void onCraftingFailed(Player player, ItemStack craftingItemStack, Exception craftingException) {}
	}

	public enum State {
		CLOSED,
		OPENING,
		CLOSING,
		IDLE,
		CRAFTING,
		CRAFTING_COMPLETED;

		State() {}
	}

	private record
	Context(Player craftingPlayer, PointBlankRecipe craftingRecipe, CraftingEventHandler craftingEventHandler) {}
}
