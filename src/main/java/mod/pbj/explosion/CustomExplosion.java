package mod.pbj.explosion;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import mod.pbj.Config;
import mod.pbj.client.effect.Effect;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.item.ExplosionDescriptor;
import mod.pbj.item.ExplosionProvider;
import mod.pbj.registry.SoundRegistry;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.SimpleHitResult;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import software.bernie.geckolib.util.ClientUtils;

public class CustomExplosion extends Explosion {
	private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
	private static final float DEFAULT_SOUND_VOLUME = 4.0F;
	private final Item item;
	private final Level level;
	private final float radius;
	private final double x;
	private final double y;
	private final double z;
	private boolean fire;
	private final ObjectArrayList<BlockPos> toBlow = new ObjectArrayList<>();
	private Explosion.BlockInteraction blockInteraction;
	private Random random;
	private final Entity source;
	private ExplosionDamageCalculator damageCalculator;
	private final Map<Player, Vec3> hitPlayers = Maps.newHashMap();

	public CustomExplosion(
		Level level,
		Item item,
		Entity entity,
		DamageSource damageSource,
		ExplosionDamageCalculator calc,
		double posX,
		double posY,
		double posZ,
		float power,
		boolean fire,
		Explosion.BlockInteraction blockInteraction) {
		super(level, entity, damageSource, calc, posX, posY, posZ, power, fire, blockInteraction);
		this.level = level;
		this.item = item;
		this.source = entity;
		this.x = posX;
		this.y = posY;
		this.z = posZ;
		this.radius = power;
		this.fire = fire;
		this.blockInteraction = blockInteraction;
		this.random = new Random();
		this.damageCalculator = calc == null ? this.makeDamageCalculator(entity) : calc;
	}

	public CustomExplosion(
		Level level, Item gunItem, Entity entity, double x, double y, double z, float power, List<BlockPos> toBlow) {
		super(level, entity, x, y, z, power, toBlow);
		this.level = level;
		this.item = gunItem;
		this.source = entity;
		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = power;
		this.toBlow.addAll(toBlow);
	}

	private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity entity) {
		return entity == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(entity);
	}

	public Item getItem() {
		return this.item;
	}

	@OnlyIn(Dist.CLIENT)
	public void finalizeClientExplosion() {
		if (item instanceof ExplosionProvider explosionProvider) {
			ExplosionDescriptor explosionDescriptor = explosionProvider.getExplosion();
			MinecraftForge.EVENT_BUS.post(new ExplosionEvent(new Vec3(this.x, this.y, this.z), explosionDescriptor));
			SoundEvent soundEvent = null;
			float soundVolume = 4.0F;
			if (explosionDescriptor != null) {
				if (explosionDescriptor.soundName() != null) {
					soundEvent = SoundRegistry.getSoundEvent(explosionDescriptor.soundName());
				}

				soundVolume = explosionDescriptor.soundVolume();
				this.applyExplosionEffects(explosionDescriptor);
			}

			if (soundEvent == null) {
				soundEvent = SoundEvents.GENERIC_EXPLODE;
			}

			if (!MiscUtil.isNearlyZero(soundVolume)) {
				this.playSound(soundEvent, soundVolume);
			}
		}

		this.finalizeExplosion(false);
	}

	private void playSound(SoundEvent soundEvent, float volume) {
		this.level.playLocalSound(
			this.x,
			this.y,
			this.z,
			soundEvent,
			SoundSource.BLOCKS,
			volume,
			(1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
			false);
	}

	private void applyExplosionEffects(ExplosionDescriptor explosionDescriptor) {
		for (Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilderSupplier :
			 explosionDescriptor.effects()) {
			EffectBuilder<? extends EffectBuilder<?, ?>, ?> effectBuilder = effectBuilderSupplier.get();
			EffectBuilder.Context context =
				(new EffectBuilder.Context())
					.withHitResult(new SimpleHitResult(this.getPosition(), Type.BLOCK, Direction.UP, -1));
			Effect effect = effectBuilder.build(context);
			effect.launch(ClientUtils.getClientPlayer());
		}
	}

	public void finalizeExplosion(boolean ignored) {
		boolean flag = this.interactsWithBlocks();
		if (flag) {
			ObjectArrayList<Pair<ItemStack, BlockPos>> objectarraylist = new ObjectArrayList<>();
			boolean flag1 = this.getIndirectSourceEntity() instanceof Player;
			Util.shuffle(this.toBlow, this.level.random);
			for (BlockPos blockpos : this.toBlow) {
				BlockState blockstate = this.level.getBlockState(blockpos);
				if (!blockstate.isAir()) {
					BlockPos blockpos1 = blockpos.immutable();
					this.level.getProfiler().push("explosion_blocks");
					if (blockstate.canDropFromExplosion(this.level, blockpos, this)) {
						if (this.level instanceof ServerLevel serverlevel) {
							BlockEntity blockentity =
								blockstate.hasBlockEntity() ? this.level.getBlockEntity(blockpos) : null;
							LootParams.Builder lootparams$builder =
								(new LootParams.Builder(serverlevel))
									.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockpos))
									.withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
									.withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockentity)
									.withOptionalParameter(LootContextParams.THIS_ENTITY, this.source);
							if (this.blockInteraction == BlockInteraction.DESTROY_WITH_DECAY) {
								lootparams$builder.withParameter(LootContextParams.EXPLOSION_RADIUS, this.radius);
							}

							blockstate.spawnAfterBreak(serverlevel, blockpos, ItemStack.EMPTY, flag1);
							blockstate.getDrops(lootparams$builder)
								.forEach((p_46074_) -> addBlockDrops(objectarraylist, p_46074_, blockpos1));
						}
					}

					blockstate.onBlockExploded(this.level, blockpos, this);
					this.level.getProfiler().pop();
				}
			}

			for (Pair<ItemStack, BlockPos> pair : objectarraylist) {
				Block.popResource(this.level, pair.getSecond(), pair.getFirst());
			}
		}

		if (this.fire) {
			for (BlockPos blockpos2 : this.toBlow) {
				if (this.random.nextInt(3) == 0 && this.level.getBlockState(blockpos2).isAir() &&
					this.level.getBlockState(blockpos2.below()).isSolidRender(this.level, blockpos2.below())) {
					this.level.setBlockAndUpdate(blockpos2, BaseFireBlock.getState(this.level, blockpos2));
				}
			}
		}
	}

	private static void
	addBlockDrops(ObjectArrayList<Pair<ItemStack, BlockPos>> blockPosToBlow, ItemStack itemStack, BlockPos blockPos) {
		int i = blockPosToBlow.size();

		for (int j = 0; j < i; ++j) {
			Pair<ItemStack, BlockPos> pair = blockPosToBlow.get(j);
			ItemStack itemstack = pair.getFirst();
			if (ItemEntity.areMergable(itemstack, itemStack)) {
				ItemStack itemstack1 = ItemEntity.merge(itemstack, itemStack, 16);
				blockPosToBlow.set(j, Pair.of(itemstack1, pair.getSecond()));
				if (itemStack.isEmpty()) {
					return;
				}
			}
		}

		blockPosToBlow.add(Pair.of(itemStack, blockPos));
	}

	private static Explosion.BlockInteraction
	getDestroyType(Level level, GameRules.Key<GameRules.BooleanValue> gameRulesKey) {
		return level.getGameRules().getBoolean(gameRulesKey) ? BlockInteraction.DESTROY_WITH_DECAY
															 : BlockInteraction.DESTROY;
	}

	public static CustomExplosion explode(
		Level level,
		Item item,
		@Nullable Entity entity,
		@Nullable DamageSource damageSource,
		@Nullable ExplosionDamageCalculator calc,
		double posX,
		double posY,
		double posZ,
		float power,
		boolean fire,
		Level.ExplosionInteraction interaction,
		boolean particlesEnabled) {
		if (!Config.explosionDestroyBlocksEnabled) {
			interaction = ExplosionInteraction.NONE;
		}

		Explosion.BlockInteraction blockInteraction;
		switch (interaction) {
			case NONE -> blockInteraction = BlockInteraction.KEEP;
			case BLOCK -> blockInteraction = getDestroyType(level, GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY);
			case TNT -> blockInteraction = getDestroyType(level, GameRules.RULE_TNT_EXPLOSION_DROP_DECAY);
			default -> throw new IncompatibleClassChangeError();
		}

		CustomExplosion explosion = new CustomExplosion(
			level, item, entity, damageSource, calc, posX, posY, posZ, power, fire, blockInteraction);
		explosion.explode();
		explosion.finalizeExplosion(false);
		return explosion;
	}

	public void explode() {
		this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
		Set<BlockPos> set = Sets.newHashSet();

		for (int j = 0; j < 16; ++j) {
			for (int k = 0; k < 16; ++k) {
				for (int l = 0; l < 16; ++l) {
					if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
						double d0 = (float)j / 15.0F * 2.0F - 1.0F;
						double d1 = (float)k / 15.0F * 2.0F - 1.0F;
						double d2 = (float)l / 15.0F * 2.0F - 1.0F;
						double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
						d0 /= d3;
						d1 /= d3;
						d2 /= d3;
						float f = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
						double d4 = this.x;
						double d6 = this.y;
						double d8 = this.z;

						for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
							BlockPos blockpos = BlockPos.containing(d4, d6, d8);
							BlockState blockstate = this.level.getBlockState(blockpos);
							FluidState fluidstate = this.level.getFluidState(blockpos);
							if (!this.level.isInWorldBounds(blockpos)) {
								break;
							}

							Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(
								this, this.level, blockpos, blockstate, fluidstate);
							if (optional.isPresent()) {
								f -= (optional.get() + 0.3F) * 0.3F;
							}

							if (f > 0.0F &&
								this.damageCalculator.shouldBlockExplode(this, this.level, blockpos, blockstate, f)) {
								set.add(blockpos);
							}

							d4 += d0 * (double)0.3F;
							d6 += d1 * (double)0.3F;
							d8 += d2 * (double)0.3F;
						}
					}
				}
			}
		}

		this.toBlow.addAll(set);
		float adjustedRadius = this.radius * 2.0F;
		int bbXMin = Mth.floor(this.x - (double)adjustedRadius - (double)1.0F);
		int bbXMax = Mth.floor(this.x + (double)adjustedRadius + (double)1.0F);
		int bbYMin = Mth.floor(this.y - (double)adjustedRadius - (double)1.0F);
		int bbYMax = Mth.floor(this.y + (double)adjustedRadius + (double)1.0F);
		int bbZMin = Mth.floor(this.z - (double)adjustedRadius - (double)1.0F);
		int bbZMax = Mth.floor(this.z + (double)adjustedRadius + (double)1.0F);
		List<Entity> list =
			this.level.getEntities(this.source, new AABB(bbXMin, bbYMin, bbZMin, bbXMax, bbYMax, bbZMax));
		ForgeEventFactory.onExplosionDetonate(this.level, this, list, adjustedRadius);
		Vec3 thisPos = new Vec3(this.x, this.y, this.z);

		for (Entity entity : list) {
			if (!entity.ignoreExplosion() && !MiscUtil.isProtected(entity)) {
				double normalizedDistanceToEntity = Math.sqrt(entity.distanceToSqr(thisPos)) / (double)adjustedRadius;
				if (normalizedDistanceToEntity <= (double)1.0F) {
					double xOffset = entity.getX() - this.x;
					double yOffset = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.y;
					double zOffset = entity.getZ() - this.z;
					double adjustedDistanceToEntity =
						Math.sqrt(xOffset * xOffset + yOffset * yOffset + zOffset * zOffset);
					if (adjustedDistanceToEntity != (double)0.0F) {
						xOffset /= adjustedDistanceToEntity;
						yOffset /= adjustedDistanceToEntity;
						zOffset /= adjustedDistanceToEntity;
						double seenPercent = getSeenPercent(thisPos, entity);
						double damage = ((double)1.0F - normalizedDistanceToEntity) * seenPercent;
						entity.hurt(
							this.getDamageSource(),
							(float)((int)((damage * damage + damage) / (double)2.0F * (double)7.5F *
											  (double)adjustedRadius +
										  (double)1.0F)));
						double d11;
						if (entity instanceof LivingEntity livingentity) {
							d11 = ProtectionEnchantment.getExplosionKnockbackAfterDampener(livingentity, damage);
						} else {
							d11 = damage;
						}

						xOffset *= d11;
						yOffset *= d11;
						zOffset *= d11;
						Vec3 knockbackMovement = new Vec3(xOffset, yOffset, zOffset);
						entity.setDeltaMovement(entity.getDeltaMovement().add(knockbackMovement));
						if (entity instanceof Player player) {
							if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
								this.hitPlayers.put(player, knockbackMovement);
							}
						}
					}
				}
			}
		}
	}
}
