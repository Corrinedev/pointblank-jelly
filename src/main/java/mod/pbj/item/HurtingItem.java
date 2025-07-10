package mod.pbj.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import mod.pbj.Config;
import mod.pbj.Enableable;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.config.ConfigManager;
import mod.pbj.config.Configurable;
import mod.pbj.entity.EntityExt;
import mod.pbj.event.BlockHitEvent;
import mod.pbj.explosion.CustomExplosion;
import mod.pbj.feature.DamageFeature;
import mod.pbj.feature.FireModeFeature;
import mod.pbj.network.CustomClientBoundExplosionPacket;
import mod.pbj.network.Network;
import mod.pbj.network.SpawnParticlePacket;
import mod.pbj.registry.EffectRegistry;
import mod.pbj.registry.SoundRegistry;
import mod.pbj.util.HitScan;
import mod.pbj.util.JsonUtil;
import mod.pbj.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

public abstract class HurtingItem extends Item implements Enableable {
	private static final float DEFAULT_EXPLOSION_SOUND_VOLUME = 4.0F;
	public static final double DEFAULT_MAX_SHOOTING_DISTANCE = 200.0F;
	public static final float DEFAULT_DAMAGE = 5.0F;
	public static final float DEFAULT_HEADSHOT_SOUND_VOLUME = 3.0F;
	public static final float DEFAULT_LIGHT_DAMAGE_SOUND_VOLUME = 3.0F;
	public static final float DEFAULT_HEAVY_DAMAGE_SOUND_VOLUME = 3.0F;
	public static final float MAX_LIGHT_DAMAGE = 15.0F;
	public static final long DEFAULT_ENTITY_HIT_SOUND_COOLDOWN = 100L;
	protected ExplosionDescriptor explosionDescriptor;
	protected double maxShootingDistance;
	protected float damage;
	protected boolean isEnabled;
	protected float headshotSoundVolume;
	protected Supplier<SoundEvent> headshotSound;
	protected float lightDamageSoundVolume;
	protected Supplier<SoundEvent> lightDamageSound;
	protected float heavyDamageSoundVolume;
	protected Supplier<SoundEvent> heavyDamageSound;
	protected long entityHitSoundCooldown = 100L;

	public HurtingItem(Item.Properties properties, Builder<?> builder) {
		super(properties);
		if (builder != null) {
			this.explosionDescriptor = builder.explosionDescriptor;
			this.maxShootingDistance = builder.maxShootingDistance;
			this.isEnabled = builder.configOptionEnabled.get();
			this.damage = builder.configOptionDamage.get().floatValue();
			this.headshotSound = builder.headshotSound;
			this.headshotSoundVolume = builder.headshotSoundVolume;
			this.lightDamageSound = builder.lightDamageSound;
			this.lightDamageSoundVolume = builder.lightDamageSoundVolume;
			this.heavyDamageSound = builder.heavyDamageSound;
			this.heavyDamageSoundVolume = builder.heavyDamageSoundVolume;
		}
	}

	public float getDamage() {
		return this.damage;
	}

	public ExplosionDescriptor getExplosion() {
		return this.explosionDescriptor;
	}

	public boolean isEnabled() {
		return this.isEnabled;
	}

	public float
	hurtEntity(LivingEntity player, EntityHitResult entityHitResult, Entity projectile, ItemStack gunStack) {
		float damage = 0.0F;
		if (this.getExplosion() != null) {
			Vec3 hitLocation = entityHitResult.getEntity().getPosition(0.0F);
			if (MiscUtil.isProtected(entityHitResult.getEntity())) {
				Vec3 pos = entityHitResult.getEntity().position();
				MiscUtil.getLevel(player).playSound(
					null, pos.x, pos.y, pos.z, SoundEvents.CAT_AMBIENT, SoundSource.AMBIENT, 3.0F, 1.0F);
			} else {
				this.explode(MiscUtil.getLevel(player), projectile, null, hitLocation.x, hitLocation.y, hitLocation.z);
			}
		} else {
			Entity hitEntity = entityHitResult.getEntity();
			if (MiscUtil.isProtected(hitEntity)) {
				Vec3 pos = hitEntity.position();
				MiscUtil.getLevel(player).playSound(
					null, pos.x, pos.y, pos.z, SoundEvents.CAT_AMBIENT, SoundSource.AMBIENT, 3.0F, 1.0F);
			} else {
				boolean isHeadshot = false;
				if (hitEntity instanceof LivingEntity livingEntity) {
					isHeadshot = HitScan.isHeadshot(livingEntity, entityHitResult.getLocation());
				}

				DamageSource damageSource;
				if (player instanceof Player) {
					damageSource = player.damageSources().playerAttack((Player)player);
				} else {
					DamageSources damageSources = MiscUtil.getLevel(player).damageSources();
					damageSource = damageSources.generic();
				}

				int origInvulnerableTime = hitEntity.invulnerableTime;
				hitEntity.invulnerableTime = 0;
				double distanceToPlayer = entityHitResult.distanceTo(player);
				double adjustedDamage = FireModeFeature.getDamage(gunStack);
				if (isHeadshot) {
					adjustedDamage *= Config.headshotDamageModifier;
				}

				adjustedDamage *= Mth.clamp(
					(double)1.0F -
						Math.pow(Math.sqrt(distanceToPlayer) / this.maxShootingDistance * (double)0.5F, 6.0F),
					0.0F,
					1.0F);
				if (gunStack != null) {
					adjustedDamage *= DamageFeature.getHitScanDamageModifier(gunStack);
					adjustedDamage *= Config.hitscanDamageModifier;
				}

				hitEntity.hurt(damageSource, (float)adjustedDamage);
				EntityExt entityExt = (EntityExt)hitEntity;
				if (System.currentTimeMillis() - entityExt.getLastHitSoundTimestamp() > this.entityHitSoundCooldown) {
					entityExt.setLastHitSoundTimestamp(System.currentTimeMillis());
					if (isHeadshot && !MiscUtil.getLevel(hitEntity).isClientSide) {
						MiscUtil.getLevel(hitEntity).playSound(
							null,
							hitEntity.getX(),
							hitEntity.getY(),
							hitEntity.getZ(),
							this.headshotSound.get(),
							SoundSource.PLAYERS,
							this.headshotSoundVolume,
							1.0F);
					} else if (adjustedDamage < (double)15.0F) {
						MiscUtil.getLevel(hitEntity).playSound(
							null,
							hitEntity.getX(),
							hitEntity.getY(),
							hitEntity.getZ(),
							this.lightDamageSound.get(),
							SoundSource.PLAYERS,
							this.lightDamageSoundVolume,
							1.0F);
					} else {
						MiscUtil.getLevel(hitEntity).playSound(
							null,
							hitEntity.getX(),
							hitEntity.getY(),
							hitEntity.getZ(),
							this.heavyDamageSound.get(),
							SoundSource.PLAYERS,
							this.heavyDamageSoundVolume,
							1.0F);
					}
				}

				hitEntity.invulnerableTime = origInvulnerableTime;
				damage = (float)adjustedDamage;
			}
		}

		if (projectile != null) {
			projectile.discard();
		}

		return damage;
	}

	public void handleBlockHit(LivingEntity player, BlockHitResult blockHitResult, @Nullable Entity projectile) {
		BlockHitEvent event = new BlockHitEvent(player, blockHitResult, projectile);
		MinecraftForge.EVENT_BUS.post(event);
		if (!event.isCanceled()) {
			spawnBlockBreakParticles((ServerPlayer)player, blockHitResult.getBlockPos(), blockHitResult.getLocation());
			if (this.getExplosion() != null) {
				Vec3 hitLocation = blockHitResult.getLocation();
				this.explode(MiscUtil.getLevel(player), projectile, null, hitLocation.x, hitLocation.y, hitLocation.z);
			}

			if (projectile != null) {
				projectile.discard();
			}
		}
	}

	public void discardProjectile(Entity projectile) {
		this.explodeProjectile(projectile);
	}

	public void explodeProjectile(Entity projectile) {
		this.explode(
			MiscUtil.getLevel(projectile), projectile, null, projectile.getX(), projectile.getY(), projectile.getZ());
	}

	private static void spawnBlockBreakParticles(ServerPlayer player, BlockPos blockPos, Vec3 hitLocation) {
		Level level = MiscUtil.getLevel(player);
		BlockState blockState = level.getBlockState(blockPos);
		if (blockState != null) {
			Network.networkChannel.send(
				PacketDistributor.PLAYER.with(() -> player),
				new SpawnParticlePacket(ParticleTypes.SMOKE, hitLocation.x, hitLocation.y, hitLocation.z, 5));
		}
	}

	private void explode(
		Level level,
		@Nullable Entity entity,
		@Nullable DamageSource damageSource,
		double posX,
		double posY,
		double posZ) {
		if (level.isClientSide) {
			throw new IllegalArgumentException("Cannot use this method on the client side");
		} else {
			float power = this.explosionDescriptor.power();
			boolean fire = this.explosionDescriptor.fire();
			ExplosionDamageCalculator calc = null;
			CustomExplosion customExplosion = CustomExplosion.explode(
				level,
				this,
				entity,
				damageSource,
				calc,
				posX,
				posY,
				posZ,
				power,
				fire,
				this.explosionDescriptor.interaction(),
				false);
			if (!customExplosion.interactsWithBlocks()) {
				customExplosion.clearToBlow();
			}

			for (ServerPlayer player : ((ServerLevel)level).getPlayers((p) -> true)) {
				if (player.distanceToSqr(posX, posY, posZ) < (double)22000.0F) {
					Network.networkChannel.send(
						PacketDistributor.PLAYER.with(() -> player),
						new CustomClientBoundExplosionPacket(
							this,
							posX,
							posY,
							posZ,
							power,
							customExplosion.getToBlow(),
							customExplosion.getHitPlayers().get(player)));
				}
			}
		}
	}

	public abstract static class Builder<T extends ItemBuilder<T>>
		extends ItemBuilder<T> implements Configurable, Enableable {
		private ExplosionDescriptor explosionDescriptor;
		private double maxShootingDistance = 200.0F;
		private float damage = 5.0F;
		private Supplier<Boolean> configOptionEnabled;
		private Supplier<Double> configOptionDamage;
		private float headshotSoundVolume = 3.0F;
		private Supplier<SoundEvent> headshotSound;
		protected float lightDamageSoundVolume;
		protected Supplier<SoundEvent> lightDamageSound;
		protected float heavyDamageSoundVolume;
		protected Supplier<SoundEvent> heavyDamageSound;

		public Builder() {
			this.headshotSound = SoundRegistry.HIT_HEADSHOT;
			this.lightDamageSoundVolume = 3.0F;
			this.lightDamageSound = SoundRegistry.HIT_LIGHT;
			this.heavyDamageSoundVolume = 3.0F;
			this.heavyDamageSound = SoundRegistry.HIT_HEAVY;
		}

		protected T cast(Builder<T> _this) {
			return (T)_this;
		}

		public T withDamage(float damage) {
			this.damage = damage;
			return this.cast(this);
		}

		public T withHeadshotSound(Supplier<SoundEvent> headshotSound, float headshotSoundVolume) {
			this.headshotSound = headshotSound;
			this.headshotSoundVolume = headshotSoundVolume;
			return this.cast(this);
		}

		public T withLightDamageSound(Supplier<SoundEvent> lightDamageSound, float lightDamageSoundVolume) {
			this.lightDamageSound = lightDamageSound;
			this.lightDamageSoundVolume = lightDamageSoundVolume;
			return this.cast(this);
		}

		public T withHeavyDamageSound(Supplier<SoundEvent> heavyDamageSound, float heavyDamageSoundVolume) {
			this.heavyDamageSound = heavyDamageSound;
			this.heavyDamageSoundVolume = heavyDamageSoundVolume;
			return this.cast(this);
		}

		public T withMaxShootingDistance(double distance) {
			this.maxShootingDistance = distance;
			return this.cast(this);
		}

		public T withExplosion(
			float power,
			boolean fire,
			Level.ExplosionInteraction explosionInteraction,
			List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> effects) {
			return this.withExplosion(power, fire, explosionInteraction, null, 4.0F, effects);
		}

		public T withExplosion(
			float power,
			boolean fire,
			Level.ExplosionInteraction explosionInteraction,
			@Nullable String soundName,
			float soundVolume,
			List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> effects) {
			this.explosionDescriptor =
				new ExplosionDescriptor(power, fire, explosionInteraction, soundName, soundVolume, effects);
			return this.cast(this);
		}

		public void configure(ConfigManager.Builder builder) {
			this.configOptionEnabled = builder.createBooleanOption()
										   .withName(this.getName() + ".enabled")
										   .withDescription("Set to `false` to remove the item from the game.")
										   .withDefault(true)
										   .getSupplier();
			this.configOptionDamage = builder.createDoubleOption()
										  .withName(this.getName() + ".damage")
										  .withDescription("Sets this item damage.")
										  .withRange(0.01, (double)100.0F)
										  .withDefault((double)this.damage)
										  .getSupplier();
		}

		public T withJsonObject(JsonObject obj) {
			this.withMaxShootingDistance(JsonUtil.getJsonDouble(obj, "maxShootingDistance", 200.0F));
			this.withDamage(JsonUtil.getJsonFloat(obj, "damage", 5.0F));
			float headshotSoundVolume = JsonUtil.getJsonFloat(obj, "headshotSoundVolume", 3.0F);
			JsonElement headshotSoundElem = obj.get("headshotSound");
			if (headshotSoundElem != null && !headshotSoundElem.isJsonNull()) {
				String headshotSoundName = headshotSoundElem.getAsString();
				this.withHeadshotSound(() -> SoundRegistry.getSoundEvent(headshotSoundName), headshotSoundVolume);
			}

			JsonObject jsExplosion = obj.getAsJsonObject("explosion");
			if (jsExplosion != null) {
				float power = JsonUtil.getJsonFloat(jsExplosion, "power", 1.0F);
				boolean fire = JsonUtil.getJsonBoolean(jsExplosion, "fire", false);
				String soundName = JsonUtil.getJsonString(jsExplosion, "sound", null);
				float soundVolume = JsonUtil.getJsonFloat(jsExplosion, "soundVolume", 4.0F);
				Level.ExplosionInteraction interaction = (Level.ExplosionInteraction)JsonUtil.getEnum(
					jsExplosion, "interaction", Level.ExplosionInteraction.class, ExplosionInteraction.BLOCK, true);
				List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> explosionEffects = new ArrayList<>();

				for (String effectName : JsonUtil.getStrings(jsExplosion, "effects")) {
					Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier =
						() -> EffectRegistry.getEffectBuilderSupplier(effectName).get();
					explosionEffects.add(supplier);
				}

				this.withExplosion(power, fire, interaction, soundName, soundVolume, explosionEffects);
			}

			return this.cast(this);
		}

		public float getDamage() {
			return this.damage;
		}

		public boolean isEnabled() {
			return this.configOptionEnabled == null || this.configOptionEnabled.get();
		}
	}
}
