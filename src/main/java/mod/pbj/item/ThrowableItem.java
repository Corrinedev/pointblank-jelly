package mod.pbj.item;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.Nameable;
import mod.pbj.client.EntityRendererBuilder;
import mod.pbj.client.ThrowableClientState;
import mod.pbj.client.effect.AbstractEffect;
import mod.pbj.client.effect.AbstractEffect.SpriteAnimationType;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.client.render.ProjectileItemEntityRenderer;
import mod.pbj.client.render.SpriteEntityRenderer;
import mod.pbj.client.render.ThrowableItemRenderer;
import mod.pbj.crafting.Craftable;
import mod.pbj.entity.EntityBuilder;
import mod.pbj.entity.EntityBuilderProvider;
import mod.pbj.entity.GenericThrowableProjectile;
import mod.pbj.entity.ProjectileLike;
import mod.pbj.network.Network;
import mod.pbj.network.ThrowProjectileRequestPacket;
import mod.pbj.registry.EffectRegistry;
import mod.pbj.registry.ExtensionRegistry;
import mod.pbj.registry.SoundRegistry;
import mod.pbj.util.ClientUtil;
import mod.pbj.util.JsonUtil;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.TimeUnit;
import mod.pbj.util.Tradeable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ThrowableItem
	extends HurtingItem implements ThrowableLike, ExplosionProvider, Drawable, Craftable, Nameable, GeoItem, Tradeable {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	public static final String ANIMATION_CONTROLLER = "default";
	private static final String ANIMATION_NAME_DRAW = "animation.model.draw";
	private static final String ANIMATION_NAME_THROW = "animation.model.throw";
	private static final RawAnimation ANIMATION_DRAW = RawAnimation.begin().thenPlay("animation.model.draw");
	private static final RawAnimation ANIMATION_THROW = RawAnimation.begin().thenPlay("animation.model.throw");
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final String name;
	private float tradePrice;
	private int tradeBundleQuantity;
	private int tradeLevel;
	private List<EffectBuilderInfo> projectileEffectBuilderSuppliers;
	private EntityBuilder<?, ?> entityBuilder;
	private long craftingDuration;
	private long drawCooldownDuration;
	private long idleCooldownDuration;
	private long inspectCooldownDuration;
	private long prepareIdleCooldownDuration;
	private long prepareThrowCooldownDuration;
	private long throwCooldownDuration;
	private long completeThrowCooldownDuration;

	public ThrowableItem(String name, Builder builder) {
		super(new Item.Properties(), builder);
		this.name = name;
		if (builder != null) {
			this.tradePrice = builder.tradePrice;
			this.tradeBundleQuantity = builder.tradeBundleQuantity;
			this.tradeLevel = builder.tradeLevel;
			this.entityBuilder = builder.getOrCreateEntityBuilder();
			this.craftingDuration = builder.craftingDuration;
			this.drawCooldownDuration = builder.drawCooldownDuration;
			this.idleCooldownDuration = builder.idleCooldownDuration;
			this.inspectCooldownDuration = builder.inspectCooldownDuration;
			this.prepareIdleCooldownDuration = builder.prepareIdleCooldownDuration;
			this.prepareThrowCooldownDuration = builder.prepareThrowCooldownDuration;
			this.throwCooldownDuration = builder.throwCooldownDuration;
			this.completeThrowCooldownDuration = builder.completeThrowCooldownDuration;
		}
	}

	public String getName() {
		return this.name;
	}

	public ProjectileLike createProjectile(LivingEntity player, double posX, double posY, double posZ) {
		ProjectileLike projectile = this.entityBuilder.build(MiscUtil.getLevel(player));
		((Entity)projectile).setPos(posX, posY, posZ);
		((Projectile)projectile).setOwner(player);
		return projectile;
	}

	public void registerControllers(AnimatableManager.ControllerRegistrar registry) {
		registry.add((new AnimationController<>(this, "default", 0, (state) -> PlayState.STOP))
						 .triggerableAnim("animation.model.draw", ANIMATION_DRAW)
						 .triggerableAnim("animation.model.throw", ANIMATION_THROW)
						 .setSoundKeyframeHandler((event) -> {
							 Player player = ClientUtil.getClientPlayer();
							 if (player != null) {
								 SoundKeyframeData soundKeyframeData = event.getKeyframeData();
								 String soundName = soundKeyframeData.getSound();
								 SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
								 if (soundEvent != null) {
									 player.playSound(soundEvent, 1.0F, 1.0F);
								 }
							 }
						 }));
	}

	public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int i, boolean bl) {
		if (!level.isClientSide()) {
			GeoItem.getOrAssignId(itemStack, (ServerLevel)level);
			CompoundTag tag = itemStack.getOrCreateTag();
			long mid = tag.getLong("mid");
			long lid = tag.getLong("lid");
			if (mid == 0L && lid == 0L) {
				UUID newId = UUID.randomUUID();
				tag.putLong("mid", newId.getMostSignificantBits());
				tag.putLong("lid", newId.getLeastSignificantBits());
			}
		} else if (entity instanceof Player player) {
			boolean isOffhand = player.getOffhandItem() == itemStack;
			ThrowableClientState clientState = ThrowableClientState.getState(player, itemStack, i, isOffhand);
			if (clientState != null) {
				clientState.updateState((LivingEntity)entity, itemStack, bl);
			}
		}
	}

	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private BlockEntityWithoutLevelRenderer renderer;

			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (this.renderer == null) {
					ResourceLocation modelResourceLocation =
						new ResourceLocation("pointblank", ThrowableItem.this.name);
					this.renderer = new ThrowableItemRenderer(modelResourceLocation);
				}

				return this.renderer;
			}

			public boolean applyForgeHandTransform(
				PoseStack poseStack,
				LocalPlayer player,
				HumanoidArm arm,
				ItemStack itemInHand,
				float partialTick,
				float equipProcess,
				float swingProcess) {
				return true;
			}
		});
	}

	public float getPrice() {
		return this.tradePrice;
	}

	public int getTradeLevel() {
		return this.tradeLevel;
	}

	public int getBundleQuantity() {
		return this.tradeBundleQuantity;
	}

	public long getCraftingDuration() {
		return this.craftingDuration;
	}

	public void draw(Player player, ItemStack itemStack) {
		long id = GeoItem.getId(itemStack);
		AnimationController<GeoAnimatable> controller =
			this.getAnimatableInstanceCache().getManagerForId(id).getAnimationControllers().get("default");
		if (controller != null) {
			controller.forceAnimationReset();
		}

		this.triggerAnim(player, id, "default", "animation.model.draw");
	}

	public boolean tryThrow(Player player, ItemStack itemStack, Entity targetEntity) {
		boolean result = false;
		int activeSlot = player.getInventory().selected;
		boolean isMainHand = player.getMainHandItem() == itemStack;
		if (isMainHand) {
			ThrowableClientState throwableClientState =
				ThrowableClientState.getState(player, itemStack, activeSlot, false);
			if (throwableClientState != null) {
				throwableClientState.setTrigger(true);
				result = throwableClientState.tryThrow(player, itemStack, targetEntity);
			}
		}

		return result;
	}

	public ThrowableClientState createState(UUID stackId) {
		return new ThrowableClientState(stackId, this);
	}

	public float getModelScale() {
		return 1.0F;
	}

	public boolean hasIdleAnimations() {
		return false;
	}

	public void requestThrowFromServer(
		ThrowableClientState throwableClientState, Player player, ItemStack itemStack, Entity targetEntity) {
		int activeSlot = player.getInventory().selected;
		Network.networkChannel.sendToServer(new ThrowProjectileRequestPacket(throwableClientState.getId(), activeSlot));
		LOGGER.debug("{} sent throw request to server", System.currentTimeMillis() % 100000L);
	}

	public long
	getDrawCooldownDuration(LivingEntity player, ThrowableClientState throwableClientState, ItemStack itemStack) {
		return this.drawCooldownDuration;
	}

	public long
	getIdleCooldownDuration(LivingEntity player, ThrowableClientState throwableClientState, ItemStack itemStack) {
		return this.idleCooldownDuration;
	}

	public long
	getInspectCooldownDuration(LivingEntity player, ThrowableClientState throwableClientState, ItemStack itemStack) {
		return this.inspectCooldownDuration;
	}

	public long getPrepareIdleCooldownDuration() {
		return this.prepareIdleCooldownDuration;
	}

	public long getPrepareThrowCooldownDuration(
		LivingEntity player, ThrowableClientState throwableClientState, ItemStack itemStack) {
		return this.prepareThrowCooldownDuration;
	}

	public long
	getThrowCooldownDuration(LivingEntity player, ThrowableClientState throwableClientState, ItemStack itemStack) {
		return this.throwCooldownDuration;
	}

	public long getCompleteThrowCooldownDuration(
		LivingEntity player, ThrowableClientState throwableClientState, ItemStack itemStack) {
		return this.completeThrowCooldownDuration;
	}

	public void setTriggerOff(Player player, ItemStack itemStack) {
		int activeSlot = player.getInventory().selected;
		boolean isOffhand = player.getOffhandItem() == itemStack;
		if (!isOffhand) {
			ThrowableClientState throwableClientState =
				ThrowableClientState.getState(player, itemStack, activeSlot, false);
			if (throwableClientState != null) {
				throwableClientState.setTrigger(false);
			}
		}
	}

	public void
	prepareThrow(ThrowableClientState throwableClientState, Player player, ItemStack itemStack, Entity targetEntity) {
		long id = GeoItem.getId(itemStack);
		this.triggerAnim(player, id, "default", "animation.model.throw");
	}

	public void handleClientThrowRequest(ServerPlayer player, UUID stateId, int slotIndex) {
		ItemStack itemStack = player.getInventory().getItem(slotIndex);
		if (itemStack.getItem() instanceof ThrowableLike && stateId.equals(GunItem.getItemStackId(itemStack))) {
			Vec3 playerEyePosition = player.getEyePosition();
			Vec3 viewVector = player.getViewVector(0.0F);
			Vec3 upVector = player.getUpVector(0.0F);
			Vec3 rightVector = viewVector.cross(upVector).normalize();
			Vec3 throwPosition =
				playerEyePosition.add(viewVector.scale(0.5F)).add(upVector.scale(0.2)).add(rightVector.scale(0.3));
			ProjectileLike projectile =
				this.createProjectile(player, throwPosition.x, throwPosition.y, throwPosition.z);
			if (!player.isCreative()) {
				itemStack.shrink(1);
			}

			projectile.launchAtLookTarget(player, 0.0F, 0L);
			MiscUtil.getLevel(player).addFreshEntity((Entity)projectile);
		} else {
			LOGGER.error("Throwable item state id {} does not match item stack in slot", stateId);
		}
	}

	public static class Builder extends HurtingItem.Builder<Builder> implements Nameable {
		private static final double DEFAULT_GRAVITY = 0.05;
		private static final float DEFAULT_INITIAL_VELOCITY = 50.0F;
		private static final float DEFAULT_WIDTH = 0.25F;
		private static final float DEFAULT_HEIGHT = 0.25F;
		private static final float DEFAULT_PRICE = Float.NaN;
		private static final int DEFAULT_TRADE_LEVEL = 0;
		private static final int DEFAULT_TRADE_BUNDLE_QUANTITY = 1;
		private static final long DEFAULT_CRAFTING_DURATION = 500L;
		private static final long DEFAULT_DRAW_COOLDOWN_DURATION = 800L;
		private static final long DEFAULT_THROW_COOLDOWN_DURATION = 1000L;
		private static final long DEFAULT_PREPARE_THROW_COOLDOWN_DURATION = 1000L;
		private static final long DEFAULT_MAX_LIFETIME = 5000L;
		private String name;
		private float tradePrice = Float.NaN;
		private int tradeBundleQuantity = 1;
		private int tradeLevel = 0;
		private Supplier<EntityBuilder<?, ?>> entityBuilderSupplier;
		private final List<EffectBuilderInfo> projectileEffectBuilderSuppliers = new ArrayList<>();
		private Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> rendererBuilder;
		private double gravity = 0.05;
		private double initialVelocity = 50.0F;
		private float boundingBoxWidth = 0.25F;
		private float boundingBoxHeight = 0.25F;
		private long maxLifetimeMillis;
		private boolean isRicochet;
		private EntityBuilder<?, ?> entityBuilder;
		private long craftingDuration = 500L;
		private long drawCooldownDuration = 800L;
		private long idleCooldownDuration = 0L;
		private long inspectCooldownDuration = 0L;
		private long prepareIdleCooldownDuration = 0L;
		private long prepareThrowCooldownDuration = 1000L;
		private long throwCooldownDuration = 1000L;
		private long completeThrowCooldownDuration = 0L;
		private ThrowableItem builtItem;

		public Builder(ExtensionRegistry.Extension extension) {
			this.extension = extension;
		}
		public Builder() {
			this.extension = new ExtensionRegistry.Extension("pointblank", Path.of("pointblank"), "pointblank");
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withTradePrice(double price, int tradeBundleQuantity, int tradeLevel) {
			this.tradePrice = (float)price;
			this.tradeLevel = tradeLevel;
			this.tradeBundleQuantity = tradeBundleQuantity;
			return this;
		}

		public Builder withTradePrice(double price, int tradeLevel) {
			return this.withTradePrice(price, 1, tradeLevel);
		}

		public Builder withProjectileInitialVelocity(double initialVelocity) {
			this.initialVelocity = initialVelocity;
			return this;
		}

		public Builder withProjectileGravity(double gravity) {
			this.gravity = gravity;
			return this;
		}

		public Builder withProjectileMaxLifetime(int duration, TimeUnit timeUnit) {
			this.maxLifetimeMillis = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withProjectileRicochet(boolean isRicochet) {
			this.isRicochet = isRicochet;
			return this;
		}

		public Builder
		withProjectileRenderer(Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> rendererBuilder) {
			this.rendererBuilder = rendererBuilder;
			return this;
		}

		public Builder withProjectileEffect(Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier) {
			this.projectileEffectBuilderSuppliers.add(new EffectBuilderInfo(effectSupplier, (p) -> true));
			return this;
		}

		public Builder withProjectileEffect(
			Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier,
			Predicate<ProjectileLike> predicate) {
			this.projectileEffectBuilderSuppliers.add(new EffectBuilderInfo(effectSupplier, predicate));
			return this;
		}

		public Builder withProjectileBoundingBoxSize(float width, float height) {
			this.boundingBoxWidth = width;
			this.boundingBoxHeight = height;
			return this;
		}

		public Builder withEntityBuilderProvider(Supplier<EntityBuilder<?, ?>> entityBuilderSupplier) {
			this.entityBuilderSupplier = entityBuilderSupplier;
			return this;
		}

		public Builder withCraftingDuration(long duration, TimeUnit timeUnit) {
			this.craftingDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withDrawCooldownDuration(long duration, TimeUnit timeUnit) {
			this.drawCooldownDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withIdleCooldownDuration(long duration, TimeUnit timeUnit) {
			this.idleCooldownDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withInspectCooldownDuration(long duration, TimeUnit timeUnit) {
			this.inspectCooldownDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withPrepareIdleCooldownDuration(long duration, TimeUnit timeUnit) {
			this.prepareIdleCooldownDuration = (int)timeUnit.toMillis(duration);
			return this;
		}

		public Builder withPrepareThrowCooldownDuration(long duration, TimeUnit timeUnit) {
			this.prepareThrowCooldownDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withThrowCooldownDuration(long duration, TimeUnit timeUnit) {
			this.throwCooldownDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withCompleteThrowCooldownDuration(long duration, TimeUnit timeUnit) {
			this.completeThrowCooldownDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			super.withJsonObject(obj);
			this.withName(JsonUtil.getJsonString(obj, "name"));
			this.withTradePrice(
				JsonUtil.getJsonFloat(obj, "tradePrice", Float.NaN),
				JsonUtil.getJsonInt(obj, "traceBundleQuantity", 1),
				JsonUtil.getJsonInt(obj, "tradeLevel", 0));
			this.withCraftingDuration(JsonUtil.getJsonInt(obj, "craftingDuration", 500), TimeUnit.MILLISECOND);
			this.withIdleCooldownDuration(JsonUtil.getJsonInt(obj, "idleCooldownDuration", 0), TimeUnit.MILLISECOND);
			this.withInspectCooldownDuration(
				JsonUtil.getJsonInt(obj, "inspectCooldownDuration", 0), TimeUnit.MILLISECOND);
			this.withPrepareIdleCooldownDuration(
				JsonUtil.getJsonInt(obj, "prepareIdleCooldownDuration", 0), TimeUnit.MILLISECOND);
			this.withPrepareThrowCooldownDuration(
				JsonUtil.getJsonInt(obj, "throwCooldownDuration", 1000), TimeUnit.MILLISECOND);
			this.withCompleteThrowCooldownDuration(
				JsonUtil.getJsonInt(obj, "completeThrowCooldownDuration", 1000), TimeUnit.MILLISECOND);
			this.withThrowCooldownDuration(
				JsonUtil.getJsonInt(obj, "throwCooldownDuration", 1000), TimeUnit.MILLISECOND);
			JsonObject projectileObj = obj.getAsJsonObject("projectile");
			if (projectileObj != null) {
				this.withProjectileMaxLifetime(JsonUtil.getJsonInt(obj, "maxLifetime", 5000), TimeUnit.MILLISECOND);
				this.withProjectileRicochet(JsonUtil.getJsonBoolean(obj, "ricochet", true));
				float size = JsonUtil.getJsonFloat(projectileObj, "boundingBoxSize", Float.NEGATIVE_INFINITY);
				if (size > 0.0F) {
					this.withProjectileBoundingBoxSize(size, size);
				} else {
					float width = JsonUtil.getJsonFloat(projectileObj, "width", 0.25F);
					float height = JsonUtil.getJsonFloat(projectileObj, "height", 0.25F);
					this.withProjectileBoundingBoxSize(width, height);
					size = Math.max(width, height);
				}

				this.withProjectileGravity(JsonUtil.getJsonDouble(projectileObj, "gravity", 0.05));
				this.withProjectileInitialVelocity(JsonUtil.getJsonDouble(projectileObj, "initialVelocity", 0.05));
				JsonObject rendererObj = projectileObj.getAsJsonObject("renderer");
				Dist side = FMLLoader.getDist();
				if (rendererObj != null && side.isClient()) {
					String rendererType = JsonUtil.getJsonString(rendererObj, "type");
					if (rendererType.equalsIgnoreCase("sprite")) {
						SpriteEntityRenderer.Builder rendererBuilder = new SpriteEntityRenderer.Builder<>();
						rendererBuilder.withTexture(JsonUtil.getJsonString(rendererObj, "texture"));
						rendererBuilder.withSize(JsonUtil.getJsonFloat(rendererObj, "size", size));
						JsonObject spritesObj = rendererObj.getAsJsonObject("sprites");
						if (spritesObj == null) {
							throw new IllegalArgumentException("Element 'sprites' not defined in json: " + rendererObj);
						}

						int rows = JsonUtil.getJsonInt(spritesObj, "rows", 1);
						int columns = JsonUtil.getJsonInt(spritesObj, "columns", 1);
						int fps = JsonUtil.getJsonInt(spritesObj, "fps", 60);
						AbstractEffect.SpriteAnimationType spriteAnimationType =
							(AbstractEffect.SpriteAnimationType)JsonUtil.getEnum(
								spritesObj,
								"type",
								AbstractEffect.SpriteAnimationType.class,
								SpriteAnimationType.LOOP,
								true);
						rendererBuilder.withSprites(rows, columns, fps, spriteAnimationType);
						rendererBuilder.withDepthTest(JsonUtil.getJsonBoolean(rendererObj, "depthTest", true));
						rendererBuilder.withGlow(JsonUtil.getJsonBoolean(rendererObj, "glow", false));
						rendererBuilder.withRotations(JsonUtil.getJsonFloat(rendererObj, "rotations", 0.0F));
						this.withProjectileRenderer(() -> rendererBuilder);
					} else if (rendererType.equalsIgnoreCase("model")) {
						this.withProjectileRenderer(() -> new ProjectileItemEntityRenderer.Builder());
					}
				}

				for (String effectName : JsonUtil.getStrings(projectileObj, "effects")) {
					Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier =
						() -> EffectRegistry.getEffectBuilderSupplier(effectName).get();
					this.withProjectileEffect(supplier);
				}
			}

			return this;
		}

		public String getName() {
			return this.name;
		}

		public ThrowableItem build() {
			if (this.builtItem == null) {
				this.builtItem = new ThrowableItem(this.name, this);
			}

			return this.builtItem;
		}

		public EntityBuilderProvider getEntityBuilderProvider() {
			return () -> this.getOrCreateEntityBuilder();
		}

		private EntityBuilder<?, ?> getOrCreateEntityBuilder() {
			if (this.entityBuilder == null) {
				if (this.entityBuilderSupplier != null) {
					this.entityBuilder = this.entityBuilderSupplier.get();
				} else {
					this.entityBuilder = GenericThrowableProjectile.builder();
				}

				this.entityBuilder.withItem(this::build);
				if (this.rendererBuilder != null) {
					this.entityBuilder.withRenderer(this.rendererBuilder);
				}

				this.entityBuilder.withName(this.name);
				this.entityBuilder.withInitialVelocity(this.initialVelocity);
				this.entityBuilder.withGravity(this.gravity);
				this.entityBuilder.withMaxLifetime(this.maxLifetimeMillis);
				this.entityBuilder.withRicochet(this.isRicochet);

				for (EffectBuilderInfo ebi : this.projectileEffectBuilderSuppliers) {
					this.entityBuilder.withEffect(ebi);
				}
			}

			return this.entityBuilder;
		}
	}
}
