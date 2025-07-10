package mod.pbj.registry;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import mod.pbj.Config;
import mod.pbj.client.SegmentsProviders;
import mod.pbj.client.effect.*;
import mod.pbj.util.Interpolators;
import mod.pbj.util.ParticleValueProviders;
import mod.pbj.util.VelocityProviders;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

public class EffectRegistry {
	private static final Map<String, Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> suppliers =
		new HashMap<>();
	private static final Map<UUID, Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> effectSuppliersById =
		new HashMap<>();
	private static final Map<Class<? extends Entity>, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>>
		entityDeathEffects = new HashMap<>();
	private static final Map<Class<? extends Entity>, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>>
		entityHitEffects = new HashMap<>();
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> MUZZLE_FLASH = register(
		"muzzle_flash",
		()
			-> (new MuzzleFlashEffect.Builder())
				   .withName("muzzle_flash")
				   .withTexture("textures/effect/flashes.png")
				   .withDuration(50L)
				   .withBrightness(1)
				   .withSprites(1, 9, 1, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withGlow(true)
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(1.25F))
				   .withAlphaProvider(new Interpolators.EaseOutFloatProvider(0.6F))
				   .withInitialRollProvider(() -> 0.0F));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> MUZZLE_FLASH_SMALL = register(
		"muzzle_flash_small",
		()
			-> (new MuzzleFlashEffect.Builder())
				   .withName("muzzle_flash_small")
				   .withTexture("textures/effect/flashes.png")
				   .withDuration(50L)
				   .withBrightness(1)
				   .withSprites(1, 9, 1, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withGlow(true)
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(1.0F))
				   .withAlphaProvider(new Interpolators.EaseOutFloatProvider(0.6F))
				   .withInitialRollProvider(() -> 0.0F));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> MUZZLE_FLASH_BIG = register(
		"muzzle_flash_big",
		()
			-> (new MuzzleFlashEffect.Builder())
				   .withName("muzzle_flash_big")
				   .withTexture("textures/effect/flashes.png")
				   .withDuration(50L)
				   .withBrightness(1)
				   .withSprites(1, 9, 1, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withGlow(true)
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(1.5F))
				   .withAlphaProvider(new Interpolators.EaseOutFloatProvider(0.6F))
				   .withInitialRollProvider(() -> 0.0F));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> MUZZLE_FLASH_BIG_EX = register(
		"muzzle_flash_big_ex",
		()
			-> (new MuzzleFlashEffect.Builder())
				   .withName("muzzle_flash_big_ex")
				   .withTexture("textures/effect/flashes.png")
				   .withDuration(50L)
				   .withBrightness(1)
				   .withSprites(1, 9, 1, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withGlow(true)
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(2.5F))
				   .withAlphaProvider(new Interpolators.EaseOutFloatProvider(0.6F))
				   .withInitialRollProvider(() -> 0.0F));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> TRACER = register(
		"tracer",
		()
			-> (new DetachedProjectileEffect.Builder())
				   .withName("tracer")
				   .withTexture("textures/effect/tracers2.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDepthTest(true)
				   .withDuration(200L)
				   .withBlades(2, 0.0F, 0.75F)
				   .withFace(0.75F, 1.0F)
				   .withBladeBrightness(2)
				   .withFaceBrightness(2)
				   .withRotations(3.0D)
				   .withGlow(true)
				   .withSegmentsProvider(new SegmentsProviders.MovingSegmentsProvider(300.0F, 0.0F))
				   .withBladeWidthProvider(new Interpolators.ConstantFloatProvider(0.2F))
				   .withFaceWidthProvider(new Interpolators.ConstantFloatProvider(0.1F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> TRACER_MINIGUN = register(
		"tracerminigun",
		()
			-> (new DetachedProjectileEffect.Builder())
				   .withName("tracerminigun")
				   .withTexture("textures/effect/tracers2.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDepthTest(true)
				   .withDuration(100L)
				   .withBlades(2, 0.0F, 0.75F)
				   .withFace(0.75F, 1.0F)
				   .withBrightness(2)
				   .withGlow(true)
				   .withSegmentsProvider(new SegmentsProviders.MovingSegmentsProvider(300.0F, 0.0F))
				   .withBladeWidthProvider(new Interpolators.ConstantFloatProvider(0.3F))
				   .withFaceWidthProvider(new Interpolators.ConstantFloatProvider(0.3F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> TRACER_025 = register(
		"tracer_025",
		()
			-> (new DetachedProjectileEffect.Builder())
				   .withName("tracer_025")
				   .withTexture("textures/effect/tracers2.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDepthTest(true)
				   .withDuration(200L)
				   .withBlades(2, 0.0F, 0.75F)
				   .withFace(0.75F, 1.0F)
				   .withBladeBrightness(2)
				   .withFaceBrightness(2)
				   .withRotations(3.0D)
				   .withGlow(true)
				   .withSegmentsProvider(new SegmentsProviders.MovingSegmentsProvider(300.0F, 0.0F))
				   .withBladeWidthProvider(new Interpolators.ConstantFloatProvider(0.1F))
				   .withFaceWidthProvider(new Interpolators.ConstantFloatProvider(0.05F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> LASER = register(
		"laser",
		()
			-> (new AttachedProjectileEffect.Builder())
				   .withName("laser")
				   .withTexture("textures/effect/laser_yellow.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDuration(200L)
				   .withBlades(2, 0.0F, 1.0F)
				   .withRotations(3.0D)
				   .withBrightness(1)
				   .withGlow(true)
				   .withDepthTest(true)
				   .withSegmentsProvider(new SegmentsProviders.StaticBeamSegmentsProvider())
				   .withWidth(0.8D));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> LASER_MUZZLE = register(
		"laser_muzzle",
		()
			-> (new AttachedProjectileEffect.Builder())
				   .withName("laser_muzzle")
				   .withTexture("textures/effect/laser_muzzle.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDepthTest(true)
				   .withDuration(1000L)
				   .withBrightness(1)
				   .withFace(0.0F, 1.0F)
				   .withSprites(6, 6, 60, AbstractEffect.SpriteAnimationType.PLAY_ONCE)
				   .withGlow(true)
				   .withSegmentsProvider(new SegmentsProviders.StaticBeamSegmentsProvider())
				   .withFaceWidthProvider(new Interpolators.ConstantFloatProvider(2.0F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> LAUNCHER_MUZZLE = register(
		"launcher_muzzle",
		()
			-> (new AttachedProjectileEffect.Builder())
				   .withName("launcher_muzzle")
				   .withTexture("textures/effect/launcher_muzzle.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDepthTest(true)
				   .withDuration(1000L)
				   .withBrightness(1)
				   .withFace(0.0F, 1.0F)
				   .withSprites(8, 8, 70, AbstractEffect.SpriteAnimationType.PLAY_ONCE)
				   .withGlow(true)
				   .withSegmentsProvider(new SegmentsProviders.StaticBeamSegmentsProvider())
				   .withFaceWidthProvider(new Interpolators.ConstantFloatProvider(2.0F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> JAVELIN_MUZZLE = register(
		"javelin_muzzle",
		()
			-> (new AttachedProjectileEffect.Builder())
				   .withName("javelin_muzzle")
				   .withTexture("textures/effect/launcher_muzzle.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDepthTest(true)
				   .withDuration(1000L)
				   .withBrightness(1)
				   .withFace(0.0F, 1.0F)
				   .withSprites(8, 8, 70, AbstractEffect.SpriteAnimationType.PLAY_ONCE)
				   .withGlow(true)
				   .withSegmentsProvider(new SegmentsProviders.StaticBeamSegmentsProvider())
				   .withFaceWidthProvider(new Interpolators.ConstantFloatProvider(4.0F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> EXPLOSION = register(
		"explosion",
		()
			-> (new ImpactEffect.Builder())
				   .withName("explosion")
				   .withTexture("textures/effect/explosion_new.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDuration(1350L)
				   .withSprites(9, 9, 60, AbstractEffect.SpriteAnimationType.PLAY_ONCE)
				   .withGlow(true)
				   .withDepthTest(true)
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(6.0F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> EXPLOSION_BIG = register(
		"explosion_big",
		()
			-> (new ImpactEffect.Builder())
				   .withName("explosion_big")
				   .withTexture("textures/effect/explosion_new.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDuration(1400L)
				   .withSprites(9, 9, 60, AbstractEffect.SpriteAnimationType.PLAY_ONCE)
				   .withGlow(true)
				   .withDepthTest(true)
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(9.0F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> DEBRIS = register(
		"debris",
		()
			-> (new ImpactEffect.Builder())
				   .withName("debris")
				   .withTexture("textures/effect/dirt3x3.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDuration(1000L)
				   .withRotations(0.5D)
				   .withSprites(3, 3, 0, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withCount(10)
				   .withVelocityProvider(
					   VelocityProviders.hemisphereVelocityProvider(3.0D, VelocityProviders.Distribution.TIGHT))
				   .withGlow(false)
				   .withDepthTest(true)
				   .withGravity(10.0F)
				   .withAlphaProvider(new Interpolators.AnotherEaseInEaseOutFloatProvider(1.0F, 0.01F, 0.5F))
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(0.2F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> CHEM_TRAIL = register(
		"chem_trail",
		()
			-> (new TrailEffect.Builder())
				   .withName("chem_trail")
				   .withTexture("textures/effect/chem_trail.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(1500L)
				   .withGlow(false)
				   .withDepthTest(true)
				   .withLongitudeOffset(2.0D)
				   .withAlphaProvider(new Interpolators.EaseOutFloatProvider(1.0F))
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(0.8F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> GRENADE_TRAIL = register(
		"grenade_trail",
		()
			-> (new TrailEffect.Builder())
				   .withName("grenade_trail")
				   .withTexture("textures/effect/chem_trail.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(1500L)
				   .withGlow(false)
				   .withDepthTest(true)
				   .withLongitudeOffset(2.0D)
				   .withAlphaProvider(new Interpolators.EaseOutFloatProvider(0.4F))
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(0.4F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> JAVELIN_TRAIL = register(
		"javelin_trail",
		()
			-> (new TrailEffect.Builder())
				   .withName("javelin_trail")
				   .withTexture("textures/effect/chem_trail.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(1500L)
				   .withGlow(false)
				   .withDepthTest(true)
				   .withLongitudeOffset(2.0D)
				   .withAlphaProvider(new Interpolators.EaseOutFloatProvider(1.0F))
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(1.2F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> ROCKET_EXHAUST_PLUME = register(
		"rocket_exhaust_plume",
		()
			-> (new AttachedProjectileEffect.Builder())
				   .withName("rocket_exhaust_plume")
				   .withTexture("textures/effect/tracers2.png")
				   .withBlendMode(Effect.BlendMode.ADDITIVE)
				   .withDuration(200L)
				   .withBlades(2, 0.0F, 0.75F)
				   .withFace(0.75F, 1.0F)
				   .withBladeBrightness(1)
				   .withFaceBrightness(1)
				   .withRotations(3.0D)
				   .withGlow(true)
				   .withDepthTest(true)
				   .withSegmentsProvider(new SegmentsProviders.SingleSegmentProvider())
				   .withBladeLengthProvider(() -> 1.3F)
				   .withBladeWidthProvider(new Interpolators.ConstantFloatProvider(0.7F))
				   .withFaceWidthProvider(new Interpolators.ConstantFloatProvider(0.7F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> BLOOD = register(
		"blood",
		()
			-> (new ImpactEffect.Builder())
				   .withName("blood")
				   .withTexture("textures/effect/blood.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(20000L)
				   .withRotations(0.0D)
				   .withSprites(2, 2, 0, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withCount(new ParticleValueProviders.DamageBasedParticleCountProvider(100, 20.0F))
				   .withVelocityProvider(VelocityProviders.hemisphereVelocityProvider(
					   0.30000001192092896D, VelocityProviders.Distribution.TIGHT))
				   .withGlow(false)
				   .withDepthTest(true)
				   .withGravity(2.0F)
				   .withAlphaProvider(new Interpolators.AnotherEaseInEaseOutFloatProvider(1.0F, 0.01F, 0.5F))
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(0.15F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> BLOODPURPLE = register(
		"bloodpurple",
		()
			-> (new ImpactEffect.Builder())
				   .withName("bloodpurple")
				   .withTexture("textures/effect/bloodpurple.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(20000L)
				   .withRotations(0.0D)
				   .withSprites(2, 2, 0, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withCount(new ParticleValueProviders.DamageBasedParticleCountProvider(50, 20.0F))
				   .withVelocityProvider(VelocityProviders.hemisphereVelocityProvider(
					   0.30000001192092896D, VelocityProviders.Distribution.TIGHT))
				   .withGlow(false)
				   .withDepthTest(true)
				   .withGravity(2.0F)
				   .withAlphaProvider(new Interpolators.AnotherEaseInEaseOutFloatProvider(1.0F, 0.01F, 0.5F))
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(0.15F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> BLOODGREEN = register(
		"bloodgreen",
		()
			-> (new ImpactEffect.Builder())
				   .withName("bloodgreen")
				   .withTexture("textures/effect/bloodgreen.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(20000L)
				   .withRotations(0.0D)
				   .withSprites(2, 2, 0, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withCount(new ParticleValueProviders.DamageBasedParticleCountProvider(50, 20.0F))
				   .withVelocityProvider(VelocityProviders.hemisphereVelocityProvider(
					   0.30000001192092896D, VelocityProviders.Distribution.TIGHT))
				   .withGlow(false)
				   .withDepthTest(true)
				   .withGravity(2.0F)
				   .withAlphaProvider(new Interpolators.AnotherEaseInEaseOutFloatProvider(1.0F, 0.01F, 0.5F))
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(0.15F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> BLOODBLUE = register(
		"bloodblue",
		()
			-> (new ImpactEffect.Builder())
				   .withName("bloodblue")
				   .withTexture("textures/effect/bloodblue.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(20000L)
				   .withRotations(0.0D)
				   .withSprites(2, 2, 0, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withCount(new ParticleValueProviders.DamageBasedParticleCountProvider(50, 20.0F))
				   .withVelocityProvider(VelocityProviders.hemisphereVelocityProvider(
					   0.30000001192092896D, VelocityProviders.Distribution.TIGHT))
				   .withGlow(false)
				   .withDepthTest(true)
				   .withGravity(2.0F)
				   .withAlphaProvider(new Interpolators.AnotherEaseInEaseOutFloatProvider(1.0F, 0.01F, 0.5F))
				   .withWidthProvider(new Interpolators.ConstantFloatProvider(0.15F)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> BLOOD_MIST_RED = register(
		"blood_mist",
		()
			-> (new ImpactEffect.Builder())
				   .withName("blood_mist")
				   .withTexture("textures/effect/blood2.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(3000L)
				   .withRotations(0.009999999776482582D)
				   .withSprites(1, 1, 0, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withCount(new ParticleValueProviders.RandomParticleCountProvider(1, 10))
				   .withVelocityProvider(
					   VelocityProviders.sphereVelocityProvider(0.03F, VelocityProviders.Distribution.NORMAL))
				   .withGlow(false)
				   .withDepthTest(true)
				   .withGravity(0.01F)
				   .withAlphaProvider(new Interpolators.AnotherEaseInEaseOutFloatProvider(1.0F, 0.01F, 0.5F))
				   .withWidth(new ParticleValueProviders.BoundingBoxBasedParticleWidthProvider(1.5D, 1.0D)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> BLOOD_MIST_PURPLE = register(
		"blood_mist_purple",
		()
			-> (new ImpactEffect.Builder())
				   .withName("blood_mist_purple")
				   .withTexture("textures/effect/bloodmistpurple.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(3000L)
				   .withRotations(0.009999999776482582D)
				   .withSprites(1, 1, 0, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withCount(new ParticleValueProviders.RandomParticleCountProvider(1, 10))
				   .withVelocityProvider(
					   VelocityProviders.sphereVelocityProvider(0.03F, VelocityProviders.Distribution.NORMAL))
				   .withGlow(false)
				   .withDepthTest(true)
				   .withGravity(0.01F)
				   .withAlphaProvider(new Interpolators.AnotherEaseInEaseOutFloatProvider(1.0F, 0.01F, 0.5F))
				   .withWidth(new ParticleValueProviders.BoundingBoxBasedParticleWidthProvider(1.5D, 1.0D)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> BLOOD_MIST_GREEN = register(
		"blood_mist_green",
		()
			-> (new ImpactEffect.Builder())
				   .withName("blood_mist_green")
				   .withTexture("textures/effect/bloodmistgreen.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(3000L)
				   .withRotations(0.009999999776482582D)
				   .withSprites(1, 1, 0, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withCount(new ParticleValueProviders.RandomParticleCountProvider(1, 10))
				   .withVelocityProvider(
					   VelocityProviders.sphereVelocityProvider(0.03F, VelocityProviders.Distribution.NORMAL))
				   .withGlow(false)
				   .withDepthTest(true)
				   .withGravity(0.01F)
				   .withAlphaProvider(new Interpolators.AnotherEaseInEaseOutFloatProvider(1.0F, 0.01F, 0.5F))
				   .withWidth(new ParticleValueProviders.BoundingBoxBasedParticleWidthProvider(1.5D, 1.0D)));
	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> BLOOD_MIST_BLUE = register(
		"blood_mist_blue",
		()
			-> (new ImpactEffect.Builder())
				   .withName("blood_mist_blue")
				   .withTexture("textures/effect/bloodmistblue.png")
				   .withBlendMode(Effect.BlendMode.NORMAL)
				   .withDuration(3000L)
				   .withRotations(0.009999999776482582D)
				   .withSprites(1, 1, 0, AbstractEffect.SpriteAnimationType.RANDOM)
				   .withCount(new ParticleValueProviders.RandomParticleCountProvider(1, 10))
				   .withVelocityProvider(
					   VelocityProviders.sphereVelocityProvider(0.03F, VelocityProviders.Distribution.NORMAL))
				   .withGlow(false)
				   .withDepthTest(true)
				   .withGravity(0.01F)
				   .withAlphaProvider(new Interpolators.AnotherEaseInEaseOutFloatProvider(1.0F, 0.01F, 0.5F))
				   .withWidth(new ParticleValueProviders.BoundingBoxBasedParticleWidthProvider(1.5D, 1.0D)));

	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> register(
		String name, Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier) {
		if (suppliers.put(name, supplier) != null) {
			throw new IllegalArgumentException("Duplicate effect: " + name);
		} else {
			UUID effectId = getEffectId(name);
			Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> existingEffectSupplier =
				effectSuppliersById.put(effectId, supplier);
			if (existingEffectSupplier != null) {
				throw new IllegalArgumentException(
					"Effect id collision for effect '" + name + "'. Try assigning a different name");
			} else {
				return supplier;
			}
		}
	}

	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> getEffectBuilderSupplier(UUID effectId) {
		Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier = effectSuppliersById.get(effectId);
		if (supplier == null) {
			throw new IllegalArgumentException("Effect with id " + effectId + " not found");
		} else {
			return supplier;
		}
	}

	public static UUID getEffectId(String name) {
		return UUID.nameUUIDFromBytes(name.getBytes());
	}

	public static Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> getEffectBuilderSupplier(String name) {
		Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier = suppliers.get(name);
		if (supplier == null) {
			throw new IllegalArgumentException("Effect '" + name + "' not found");
		} else {
			return supplier;
		}
	}

	private static List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> getEntityEffects(
		Entity entity,
		Map<Class<? extends Entity>, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>> effects) {
		if (!Config.goreEnabled) {
			return Collections.emptyList();
		} else {
			List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> exactMatchResults = null;
			List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> baseMatchResults = null;

			for (Entry<Class<? extends Entity>, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>> e :
				 effects.entrySet()) {
				if (e.getKey() == entity.getClass()) {
					exactMatchResults = e.getValue();
					break;
				}

				if (baseMatchResults == null && e.getKey().isAssignableFrom(entity.getClass())) {
					baseMatchResults = e.getValue();
				}
			}

			if (exactMatchResults != null) {
				return exactMatchResults;
			} else {
				return baseMatchResults != null ? baseMatchResults : Collections.emptyList();
			}
		}
	}

	public static List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>
	getEntityDeathEffects(LivingEntity entity) {
		return getEntityEffects(entity, entityDeathEffects);
	}

	public static List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> getEntityHitEffects(Entity entity) {
		return getEntityEffects(entity, entityHitEffects);
	}

	static {
		entityDeathEffects.put(Player.class, List.of(BLOOD_MIST_RED));
		entityDeathEffects.put(EnderMan.class, List.of(BLOOD_MIST_PURPLE));
		entityDeathEffects.put(Endermite.class, List.of(BLOOD_MIST_PURPLE));
		entityDeathEffects.put(Shulker.class, List.of(BLOOD_MIST_PURPLE));
		entityDeathEffects.put(EnderDragon.class, List.of(BLOOD_MIST_PURPLE));
		entityDeathEffects.put(Slime.class, List.of(BLOOD_MIST_GREEN));
		entityDeathEffects.put(Creeper.class, List.of(BLOOD_MIST_GREEN));
		entityDeathEffects.put(Warden.class, List.of(BLOOD_MIST_BLUE));
		entityDeathEffects.put(LivingEntity.class, List.of(BLOOD_MIST_RED));
		entityDeathEffects.put(Skeleton.class, Collections.emptyList());
		entityHitEffects.put(Player.class, List.of(BLOOD));
		entityHitEffects.put(LivingEntity.class, List.of(BLOOD));
		entityHitEffects.put(EnderMan.class, List.of(BLOODPURPLE));
		entityHitEffects.put(Endermite.class, List.of(BLOODPURPLE));
		entityHitEffects.put(Shulker.class, List.of(BLOODPURPLE));
		entityHitEffects.put(EnderDragon.class, List.of(BLOODPURPLE));
		entityHitEffects.put(Slime.class, List.of(BLOODGREEN));
		entityHitEffects.put(Creeper.class, List.of(BLOODGREEN));
		entityHitEffects.put(Warden.class, List.of(BLOODBLUE));
		entityHitEffects.put(Skeleton.class, Collections.emptyList());
	}
}
