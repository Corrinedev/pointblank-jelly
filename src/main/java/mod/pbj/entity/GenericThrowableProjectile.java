package mod.pbj.entity;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import mod.pbj.client.EntityRendererBuilder;
import mod.pbj.client.effect.AttachedProjectileEffect;
import mod.pbj.client.effect.Effect;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.client.effect.TrailEffect;
import mod.pbj.item.EffectBuilderInfo;
import mod.pbj.item.GunItem;
import mod.pbj.item.GunItem.FirePhase;
import mod.pbj.item.HurtingItem;
import mod.pbj.registry.EntityRegistry;
import mod.pbj.util.HitScan;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.TimeUnit;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class GenericThrowableProjectile
	extends ThrowableProjectile implements ProjectileLike, IEntityAdditionalSpawnData {
	private static final float DEFAULT_GRAVITY = 0.05F;
	private static final float DEFAULT_WIDTH = 0.25F;
	private static final float DEFAULT_HEIGHT = 0.25F;
	private static final int DEFAULT_CLIENT_TRACKING_RANGE = 1024;
	private static final int DEFAULT_UPDATE_INTERVAL = 1;
	private static final int DEFAULT_LIFETIME_TICKS = 200;
	private static final double MIN_SPEED_THRESHOLD = 0.01;
	private HurtingItem throwableItem;
	private ItemStack throwableItemStack;
	private double initialVelocityBlocksPerTick;
	private boolean isRicochet;
	private float gravity;
	private int maxLifetimeTicks;
	private List<ProjectileLike.EffectInfo> trailEffects;
	private List<ProjectileLike.EffectInfo> attachedEffects;
	private List<Effect> activeTrailEffects = Collections.emptyList();
	private List<Effect> activeAttachedEffects = Collections.emptyList();

	public static Builder builder() {
		return new Builder();
	}

	public GenericThrowableProjectile(EntityType<? extends GenericThrowableProjectile> entityType, Level level) {
		super(entityType, level);
	}

	public GenericThrowableProjectile(
		EntityType<? extends GenericThrowableProjectile> entityType, LivingEntity owner, Level level) {
		super(entityType, owner, level);
	}

	private void setInitialVelocityBlocksPerTick(double initialVelocityBlocksPerTick) {
		this.initialVelocityBlocksPerTick = initialVelocityBlocksPerTick;
	}

	private void setGravity(float gravity) {
		this.gravity = gravity;
	}

	protected float getGravity() {
		return this.gravity;
	}

	public void setMaxLifetimeTicks(int maxLifetimeTicks) {
		this.maxLifetimeTicks = maxLifetimeTicks;
	}

	public ItemStack getItem() {
		return this.throwableItemStack;
	}

	public List<Effect> getActiveAttachedEffects() {
		return this.activeTrailEffects;
	}

	public float getProgress(float partialTick) {
		return 0.0F;
	}

	public long getElapsedTimeMillis() {
		return 0L;
	}

	public double getInitialVelocityBlocksPerTick() {
		return this.initialVelocityBlocksPerTick;
	}

	public void launchAtTargetEntity(LivingEntity player, HitResult hitResult, Entity targetEntity) {
		Vec3 hitLocation = hitResult.getLocation();
		Vec3 muzzleWorldPos = this.position();
		Vec3 eyePos = player.getEyePosition();
		Vec3 viewHitVector = hitLocation.subtract(eyePos);
		Vec3 spawnOffset = muzzleWorldPos.subtract(eyePos);
		Vec3 direction = viewHitVector.subtract(spawnOffset).normalize();
		this.shoot(direction.x, direction.y, direction.z, (float)this.getInitialVelocityBlocksPerTick(), 0.0F);
	}

	public void launchAtLookTarget(LivingEntity player, double inaccuracy, long seed) {
		HitResult hitScanTarget = HitScan.getNearestObjectInCrosshair(
			player, 0.0F, 150.0F, inaccuracy, seed, (block) -> false, (block) -> false, new ArrayList<>());
		Vec3 hitLocation = hitScanTarget.getLocation();
		Vec3 muzzleWorldPos = this.position();
		Vec3 eyePos = player.getEyePosition();
		Vec3 viewHitVector = hitLocation.subtract(eyePos);
		Vec3 spawnOffset = muzzleWorldPos.subtract(eyePos);
		Vec3 direction = viewHitVector.subtract(spawnOffset).normalize();
		this.shoot(direction.x, direction.y, direction.z, (float)this.getInitialVelocityBlocksPerTick(), 0.0F);
	}

	private double getSpeedSqr() {
		return this.getDeltaMovement().lengthSqr();
	}

	protected void onHitBlock(BlockHitResult blockHitResult) {
		if (this.isRicochet) {
			BlockPos resultPos = blockHitResult.getBlockPos();
			Level level = MiscUtil.getLevel(this);
			BlockState state = level.getBlockState(resultPos);
			SoundEvent event = state.getBlock().getSoundType(state).getStepSound();
			if (this.getSpeedSqr() > 0.01) {
				level.playSound(
					null,
					blockHitResult.getLocation().x,
					blockHitResult.getLocation().y,
					blockHitResult.getLocation().z,
					event,
					SoundSource.AMBIENT,
					1.0F,
					1.0F);
			}

			this.ricochet(blockHitResult.getDirection());
		} else {
			Entity owner = this.getOwner();
			if (owner instanceof LivingEntity player) {
				this.throwableItem.handleBlockHit(player, blockHitResult, this);
			}
		}
	}

	protected void onHitEntity(EntityHitResult entityHitResult) {
		Entity owner = this.getOwner();
		if (this.isRicochet) {
			if (owner instanceof LivingEntity && this.getSpeedSqr() > 0.01) {
				Entity entity = entityHitResult.getEntity();
				if (!MiscUtil.isProtected(entity)) {
					entity.hurt(entity.damageSources().thrown(this, this.getOwner()), 0.5F);
				}
			}

			this.ricochet(
				Direction
					.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z())
					.getOpposite(),
				0.3,
				1.0F,
				0.3);
		} else if (owner instanceof LivingEntity player) {
			this.throwableItem.hurtEntity(player, entityHitResult, this, this.throwableItemStack);
		}
	}

	private void ricochet(Direction direction) {
		this.ricochet(direction, 1.0F, 1.0F, 1.0F);
	}

	private void ricochet(Direction direction, double mx, double my, double mz) {
		Direction.Axis axis = direction.getAxis();
		Vec3 delta = this.getDeltaMovement();
		delta = delta.multiply(
			axis == Axis.X ? (double)-0.5F : 0.7, axis == Axis.Y ? -0.2 : 0.7, axis == Axis.Z ? (double)-0.5F : 0.7);
		if (axis == Axis.Y && delta.y < (double)this.getGravity()) {
			delta = new Vec3(delta.x, 0.0F, delta.z);
		}

		this.setDeltaMovement(delta.multiply(mx, my, mz));
	}

	public boolean isNoGravity() {
		return false;
	}

	public void writeSpawnData(FriendlyByteBuf buffer) {
		buffer.writeItem(this.throwableItemStack);
	}

	public void readSpawnData(FriendlyByteBuf buffer) {
		this.throwableItemStack = buffer.readItem();
	}

	public void tick() {
		super.tick();
		if (this.tickCount >= this.maxLifetimeTicks) {
			this.doDiscard();
		}

		if (MiscUtil.isClientSide(this)) {
			this.activeAttachedEffects = this.attachedEffects.stream()
											 .filter((ei) -> ei.predicate().test(this))
											 .map(EffectInfo::effect)
											 .toList();
			this.activeTrailEffects =
				this.trailEffects.stream().filter((ei) -> ei.predicate().test(this)).map(EffectInfo::effect).toList();
			Vec3 dm = this.getDeltaMovement();

			for (Effect trailEffect : this.activeTrailEffects) {
				((TrailEffect)trailEffect).launchNext(this, new Vec3(this.getX(), this.getY(), this.getZ()), dm);
			}
		}
	}

	public void doDiscard() {
		if (!MiscUtil.isClientSide(this)) {
			if (this.throwableItem != null) {
				this.throwableItem.discardProjectile(this);
			}

			this.discard();
		}
	}

	protected void defineSynchedData() {}

	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static class Builder implements EntityBuilder<Builder, GenericThrowableProjectile> {
		private String name;
		private float width = 0.25F;
		private float height = 0.25F;
		private double initialVelocityBlocksPerSecond;
		private int maxLifetimeTicks = 200;
		private float gravity;
		private boolean isRicochet;
		private final List<EffectBuilderInfo> effectBuilderSuppliers = new ArrayList<>();
		private Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> rendererBuilder;
		private Supplier<HurtingItem> hurtingItem;

		private Builder() {}

		public String getName() {
			return this.name;
		}

		public Builder withItem(Supplier<Item> hurtingItem) {
			this.hurtingItem = () -> (HurtingItem)hurtingItem.get();
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withMaxLifetime(long lifetimeMillis) {
			this.maxLifetimeTicks = (int)TimeUnit.MILLISECOND.toTicks(lifetimeMillis);
			return this;
		}

		public Builder withSize(float width, float height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public Builder withInitialVelocity(double initialVelocityBlocksPerSecond) {
			this.initialVelocityBlocksPerSecond = initialVelocityBlocksPerSecond;
			return this;
		}

		public Builder
		withRenderer(Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> rendererBuilder2) {
			this.rendererBuilder = rendererBuilder2;
			return this;
		}

		public Builder withRicochet(boolean isRicochet) {
			this.isRicochet = isRicochet;
			return this;
		}

		@Override
		public GenericThrowableProjectile build(Level var1) {
			return build((EntityType<GenericThrowableProjectile>)EntityRegistry.getTypeByName(this.name).get(), var1);
		}

		public Builder withGravity(boolean isGravityEnabled) {
			this.gravity = isGravityEnabled ? 0.05F : 0.0F;
			return this;
		}

		public Builder withGravity(double gravity) {
			this.gravity = Mth.clamp((float)gravity, -1.0F, 1.0F);
			return this;
		}

		public Builder withEffect(EffectBuilderInfo effectInfo) {
			this.effectBuilderSuppliers.add(effectInfo);
			return this;
		}

		public EntityBuilder.EntityTypeExt getEntityTypeExt() {
			return EntityTypeExt.PROJECTILE;
		}

		public EntityType.Builder<GenericThrowableProjectile> getEntityTypeBuilder() {
			return EntityType.Builder
				.of((EntityType.EntityFactory<GenericThrowableProjectile>)this::build, MobCategory.MISC)
				.sized(this.width, this.height)
				.clientTrackingRange(1024)
				.noSummon()
				.fireImmune()
				.updateInterval(1);
		}

		public GenericThrowableProjectile build(EntityType<GenericThrowableProjectile> entityType, Level level) {
			GenericThrowableProjectile projectile = new GenericThrowableProjectile(entityType, level);
			if (level.isClientSide) {
				this.initEffects(projectile);
			}

			if (this.hurtingItem != null) {
				projectile.throwableItem = this.hurtingItem.get();
				projectile.throwableItemStack = new ItemStack(projectile.throwableItem);
			}

			projectile.maxLifetimeTicks = this.maxLifetimeTicks;
			projectile.isRicochet = this.isRicochet;
			projectile.setInitialVelocityBlocksPerTick(this.initialVelocityBlocksPerSecond * (double)0.05F);
			projectile.setMaxLifetimeTicks(this.maxLifetimeTicks);
			projectile.setNoGravity(MiscUtil.isNearlyZero(this.gravity));
			projectile.setGravity(this.gravity);
			return projectile;
		}

		public void initEffects(GenericThrowableProjectile projectile) {
			List<ProjectileLike.EffectInfo> trailEffects = new ArrayList<>();
			List<ProjectileLike.EffectInfo> attachedEffects = new ArrayList<>();
			GunItem.FirePhase phase = FirePhase.FLYING;

			for (EffectBuilderInfo effectBuilderInfo : this.effectBuilderSuppliers) {
				EffectBuilder<?, ?> effectBuilder = effectBuilderInfo.effectSupplier().get();
				if (effectBuilder.getCompatiblePhases().contains(FirePhase.FLYING)) {
					EffectBuilder.Context context = new EffectBuilder.Context();
					TrailEffect effect = (TrailEffect)effectBuilder.build(context);
					trailEffects.add(new ProjectileLike.EffectInfo(effect, effectBuilderInfo.predicate()));
				} else {
					if (!(effectBuilder instanceof AttachedProjectileEffect.Builder)) {
						throw new IllegalStateException(
							"Effect builder " + effectBuilder + " is not compatible with phase '" + phase +
							"'. Check how you construct projectile: " + this.getName());
					}

					EffectBuilder.Context context = new EffectBuilder.Context();
					AttachedProjectileEffect effect = (AttachedProjectileEffect)effectBuilder.build(context);
					attachedEffects.add(new ProjectileLike.EffectInfo(effect, effectBuilderInfo.predicate()));
				}
			}

			projectile.trailEffects = Collections.unmodifiableList(trailEffects);
			projectile.attachedEffects = Collections.unmodifiableList(attachedEffects);
		}

		public boolean hasRenderer() {
			return this.rendererBuilder != null;
		}

		public EntityRenderer<Entity> createEntityRenderer(EntityRendererProvider.Context context) {
			return this.rendererBuilder.get().build(context);
		}

		public Builder withJsonObject(JsonObject obj) {
			throw new UnsupportedOperationException();
		}
	}
}
