package com.vicmatskiv.pointblank.registry;

import com.vicmatskiv.pointblank.client.render.ProjectileItemEntityRenderer;
import com.vicmatskiv.pointblank.entity.SlowProjectile;
import com.vicmatskiv.pointblank.item.AmmoItem;
import com.vicmatskiv.pointblank.item.ItemBuilder;
import com.vicmatskiv.pointblank.item.ThrowableItem;
import com.vicmatskiv.pointblank.util.TimeUnit;
import com.vicmatskiv.pointblank.util.TopDownAttackTrajectory;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level.ExplosionInteraction;

public class AmmoRegistry {
   public static final Supplier<AmmoItem> AMMO9MM;
   public static final Supplier<AmmoItem> DEFAULT_AMMO_POOL;
   public static final Supplier<AmmoItem> AMMO45ACP;
   public static final Supplier<AmmoItem> AMMO46;
   public static final Supplier<AmmoItem> AMMO357;
   public static final Supplier<AmmoItem> AMMO57;
   public static final Supplier<AmmoItem> AMMO50AE;
   public static final Supplier<AmmoItem> AMMO545;
   public static final Supplier<AmmoItem> AMMO556;
   public static final Supplier<AmmoItem> AMMO68;
   public static final Supplier<AmmoItem> AMMO762;
   public static final Supplier<AmmoItem> AMMO762X51;
   public static final Supplier<AmmoItem> AMMO338LAPUA;
   public static final Supplier<AmmoItem> AMMO50BMG;
   public static final Supplier<AmmoItem> AMMO12GAUGE;
   public static final Supplier<AmmoItem> AMMOLASERCHARGE;
   public static final Supplier<AmmoItem> AMMOCREATIVE;
   public static final Supplier<AmmoItem> GRENADE40MM;
   public static final Supplier<AmmoItem> GRENADE20MM;
   public static final Supplier<AmmoItem> SMAW_ROCKET;
   public static final Supplier<AmmoItem> AT4_ROCKET;
   public static final Supplier<AmmoItem> JAVELIN_ROCKET;
   public static final Supplier<ThrowableItem> GRENADE;

   public static void init() {
   }

   public static void registerTabItems(Consumer<ItemLike> entries) {
      entries.accept((ItemLike)AMMOCREATIVE.get());
      entries.accept((ItemLike)AMMO9MM.get());
      entries.accept((ItemLike)AMMO45ACP.get());
      entries.accept((ItemLike)AMMO57.get());
      entries.accept((ItemLike)AMMO357.get());
      entries.accept((ItemLike)AMMO50AE.get());
      entries.accept((ItemLike)AMMO46.get());
      entries.accept((ItemLike)AMMO545.get());
      entries.accept((ItemLike)AMMO556.get());
      entries.accept((ItemLike)AMMO68.get());
      entries.accept((ItemLike)AMMO762.get());
      entries.accept((ItemLike)AMMO762X51.get());
      entries.accept((ItemLike)AMMO338LAPUA.get());
      entries.accept((ItemLike)AMMO50BMG.get());
      entries.accept((ItemLike)AMMO12GAUGE.get());
      entries.accept((ItemLike)AMMOLASERCHARGE.get());
      entries.accept((ItemLike)GRENADE20MM.get());
      entries.accept((ItemLike)GRENADE40MM.get());
      entries.accept((ItemLike)SMAW_ROCKET.get());
      entries.accept((ItemLike)AT4_ROCKET.get());
      entries.accept((ItemLike)JAVELIN_ROCKET.get());
      entries.accept((ItemLike)GRENADE.get());
   }

   static {
      AMMO9MM = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo9mm").withTradePrice(100.0D, 30, 1));
      DEFAULT_AMMO_POOL = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammodefault"));
      AMMO45ACP = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo45acp").withTradePrice(100.0D, 10, 1));
      AMMO46 = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo46").withTradePrice(100.0D, 10, 1));
      AMMO357 = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo357").withTradePrice(100.0D, 10, 1));
      AMMO57 = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo57").withTradePrice(100.0D, 10, 1));
      AMMO50AE = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo50ae").withTradePrice(100.0D, 10, 1));
      AMMO545 = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo545").withTradePrice(100.0D, 10, 1));
      AMMO556 = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo556").withTradePrice(100.0D, 10, 1));
      AMMO68 = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo68").withTradePrice(100.0D, 10, 1));
      AMMO762 = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo762").withTradePrice(100.0D, 10, 1));
      AMMO762X51 = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo762x51").withTradePrice(100.0D, 10, 1));
      AMMO338LAPUA = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo338lapua").withTradePrice(100.0D, 10, 1));
      AMMO50BMG = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo50bmg").withTradePrice(100.0D, 10, 1));
      AMMO12GAUGE = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammo12gauge").withTradePrice(100.0D, 10, 1));
      AMMOLASERCHARGE = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammolasercharge"));
      AMMOCREATIVE = ItemRegistry.ITEMS.register((ItemBuilder)(new AmmoItem.Builder()).withName("ammocreative"));
      GRENADE40MM = ItemRegistry.ITEMS.register((ItemBuilder)((AmmoItem.Builder)(new AmmoItem.Builder()).withName("grenade40mm").withProjectileInitialVelocity(50.0D).withProjectileGravity(0.08D).withProjectileEffect(EffectRegistry.GRENADE_TRAIL).withExplosion(3.0F, false, ExplosionInteraction.BLOCK, (String)null, 8.0F, List.of(EffectRegistry.EXPLOSION, EffectRegistry.DEBRIS))).withProjectileRenderer(() -> {
         return new ProjectileItemEntityRenderer.Builder();
      }));
      GRENADE20MM = ItemRegistry.ITEMS.register((ItemBuilder)((AmmoItem.Builder)(new AmmoItem.Builder()).withName("grenade20mm").withProjectileInitialVelocity(50.0D).withProjectileGravity(0.05D).withProjectileEffect(EffectRegistry.GRENADE_TRAIL).withExplosion(2.0F, false, ExplosionInteraction.BLOCK, (String)null, 6.0F, List.of(EffectRegistry.EXPLOSION, EffectRegistry.DEBRIS))).withProjectileRenderer(() -> {
         return new ProjectileItemEntityRenderer.Builder();
      }));
      SMAW_ROCKET = ItemRegistry.ITEMS.register((ItemBuilder)((AmmoItem.Builder)(new AmmoItem.Builder()).withName("smaw_rocket").withTradePrice(10000.0D, 3).withProjectileInitialVelocity(50.0D).withProjectileGravity(0.005D).withProjectileEffect(EffectRegistry.CHEM_TRAIL).withProjectileEffect(EffectRegistry.ROCKET_EXHAUST_PLUME).withExplosion(4.0F, false, ExplosionInteraction.BLOCK, (String)null, 8.0F, List.of(EffectRegistry.EXPLOSION, EffectRegistry.DEBRIS))).withProjectileRenderer(() -> {
         return new ProjectileItemEntityRenderer.Builder();
      }));
      AT4_ROCKET = ItemRegistry.ITEMS.register((ItemBuilder)((AmmoItem.Builder)(new AmmoItem.Builder()).withName("at4_rocket").withTradePrice(10000.0D, 3).withProjectileInitialVelocity(50.0D).withProjectileGravity(0.005D).withProjectileEffect(EffectRegistry.CHEM_TRAIL).withProjectileEffect(EffectRegistry.ROCKET_EXHAUST_PLUME).withExplosion(4.0F, false, ExplosionInteraction.BLOCK, (String)null, 8.0F, List.of(EffectRegistry.EXPLOSION, EffectRegistry.DEBRIS))).withProjectileRenderer(() -> {
         return new ProjectileItemEntityRenderer.Builder();
      }));
      JAVELIN_ROCKET = ItemRegistry.ITEMS.register((ItemBuilder)((AmmoItem.Builder)(new AmmoItem.Builder()).withName("javelin_rocket").withProjectileInitialVelocity(50.0D).withProjectileGravity(0.002D).withProjectileTopDownAttackEnabled(true).withProjectileEffect(EffectRegistry.JAVELIN_TRAIL).withProjectileEffect(EffectRegistry.ROCKET_EXHAUST_PLUME, SlowProjectile.topDownTrajectoryPhasePredicate((t) -> {
         return t.getPhase() != TopDownAttackTrajectory.Phase.SOFT_LAUNCH;
      })).withProjectileTopDownAttackPhaseSound(TopDownAttackTrajectory.Phase.CLIMB, SoundRegistry.IGNITE, 4.0F).withExplosion(7.0F, false, ExplosionInteraction.BLOCK, (String)null, 10.0F, List.of(EffectRegistry.EXPLOSION_BIG, EffectRegistry.DEBRIS))).withProjectileRenderer(() -> {
         return new ProjectileItemEntityRenderer.Builder();
      }));
      GRENADE = ItemRegistry.ITEMS.register((ItemBuilder)((ThrowableItem.Builder)(new ThrowableItem.Builder()).withName("grenade").withTradePrice(100.0D, 1).withProjectileInitialVelocity(25.0D).withProjectileGravity(0.03D).withDrawCooldownDuration(380L, TimeUnit.MILLISECOND).withExplosion(4.0F, false, ExplosionInteraction.BLOCK, (String)null, 4.0F, List.of(EffectRegistry.EXPLOSION, EffectRegistry.DEBRIS))).withProjectileMaxLifetime(5000, TimeUnit.MILLISECOND).withProjectileRicochet(true).withProjectileRenderer(() -> {
         return new ProjectileItemEntityRenderer.Builder();
      }));
   }
}
