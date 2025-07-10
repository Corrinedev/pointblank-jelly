package mod.pbj.entity;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import mod.pbj.client.EntityRendererBuilder;
import mod.pbj.client.effect.AttachedProjectileEffect;
import mod.pbj.client.effect.Effect;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.client.effect.TrailEffect;
import mod.pbj.item.AmmoItem;
import mod.pbj.item.EffectBuilderInfo;
import mod.pbj.item.GunItem;
import mod.pbj.item.GunItem.FirePhase;
import mod.pbj.item.HurtingItem;
import mod.pbj.registry.EntityRegistry;
import mod.pbj.util.DirectAttackTrajectory;
import mod.pbj.util.HitScan;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.TimeUnit;
import mod.pbj.util.TopDownAttackTrajectory;
import mod.pbj.util.Trajectory;
import mod.pbj.util.TrajectoryProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SlowProjectile extends AbstractHurtingProjectile implements ProjectileLike, IEntityAdditionalSpawnData {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private static final double MAX_RENDER_DISTANCE_SQR = 40000.0F;
	private static final float MAX_HIT_SCAN_DISTANCE = 10.0F;
	static final float DEFAULT_MAX_DISTANCE = 150.0F;
	private static final double DEFAULT_GRAVITY = 0.05;
	private static final EntityDataAccessor<OptionalInt> DATA_ATTACHED_TO_TARGET;
	private static final EntityDataAccessor<Boolean> DATA_SHOT_AT_ANGLE;
	private double initialVelocityBlocksPerTick;
	private int life;
	private int lifetime;
	private HitResult hitScanTarget;
	private double gravity;
	@Nullable private LivingEntity attachedToEntity;
	private final float initialAngle;
	private final long startTimeMillis;
	private List<ProjectileLike.EffectInfo> trailEffects;
	private List<ProjectileLike.EffectInfo> attachedEffects;
	private List<Effect> activeTrailEffects = Collections.emptyList();
	private List<Effect> activeAttachedEffects = Collections.emptyList();
	private Trajectory<?> trajectory;
	private Entity targetEntity;
	public HurtingItem hurtingItem;
	private ItemStack hurtingItemStack;

	public static Builder builder() {
		return new Builder();
	}

	public SlowProjectile(EntityType<? extends SlowProjectile> entityType, Level level) {
		super(entityType, level);
		this.initialAngle = this.random.nextFloat() * 360.0F;
		this.startTimeMillis = System.currentTimeMillis();
	}

	public void launchAtLookTarget(LivingEntity entity, double inaccuracy, long seed) {
		this.hitScanTarget = HitScan.getNearestObjectInCrosshair(
			entity, 0.0F, 150.0F, inaccuracy, seed, (block) -> false, (block) -> false, new ArrayList<>());
		Vec3 hitLocation = this.hitScanTarget.getLocation();
		Vec3 muzzleWorldPos = this.position();
		Vec3 eyePos = entity.getEyePosition();
		Vec3 viewHitVector = hitLocation.subtract(eyePos);
		Vec3 spawnOffset = muzzleWorldPos.subtract(eyePos);
		Vec3 direction = viewHitVector.subtract(spawnOffset).normalize();
		this.shoot(direction.x, direction.y, direction.z, this.getInitialVelocityBlocksPerTick());
	}

	public void launchAtTargetEntity(LivingEntity player, HitResult hitResult, Entity targetEntity) {
		this.hitScanTarget = hitResult;
		this.targetEntity = targetEntity;
		Vec3 hitLocation = this.hitScanTarget.getLocation();
		Vec3 muzzleWorldPos = this.position();
		Vec3 eyePos = player.getEyePosition();
		Vec3 viewHitVector = hitLocation.subtract(eyePos);
		Vec3 spawnOffset = muzzleWorldPos.subtract(eyePos);
		Vec3 direction = viewHitVector.subtract(spawnOffset).normalize();
		this.shoot(direction.x, direction.y, direction.z, this.getInitialVelocityBlocksPerTick());
	}

	public void shoot(double dx, double dy, double dz, double initialSpeed) {
		Vec3 deltaMovement = (new Vec3(dx, dy, dz)).normalize().scale(initialSpeed);
		this.setDeltaMovement(deltaMovement);
		double horizontalDistance = deltaMovement.horizontalDistance();
		this.setYRot((float)(Mth.atan2(deltaMovement.x, deltaMovement.z) * (double)(180F / (float)Math.PI)));
		this.setXRot((float)(Mth.atan2(deltaMovement.y, horizontalDistance) * (double)(180F / (float)Math.PI)));
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
		LOGGER.debug("{} initializing projectile trajectory", System.currentTimeMillis() % 100000L);
		this.initTrajectory(this.hitScanTarget.getLocation());
		LOGGER.debug("{} performed projectile shot", System.currentTimeMillis() % 100000L);
	}

	private void initTrajectory(Vec3 targetLocation) {
		Vec3 startPosition = new Vec3(this.getX(), this.getY(), this.getZ());
		if (this.hurtingItem instanceof TrajectoryProvider trajectoryProvider) {
			this.trajectory = trajectoryProvider.createTrajectory(this.level(), startPosition, targetLocation);
		}

		if (this.trajectory == null) {
			this.trajectory = new DirectAttackTrajectory(startPosition, this.getDeltaMovement(), this.gravity);
		}
	}

	public int getLife() {
		return this.life;
	}

	public int getLifetime() {
		return this.lifetime;
	}

	protected void defineSynchedData() {
		this.entityData.define(DATA_ATTACHED_TO_TARGET, OptionalInt.empty());
		this.entityData.define(DATA_SHOT_AT_ANGLE, false);
	}

	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < (double)40000.0F;
	}

	public void tick() {
		Level level = MiscUtil.getLevel(this);
		if (MiscUtil.isClientSide(this)) {
			Vec3 dm = this.getDeltaMovement();

			for (Effect trailEffect : this.getActiveTrailEffects()) {
				((TrailEffect)trailEffect).launchNext(this, new Vec3(this.getX(), this.getY(), this.getZ()), dm);
			}
		}

		if (level.isClientSide && this.attachedToEntity == null) {
			int entityId = this.entityData.get(DATA_ATTACHED_TO_TARGET).orElse(-1);
			if (entityId >= 0) {
				Entity entity = MiscUtil.getLevel(this).getEntity(entityId);
				if (entity instanceof LivingEntity) {
					this.attachedToEntity = (LivingEntity)entity;
				}
			}
		}

		if (this.life > this.getLifetime()) {
			this.discard();
		}

		if (this.trajectory != null) {
			this.trajectory.tick();
			if (!level.isClientSide && this.trajectory.isCompleted()) {
				this.explode();
			}
		}

		if (level.isClientSide) {
			this.activeAttachedEffects = this.attachedEffects.stream()
											 .filter((ei) -> ei.predicate().test(this))
											 .map(EffectInfo::effect)
											 .toList();
			this.activeTrailEffects =
				this.trailEffects.stream().filter((ei) -> ei.predicate().test(this)).map(EffectInfo::effect).toList();
		}

		if (this.targetEntity != null) {
			this.trajectory.setTargetPosition(this.targetEntity.getPosition(0.0F));
		}

		Entity entity = this.getOwner();
		if (level.isClientSide ||
			(entity == null || !entity.isRemoved()) && MiscUtil.getLevel(this).hasChunkAt(this.blockPosition())) {
			HitResult hitresult = this.getHitResultOnMoveOrViewVector();
			if (hitresult.getType() != Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hitresult)) {
				this.onHit(hitresult);
			}

			if (this.trajectory != null) {
				this.setDeltaMovement(this.trajectory.getDeltaMovement());
				this.setPos(this.trajectory.getEndOfTickPosition());
			} else {
				Vec3 deltaMovement = this.getDeltaMovement();
				double d0 = this.getX() + deltaMovement.x;
				double d1 = this.getY() + deltaMovement.y;
				double d2 = this.getZ() + deltaMovement.z;
				this.setDeltaMovement(deltaMovement.x, deltaMovement.y - this.getGravity(), deltaMovement.z);
				this.setPos(d0, d1, d2);
			}
		} else {
			this.discard();
		}

		if (!level.isClientSide && this.life > this.getLifetime()) {
			this.explode();
		}

		++this.life;
	}

	public void lerpTo(double x, double y, double z, float xRot, float yRot, int p_19901_, boolean p_19902_) {}

	public void setDeltaMovement(double dx, double dy, double dz) {
		super.setDeltaMovement(dx, dy, dz);
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		this.setYRot((float)(Mth.atan2(dx, dz) * (180D / Math.PI)));
		this.setXRot((float)(Mth.atan2(dy, horizontalDistance) * (180D / Math.PI)));
	}

	public void setDeltaMovement(Vec3 dm) {
		super.setDeltaMovement(dm);
		double horizontalDistance = Math.sqrt(dm.x * dm.x + dm.z * dm.z);
		this.setYRot((float)(Mth.atan2(dm.x, dm.z) * (180D / Math.PI)));
		this.setXRot((float)(Mth.atan2(dm.y, horizontalDistance) * (180D / Math.PI)));
	}

	private HitResult getHitResultOnMoveOrViewVector() {
		Entity owner = this.getOwner();
		HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (owner != null) {
			Vec3 ownerPos = owner.position();
			Vec3 originalHitPos = hitResult.getLocation();
			double distanceToOwner = originalHitPos.distanceTo(ownerPos);
			if (distanceToOwner <= (double)10.0F && this.hitScanTarget != null &&
				this.hitScanTarget.getLocation().distanceTo(ownerPos) <= (double)10.0F) {
				hitResult = this.hitScanTarget;
			}
		}

		return hitResult;
	}

	protected boolean canHitEntity(Entity entity) {
		return entity != this.getOwner() && super.canHitEntity(entity) && !entity.noPhysics;
	}

	protected float getWaterInertia() {
		return 0.6F;
	}

	protected EntityHitResult findHitEntity(Vec3 p_36758_, Vec3 p_36759_) {
		return ProjectileUtil.getEntityHitResult(
			MiscUtil.getLevel(this),
			this,
			p_36758_,
			p_36759_,
			this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0F),
			this::canHitEntity);
	}

	protected void onHit(HitResult result) {
		if (result.getType() == Type.MISS || !ForgeEventFactory.onProjectileImpact(this, result)) {
			super.onHit(result);
			this.discard();
		}
	}

	private void explode() {
		if (this.hurtingItem != null) {
			this.hurtingItem.explodeProjectile(this);
		}

		this.discard();
	}

	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		Entity owner = this.getOwner();
		HurtingItem var4 = this.hurtingItem;
		if (var4 instanceof AmmoItem ammoItem) {
			if (owner instanceof LivingEntity) {
				this.hurtingItem.hurtEntity((LivingEntity)owner, entityHitResult, this, new ItemStack(ammoItem));
			}
		}
	}

	protected void onHitBlock(BlockHitResult blockHitResult) {
		BlockPos blockpos = new BlockPos(blockHitResult.getBlockPos());
		MiscUtil.getLevel(this).getBlockState(blockpos).entityInside(MiscUtil.getLevel(this), blockpos, this);
		Entity owner = this.getOwner();
		if (this.hurtingItem != null && !MiscUtil.isClientSide(this) && owner instanceof LivingEntity) {
			this.hurtingItem.handleBlockHit((Player)owner, blockHitResult, this);
		}

		super.onHitBlock(blockHitResult);
	}

	public boolean isShotAtAngle() {
		return this.entityData.get(DATA_SHOT_AT_ANGLE);
	}

	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Life", this.life);
		tag.putInt("LifeTime", this.getLifetime());
		tag.putBoolean("ShotAtAngle", this.entityData.get(DATA_SHOT_AT_ANGLE));
	}

	public void readAdditionalSaveData(CompoundTag compoundTag) {
		this.discard();
	}

	public boolean isAttackable() {
		return false;
	}

	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public void writeSpawnData(FriendlyByteBuf buffer) {
		Vec3 movement = this.getDeltaMovement();
		buffer.writeInt(this.getLifetime());
		buffer.writeDouble(this.getX());
		buffer.writeDouble(this.getY());
		buffer.writeDouble(this.getZ());
		buffer.writeDouble(movement.x);
		buffer.writeDouble(movement.y);
		buffer.writeDouble(movement.z);
		buffer.writeFloat(this.getXRot());
		buffer.writeFloat(this.getYHeadRot());
		Vec3 targetLocation;
		if (this.hitScanTarget != null) {
			targetLocation = this.hitScanTarget.getLocation();
		} else {
			targetLocation = Vec3.ZERO;
		}

		buffer.writeDouble(targetLocation.x);
		buffer.writeDouble(targetLocation.y);
		buffer.writeDouble(targetLocation.z);
		buffer.writeInt(this.targetEntity != null ? this.targetEntity.getId() : -1);
	}

	public void readSpawnData(FriendlyByteBuf buffer) {
		this.setLifetime(buffer.readInt());
		double x = buffer.readDouble();
		double y = buffer.readDouble();
		double z = buffer.readDouble();
		double dx = buffer.readDouble();
		double dy = buffer.readDouble();
		double dz = buffer.readDouble();
		float xRot = buffer.readFloat();
		float yRot = buffer.readFloat();
		double hitLocationX = buffer.readDouble();
		double hitLocationY = buffer.readDouble();
		double hitLocationZ = buffer.readDouble();
		int entityId = buffer.readInt();
		this.absMoveTo(x, y, z, yRot, xRot);
		this.setDeltaMovement(dx, dy, dz);
		this.setYHeadRot(yRot);
		this.setYBodyRot(yRot);
		if (entityId >= 0) {
			this.targetEntity = this.level().getEntity(entityId);
		}

		this.initTrajectory(new Vec3(hitLocationX, hitLocationY, hitLocationZ));
	}

	public float getProgress(float partialTick) {
		return ((float)this.life + partialTick) / (float)this.getLifetime();
	}

	public long getElapsedTimeMillis() {
		return System.currentTimeMillis() - this.startTimeMillis;
	}

	public float getInitialAngle() {
		return this.initialAngle;
	}

	public ItemStack getItem() {
		return this.hurtingItemStack;
	}

	public void setLifetime(int lifetime) {
		this.lifetime = lifetime;
	}

	public double getGravity() {
		return this.gravity;
	}

	private void setGravity(double gravity) {
		this.gravity = gravity;
	}

	public double getInitialVelocityBlocksPerTick() {
		return this.initialVelocityBlocksPerTick;
	}

	private void setInitialVelocityBlocksPerTick(double initialVelocityBlocksPerTick) {
		this.initialVelocityBlocksPerTick = initialVelocityBlocksPerTick;
	}

	public List<Effect> getActiveAttachedEffects() {
		return this.activeAttachedEffects;
	}

	public List<Effect> getActiveTrailEffects() {
		return this.activeTrailEffects;
	}

	public void setTrailEffects(List<ProjectileLike.EffectInfo> trailEffects) {
		this.trailEffects = trailEffects;
	}

	public void setAttachedEffects(List<ProjectileLike.EffectInfo> attachedEffects) {
		this.attachedEffects = attachedEffects;
	}

	public Trajectory<?> getTrajectory() {
		return this.trajectory;
	}

	public static Predicate<ProjectileLike> topDownTrajectoryPhasePredicate(Predicate<TopDownAttackTrajectory> tp) {
		return (projectile) -> {
			boolean var10000;
			if (projectile.getTrajectory() != null) {
				var patt29669$temp = projectile.getTrajectory();
				if (patt29669$temp instanceof TopDownAttackTrajectory tdat) {
					if (tp.test(tdat)) {
						var10000 = true;
						return var10000;
					}
				}
			}

			var10000 = false;
			return var10000;
		};
	}

	static {
		DATA_ATTACHED_TO_TARGET =
			SynchedEntityData.defineId(SlowProjectile.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT);
		DATA_SHOT_AT_ANGLE = SynchedEntityData.defineId(SlowProjectile.class, EntityDataSerializers.BOOLEAN);
	}

	public static class Builder implements EntityBuilder<Builder, SlowProjectile> {
		private static final float DEFAULT_WIDTH = 0.25F;
		private static final float DEFAULT_HEIGHT = 0.25F;
		private static final int DEFAULT_TRACKING_RANGE = 1024;
		private static final int DEFAULT_CLIENT_TRACKING_RANGE = 1024;
		private static final int DEFAULT_UPDATE_INTERVAL = 1;
		private static final int DEFAULT_LIFETIME_TICKS = 200;
		private String name;
		private float width = 0.25F;
		private float height = 0.25F;
		private final int clientTrackingRange = 1024;
		private final int trackingRange = 1024;
		private int updateInterval = 50;
		private double initialVelocityBlocksPerSecond;
		private int lifetimeTicks = 200;
		private double gravity;
		private final List<EffectBuilderInfo> effectBuilderSuppliers = new ArrayList<>();
		private Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> rendererBuilder;
		private Supplier<HurtingItem> hurtingItem;

		private Builder() {
			this.updateInterval = 1;
		}

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
			this.lifetimeTicks = (int)TimeUnit.MILLISECOND.toTicks(lifetimeMillis);
			return this;
		}

		public Builder withSize(float width, float height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public EntityBuilder<?, ?> withRicochet(boolean ricochet) {
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

		public Builder withGravity(boolean isGravityEnabled) {
			this.gravity = isGravityEnabled ? 0.05 : (double)0.0F;
			return this;
		}

		public Builder withGravity(double gravity) {
			this.gravity = Mth.clamp(gravity, -1.0F, 1.0F);
			return this;
		}

		public Builder withEffect(EffectBuilderInfo effectInfo) {
			this.effectBuilderSuppliers.add(effectInfo);
			return this;
		}

		public EntityBuilder.EntityTypeExt getEntityTypeExt() {
			return EntityTypeExt.PROJECTILE;
		}

		public EntityType.Builder<SlowProjectile> getEntityTypeBuilder() {
			return net.minecraft.world.entity.EntityType.Builder.of(this::create, MobCategory.MISC)
				.sized(this.width, this.height)
				.clientTrackingRange(this.clientTrackingRange)
				.setTrackingRange(this.trackingRange)
				.updateInterval(this.updateInterval)
				.setShouldReceiveVelocityUpdates(false);
		}

		public SlowProjectile create(EntityType<SlowProjectile> entityType, Level level) {
			SlowProjectile projectile = new SlowProjectile(entityType, level);
			if (level.isClientSide) {
				this.initEffects(projectile);
			}

			projectile.setGravity(this.gravity);
			if (this.hurtingItem != null) {
				projectile.hurtingItem = this.hurtingItem.get();
				projectile.hurtingItemStack = new ItemStack(projectile.hurtingItem);
			}

			return projectile;
		}

		public SlowProjectile build(Level level) {
			Supplier<EntityType<?>> entityType = EntityRegistry.getTypeByName(this.name);
		 SlowProjectile projectile = new SlowProjectile((EntityType<? extends SlowProjectile>) entityType.get(), level);
		 projectile.setInitialVelocityBlocksPerTick(this.initialVelocityBlocksPerSecond * (double)0.05F);
		 projectile.setLifetime(this.lifetimeTicks);
		 projectile.setNoGravity(MiscUtil.isNearlyZero(this.gravity));
		 projectile.setGravity(this.gravity);
		 if (this.hurtingItem != null) {
			 projectile.hurtingItem = this.hurtingItem.get();
			 projectile.hurtingItemStack = new ItemStack(projectile.hurtingItem);
		 }

		 if (level.isClientSide) {
			 this.initEffects(projectile);
		 }

		 return projectile;
		}

		public void initEffects(SlowProjectile projectile) {
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

		@OnlyIn(Dist.CLIENT)
		public EntityRenderer<Entity> createEntityRenderer(EntityRendererProvider.Context context) {
			return (this.rendererBuilder.get()).build(context);
		}

		public Builder withJsonObject(JsonObject obj) {
			throw new UnsupportedOperationException();
		}
	}
}
