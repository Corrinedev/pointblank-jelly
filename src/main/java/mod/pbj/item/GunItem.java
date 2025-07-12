package mod.pbj.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import mod.pbj.Config;
import mod.pbj.Nameable;
import mod.pbj.attachment.Attachment;
import mod.pbj.attachment.AttachmentCategory;
import mod.pbj.attachment.AttachmentHost;
import mod.pbj.attachment.Attachments;
import mod.pbj.client.*;
import mod.pbj.client.GunClientState.FireState;
import mod.pbj.client.GunStatePoseProvider.PoseContext;
import mod.pbj.client.controller.*;
import mod.pbj.client.effect.AbstractEffect.SpriteAnimationType;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.client.effect.EffectLauncher;
import mod.pbj.client.effect.MuzzleFlashEffect;
import mod.pbj.client.render.GunItemRenderer;
import mod.pbj.crafting.Craftable;
import mod.pbj.entity.ProjectileBulletEntity;
import mod.pbj.entity.ProjectileLike;
import mod.pbj.feature.*;
import mod.pbj.network.*;
import mod.pbj.registry.*;
import mod.pbj.script.Script;
import mod.pbj.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.TriPredicate;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.ClientUtils;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GunItem extends HurtingItem implements ScriptHolder, Craftable, AttachmentHost, Nameable, GeoItem,
													LockableTarget.TargetLocker, Tradeable, SlotFeature.SlotHolder {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private static final String DEFAULT_ANIMATION_IDLE = "animation.model.idle";
	private static final String DEFAULT_ANIMATION_RELOAD = "animation.model.reload";
	private static final String DEFAULT_ANIMATION_INSPECT = "animation.model.inspect";
	private static final String DEFAULT_ANIMATION_DRAW = "animation.model.draw";
	private static final String DEFAULT_ANIMATION_ENABLE_FIRE_MODE = "animation.model.enablefiremode";
	private static final String DEFAULT_ANIMATION_COMPLETE_FIRE = "animation.model.completefire";
	private static final String DEFAULT_ANIMATION_PREPARE_FIRE = "animation.model.preparefire";
	public static final String DEFAULT_ANIMATION_FIRE = "animation.model.fire";
	public static final String DEFAULT_ANIMATION_OFF_GROUND = "animation.model.off_ground";
	public static final String DEFAULT_ANIMATION_OFF_GROUND_SPRINTING = "animation.model.off_ground_sprinting";
	public static final String DEFAULT_ANIMATION_STANDING = "animation.model.standing";
	public static final String DEFAULT_ANIMATION_WALKING = "animation.model.walking";
	public static final String DEFAULT_ANIMATION_CROUCHING = "animation.model.crouching";
	public static final String DEFAULT_ANIMATION_WALKING_AIMING = "animation.model.walking_aiming";
	public static final String DEFAULT_ANIMATION_WALKING_BACKWARDS = "animation.model.walking_backwards";
	public static final String DEFAULT_ANIMATION_WALKING_LEFT = "animation.model.walking_left";
	public static final String DEFAULT_ANIMATION_WALKING_RIGHT = "animation.model.walking_right";
	public static final String DEFAULT_ANIMATION_RUNNING = "animation.model.running";
	public static final String DEFAULT_ANIMATION_PREPARE_RUNNING = "animation.model.runningstart";
	public static final String DEFAULT_ANIMATION_COMPLETE_RUNNING = "animation.model.runningend";
	public static final int INFINITE_AMMO = Integer.MAX_VALUE;
	public static final String DEFAULT_RETICLE_OVERLAY = "textures/item/reticle.png";
	public static final String DEFAULT_RETICLE_OVERLAY_PARALLAX = "textures/item/reticle4.png";
	public static final RawAnimation RAW_ANIMATION_OFF_GROUND =
		RawAnimation.begin().thenPlay("animation.model.off_ground");
	public static final RawAnimation RAW_ANIMATION_OFF_GROUND_SPRINTING =
		RawAnimation.begin().thenPlay("animation.model.off_ground_sprinting");
	public static final RawAnimation RAW_ANIMATION_STANDING = RawAnimation.begin().thenPlay("animation.model.standing");
	public static final RawAnimation RAW_ANIMATION_WALKING = RawAnimation.begin().thenPlay("animation.model.walking");
	public static final RawAnimation RAW_ANIMATION_CROUCHING =
		RawAnimation.begin().thenPlay("animation.model.crouching");
	public static final RawAnimation RAW_ANIMATION_WALKING_AIMING =
		RawAnimation.begin().thenPlay("animation.model.walking_aiming");
	public static final RawAnimation RAW_ANIMATION_WALKING_BACKWARDS =
		RawAnimation.begin().thenPlay("animation.model.walking_backwards");
	public static final RawAnimation RAW_ANIMATION_WALKING_LEFT =
		RawAnimation.begin().thenPlay("animation.model.walking_left");
	public static final RawAnimation RAW_ANIMATION_WALKING_RIGHT =
		RawAnimation.begin().thenPlay("animation.model.walking_right");
	public static final RawAnimation RAW_ANIMATION_PREPARE_RUNNING =
		RawAnimation.begin().thenPlay("animation.model.runningstart");
	public static final RawAnimation RAW_ANIMATION_COMPLETE_RUNNING =
		RawAnimation.begin().thenPlay("animation.model.runningend");
	public static final RawAnimation RAW_ANIMATION_RUNNING = RawAnimation.begin().thenPlay("animation.model.running");
	private static final List<ResourceLocation> FALLBACK_COMMON_ANIMATIONS =
		List.of(ResourceLocation.fromNamespaceAndPath("pointblank", "common"));
	private static final List<ResourceLocation> FALLBACK_PISTOL_ANIMATIONS = List.of(
		ResourceLocation.fromNamespaceAndPath("pointblank", "pistol"),
		ResourceLocation.fromNamespaceAndPath("pointblank", "common"));
	private static final Random random = new Random();
	private static final double MAX_SHOOTING_DISTANCE_WITHOUT_AIMING = 100.0F;
	private static final ResourceLocation DEFAULT_SCOPE_OVERLAY =
		ResourceLocation.fromNamespaceAndPath("pointblank", "textures/gui/scope.png");
	private static final int MAX_ATTACHMENT_CATEGORIES = 11;
	private final String name;
	private final String nameSpace;
	private final float tradePrice;
	private final int tradeLevel;
	private final int tradeBundleQuantity;
	private final int maxAmmoCapacity;
	private boolean requiresPhasedReload;
	private final boolean isAimingEnabled;
	private final int rpm;
	private final long prepareIdleCooldownDuration;
	private final long prepareFireCooldownDuration;
	private final long completeFireCooldownDuration;
	private final long enableFireModeCooldownDuration;
	private final long targetLockTimeTicks;
	private final Set<Supplier<AmmoItem>> compatibleBullets;
	private final double viewRecoilAmplitude;
	private final double shakeRecoilAmplitude;
	private final int viewRecoilMaxPitch;
	private final long viewRecoilDuration;
	private final double shakeRecoilSpeed;
	private final double shakeDecay;
	private final long shakeRecoilDuration;
	private final double gunRecoilInitialAmplitude;
	private final double gunRecoilRateOfAmplitudeDecay;
	private final double gunRecoilInitialAngularFrequency;
	private final double gunRecoilRateOfFrequencyIncrease;
	private final double gunRecoilPitchMultiplier;
	private final long gunRecoilDuration;
	private final int shotsPerRecoil;
	private final int shotsPerTrace;
	private final long idleRandomizationDuration;
	private final long recoilRandomizationDuration;
	private final double gunRandomizationAmplitude;
	private final int burstShots;
	private final SoundEvent fireSound;
	private final float fireSoundVolume;
	private final SoundEvent targetLockedSound;
	private final SoundEvent targetStartLockingSound;
	private final long reloadCooldownTime;
	private final float bobbing;
	private final float bobbingOnAim;
	private final float bobbingRollMultiplier;
	private final double jumpMultiplier;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final double aimingCurveX;
	private final double aimingCurveY;
	private final double aimingCurveZ;
	private final double aimingCurvePitch;
	private final double aimingCurveYaw;
	private final double aimingCurveRoll;
	private final double aimingZoom;
	private final double pipScopeZoom;
	private final ResourceLocation scopeOverlay;
	private final ResourceLocation targetLockOverlay;
	private final ResourceLocation modelResourceLocation;
	private List<PhasedReload> phasedReloads;
	private AnimationProvider drawAnimationProvider;
	private AnimationProvider inspectAnimationProvider;
	private AnimationProvider idleAnimationProvider;
	private final String reloadAnimation;
	private int pelletCount;
	private final double pelletSpread;
	private final double inaccuracy;
	private final double inaccuracyAiming;
	private final double inaccuracySprinting;
	public long adsSpeed;
	private final List<Tuple<Long, AbstractProceduralAnimationController>> reloadEffectControllers;
	private final List<GlowAnimationController.Builder> glowEffectBuilders;
	private final List<RotationAnimationController.Builder> rotationEffectBuilders;
	private final EffectLauncher effectLauncher;
	private final BulletData bulletData;
	private final float modelScale;
	private final boolean hitscan;
	private final List<Supplier<Attachment>> compatibleAttachmentSuppliers;
	private final List<String> compatibleAttachmentGroups;
	private Set<Attachment> compatibleAttachments;
	private final List<Supplier<Attachment>> defaultAttachmentSuppliers;
	private final long craftingDuration;
	private final Map<Class<? extends Feature>, Feature> features;
	private final AnimationType animationType;
	private ResourceLocation firstPersonFallbackAnimations;
	private final String thirdPersonFallbackAnimations;
	@Nullable private Script script = null;

	public GunItem(Builder builder, String namespace) {
		super(new Properties(), builder);
		this.name = builder.name;
		this.nameSpace = builder.extension.getName();
		if (this.name.contains(":")) {
			this.modelResourceLocation = ResourceLocation.parse(this.name);
		} else {
			this.modelResourceLocation = ResourceLocation.fromNamespaceAndPath(namespace, this.name);
		}

		this.modelScale = builder.modelScale;
		this.tradePrice = builder.tradePrice;
		this.tradeLevel = builder.tradeLevel;
		this.hitscan = builder.hitscan;
		this.adsSpeed = builder.adsSpeed;
		this.tradeBundleQuantity = builder.tradeBundleQuantity;
		this.script = builder.mainScript;
		this.maxAmmoCapacity = builder.maxAmmoCapacity;
		this.rpm = builder.rpm;
		this.isAimingEnabled = builder.isAimingEnabled;
		this.compatibleBullets = builder.compatibleAmmo != null && !builder.compatibleAmmo.isEmpty()
									 ? builder.compatibleAmmo
									 : Collections.emptySet();
		this.targetLockTimeTicks = builder.targetLockTimeTicks;
		this.bulletData = builder.bulletData;
		this.viewRecoilAmplitude = builder.viewRecoilAmplitude;
		this.shakeRecoilAmplitude = builder.shakeRecoilAmplitude;
		this.viewRecoilMaxPitch = builder.viewRecoilMaxPitch;
		this.viewRecoilDuration = builder.viewRecoilDuration;
		this.shakeRecoilSpeed = builder.shakeRecoilSpeed;
		this.shakeRecoilDuration = builder.shakeRecoilDuration;
		this.shakeDecay = builder.shakeDecay;
		this.gunRecoilInitialAmplitude = builder.gunRecoilInitialAmplitude;
		this.gunRecoilRateOfAmplitudeDecay = builder.gunRecoilRateOfAmplitudeDecay;
		this.gunRecoilInitialAngularFrequency = builder.gunRecoilInitialAngularFrequency;
		this.gunRecoilRateOfFrequencyIncrease = builder.gunRecoilRateOfFrequencyIncrease;
		this.gunRecoilPitchMultiplier = builder.gunRecoilPitchMultiplier;
		this.gunRecoilDuration = builder.gunRecoilDuration;
		this.shotsPerRecoil = builder.shotsPerRecoil;
		this.shotsPerTrace = builder.shotsPerTrace;
		this.gunRandomizationAmplitude = builder.gunRandomizationAmplitude;
		this.idleRandomizationDuration = builder.idleRandomizationDuration;
		this.recoilRandomizationDuration = builder.recoilRandomizationDuration;
		this.jumpMultiplier = builder.jumpMultiplier;
		this.burstShots = builder.burstShots;
		this.fireSound = builder.fireSound != null ? builder.fireSound.get() : null;
		this.fireSoundVolume = builder.fireSoundVolume;
		this.prepareIdleCooldownDuration = builder.prepareIdleCooldownDuration;
		this.prepareFireCooldownDuration = builder.prepareFireCooldownDuration;
		this.completeFireCooldownDuration = builder.completeFireCooldownDuration;
		this.enableFireModeCooldownDuration = builder.enableFireModeCooldownDuration;
		this.craftingDuration = builder.craftingDuration;
		this.aimingCurveX = builder.aimingCurveX;
		this.aimingCurveY = builder.aimingCurveY;
		this.aimingCurveZ = builder.aimingCurveZ;
		this.aimingCurvePitch = builder.aimingCurvePitch;
		this.aimingCurveYaw = builder.aimingCurveYaw;
		this.aimingCurveRoll = builder.aimingCurveRoll;
		this.pipScopeZoom = builder.pipScopeZoom;
		this.aimingZoom = builder.aimingZoom;
		this.scopeOverlay = builder.scopeOverlay != null
								? ResourceLocation.fromNamespaceAndPath("pointblank", builder.scopeOverlay)
								: null;
		this.targetLockOverlay = builder.targetLockOverlay != null
									 ? ResourceLocation.fromNamespaceAndPath("pointblank", builder.targetLockOverlay)
									 : null;
		this.targetLockedSound = builder.targetLockedSound != null ? builder.targetLockedSound.get() : null;
		this.targetStartLockingSound =
			builder.targetStartLockingSound != null ? builder.targetStartLockingSound.get() : null;
		this.bobbing = builder.bobbing;
		this.bobbingOnAim = builder.bobbingOnAim;
		this.bobbingRollMultiplier = builder.bobbingRollMultiplier;
		this.reloadEffectControllers = Collections.unmodifiableList(builder.reloadEffectControllers);
		this.phasedReloads = builder.phasedReloads;
		if (!builder.drawAnimationsBuilder.getAnimations().isEmpty()) {
			this.drawAnimationProvider = builder.drawAnimationsBuilder.build();
		}

		if (!builder.inspectAnimationsBuilder.getAnimations().isEmpty()) {
			this.inspectAnimationProvider = builder.inspectAnimationsBuilder.build();
		}

		if (!builder.idleAnimationBuilder.getAnimations().isEmpty()) {
			this.idleAnimationProvider = builder.idleAnimationBuilder.build();
		}

		this.animationType = builder.animationType;
		if (builder.firstPersonFallbackAnimations != null) {
			this.firstPersonFallbackAnimations =
				ResourceLocation.fromNamespaceAndPath("pointblank", builder.firstPersonFallbackAnimations);
		}

		this.thirdPersonFallbackAnimations = builder.thirdPersonFallbackAnimations;
		this.pelletSpread = builder.pelletSpread;
		if (builder.pelletCount > 1) {
			this.maxShootingDistance = Math.min(this.maxShootingDistance, 50.0F);
		}

		this.inaccuracyAiming = builder.inaccuracyAiming;
		this.inaccuracy = builder.inaccuracy;
		this.inaccuracySprinting = builder.inaccuracySprinting;
		this.reloadCooldownTime = builder.reloadCooldownTime;
		this.reloadAnimation = builder.reloadAnimation;

		if (hasFunction("overrideReloads")) {
			this.phasedReloads = (List<PhasedReload>)invokeFunction("overrideReloads", builder);
			System.out.println("reloads = " + phasedReloads);
			this.requiresPhasedReload = true;
		}
		if (!requiresPhasedReload) {
			if (this.phasedReloads.isEmpty() && this.reloadAnimation != null) {
				this.phasedReloads.add(
					new PhasedReload(ReloadPhase.RELOADING, this.reloadCooldownTime, this.reloadAnimation));
			} else {
				this.requiresPhasedReload = true;
			}
		}

		this.compatibleAttachmentSuppliers = Collections.unmodifiableList(builder.compatibleAttachments);
		this.compatibleAttachmentGroups = Collections.unmodifiableList(builder.compatibleAttachmentGroups);
		this.defaultAttachmentSuppliers = Collections.unmodifiableList(builder.defaultAttachments);
		Map<Class<? extends Feature>, Feature> features = new HashMap<>();

		for (FeatureBuilder<?, ?> featureBuilder : builder.featureBuilders) {
			Feature feature = featureBuilder.build(this);
			features.put(feature.getClass(), feature);
		}

		ActiveMuzzleFeature activeMuzzleFeature = (ActiveMuzzleFeature)features.get(ActiveMuzzleFeature.class);
		if (activeMuzzleFeature == null) {
			FeatureBuilder<?, ?> activeMuzzleFeatureBuilder =
				(new ActiveMuzzleFeature.Builder())
					.withCondition(Conditions.isUsingDefaultMuzzle().and(
						Conditions.doesNotHaveAttachmentInCategory(AttachmentCategory.MUZZLE)));
			features.put(ActiveMuzzleFeature.class, activeMuzzleFeatureBuilder.build(this));
		}

		FireModeFeature fireModeFeature = (FireModeFeature)features.get(FireModeFeature.class);
		if (fireModeFeature == null) {
			FireModeFeature.Builder fireModeFeatureBuilder = new FireModeFeature.Builder();
			if (builder.fireModes != null) {
				for (FireMode fireMode : builder.fireModes) {
					AnimationProvider fireAnimationProvider = builder.fireAnimationsBuilder.build();
					fireModeFeatureBuilder.withFireMode(
						(new FireModeFeature.FireModeDescriptor.Builder())
							.withName(fireMode.name())
							.withType(fireMode)
							.withDisplayName(Component.translatable(
								String.format("label.%s.fireMode.%s", "pointblank", fireMode.name().toLowerCase())))
							.withMaxAmmoCapacity(this.maxAmmoCapacity)
							.withRpm(builder.rpm)
							.withBurstShots(builder.burstShots)
							.withDamage(this.getDamage())
							.withMaxShootingDistance((int)this.maxShootingDistance)
							.withPelletCount(builder.pelletCount)
							.withPelletSpread(builder.pelletSpread)
							.withIsUsingDefaultMuzzle(true)
							.withFireAnimationProvider(fireAnimationProvider)
							.build());
				}
			}

			features.put(FireModeFeature.class, fireModeFeatureBuilder.build(this));
		}

		MuzzleFlashFeature muzzleFlashFeature = (MuzzleFlashFeature)features.get(MuzzleFlashFeature.class);
		if (muzzleFlashFeature == null) {
			MuzzleFlashFeature.Builder muzzleFlashFeatureBulder = new MuzzleFlashFeature.Builder();
			List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> fbsl =
				builder.effectBuilders.get(FirePhase.FIRING);
			if (fbsl != null) {
				for (Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> s : fbsl) {
					EffectBuilder<? extends EffectBuilder<?, ?>, ?> eb = s.get();
					if (eb instanceof MuzzleFlashEffect.Builder) {
						muzzleFlashFeatureBulder.withEffect(FirePhase.FIRING, s);
					}
				}
			}

			muzzleFlashFeature = muzzleFlashFeatureBulder.build(this);
			features.put(MuzzleFlashFeature.class, muzzleFlashFeature);
		}

		ReticleFeature reticleFeature = (ReticleFeature)features.get(ReticleFeature.class);
		if (reticleFeature == null && builder.reticleOverlay != null &&
			!MiscUtil.isGreaterThanZero(builder.pipScopeZoom)) {
			features.put(
				ReticleFeature.class, (new ReticleFeature.Builder()).withTexture(builder.reticleOverlay).build(this));
		}

		AimingFeature aimingFeature = (AimingFeature)features.get(AimingFeature.class);
		if (aimingFeature == null && builder.isAimingEnabled) {
			features.put(AimingFeature.class, (new AimingFeature.Builder()).withZoom(this.aimingZoom).build(this));
		}

		PipFeature pipFeature = (PipFeature)features.get(PipFeature.class);
		if (pipFeature == null && MiscUtil.isGreaterThanZero(builder.pipScopeZoom)) {
			features.put(
				PipFeature.class,
				(new PipFeature.Builder())
					.withZoom(builder.pipScopeZoom)
					.withOverlayTexture(builder.reticleOverlay)
					.build(this));
		}

		ReloadFeature reloadFeature = (ReloadFeature)features.get(ReloadFeature.class);
		if (reloadFeature == null) {
			features.put(
				ReloadFeature.class,
				(new ReloadFeature.Builder())
					.withMaxAmmoPerReloadIteration(builder.maxAmmoPerReloadIteration)
					.build(this));
		}

		this.glowEffectBuilders = builder.glowEffectBuilders;
		this.rotationEffectBuilders = builder.rotationEffectBuilders;
		this.effectLauncher = new EffectLauncher(builder.effectBuilders);
		if (!this.glowEffectBuilders.isEmpty() && !features.containsKey(GlowFeature.class)) {
			features.put(GlowFeature.class, new GlowFeature());
		}

		this.features = Collections.unmodifiableMap(features);
		SingletonGeoAnimatable.registerSyncedAnimatable(this);
	}

	public String getName() {
		return this.name;
	}

	public Component getName(ItemStack itemStack) {
		return Component.translatable(this.getDescriptionId(itemStack));
	}

	public float getModelScale() {
		return this.modelScale;
	}

	public Collection<Feature> getFeatures() {
		return this.features.values();
	}

	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
		FireModeInstance currentFireMode = getFireModeInstance(stack);
		if (currentFireMode != null && currentFireMode.getPelletCount() > 0) {
			tooltip.add(Component.translatable("label.pointblank.damage")
							.append(": ")
							.append(String.format(
								"%.2f (%.2fx%d)",
								(currentFireMode.getDamage() * currentFireMode.getPelletCount()),
								currentFireMode.getDamage(),
								currentFireMode.getPelletCount()))
							.withStyle(ChatFormatting.RED)
							.withStyle(ChatFormatting.ITALIC));
		} else if (currentFireMode != null) {
			tooltip.add(Component.translatable("label.pointblank.damage")
							.append(": ")
							.append(String.format("%.2f", currentFireMode.getDamage()))
							.withStyle(ChatFormatting.RED)
							.withStyle(ChatFormatting.ITALIC));
		} else {
			tooltip.add(Component.translatable("label.pointblank.damage")
							.append(": ")
							.append(String.format("%.2f", this.getDamage()))
							.withStyle(ChatFormatting.RED)
							.withStyle(ChatFormatting.ITALIC));
		}
		tooltip.add(Component.translatable("label.pointblank.rpm")
						.append(": ")
						.append(String.format("%d", this.rpm))
						.withStyle(ChatFormatting.RED)
						.withStyle(ChatFormatting.ITALIC));
		MutableComponent ammoDescription = Component.translatable("label.pointblank.ammo").append(": ");
		boolean isFirst = true;

		for (Supplier<AmmoItem> ammoItemSupplier : this.compatibleBullets) {
			if (!isFirst) {
				ammoDescription.append(", ");
			}

			AmmoItem ammoItem = ammoItemSupplier.get();
			if (ammoItem != null) {
				ammoDescription.append(Component.translatable(ammoItemSupplier.get().getDescriptionId()));
			} else {
				ammoDescription.append(Component.translatable("missing_ammo"));
			}

			isFirst = false;
		}

		ammoDescription.withStyle(ChatFormatting.RED).withStyle(ChatFormatting.ITALIC);
		tooltip.add(ammoDescription);
		if (hasFunction("appendHoverText")) {
			invokeFunction("appendHoverText", stack, world, tooltip, flag);
		}
	}

	public MutableComponent getDisplayName() {
		return Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.YELLOW);
	}

	public boolean requiresPhasedReload() {
		return this.requiresPhasedReload;
	}

	public List<FireModeInstance> getMainFireModes() {
		FireModeFeature fireModeFeature = this.getFeature(FireModeFeature.class);
		return fireModeFeature.getFireModes();
	}

	public int getRpm() {
		return this.rpm;
	}

	public boolean isAimingEnabled() {
		return this.isAimingEnabled;
	}

	public long getPrepareFireCooldownDuration() {
		return this.prepareFireCooldownDuration;
	}

	public long getCompleteFireCooldownDuration() {
		return this.completeFireCooldownDuration;
	}

	public long getEnableFireModeCooldownDuration() {
		return this.enableFireModeCooldownDuration;
	}

	public long getPrepareIdleCooldownDuration() {
		return this.prepareIdleCooldownDuration;
	}

	public int getPelletCount() {
		return this.pelletCount;
	}

	public double getPelletSpread() {
		return this.pelletSpread;
	}

	public long getDrawCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
		if (this.drawAnimationProvider == null) {
			return 0L;
		} else {
			AnimationProvider.Descriptor descriptor =
				this.drawAnimationProvider.getDescriptor(player, itemStack, state);
			return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration()) : 0L;
		}
	}

	public long getInspectCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
		if (this.inspectAnimationProvider == null) {
			return 0L;
		} else {
			AnimationProvider.Descriptor descriptor =
				this.inspectAnimationProvider.getDescriptor(player, itemStack, state);
			return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration()) : 0L;
		}
	}

	public long getIdleCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
		if (this.idleAnimationProvider == null) {
			return 0L;
		} else {
			AnimationProvider.Descriptor descriptor =
				this.idleAnimationProvider.getDescriptor(player, itemStack, state);
			return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration()) : 0L;
		}
	}

	public long
	getReloadingCooldownTime(ReloadPhase phase, LivingEntity player, GunClientState state, ItemStack itemStack) {
		long cooldownTime = 0L;

		for (PhasedReload reload : this.phasedReloads) {
			if (phase == reload.phase && reload.predicate.test(new ConditionContext(player, itemStack, state, null))) {
				cooldownTime = reload.timeUnit.toMillis(reload.cooldownTime);
				break;
			}
		}

		return cooldownTime;
	}

	public int getBurstShots(ItemStack itemStack, FireModeInstance fireModeInstance) {
		FireModeFeature mainFireModeFeature = this.getFeature(FireModeFeature.class);
		if (mainFireModeFeature.getFireModes().contains(fireModeInstance)) {
			int burstShots;
			if (fireModeInstance.getBurstShots() == -1) {
				burstShots = this.burstShots;
			} else {
				burstShots = fireModeInstance.getBurstShots();
			}

			return burstShots;
		} else {
			List<FireModeInstance> allFireModes = getFireModes(itemStack);
			return allFireModes.contains(fireModeInstance) ? fireModeInstance.getBurstShots() : this.burstShots;
		}
	}

	public void cancelReload(ItemStack stack) {}

	public int getMaxAmmoCapacity(ItemStack itemStack, FireModeInstance fireModeInstance) {
		FireModeFeature mainFireModeFeature = this.getFeature(FireModeFeature.class);
		if (mainFireModeFeature.getFireModes().contains(fireModeInstance)) {
			int ammoCapacity;
			if (fireModeInstance.getMaxAmmoCapacity() == Integer.MAX_VALUE) {
				ammoCapacity = Integer.MAX_VALUE;
				return AmmoCapacityFeature.modifyAmmoCapacity(itemStack, ammoCapacity);
			}
			if (fireModeInstance.getAmmo() == AmmoRegistry.DEFAULT_AMMO_POOL.get()) {
				ammoCapacity = this.maxAmmoCapacity;
			} else {
				ammoCapacity = fireModeInstance.getMaxAmmoCapacity();
			}

			return AmmoCapacityFeature.modifyAmmoCapacity(itemStack, ammoCapacity);
		} else {
			List<FireModeInstance> allFireModes = getFireModes(itemStack);
			if (allFireModes.contains(fireModeInstance)) {
				int ammoCapacity;
				if (fireModeInstance.getAmmo() == AmmoRegistry.DEFAULT_AMMO_POOL.get()) {
					ammoCapacity = this.maxAmmoCapacity;
				} else {
					ammoCapacity = fireModeInstance.getMaxAmmoCapacity();
				}

				return AmmoCapacityFeature.modifyAmmoCapacity(itemStack, ammoCapacity);
			} else {
				return 0;
			}
		}
	}

	public ResourceLocation getScopeOverlay() {
		return this.scopeOverlay == null && MiscUtil.isGreaterThanZero(this.pipScopeZoom) && !Config.pipScopesEnabled
			? DEFAULT_SCOPE_OVERLAY
			: this.scopeOverlay;
	}

	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private GunItemRenderer renderer;

			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (this.renderer == null) {
					List<ResourceLocation> fallbackAnimations;
					if (GunItem.this.firstPersonFallbackAnimations != null) {
						fallbackAnimations = new ArrayList<>();
						fallbackAnimations.add(GunItem.this.firstPersonFallbackAnimations);
						fallbackAnimations.addAll(GunItem.this.animationType.getFallbackFirstPersonAnimations());
					} else {
						fallbackAnimations = GunItem.this.animationType.getFallbackFirstPersonAnimations();
					}

					this.renderer = new GunItemRenderer(
						GunItem.this.modelResourceLocation, fallbackAnimations, GunItem.this.glowEffectBuilders);
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

	public int getMaxStackSize(ItemStack stack) {
		return 1;
	}

	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return true;
	}

	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		return InteractionResult.SUCCESS;
	}

	public InteractionResult
	interactLivingEntity(ItemStack itemStack, Player player, LivingEntity entity, InteractionHand hand) {
		invokeFunction("interactLivingEntity", itemStack, player, entity, hand);
		return InteractionResult.SUCCESS;
	}

	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		if (hasFunction("onLeftClickEntity"))
			return (boolean)invokeFunction("onLeftClickEntity", stack, player, entity);

		return true;
	}

	public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
		if (hasFunction("onEntitySwing"))
			return (boolean)invokeFunction("onEntitySwing", stack, entity);

		return true;
	}

	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add((new BlendingAnimationController<>(this, "walking", 2, false, (state) -> PlayState.STOP))
							.withTransition("animation.model.standing", "animation.model.walking", 2, false)
							.withTransition("animation.model.standing", "animation.model.walking_backwards", 2, false)
							.withTransition("animation.model.standing", "animation.model.runningstart", 2, false)
							.withTransition("animation.model.walking", "animation.model.runningstart", 2, false)
							.withTransition("animation.model.runningstart", "animation.model.running", 2, false)
							.withTransition("animation.model.runningstart", "animation.model.runningend", 3, false)
							.withTransition("animation.model.running", "animation.model.runningend", 2, false)
							.withTransition("animation.model.runningend", "animation.model.walking", 3, false)
							.withTransition("animation.model.runningend", "animation.model.standing", 3, false)
							.withTransition("animation.model.runningend", "animation.model.runningstart", 2, false)
							.withSpeedProvider((p, c) -> {
								double speed = p.getSpeed();
								AttributeInstance attribute = p.getAttribute(Attributes.MOVEMENT_SPEED);
								double baseSpeed = attribute != null ? attribute.getBaseValue() : speed;
								if (baseSpeed == (double)0.0F) {
									baseSpeed = speed;
								}

								if (p.isSprinting()) {
									baseSpeed += baseSpeed * 0.3;
								}

								double ratio = speed / baseSpeed;
								if (((LocalPlayer)p).isMovingSlowly() || p.isSwimming() || p.isInWater()) {
									ratio *= 0.6;
								}

								return Math.sqrt(Mth.clamp(ratio, 0.1, 10.0F));
							})
							.triggerableAnim("animation.model.walking", RAW_ANIMATION_WALKING)
							.triggerableAnim("animation.model.crouching", RAW_ANIMATION_CROUCHING)
							.triggerableAnim("animation.model.walking_aiming", RAW_ANIMATION_WALKING_AIMING)
							.triggerableAnim("animation.model.walking_backwards", RAW_ANIMATION_WALKING_BACKWARDS)
							.triggerableAnim("animation.model.walking_left", RAW_ANIMATION_WALKING_LEFT)
							.triggerableAnim("animation.model.walking_right", RAW_ANIMATION_WALKING_RIGHT)
							.triggerableAnim("animation.model.runningstart", RAW_ANIMATION_PREPARE_RUNNING)
							.triggerableAnim("animation.model.running", RAW_ANIMATION_RUNNING)
							.triggerableAnim("animation.model.runningend", RAW_ANIMATION_COMPLETE_RUNNING)
							.triggerableAnim("animation.model.standing", RAW_ANIMATION_STANDING)
							.triggerableAnim("animation.model.off_ground", RAW_ANIMATION_OFF_GROUND)
							.triggerableAnim("animation.model.off_ground_sprinting", RAW_ANIMATION_OFF_GROUND_SPRINTING)
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

		for (GunStateAnimationController reloadAnimationController : this.createReloadAnimationControllers()) {
			controllers.add(reloadAnimationController);
		}

		AnimationController<GunItem> fireAnimationController = new GunStateAnimationController(
			this,
			"fire_controller",
			"animation.model.fire",
			(ctx)
				-> ctx.gunClientState().getFireState() == FireState.FIRE_SINGLE ||
					   ctx.gunClientState().getFireState() == FireState.FIRE_AUTO ||
					   ctx.gunClientState().getFireState() == FireState.FIRE_BURST ||
					   ctx.gunClientState().getFireState() == FireState.FIRE_COOLDOWN_SINGLE ||
					   ctx.gunClientState().getFireState() == FireState.FIRE_COOLDOWN_AUTO ||
					   ctx.gunClientState().getFireState() == FireState.FIRE_COOLDOWN_BURST ||
					   this.completeFireCooldownDuration == 0L && ctx.gunClientState().isIdle()) {
			public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
				if (ClientUtil.isFirstPerson(player)) {
					this.scheduleReset(
						player, state, itemStack, FireModeFeature.getFireAnimation(player, state, itemStack));
				}
			}
		};
		fireAnimationController.setSoundKeyframeHandler((event) -> {
			Player player = ClientUtils.getClientPlayer();
			if (player != null) {
				SoundKeyframeData soundKeyframeData = event.getKeyframeData();
				String soundName = soundKeyframeData.getSound();
				SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
				if (soundEvent != null) {
					player.playSound(soundEvent, this.fireSoundVolume, 1.0F);
				}
			}
		});
		controllers.add(fireAnimationController);
		AnimationController<GunItem> prepareFiringAnimationController = new GunStateAnimationController(
			this,
			"prepare_fire_controller",
			"animation.model.preparefire",
			(ctx) -> ctx.gunClientState().isPreparingFiring()) {
			public void onPrepareFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
				if (ClientUtil.isFirstPerson(player)) {
					this.scheduleReset(
						player, state, itemStack, FireModeFeature.getPrepareFireAnimation(player, state, itemStack));
				}
			}
		};
		prepareFiringAnimationController.setSoundKeyframeHandler((event) -> {
			Player player = ClientUtils.getClientPlayer();
			if (player != null) {
				SoundKeyframeData soundKeyframeData = event.getKeyframeData();
				String soundName = soundKeyframeData.getSound();
				SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
				if (soundEvent != null) {
					player.playSound(soundEvent, 1.0F, 1.0F);
				}
			}
		});
		controllers.add(prepareFiringAnimationController);
		AnimationController<GunItem> completeFiringAnimationController = new GunStateAnimationController(
			this,
			"complete_fire_controller",
			"animation.model.completefire",
			(ctx) -> ctx.gunClientState().isCompletingFiring()) {
			public void onCompleteFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
				if (ClientUtil.isFirstPerson(player)) {
					this.scheduleReset(
						player, state, itemStack, FireModeFeature.getCompleteFireAnimation(player, state, itemStack));
				}
			}
		};
		completeFiringAnimationController.setSoundKeyframeHandler((event) -> {
			Player player = ClientUtils.getClientPlayer();
			if (player != null) {
				SoundKeyframeData soundKeyframeData = event.getKeyframeData();
				String soundName = soundKeyframeData.getSound();
				SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
				if (soundEvent != null) {
					player.playSound(soundEvent, 1.0F, 1.0F);
				}
			}
		});
		controllers.add(completeFiringAnimationController);
		AnimationController<GunItem> drawAnimationController = new GunStateAnimationController(
			this, "draw_controller", "animation.model.draw", (ctx) -> ctx.gunClientState().isDrawing()) {
			private String getAnimationName(LivingEntity player, GunClientState state, ItemStack itemStack) {
				if (GunItem.this.drawAnimationProvider == null) {
					return null;
				} else {
					AnimationProvider.Descriptor descriptor =
						GunItem.this.drawAnimationProvider.getDescriptor(player, itemStack, state);
					return descriptor != null ? descriptor.animationName() : null;
				}
			}

			public void onDrawing(LivingEntity player, GunClientState state, ItemStack itemStack) {
				if (ClientUtil.isFirstPerson(player)) {
					this.scheduleReset(player, state, itemStack, this.getAnimationName(player, state, itemStack));
				}
			}
		};
		drawAnimationController.setSoundKeyframeHandler((event) -> {
			Player player = ClientUtils.getClientPlayer();
			if (player != null) {
				SoundKeyframeData soundKeyframeData = event.getKeyframeData();
				String soundName = soundKeyframeData.getSound();
				SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
				if (soundEvent != null) {
					player.playSound(soundEvent, 1.0F, 1.0F);
				}
			}
		});
		controllers.add(drawAnimationController);
		AnimationController<GunItem> inspectAnimationController = new GunStateAnimationController(
			this, "inspect_controller", "animation.model.inspect", (ctx) -> ctx.gunClientState().isInspecting()) {
			private String getAnimationName(LivingEntity player, GunClientState state, ItemStack itemStack) {
				if (GunItem.this.inspectAnimationProvider == null) {
					return null;
				} else {
					AnimationProvider.Descriptor descriptor =
						GunItem.this.inspectAnimationProvider.getDescriptor(player, itemStack, state);
					return descriptor != null ? descriptor.animationName() : null;
				}
			}

			public void onInspecting(LivingEntity player, GunClientState state, ItemStack itemStack) {
				if (ClientUtil.isFirstPerson(player)) {
					this.scheduleReset(player, state, itemStack, this.getAnimationName(player, state, itemStack));
				}
			}
		};
		inspectAnimationController.setSoundKeyframeHandler((event) -> {
			Player player = ClientUtils.getClientPlayer();
			if (player != null) {
				SoundKeyframeData soundKeyframeData = event.getKeyframeData();
				String soundName = soundKeyframeData.getSound();
				SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
				if (soundEvent != null) {
					player.playSound(soundEvent, 1.0F, 1.0F);
				}
			}
		});
		controllers.add(inspectAnimationController);
		AnimationController<GunItem> idleAnimationController = new GunStateAnimationController(
			this,
			"idle_controller",
			"animation.model.idle",
			(ctx)
				-> ctx.gunClientState().getFireState() == FireState.IDLE ||
					   ctx.gunClientState().getFireState() == FireState.IDLE_COOLDOWN) {
			private String getAnimationName(LivingEntity player, GunClientState state, ItemStack itemStack) {
				if (GunItem.this.idleAnimationProvider == null) {
					return null;
				} else {
					AnimationProvider.Descriptor descriptor =
						GunItem.this.idleAnimationProvider.getDescriptor(player, itemStack, state);
					return descriptor != null ? descriptor.animationName() : null;
				}
			}

			public void onIdle(LivingEntity player, GunClientState state, ItemStack itemStack) {
				if (ClientUtil.isFirstPerson(player)) {
					this.scheduleReset(player, state, itemStack, this.getAnimationName(player, state, itemStack));
				}
			}
		};
		idleAnimationController.setSoundKeyframeHandler((event) -> {
			Player player = ClientUtils.getClientPlayer();
			if (player != null) {
				SoundKeyframeData soundKeyframeData = event.getKeyframeData();
				String soundName = soundKeyframeData.getSound();
				SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
				if (soundEvent != null) {
					player.playSound(soundEvent, 1.0F, 1.0F);
				}
			}
		});
		controllers.add(idleAnimationController);
		AnimationController<GunItem> enableFireModeAimationController = new GunStateAnimationController(
			this,
			"enable_fire_mode_controller",
			"animation.model.enablefiremode",
			(ctx) -> ctx.gunClientState().isChangingFireMode()) {
			public void onEnablingFireMode(LivingEntity player, GunClientState state, ItemStack itemStack) {
				if (ClientUtil.isFirstPerson(player)) {
					this.scheduleReset(
						player, state, itemStack, FireModeFeature.getEnableFireModeAnimation(player, state, itemStack));
				}
			}
		};
		enableFireModeAimationController.setSoundKeyframeHandler((event) -> {
			Player player = ClientUtils.getClientPlayer();
			if (player != null) {
				SoundKeyframeData soundKeyframeData = event.getKeyframeData();
				String soundName = soundKeyframeData.getSound();
				SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
				if (soundEvent != null) {
					player.playSound(soundEvent, 1.0F, 1.0F);
				}
			}
		});
		controllers.add(enableFireModeAimationController);
	}

	// reload and Compatible Ammo
	private static boolean isCompatibleBullet(Item ammoItem, ItemStack gunStack, FireModeInstance fireModeInstance) {
		boolean result = false;
		Item item = gunStack.getItem();
		if (item instanceof GunItem gunItem) {
			if (fireModeInstance.hasFunction("isCompatibleBullet"))
				return (boolean)fireModeInstance.invokeFunction(
					"isCompatibleBullet", (AmmoItem)ammoItem, gunStack, fireModeInstance);

			List<Features.EnabledFeature> overrides = Features.getEnabledFeatures(gunStack, AmmoOverrideFeature.class);
			boolean hasOverrideOnly = false;

			for (Features.EnabledFeature ef : overrides) {
				AmmoOverrideFeature feature = (AmmoOverrideFeature)ef.feature();

				if (feature.getOverrideAmmo() == ammoItem) {
					return true; // Munição permitida via override
				}

				if (feature.isOverrideOnly()) {
					hasOverrideOnly = true; // 🟠 Marcar que o override é exclusivo
				}
			}

			// Se tem overrideOnly e não bateu com o ammoItem, não pode usar nenhum outro
			if (hasOverrideOnly) {
				return false;
			}

			List<FireModeInstance> firModeInstances = getFireModes(gunStack);
			if (!firModeInstances.contains(fireModeInstance)) {
				return false;
			} else {
				if (fireModeInstance.isUsingDefaultAmmoPool()) {
					for (Supplier<AmmoItem> compatibleBullet : gunItem.compatibleBullets) {
						if (Objects.equals(compatibleBullet.get(), ammoItem)) {
							result = true;
							break;
						}
					}
				} else {
					result = ammoItem == fireModeInstance.getAmmo();
				}

				return result;
			}
		} else {
			return false;
		}
	}

	public int canReloadGun(ItemStack gunStack, Player player, FireModeInstance fireModeInstance) {
		GunItem gunItem = (GunItem)gunStack.getItem();
		int maxCapacity = gunItem.getMaxAmmoCapacity(gunStack, fireModeInstance);
		int currentAmmo = getAmmo(gunStack, fireModeInstance);
		int ammoNeeded = maxCapacity - currentAmmo;
		if (ammoNeeded <= 0) {
			return 0;
		} else if (player.isCreative()) {
			return ammoNeeded;
		} else {
			int availableBullets = 0;

			for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
				ItemStack itemStack = player.getInventory().getItem(i);
				if (isCompatibleBullet(itemStack.getItem(), gunStack, fireModeInstance)) {
					availableBullets += itemStack.getCount();
				}

				if (availableBullets >= ammoNeeded) {
					break;
				}
			}

			int potentialReloadAmount = Math.min(ammoNeeded, availableBullets);
			return potentialReloadAmount;
		}
	}

	int reloadGun(ItemStack gunStack, Player player, FireModeInstance fireModeInstance) {
		if (!(gunStack.getItem() instanceof GunItem gunItem)) {
			return 0;
		} else {
			if (!gunItem.isEnabled()) {
				return 0;
			} else {
				int maxCapacity = gunItem.getMaxAmmoCapacity(gunStack, fireModeInstance);
				int currentAmmo = getAmmo(gunStack, fireModeInstance);
				int neededAmmo = maxCapacity - currentAmmo;
				if (neededAmmo <= 0) {
					return currentAmmo;
				} else if (player.isCreative()) {
					int newAmmo = currentAmmo + neededAmmo;
					setAmmo(gunStack, fireModeInstance, newAmmo);
					return newAmmo;
				} else {
					int foundAmmoCount = 0;

					for (int i = 0; i < player.getInventory().items.size(); ++i) {
						ItemStack inventoryItem = player.getInventory().items.get(i);
						if (isCompatibleBullet(inventoryItem.getItem(), gunStack, fireModeInstance)) {
							int availableBullets = inventoryItem.getCount();
							if (availableBullets <= neededAmmo) {
								foundAmmoCount += availableBullets;
								neededAmmo -= availableBullets;
								player.getInventory().items.set(i, ItemStack.EMPTY);
							} else {
								inventoryItem.shrink(neededAmmo);
								foundAmmoCount += neededAmmo;
								neededAmmo = 0;
							}

							if (neededAmmo == 0) {
								break;
							}
						}
					}

					int newAmmo = currentAmmo + foundAmmoCount;
					setAmmo(gunStack, fireModeInstance, newAmmo);
					return newAmmo;
				}
			}
		}
	}

	public void handleClientReloadRequest(
		ServerPlayer player,
		ItemStack itemStack,
		UUID clientStateId,
		int slotIndex,
		FireModeInstance fireModeInstance) {
		boolean isOffhand = player.getOffhandItem() == itemStack;
		if (!isOffhand && itemStack != null) {
			int ammo = this.reloadGun(itemStack, player, fireModeInstance);
			UUID itemStackId = getItemStackId(itemStack);
			Network.networkChannel.send(
				PacketDistributor.PLAYER.with(() -> player),
				new ReloadResponsePacket(itemStackId, slotIndex, 0, ammo > 0, ammo, fireModeInstance));
		}
	}

	public static UUID getItemStackId(ItemStack itemStack) {
		CompoundTag idTag = itemStack.getTag();
		return idTag != null ? MiscUtil.getTagId(idTag) : null;
	}

	public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int itemSlot, boolean isSelected) {
		if (entity instanceof Player plr)
			invokeFunction("onInventoryTick", itemStack, level, plr, itemSlot, isSelected);
		boolean isOffhand = entity instanceof Player && ((Player)entity).getOffhandItem() == itemStack;
		if (!level.isClientSide) {
			this.ensureItemStack(itemStack, level, entity, isOffhand);
		} else {
			GunClientState state = null;
			if (entity instanceof Player) {
				state = GunClientState.getState((Player)entity, itemStack, itemSlot, isOffhand);
			}
			if (state != null && entity instanceof Player) {
				state.inventoryTick((Player)entity, itemStack, isSelected);
			}
		}
	}

	public void ensureItemStack(ItemStack itemStack, Level level, Entity entity, boolean isOffhand) {
		GeoItem.getOrAssignId(itemStack, (ServerLevel)level);
		getOrAssignRandomSeed(itemStack);
		CompoundTag stateTag = itemStack.getOrCreateTag();
		long mid = stateTag.getLong("mid");
		long lid = stateTag.getLong("lid");
		if (mid == 0L && lid == 0L) {
			UUID newId = UUID.randomUUID();
			stateTag.putLong("mid", newId.getMostSignificantBits());
			stateTag.putLong("lid", newId.getLeastSignificantBits());
			stateTag.putInt("ammo", this.maxAmmoCapacity == Integer.MAX_VALUE ? Integer.MAX_VALUE : 0);
			stateTag.put("ammox", new CompoundTag());
			List<FireModeInstance> mainFireModes = this.getMainFireModes();
			if (mainFireModes != null && !mainFireModes.isEmpty()) {
				stateTag.putUUID("fmid", this.getMainFireModes().get(0).getId());
			}

			stateTag.putBoolean("aim", false);
			Item defaultAttachments = itemStack.getItem();
			if (defaultAttachments instanceof AttachmentHost attachmentHost) {
				for (Attachment attachment : attachmentHost.getDefaultAttachments()) {
					Attachments.addAttachment(itemStack, new ItemStack(attachment), true);
				}
			}
		} else {
			this.ensureValidFireModeSelected(itemStack);
		}

		Attachments.ensureValidAttachmentsSelected(itemStack);
	}

	public static void initStackForCrafting(ItemStack itemStack) {
		Item item = itemStack.getItem();
		if (item instanceof GunItem gunItem) {
			CompoundTag stateTag = itemStack.getOrCreateTag();
			long mid = stateTag.getLong("mid");
			long lid = stateTag.getLong("lid");
			if (mid == 0L && lid == 0L) {
				UUID newId = UUID.randomUUID();
				stateTag.putLong("mid", newId.getMostSignificantBits());
				stateTag.putLong("lid", newId.getLeastSignificantBits());
				stateTag.put("ammox", new CompoundTag());
				List<FireModeInstance> mainFireModes = gunItem.getMainFireModes();
				if (mainFireModes != null && !mainFireModes.isEmpty()) {
					stateTag.putUUID("fmid", gunItem.getMainFireModes().get(0).getId());
				}

				stateTag.putBoolean("aim", false);
				Item defaultAttachments = itemStack.getItem();
				if (defaultAttachments instanceof AttachmentHost attachmentHost) {
					for (Attachment attachment : attachmentHost.getDefaultAttachments()) {
						Attachments.addAttachment(itemStack, new ItemStack(attachment), true);
					}
				}
			}
		}
	}

	private void ensureValidFireModeSelected(ItemStack itemStack) {
		CompoundTag idTag = itemStack.getTag();
		if (idTag != null) {
			UUID fireModeInstanceId = idTag.hasUUID("fmid") ? idTag.getUUID("fmid") : null;
			FireModeInstance selectedModeInstance = FireModeInstance.getOrElse(fireModeInstanceId, null);
			List<FireModeInstance> fireModes = getFireModes(itemStack);
			if (selectedModeInstance != null && !fireModes.contains(selectedModeInstance)) {
				selectedModeInstance = null;
			}

			if (selectedModeInstance == null && !fireModes.isEmpty()) {
				selectedModeInstance = fireModes.get(0);
				setFireModeInstance(itemStack, selectedModeInstance);
			}

			if (selectedModeInstance == null) {
				idTag.remove("fmid");
			}
		}
	}

	private static long getOrAssignRandomSeed(ItemStack stack) {
		CompoundTag tag = stack.getOrCreateTag();
		long seed = tag.getLong("seed");
		if (!tag.contains("seed", 99)) {
			seed = random.nextLong();
			tag.putLong("seed", seed);
		}
		return seed;
	}

	public void releaseUsing(ItemStack stack, Level level, LivingEntity shooter, int ticksRemaining) {}

	private Predicate<Block> getDestroyBlockByHitScanPredicate() {
		return (block)
				   -> Config.bulletsBreakGlassEnabled &&
						  (block instanceof AbstractGlassBlock || block instanceof StainedGlassPaneBlock ||
						   block == Blocks.GLASS_PANE);
	}

	private Predicate<Block> getPassThroughBlocksByHitScanPredicate() {
		return (block) -> block instanceof BushBlock || block instanceof LeavesBlock;
	}

	@OnlyIn(Dist.CLIENT)
	public void
	requestFireFromServer(GunClientState gunClientState, Player player, ItemStack itemStack, Entity targetEntity) {
		int activeSlot = player.getInventory().selected;
		LOGGER.debug("{} requesting fire from server", System.currentTimeMillis() % 100000L);
		SoundFeature.playFireSound(player, itemStack);
		Pair<Integer, Double> pcs = FireModeFeature.getPelletCountAndSpread(player, gunClientState, itemStack);
		int shotCount = pcs.getFirst() > 0 ? pcs.getFirst() : 1;
		long requestSeed = random.nextLong();
		FireModeInstance fireModeInstance = getFireModeInstance(itemStack);
		if (fireModeInstance.getType() == FireMode.MELEE)
			return;
		AmmoItem projectileItem = this.getFirstCompatibleProjectile(itemStack, fireModeInstance);

		if (projectileItem != null) {
			GunStatePoseProvider gunStatePoseProvider = GunStatePoseProvider.getInstance();
			Vec3[] pd = gunStatePoseProvider.getPositionAndDirection(gunClientState, PoseContext.FIRST_PERSON_MUZZLE);
			if (pd == null) {
				pd =
					gunStatePoseProvider.getPositionAndDirection(gunClientState, PoseContext.FIRST_PERSON_MUZZLE_FLASH);
			}

			Vec3 startPos;
			Vec3 direction;
			if (pd != null) {
				startPos = pd[0];
				direction = pd[1];
			} else {
				startPos = player.getEyePosition();
				direction = player.getViewVector(0.0F);
				startPos = startPos.add(direction.normalize().multiply(2.0F, 2.0F, 2.0F));
			}
			Network.networkChannel.sendToServer(new ProjectileFireRequestPacket(
				fireModeInstance,
				getItemStackId(itemStack),
				activeSlot,
				gunClientState.isAiming(),
				startPos.x,
				startPos.y,
				startPos.z,
				direction.x,
				direction.y,
				direction.z,
				targetEntity != null ? targetEntity.getId() : -1,
				requestSeed));
		} else {
			double adjustedInaccuracy = this.adjustInaccuracy(player, itemStack, gunClientState.isAiming());
			long itemSeed = getOrAssignRandomSeed(itemStack);
			long xorSeed = itemSeed ^ requestSeed;
			this.acquireHitScan(player, itemStack, gunClientState, shotCount, xorSeed, adjustedInaccuracy);
			Network.networkChannel.sendToServer(new HitScanFireRequestPacket(
				fireModeInstance, getItemStackId(itemStack), activeSlot, gunClientState.isAiming(), requestSeed));
			LOGGER.debug("{} sent fire request to server", System.currentTimeMillis() % 100000L);
		}
	}

	private void acquireHitScan(
		Player player,
		ItemStack itemStack,
		@NotNull GunClientState gunClientState,
		int shotCount,
		long seed,
		double adjustedInaccuracy) {
		if (shotCount <= 1) {
			double maxDistance = this.getMaxClientShootingDistance(itemStack, gunClientState);

			for (HitResult hitResult : HitScan.getObjectsInCrosshair(
					 player,
					 player.getEyePosition(),
					 player.getViewVector(0.0F),
					 1.0F,
					 maxDistance,
					 shotCount,
					 adjustedInaccuracy,
					 seed,
					 this.getDestroyBlockByHitScanPredicate(),
					 this.getPassThroughBlocksByHitScanPredicate(),
					 new ArrayList<>())) {
				gunClientState.acquireHitScan(player, itemStack, hitResult);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	private double getMaxClientShootingDistance(ItemStack itemStack, GunClientState gunClientState) {
		Minecraft mc = Minecraft.getInstance();
		double maxDistance =
			Math.min(mc.options.getEffectiveRenderDistance() * 16, FireModeFeature.getMaxShootingDistance(itemStack));
		if (!gunClientState.isAiming()) {
			maxDistance = Math.min(maxDistance, 100.0F);
		}

		return maxDistance;
	}

	private double adjustInaccuracy(Player player, ItemStack itemStack, boolean isAiming) {
		double adjustedInaccuracy;
		if (isAiming) {
			adjustedInaccuracy = this.inaccuracyAiming;
		} else if (player.isSprinting()) {
			adjustedInaccuracy = this.inaccuracySprinting;
		} else {
			adjustedInaccuracy = this.inaccuracy;
		}

		Pair<Integer, Double> pcs = FireModeFeature.getPelletCountAndSpread(player, null, itemStack);
		if (pcs.getFirst() > 0) {
			adjustedInaccuracy += pcs.getSecond();
		}

		float accuracyModifier = AccuracyFeature.getAccuracyModifier(itemStack);
		return adjustedInaccuracy / (double)accuracyModifier;
	}

	public static LazyOptional<Integer> getClientSideAmmo(Player player, ItemStack itemStack, int slotIndex) {
		GunClientState state = GunClientState.getState(player, itemStack, slotIndex, false);
		return LazyOptional.of(state != null ? () -> state.getAmmoCount(getFireModeInstance(itemStack)) : null);
	}

	public double getInacuracy() {
		return this.inaccuracy;
	}

	public int getShotsPerTrace() {
		return this.shotsPerTrace;
	}

	public static int getAmmo(ItemStack itemStack, FireModeInstance fireModeInstance) {
		if (itemStack.getItem() instanceof GunItem) {
			CompoundTag idTag = itemStack.getTag();
			if (idTag != null) {
				if (GunItem.getFireModeInstance(itemStack).getMaxAmmoCapacity() == Integer.MAX_VALUE) {
					return Integer.MAX_VALUE;
				}

				if (fireModeInstance.isUsingDefaultAmmoPool()) {
					return idTag.getInt("ammo");
				}

				CompoundTag auxAmmoTag = idTag.getCompound("ammox");
				if (auxAmmoTag != null) {
					return auxAmmoTag.getInt(fireModeInstance.getAmmo().getName());
				}
			}
		}
		return 0;
	}

	public static void setAmmo(ItemStack itemStack, FireModeInstance fireModeInstance, int ammo) {
		if (itemStack.getItem() instanceof GunItem) {
			LOGGER.debug("Setting ammo in stack {} to {}", System.identityHashCode(itemStack), ammo);
			CompoundTag idTag = itemStack.getTag();
			if (idTag != null) {
				if (fireModeInstance.isUsingDefaultAmmoPool()) {
					idTag.putInt("ammo", ammo);
				} else {
					CompoundTag auxAmmoTag = idTag.getCompound("ammox");
					auxAmmoTag.putInt(fireModeInstance.getAmmo().getName(), ammo);
					idTag.put("ammox", auxAmmoTag);
				}
			}
		}
	}

	public static int decrementAmmo(ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof GunItem)) {
			return 0;
		} else {
			CompoundTag idTag = itemStack.getTag();
			if (idTag != null) {
				int ammo = idTag.getInt("ammo");
				if (ammo <= 0) {
					return -1;
				} else {
					--ammo;
					idTag.putInt("ammo", ammo);
					return ammo;
				}
			} else {
				return 0;
			}
		}
	}

	public static FireMode getSelectedFireModeType(ItemStack itemStack) {
		FireModeInstance fireModeInstance = getFireModeInstance(itemStack);
		return fireModeInstance != null ? fireModeInstance.getType() : null;
	}

	public static FireModeInstance getFireModeInstance(ItemStack itemStack) {
		Item item = itemStack.getItem();
		if (item instanceof GunItem gunItem) {
			CompoundTag idTag = itemStack.getTag();
			if (idTag != null) {
				UUID fireModeInstanceId = idTag.hasUUID("fmid") ? idTag.getUUID("fmid") : null;
				FireModeInstance fireModeInstance = null;
				if (fireModeInstanceId != null) {
					fireModeInstance = FireModeInstance.getOrElse(fireModeInstanceId, null);
				}

				if (fireModeInstance == null) {
					fireModeInstance = gunItem.getMainFireModes().get(0);
				}

				return fireModeInstance;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private static void setFireModeInstance(ItemStack itemStack, FireModeInstance fireModeInstance) {
		if (itemStack.getItem() instanceof GunItem) {
			CompoundTag idTag = itemStack.getTag();
			if (idTag != null) {
				idTag.putUUID("fmid", fireModeInstance.getId());
				LOGGER.debug("Set fire mode instance to {}, tag: {}", fireModeInstance.getDisplayName(), idTag);
			}
		}
	}

	public void handleClientProjectileFireRequest(
		ServerPlayer player,
		FireModeInstance fireModeInstance,
		UUID stateId,
		int slotIndex,
		int correlationId,
		boolean isAiming,
		double spawnPositionX,
		double spawnPositionY,
		double spawnPositionZ,
		double spawnDirectionX,
		double spawnDirectionY,
		double spawnDirectionZ,
		int targetEntityId,
		long requestSeed) {
		ItemStack itemStack = player.getInventory().getItem(slotIndex);
		AmmoItem projectileItem = this.getFirstCompatibleProjectile(itemStack, fireModeInstance);
		if (projectileItem == null) {
			LOGGER.error(
				"Attempted to handle client projectile fire request with an item that does not support projectiles: " +
				this);
		} else if (this.isEnabled() && projectileItem.isEnabled()) {
			boolean isOffhand = player.getOffhandItem() == itemStack;
			if (itemStack != null && !isOffhand && itemStack.getItem() instanceof GunItem) {
				int ammo = 0;
				if ((ammo = getAmmo(itemStack, fireModeInstance)) > 0) {
					LOGGER.debug("Received client projectile file request");
					Entity targetEntity = null;
					if (targetEntityId >= 0) {
						targetEntity = MiscUtil.getLevel(player).getEntity(targetEntityId);
					}

					ProjectileLike projectile = null;
					if (targetEntity != null) {
						HitResult hitResult = HitScan.ensureEntityInCrosshair(player, targetEntity, 0.0F, 400.0F, 2.0F);
						if (hitResult != null && hitResult.getType() == Type.ENTITY &&
							((EntityHitResult)hitResult).getEntity() == targetEntity) {
							projectile =
								projectileItem.createProjectile(player, spawnPositionX, spawnPositionY, spawnPositionZ);
							projectile.launchAtTargetEntity(player, hitResult, targetEntity);
						}
					} else {
						projectile =
							projectileItem.createProjectile(player, spawnPositionX, spawnPositionY, spawnPositionZ);
						long xorSeed = getOrAssignRandomSeed(itemStack) ^ requestSeed;
						double adjustedInaccuracy = this.adjustInaccuracy(player, itemStack, isAiming);
						projectile.launchAtLookTarget(player, adjustedInaccuracy, xorSeed);
					}

					if (projectile != null) {
						if (this.getMaxAmmoCapacity(itemStack, fireModeInstance) < Integer.MAX_VALUE) {
							setAmmo(itemStack, fireModeInstance, ammo - 1);
						}

						SoundFeature.playFireSound(player, itemStack);
						MiscUtil.getLevel(player).addFreshEntity((Entity)projectile);
					} else {
						LOGGER.debug("Did not fire projectile");
					}
				}
			}
		}
	}

	public List<AmmoItem> getCompatibleAmmo() {
		return this.compatibleBullets.stream().map(Supplier::get).toList();
	}

	private AmmoItem getFirstCompatibleProjectile(ItemStack gunStack, FireModeInstance fireModeInstance) {
		Item item = gunStack.getItem();
		if (item instanceof GunItem gunItem) {
			List<FireModeInstance> firModeInstances = getFireModes(gunStack);
			if (!firModeInstances.contains(fireModeInstance)) {
				return null;
			} else {
				AmmoItem projectileItem = null;
				if (fireModeInstance.isUsingDefaultAmmoPool()) {
					for (Supplier<AmmoItem> ammoSupplier : gunItem.compatibleBullets) {
						AmmoItem ammoItem = ammoSupplier.get();
						if (ammoItem != null && ammoItem.isHasProjectile()) {
							projectileItem = ammoItem;
							break;
						}
					}
				} else {
					AmmoItem ammoItem = fireModeInstance.getAmmo();
					if (ammoItem.isHasProjectile()) {
						projectileItem = ammoItem;
					}
				}

				return projectileItem;
			}
		} else {
			return null;
		}
	}

	public void handleClientHitScanFireRequest(
		ServerPlayer player,
		FireModeInstance fireModeInstance,
		UUID stateId,
		int slotIndex,
		int correlationId,
		boolean isAiming,
		long requestSeed) {
		try {
			LOGGER.debug("{} handling client fire request", System.currentTimeMillis() % 100000L);
			ItemStack itemStack = player.getInventory().getItem(slotIndex);
			AmmoItem projectileItem = this.getFirstCompatibleProjectile(itemStack, fireModeInstance);
			if (projectileItem != null) {
				LOGGER.error(
					"Attempted to handle client hit scan fire request with "
					+ "an item that fires projectiles: " + this);
				return;
			}

			if (!this.isEnabled()) {
				return;
			}

			boolean isOffhand = player.getOffhandItem() == itemStack;
			if (itemStack == null || isOffhand || !(itemStack.getItem() instanceof GunItem)) {
				return;
			}

			List<HitResult> hitResults = new ArrayList<>();
			int ammo = 0;
			if ((ammo = getAmmo(itemStack, fireModeInstance)) > 0) {
				if (this.getMaxAmmoCapacity(itemStack, fireModeInstance) < Integer.MAX_VALUE) {
					setAmmo(itemStack, fireModeInstance, ammo - 1);
				}

				SoundFeature.playFireSound(player, itemStack);
				Pair<Integer, Double> pcs = FireModeFeature.getPelletCountAndSpread(player, null, itemStack);
				int shotCount = pcs.getFirst() > 0 ? pcs.getFirst() : 1;
				double adjustedInaccuracy = this.adjustInaccuracy(player, itemStack, isAiming);
				long xorSeed = getOrAssignRandomSeed(itemStack) ^ requestSeed;
				Vec3 eyePos = player.getEyePosition();
				Vec3 lookVec = player.getViewVector(0.0F);
				ServerLevel level = (ServerLevel)MiscUtil.getLevel(player);
				double maxHitScanDistance = this.getMaxServerShootingDistance(itemStack, isAiming, level);
				List<BlockPos> blockPosToDestroy = new ArrayList<>();
				if (this.hitscan) {
					hitResults.addAll(HitScan.getObjectsInCrosshair(
						player,
						eyePos,
						lookVec,
						0.0F,
						maxHitScanDistance,
						shotCount,
						adjustedInaccuracy,
						xorSeed,
						this.getDestroyBlockByHitScanPredicate(),
						this.getPassThroughBlocksByHitScanPredicate(),
						blockPosToDestroy));
				} else { // PBJ code to fire projectile bullets instead of hitscan
					BulletData modifiedBulletData = this.bulletData;
					List<Features.EnabledFeature> modifiers =
						Features.getEnabledFeatures(itemStack, BulletModifierFeature.class);
					for (Features.EnabledFeature feature : modifiers) {
						BulletModifierFeature mod = (BulletModifierFeature)feature.feature();
						modifiedBulletData = new BulletData(
							modifiedBulletData.velocity() + mod.getVelocityModifier(),
							modifiedBulletData.speedOffset() + mod.getSpeedOffsetModifier(),
							modifiedBulletData.maxSpeedOffset() + mod.getMaxSpeedOffsetModifier(),
							modifiedBulletData.inaccuracy() + mod.getInaccuracyModifier(),
							modifiedBulletData.gravity() + mod.getGravityModifier());
					}

					for (int i = 0; i < shotCount; i++) {
						float speed =
							modifiedBulletData.speedOffset() +
							Mth.clamp(
								(fireModeInstance.getDamage() * shotCount) / (modifiedBulletData.velocity() + 1f),
								0,
								modifiedBulletData.maxSpeedOffset());
						ProjectileBulletEntity bullet;
						float damage = fireModeInstance.getDamage();
						bullet = new ProjectileBulletEntity(
							player,
							player.level(),
							damage,
							speed,
							shotCount,
							fireModeInstance.getMaxShootingDistance(),
							fireModeInstance.getHeadshotMultiplier(),
							itemStack,
							correlationId);
						bullet.setOwner(player);
						bullet.setBulletGravity(modifiedBulletData.gravity());
						bullet.shootFromRotation(
							bullet,
							player.getXRot(),
							player.getYRot(),
							0.0F,
							speed,
							(float)adjustedInaccuracy * modifiedBulletData.inaccuracy());
						player.level().addFreshEntity(bullet);
					}
				}
				LOGGER.debug("{} obtained hit results", System.currentTimeMillis() % 100000L);

				for (HitResult hitResult : hitResults) {
					this.hitScanTarget(
						player, itemStack, slotIndex, correlationId, hitResult, maxHitScanDistance, blockPosToDestroy);
					if (hasFunction("postFire"))
						invokeFunction("postFire", itemStack, player, this, hitResult);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to handle client hit scan fire request: {}", e);
		}
	}

	private double getMaxServerShootingDistance(ItemStack itemStack, boolean isAiming, ServerLevel level) {
		MinecraftServer server = level.getServer();
		double maxHitScanDistance =
			Math.min(server.getPlayerList().getViewDistance() * 16, FireModeFeature.getMaxShootingDistance(itemStack));
		if (!isAiming) {
			maxHitScanDistance = Math.min(maxHitScanDistance, 100.0F);
		}

		return maxHitScanDistance;
	}

	private void hitScanTarget(
		Player player,
		ItemStack itemStack,
		int slotIndex,
		int correlationId,
		HitResult hitResult,
		double maxHitScanDistance,
		List<BlockPos> blockPosToDestroy) {
		float entityDamage = 0.0F;
		LOGGER.debug("Executing hit target task for hit result {}", hitResult);
		if (hitResult.getType() == Type.ENTITY) {
			entityDamage = this.hurtEntity(player, (EntityHitResult)hitResult, null, itemStack);
		} else if (hitResult.getType() == Type.BLOCK) {
			this.handleBlockHit(player, (BlockHitResult)hitResult, null);
		}

		for (BlockPos bp : blockPosToDestroy) {
			MiscUtil.getLevel(player).destroyBlock(bp, true, player);
		}

		double maxHitScanDistanceSqr = maxHitScanDistance * maxHitScanDistance;

		for (ServerPlayer serverPlayer : ((ServerLevel)MiscUtil.getLevel(player)).getPlayers((p) -> true)) {
			if (serverPlayer == player || serverPlayer.distanceToSqr(player) < maxHitScanDistanceSqr) {
				LOGGER.debug("{} sends hit scan notification to {}", player, serverPlayer);
				Network.networkChannel.send(
					PacketDistributor.PLAYER.with(() -> serverPlayer),
					new HitScanFireResponsePacket(
						player.getId(),
						getItemStackId(itemStack),
						slotIndex,
						correlationId,
						SimpleHitResult.fromHitResult(hitResult),
						entityDamage));
			}
		}
	}

	public void handleClientFireModeRequest(
		ServerPlayer player, UUID stateId, int slotIndex, int correlationId, FireModeInstance fireModeInstance) {
		ItemStack itemStack = player.getInventory().getItem(slotIndex);
		if (itemStack != null && itemStack.getItem() instanceof GunItem) {
			boolean isSuccess = getFireModes(itemStack).contains(fireModeInstance);
			if (isSuccess) {
				setFireModeInstance(itemStack, fireModeInstance);
			}

			Network.networkChannel.send(
				PacketDistributor.PLAYER.with(() -> player),
				new FireModeResponsePacket(stateId, slotIndex, correlationId, isSuccess, fireModeInstance));
		} else {
			Network.networkChannel.send(
				PacketDistributor.PLAYER.with(() -> player),
				new FireModeResponsePacket(stateId, slotIndex, correlationId, false, fireModeInstance));
		}
	}

	public void processServerStateSyncResponse(
		UUID stateId, int correlationId, boolean isSuccess, ItemStack itemStack, GunClientState gunClientState) {
		if (isSuccess) {
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void processServerHitScanFireResponse(
		Player player,
		UUID stateId,
		ItemStack itemStack,
		GunClientState gunClientState,
		SimpleHitResult hitResult,
		float damage) {
		if (gunClientState != null) {
			Player mainPlayer = ClientUtils.getClientPlayer();
			if (player == mainPlayer) {
				if (hitResult.getType() != Type.MISS) {
					gunClientState.confirmHitScanTarget(mainPlayer, itemStack, hitResult, damage);
				}
			} else if (itemStack != null) {
				Item var9 = itemStack.getItem();
				if (var9 instanceof GunItem gunItem) {
					gunItem.effectLauncher.onStartFiring(mainPlayer, gunClientState, itemStack);
					gunItem.effectLauncher.onHitScanTargetAcquired(mainPlayer, gunClientState, itemStack, hitResult);
					if (hitResult.getType() != Type.MISS) {
						gunItem.effectLauncher.onHitScanTargetConfirmed(
							mainPlayer, gunClientState, itemStack, hitResult, damage);
					}
				}
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void processServerFireModeResponse(
		UUID stateId,
		int correlationId,
		boolean isSuccess,
		ItemStack itemStack,
		GunClientState gunClientState,
		FireModeInstance fireModeInstance) {
		LOGGER.debug("Process fire mode response: {}", isSuccess);
		if (isSuccess && getFireModes(itemStack).contains(fireModeInstance)) {
			setFireModeInstance(itemStack, fireModeInstance);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void processServerReloadResponse(
		int correlationId,
		boolean isSuccess,
		ItemStack itemStack,
		GunClientState gunClientState,
		int ammo,
		FireModeInstance fireModeInstance) {
		LOGGER.debug("Process server reload response with ammo count {}", ammo);
		gunClientState.reloadAmmo(ClientUtils.getLevel(), fireModeInstance, ammo);
	}

	@OnlyIn(Dist.CLIENT)
	public boolean tryReload(Player player, ItemStack itemStack) {
		boolean result = false;
		int activeSlot = player.getInventory().selected;
		boolean isMainHand = player.getMainHandItem() == itemStack;
		if (isMainHand) {
			GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
			if (gunClientState != null) {
				result = gunClientState.tryReload(player, itemStack);
			}
		}

		return result;
	}

	@OnlyIn(Dist.CLIENT)
	public boolean tryFire(LocalPlayer player, ItemStack itemStack, Entity targetEntity) {
		boolean result = false;
		int activeSlot = player.getInventory().selected;
		boolean isMainHand = player.getMainHandItem() == itemStack;
		if (isMainHand) {
			GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
			if (gunClientState != null) {
				gunClientState.setTrigger(true);
				result = gunClientState.tryFire(player, itemStack, targetEntity);
			}
		}

		return result;
	}

	@OnlyIn(Dist.CLIENT)
	public boolean requestReloadFromServer(Player player, ItemStack itemStack) {
		LOGGER.debug("{} Initiating client side reload", System.currentTimeMillis() % 100000L);
		boolean result = false;
		int activeSlot = player.getInventory().selected;
		boolean isMainHandItem = player.getMainHandItem() == itemStack;
		if (isMainHandItem) {
			FireModeInstance fireModeInstance = getFireModeInstance(itemStack);
			int ammoToReload = this.canReloadGun(itemStack, player, fireModeInstance);
			if (ammoToReload > 0) {
				Network.networkChannel.sendToServer(
					new ReloadRequestPacket(getItemStackId(itemStack), activeSlot, fireModeInstance));
				result = true;
			} else {
				LOGGER.debug("No ammo to reload");
			}
		}

		return result;
	}

	public double getAimingCurveX() {
		return this.aimingCurveX;
	}

	public double getAimingCurveY() {
		return this.aimingCurveY;
	}

	public double getAimingCurveZ() {
		return this.aimingCurveZ;
	}

	public double getAimingCurvePitch() {
		return this.aimingCurvePitch;
	}

	public double getAimingCurveYaw() {
		return this.aimingCurveYaw;
	}

	public double getAimingCurveRoll() {
		return this.aimingCurveRoll;
	}

	public double getAimingZoom() {
		return this.aimingZoom;
	}

	private static FireModeInstance getNextFireModeInstance(ItemStack itemStack, FireModeInstance currentMode) {
		List<FireModeInstance> allFireModes = getFireModes(itemStack);
		int currentIndex = allFireModes.indexOf(currentMode);
		int nextIndex = (currentIndex + 1) % allFireModes.size();
		return allFireModes.get(nextIndex);
	}

	public static List<FireModeInstance> getFireModes(ItemStack itemStack) {
		List<FireModeInstance> allFireModes = new ArrayList<>();

		for (Features.EnabledFeature efmf : Features.getEnabledFeatures(itemStack, FireModeFeature.class)) {
			FireModeFeature fmf = (FireModeFeature)efmf.feature();
			allFireModes.addAll(fmf.getFireModes());
		}

		return allFireModes;
	}

	@OnlyIn(Dist.CLIENT)
	public void initiateClientSideFireMode(Player player, ItemStack itemStack) {
		int activeSlot = player.getInventory().selected;
		boolean isOffhand = player.getOffhandItem() == itemStack;
		if (!isOffhand) {
			GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
			if (gunClientState != null && !gunClientState.isReloading() && !gunClientState.isFiring() &&
				!gunClientState.isInspecting()) {
				FireModeInstance currentFireMode = getFireModeInstance(itemStack);
				FireModeInstance nextFireMode = getNextFireModeInstance(itemStack, currentFireMode);
				if (nextFireMode != currentFireMode) {
					LOGGER.debug(
						"Requesting fire mode change from {} to {}",
						currentFireMode.getDisplayName(),
						nextFireMode.getDisplayName());
					Network.networkChannel.sendToServer(
						new FireModeRequestPacket(getItemStackId(itemStack), activeSlot, nextFireMode));
				}
			}
		}
	}

	public void handleClientStopFireRequest(ServerPlayer player, UUID stateId, int slotIndex, int correlationId) {}

	@OnlyIn(Dist.CLIENT)
	public void processStopServerFireResponse(
		UUID stateId, int correlationId, boolean isSuccess, ItemStack itemStack, GunClientState gunClientState) {}

	@OnlyIn(Dist.CLIENT)
	public void setTriggerOff(LocalPlayer player, ItemStack itemStack) {
		int activeSlot = player.getInventory().selected;
		boolean isOffhand = player.getOffhandItem() == itemStack;
		if (!isOffhand) {
			GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
			if (gunClientState != null) {
				gunClientState.setTrigger(false);
			}
		}
	}

	public GunClientState createState(UUID stateId) {
		GunClientState state = new GunClientState(stateId, this);
		LOGGER.debug("Creating state {}", stateId);
		state.setAnimationController(
			"playerRecoil",
			new PlayerRecoilController(
				this.viewRecoilAmplitude, this.viewRecoilMaxPitch, (double)this.viewRecoilDuration));
		state.setAnimationController(
			"shake",
			new ViewShakeAnimationController(
				this.shakeRecoilAmplitude, this.shakeRecoilSpeed, this.shakeDecay, this.shakeRecoilDuration) {
				public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
					FireModeInstance.ViewShakeDescriptor viewShakeDescriptor =
						FireModeFeature.getViewShakeDescriptor(itemStack);
					this.reset(viewShakeDescriptor);
				}
			});
		state.setAnimationController(
			"recoil2",
			new GunRecoilAnimationController(
				this.gunRecoilInitialAmplitude,
				this.gunRecoilRateOfAmplitudeDecay,
				this.gunRecoilInitialAngularFrequency,
				this.gunRecoilRateOfFrequencyIncrease,
				this.gunRecoilPitchMultiplier,
				this.gunRecoilDuration,
				this.shotsPerRecoil));
		state.setAnimationController(
			"randomizer",
			new GunRandomizingAnimationController(
				this.gunRandomizationAmplitude, this.idleRandomizationDuration, this.recoilRandomizationDuration));
		state.setAnimationController("reloadTimer", this.createReloadTimerController());

		for (GlowAnimationController.Builder builder : this.glowEffectBuilders) {
			String controllerId = "glowEffect" + builder.getEffectId();
			state.setAnimationController(controllerId, builder.build());
		}

		for (RotationAnimationController.Builder builder : this.rotationEffectBuilders) {
			String controllerId = "rotation" + builder.getModelPartName();
			state.setAnimationController(controllerId, builder.build());
		}

		state.addListener(new DynamicGeoListener());
		state.addListener(this.effectLauncher);
		state.setAnimationController("aiming", new BiDirectionalInterpolator(this.adsSpeed) {
			public void onToggleAiming(boolean isAiming, Player player) {
				setFullDuration(AdsSpeedFeature.getTotalAdsSpeed(player.getMainHandItem(), player, state));
				this.set(isAiming ? Position.END : Position.START, false);
			}
		});
		return state;
	}

	private List<GunStateAnimationController> createReloadAnimationControllers() {
		List<GunStateAnimationController> reloadAnimationControllers = new ArrayList<>();
		if (this.phasedReloads.isEmpty()) {
			GunStateAnimationController reloadAnimationController = new GunStateAnimationController(
				this, "reload_controller", "animation.model.reload", (ctx) -> ctx.gunClientState().isReloading()) {
				public void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
					if (ClientUtil.isFirstPerson(player)) {
						this.scheduleReset(player, state, itemStack);
					}
				}
			};
			reloadAnimationControllers.add(reloadAnimationController);
		} else {
			long maxReloadDuration = 0L;

			for (PhasedReload phasedReload : this.phasedReloads) {
				long conditionalReloadTimeMillis = phasedReload.timeUnit.toMillis(phasedReload.cooldownTime);
				if (conditionalReloadTimeMillis > maxReloadDuration) {
					maxReloadDuration = conditionalReloadTimeMillis;
				}
			}

			int counter = 0;

			for (final PhasedReload phasedReload : this.phasedReloads) {
				ReloadAnimation reloadAnimation = phasedReload.reloadAnimation;
				final Predicate<ConditionContext> combinedPredicate =
					(ctx) -> ctx.gunClientState().isReloading() && phasedReload.predicate.test(ctx);
				GunStateAnimationController reloadAnimationController = new GunStateAnimationController(
					this,
					reloadAnimation.animationName + "_" + counter++,
					reloadAnimation.animationName,
					combinedPredicate) {
					public void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
						if (ClientUtil.isFirstPerson(player) && phasedReload.phase == ReloadPhase.RELOADING &&
							combinedPredicate.test(new ConditionContext(player, itemStack, state, null))) {
							GunItem.LOGGER.debug(
								"Reset {} on start reloading. Iter: {}",
								this.getName(),
								state.getReloadIterationIndex());
							this.scheduleReset(player, state, itemStack);
						}
					}

					public void onCompleteReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
						if (ClientUtil.isFirstPerson(player) && phasedReload.phase == ReloadPhase.COMPLETETING &&
							combinedPredicate.test(new ConditionContext(player, itemStack, state, null))) {
							GunItem.LOGGER.debug(
								"Reset {} on complete reloading. Iter: {}",
								this.getName(),
								state.getReloadIterationIndex());
							this.scheduleReset(player, state, itemStack);
						}
					}

					public void onPrepareReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
						if (ClientUtil.isFirstPerson(player) && phasedReload.phase == ReloadPhase.PREPARING &&
							combinedPredicate.test(new ConditionContext(player, itemStack, state, null))) {
							GunItem.LOGGER.debug(
								"Reset {} on prepare reloading. Iter: {}",
								this.getName(),
								state.getReloadIterationIndex());
							this.scheduleReset(player, state, itemStack);
						}
					}
				};
				reloadAnimationController.setSoundKeyframeHandler((event) -> {
					Player player = ClientUtils.getClientPlayer();
					if (player != null) {
						SoundKeyframeData soundKeyframeData = event.getKeyframeData();
						String soundName = soundKeyframeData.getSound();
						SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
						if (soundEvent != null) {
							player.playSound(soundEvent, 1.0F, 1.0F);
						}
					}
				});
				reloadAnimationControllers.add(reloadAnimationController);
			}
		}

		return reloadAnimationControllers;
	}

	private TimerController createReloadTimerController() {
		TimerController reloadTimerController;
		if (this.phasedReloads.isEmpty()) {
			reloadTimerController = new TimerController(this.reloadCooldownTime) {
				public void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
					this.reset();
				}
			};

			for (Tuple<Long, AbstractProceduralAnimationController> t : this.reloadEffectControllers) {
				reloadTimerController.schedule(ReloadPhase.RELOADING, t.getA(), TimeUnit.MILLISECOND, t.getB(), null);
			}
		} else {
			long maxReloadDuration = 0L;

			for (PhasedReload phasedReload : this.phasedReloads) {
				long conditionalReloadTimeMillis = phasedReload.timeUnit.toMillis(phasedReload.cooldownTime);
				if (conditionalReloadTimeMillis > maxReloadDuration) {
					maxReloadDuration = conditionalReloadTimeMillis;
				}
			}

			reloadTimerController = new TimerController(maxReloadDuration) {
				public void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
					if (ClientUtil.isFirstPerson(player)) {
						this.reset();
					}
				}

				public void onCompleteReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
					if (ClientUtil.isFirstPerson(player)) {
						this.reset();
					}
				}

				public void onPrepareReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
					if (ClientUtil.isFirstPerson(player)) {
						this.reset();
					}
				}
			};

			for (PhasedReload phasedReload : this.phasedReloads) {
				ReloadAnimation reloadAnimation = phasedReload.reloadAnimation;
				Predicate<ConditionContext> combinedPredicate =
					(ctx) -> ctx.gunClientState().isReloading() && phasedReload.predicate.test(ctx);

				for (ReloadShakeEffect effect : reloadAnimation.shakeEffects) {
					ViewShakeAnimationController2 controller = new ViewShakeAnimationController2(
						effect.initialAmplitude,
						effect.rateOfAmplitudeDecay,
						effect.initialAngularFrequency,
						effect.rateOfFrequencyIncrease,
						effect.duration);
					reloadTimerController.schedule(
						phasedReload.phase,
						phasedReload.timeUnit.toMillis(effect.startTime),
						TimeUnit.MILLISECOND,
						controller,
						combinedPredicate);
				}
			}
		}

		return reloadTimerController;
	}

	public Map<String, AnimationController<GeoAnimatable>> getGeoAnimationControllers(ItemStack itemStack) {
		long geoId = GeoItem.getId(itemStack);
		AnimatableManager<GeoAnimatable> animatableManager = this.cache.getManagerForId(geoId);
		return animatableManager.getAnimationControllers();
	}

	public AnimationController<GeoAnimatable> getGeoAnimationController(String controllerId, ItemStack itemStack) {
		Map<String, AnimationController<GeoAnimatable>> controllers = this.getGeoAnimationControllers(itemStack);
		return controllers.get(controllerId);
	}

	public ResourceLocation getTargetLockOverlay() {
		return this.targetLockOverlay;
	}

	public long getTargetLockTimeTicks() {
		return this.targetLockTimeTicks;
	}

	@OnlyIn(Dist.CLIENT)
	public void onTargetStartLocking(Entity targetEntity) {
		LOGGER.debug("Locking target: {}", targetEntity);
		if (this.targetStartLockingSound != null) {
			Player player = ClientUtils.getClientPlayer();
			MiscUtil.getLevel(player).playSound(
				player,
				player.getX(),
				player.getY(),
				player.getZ(),
				this.targetStartLockingSound,
				SoundSource.PLAYERS,
				1.0F,
				1.0F);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void onTargetLocked(Entity targetEntity) {
		LOGGER.debug("Target locked: {}", targetEntity);
		Player player = ClientUtils.getClientPlayer();
		if (this.targetLockedSound != null) {
			MiscUtil.getLevel(player).playSound(
				player,
				player.getX(),
				player.getY(),
				player.getZ(),
				this.targetLockedSound,
				SoundSource.PLAYERS,
				1.0F,
				1.0F);
		}

		GunClientState state = GunClientState.getMainHeldState();
		if (state != null) {
			MutableComponent var10001 = Component.translatable("message.pointblank.targetAcquired")
											.append(": ")
											.append(targetEntity.getName())
											.append(". ");
			MutableComponent var10002 = Component.translatable("message.pointblank.distance").append(": ");
			float var10003 = targetEntity.distanceTo(player);
			state.publishMessage(var10001.append(var10002.append("" + Math.round(var10003))), 1000L, (s) -> true);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void onTargetStartUnlocking(Entity targetEntity) {
		LOGGER.debug("Target unlocked: {}", targetEntity);
	}

	public float getPrice() {
		return this.tradePrice;
	}

	public int getBundleQuantity() {
		return this.tradeBundleQuantity;
	}

	public int getTradeLevel() {
		return this.tradeLevel;
	}

	public float getBobbing() {
		return this.bobbing;
	}

	public float getBobbingOnAim() {
		return this.bobbingOnAim;
	}

	public float getBobbingRollMultiplier() {
		return this.bobbingRollMultiplier;
	}

	public double getJumpMultiplier() {
		return this.jumpMultiplier;
	}

	public double getPipScopeZoom() {
		return this.pipScopeZoom;
	}

	public void
	handleAimingChangeRequest(Player player, ItemStack itemStack, UUID stateId, int slotIndex, boolean isAiming) {
		if (itemStack.getItem() instanceof GunItem) {
			CompoundTag idTag = itemStack.getTag();
			if (idTag != null) {
				idTag.putBoolean("aim", isAiming && this.isAimingEnabled);
			}

			if (player.isSprinting() && isAiming && this.isAimingEnabled) {
				player.setSprinting(false);
			}
		}
	}

	public static boolean isAiming(ItemStack itemStack) {
		Item item = itemStack.getItem();
		if (item instanceof GunItem gunItem) {
			CompoundTag idTag = itemStack.getTag();
			if (idTag == null) {
				return false;
			} else {
				return gunItem.isAimingEnabled && idTag.getBoolean("aim");
			}
		} else {
			return false;
		}
	}

	public int getMaxAttachmentCategories() {
		return 11;
	}

	public List<Attachment> getDefaultAttachments() {
		return this.defaultAttachmentSuppliers.stream().map(Supplier::get).toList();
	}

	public Collection<Attachment> getCompatibleAttachments() {
		if (this.compatibleAttachments == null) {
			Set<AttachmentCategory> attachmentCategories = new HashSet<>();
			Set<Attachment> compatibleAttachments = new LinkedHashSet<>();

			for (Attachment attachment : this.getDefaultAttachments()) {
				if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
					LOGGER.warn(
						"Cannot add compatible attachment {} with category {} to  item {} because the existing "
							+ "number of compatible categories for this item cannot exceed {}",
						attachment.getName(),
						attachment.getCategory(),
						this.getName(),
						this.getMaxAttachmentCategories());
					break;
				}

				attachmentCategories.add(attachment.getCategory());
				compatibleAttachments.add(attachment);
			}

			for (Supplier<Attachment> attachmentSupplier : this.compatibleAttachmentSuppliers) {
				Attachment attachment = attachmentSupplier.get();
				if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
					LOGGER.warn(
						"Cannot add compatible attachment {} with category {} to  item {} because the existing "
							+ "number of compatible categories for this item cannot exceed {}",
						attachment.getName(),
						attachment.getCategory(),
						this.getName(),
						this.getMaxAttachmentCategories());
					break;
				}

				attachmentCategories.add(attachment.getCategory());
				compatibleAttachments.add(attachment);
			}

			for (String group : this.compatibleAttachmentGroups) {
				for (Supplier<? extends Item> ga : ItemRegistry.ITEMS.getAttachmentsForGroup(group)) {
					Item item = ga.get();
					if (item instanceof Attachment attachment) {
						if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
							LOGGER.warn(
								"Cannot add compatible attachment {} with category {} to  item {} because the "
									+ "existing number of compatible categories for this item cannot exceed {}",
								attachment.getName(),
								attachment.getCategory(),
								this.getName(),
								this.getMaxAttachmentCategories());
							break;
						}

						compatibleAttachments.add(attachment);
					}
				}
			}

			this.compatibleAttachments = compatibleAttachments;
		}

		return this.compatibleAttachments;
	}

	public <T extends Feature> T getFeature(Class<T> featureClass) {
		return featureClass.cast(this.features.get(featureClass));
	}

	public long getCraftingDuration() {
		return this.craftingDuration;
	}

	public SoundEvent getFireSound() {
		return this.fireSound;
	}

	public float getFireSoundVolume() {
		return this.fireSoundVolume;
	}

	public static ItemStack getMainHeldGunItemStack(LivingEntity player) {
		ItemStack itemStack = player.getMainHandItem();
		return itemStack != null && itemStack.getItem() instanceof GunItem ? itemStack : null;
	}

	public boolean hasIdleAnimations() {
		return this.idleAnimationProvider != null;
	}

	public AnimationType getAnimationType() {
		return this.animationType;
	}

	public String getThirdPersonFallbackAnimations() {
		return this.thirdPersonFallbackAnimations;
	}

	@Override
	public Script getScript() {
		return script;
	}

	public boolean hasScript() {
		return script != null;
	}

	// Bundle Code BEGIN

	@Override
	public boolean overrideOtherStackedOnMe(
		ItemStack pStack, ItemStack pOther, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess) {
		return stack(pStack, pOther, pAction, pPlayer, pAccess);
	}

	// Bundle Code
	private static final int BAR_COLOR = Mth.color(0.4F, 0.4F, 1.0F);

	public boolean isBarVisible(ItemStack pStack) {
		return false;
	}

	public Optional<TooltipComponent> getTooltipImage(ItemStack pStack) {
		NonNullList<ItemStack> nonnulllist = NonNullList.create();
		getContents(pStack).forEach(nonnulllist::add);
		var weight = getTotalWeight(pStack);
		if (getMaxWeight(pStack) == 0)
			return Optional.empty();
		return Optional.of(new BundleTooltip(nonnulllist, weight));
	}

	public void onDestroyed(ItemEntity pItemEntity) {
		ItemUtils.onContainerDestroyed(pItemEntity, getContents(pItemEntity.getItem()));
	}

	private void playRemoveOneSound(Entity pEntity) {
		pEntity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + pEntity.level().getRandom().nextFloat() * 0.4F);
	}

	private void playInsertSound(Entity pEntity) {
		pEntity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + pEntity.level().getRandom().nextFloat() * 0.4F);
	}

	private void playDropContentsSound(Entity pEntity) {
		pEntity.playSound(
			SoundEvents.BUNDLE_DROP_CONTENTS, 0.8F, 0.8F + pEntity.level().getRandom().nextFloat() * 0.4F);
	}

	// Bundle Code END

	public enum AnimationType {
		RIFLE("__DEFAULT_RIFLE_ANIMATIONS__", GunItem.FALLBACK_COMMON_ANIMATIONS),
		PISTOL("__DEFAULT_PISTOL_ANIMATIONS__", GunItem.FALLBACK_PISTOL_ANIMATIONS);

		private final String defaultThirdPersonAnimation;
		private final List<ResourceLocation> fallbackFirstPersonAnimations;

		AnimationType(String defaultThirdPersonAnimation, List<ResourceLocation> fallbackFirstPersonAnimations) {
			this.defaultThirdPersonAnimation = defaultThirdPersonAnimation;
			this.fallbackFirstPersonAnimations = fallbackFirstPersonAnimations;
		}

		public String getDefaultThirdPersonAnimation() {
			return this.defaultThirdPersonAnimation;
		}

		private List<ResourceLocation> getFallbackFirstPersonAnimations() {
			return this.fallbackFirstPersonAnimations;
		}
	}

	public enum ReloadPhase {
		PREPARING,
		RELOADING,
		COMPLETETING;

		ReloadPhase() {}
	}

	public enum FirePhase implements Nameable {
		PREPARING,
		FIRING,
		COMPLETETING,
		HIT_SCAN_ACQUIRED,
		HIT_TARGET,
		ANY,
		FLYING;

		FirePhase() {}

		@Override
		public String getName() {
			return this.name();
		}
	}

	public record ReloadShakeEffect(
		long startTime,
		long duration,
		TimeUnit timeUnit,
		double initialAmplitude,
		double rateOfAmplitudeDecay,
		double initialAngularFrequency,
		double rateOfFrequencyIncrease) {
		private static final double DEFAULT_INITIAL_ANGULAR_FREQUENCY = 1.0F;
		private static final double DEFAULT_RATE_OF_FREQUENCY_INCREASE = 0.01;

		public ReloadShakeEffect(
			long startTime,
			long duration,
			TimeUnit timeUnit,
			double initialAmplitude,
			double rateOfAmplitudeDecay,
			double initialAngularFrequency,
			double rateOfFrequencyIncrease) {
			this.startTime = startTime;
			this.duration = duration;
			this.timeUnit = timeUnit;
			this.initialAmplitude = initialAmplitude;
			this.rateOfAmplitudeDecay = rateOfAmplitudeDecay;
			this.initialAngularFrequency = initialAngularFrequency;
			this.rateOfFrequencyIncrease = rateOfFrequencyIncrease;
		}

		public ReloadShakeEffect(long startTime, long duration, double initialAmplitude, double rateOfAmplitudeDecay) {
			this(
				startTime,
				duration,
				TimeUnit.MILLISECOND,
				initialAmplitude,
				rateOfAmplitudeDecay,
				DEFAULT_INITIAL_ANGULAR_FREQUENCY,
				DEFAULT_RATE_OF_FREQUENCY_INCREASE);
		}

		public long startTime() {
			return this.startTime;
		}

		public long duration() {
			return this.duration;
		}

		public TimeUnit timeUnit() {
			return this.timeUnit;
		}

		public double initialAmplitude() {
			return this.initialAmplitude;
		}

		public double rateOfAmplitudeDecay() {
			return this.rateOfAmplitudeDecay;
		}

		public double initialAngularFrequency() {
			return this.initialAngularFrequency;
		}

		public double rateOfFrequencyIncrease() {
			return this.rateOfFrequencyIncrease;
		}
	}

	public record ReloadAnimation(String animationName, List<ReloadShakeEffect> shakeEffects) {
		public ReloadAnimation(String animationName, List<ReloadShakeEffect> shakeEffects) {
			this.animationName = animationName;
			this.shakeEffects = shakeEffects;
		}

		public ReloadAnimation(String animationName) {
			this(animationName, Collections.emptyList());
		}

		public String animationName() {
			return this.animationName;
		}

		public List<ReloadShakeEffect> shakeEffects() {
			return this.shakeEffects;
		}
	}

	public record PhasedReload(
		ReloadPhase phase,
		Predicate<ConditionContext> predicate,
		long cooldownTime,
		TimeUnit timeUnit,
		ReloadAnimation reloadAnimation) {
		public PhasedReload(
			ReloadPhase phase,
			Predicate<ConditionContext> predicate,
			long cooldownTime,
			TimeUnit timeUnit,
			ReloadAnimation reloadAnimation) {
			if (cooldownTime == 0L) {
				throw new IllegalArgumentException("cooldownTime cannot be null");
			} else if (timeUnit == null) {
				throw new IllegalArgumentException("timeUnit cannot be null");
			} else if (predicate == null) {
				throw new IllegalArgumentException("predicate cannot be null");
			} else {
				this.phase = phase;
				this.predicate = predicate;
				this.cooldownTime = cooldownTime;
				this.timeUnit = timeUnit;
				this.reloadAnimation = reloadAnimation;
			}
		}

		public PhasedReload(
			ReloadPhase phase,
			TriPredicate<LivingEntity, GunClientState, ItemStack> predicate,
			long cooldownTime,
			ReloadAnimation reloadAnimation) {
			this(
				phase,
				(ctx)
					-> predicate.test(ctx.player(), ctx.gunClientState(), ctx.currentItemStack()),
				cooldownTime,
				TimeUnit.MILLISECOND,
				reloadAnimation);
		}

		public PhasedReload(ReloadPhase phase, long cooldownTime, ReloadAnimation reloadAnimation) {
			this(phase, (ctx) -> true, cooldownTime, TimeUnit.MILLISECOND, reloadAnimation);
		}

		public PhasedReload(ReloadPhase phase, long cooldownTime, String animationName) {
			this(phase, (ctx) -> true, cooldownTime, TimeUnit.MILLISECOND, new ReloadAnimation(animationName));
		}

		public PhasedReload(
			ReloadPhase phase, long cooldownTime, String animationName, Predicate<ConditionContext> predicate) {
			this(phase, predicate, cooldownTime, TimeUnit.MILLISECOND, new ReloadAnimation(animationName));
		}

		public ReloadPhase phase() {
			return this.phase;
		}

		public Predicate<ConditionContext> predicate() {
			return this.predicate;
		}

		public long cooldownTime() {
			return this.cooldownTime;
		}

		public TimeUnit timeUnit() {
			return this.timeUnit;
		}

		public ReloadAnimation reloadAnimation() {
			return this.reloadAnimation;
		}
	}

	public static class Builder extends HurtingItem.Builder<Builder> implements Nameable, ScriptHolder {
		private static final float DEFAULT_PRICE = Float.NaN;
		private static final int DEFAULT_TRADE_LEVEL = 0;
		private static final int DEFAULT_TRADE_BUNDLE_QUANTITY = 1;
		private static final int DEFAULT_VIEW_RECOIL_DURATION = 100;
		private static final int DEFAULT_SHAKE_RECOIL_DURATION = 400;
		private static final int DEFAULT_GUN_RECOIL_DURATION = 500;
		private static final int DEFAULT_IDLE_RANDOMIZATION_DURATION = 2500;
		private static final int DEFAULT_RECOIL_RANDOMIZATION_DURATION = 250;
		private static final int DEFAULT_DRAW_COOLDOWN_DURATION = 500;
		private static final int DEFAULT_PREPARE_IDLE_COOLDOWN_DURATION = 0;
		private static final int DEFAULT_INSPECT_COOLDOWN_DURATION = 1000;
		private static final int DEFAULT_CRAFTING_DURATION = 1000;
		private static final float DEFAULT_HIT_SCAN_SPEED = 800.0F;
		private static final float DEFAULT_HIT_SCAN_ACCELERATION = 0.0F;
		private static final double DEFAULT_VIEW_RECOIL_AMPLITUDE = 1.0F;
		private static final double DEFAULT_SHAKE_RECOIL_AMPLITUDE = 0.5F;
		private static final int DEFAULT_VIEW_RECOIL_MAX_PITCH = 20;
		private static final double DEFAULT_SHAKE_RECOIL_SPEED = 8.0F;
		private static final double DEFAULT_SHAKE_DECAY = 0.98;
		private static final double DEFAULT_GUN_RECOIL_INITIAL_AMPLITUDE = 0.3;
		private static final double DEFAULT_GUN_RECOIL_RATE_OF_AMPLITUDE_DECAY = 0.8;
		private static final double DEFAULT_GUN_RECOIL_INITIAL_ANGULAR_FREQUENCY = 1.0F;
		private static final double DEFAULT_GUN_RECOIL_RATE_OF_FREQUENCY_INCREASE = 0.05;
		private static final double DEFAULT_GUN_RANDOMIZATION_AMPLITUDE = 0.01;
		private static final double DEFAULT_GUN_RECOIL_PITCH_MULTIPLIER = 1.0F;
		private static final double DEFAULT_JUMP_MULTIPLIER = 1.0F;
		private static final double DEFAULT_RELOAD_SHAKE_INITIAL_AMPLITUDE = 0.15;
		private static final double DEFAULT_RELOAD_SHAKE_RATE_OF_AMPLITUDE_DECAY = 0.3;
		private static final double DEFAULT_RELOAD_SHAKE_INITIAL_ANGULAR_FREQUENCY = 1.0F;
		private static final double DEFAULT_RELOAD_SHAKE_RATE_OF_FREQUENCY_INCREASE = 0.01;
		private static final int DEFAULT_BURST_SHOTS = 3;
		private static final int DEFAULT_RELOAD_COOLDOWN_TIME = 1000;
		public static final double DEFAULT_AIMING_CURVE_X = 0.0F;
		public static final double DEFAULT_AIMING_CURVE_Y = -0.07;
		public static final double DEFAULT_AIMING_CURVE_Z = 0.3;
		public static final double DEFAULT_AIMING_CURVE_PITCH = -0.01;
		public static final double DEFAULT_AIMING_CURVE_YAW = -0.01;
		public static final double DEFAULT_AIMING_CURVE_ROLL = -0.01;
		public static final double DEFAULT_AIMING_ZOOM = 0.05;
		public static final double DEFAULT_PELLET_SPREAD = 1.0F;
		private static final double DEFAULT_INACCURACY = 0.03;
		private static final double DEFAULT_INACCURACY_AIMING = 0.0F;
		private static final double DEFAULT_INACCURACY_SPRINTING = 0.1;
		private static final int DEFAULT_RPM = 600;
		private static final float DEFAULT_FIRE_SOUND_VOLUME = 5.0F;
		public static final int DEFAULT_MAX_AMMO_PER_RELOAD_ITERATION = Integer.MAX_VALUE;
		private static final String DEFAULT_RETICLE_OVERLAY = "textures/item/reticle.png";
		public static final int MAX_PELLET_SHOOTING_RANGE = 50;
		private static final float DEFAULT_BOBBING = 1.0F;
		private static final float DEFAULT_BOBBING_ON_AIM = 0.3F;
		private static final float DEFAULT_BOBBING_ROLL_MULTIPLIER = 1.0F;
		public boolean hitscan = false;
		public BulletData bulletData;
		public long adsSpeed;
		private long targetLockTimeTicks;
		private double viewRecoilAmplitude = 1.0F;
		private double shakeRecoilAmplitude = 0.5F;
		private int viewRecoilMaxPitch = 20;
		private long viewRecoilDuration = 100L;
		private double shakeRecoilSpeed = 8.0F;
		private double shakeDecay = 0.98;
		private long shakeRecoilDuration = 400L;
		private double gunRecoilInitialAmplitude = 0.3;
		private double gunRecoilRateOfAmplitudeDecay = 0.8;
		private double gunRecoilInitialAngularFrequency = 1.0F;
		private double gunRecoilRateOfFrequencyIncrease = 0.05;
		private double gunRandomizationAmplitude = 0.01;
		private double gunRecoilPitchMultiplier = 1.0F;
		private long gunRecoilDuration = 500L;
		private double jumpMultiplier = 1.0F;
		private int shotsPerRecoil = 1;
		private int shotsPerTrace = 1;
		private long idleRandomizationDuration = 2500L;
		private long recoilRandomizationDuration = 250L;
		private int burstShots = 3;
		private long prepareIdleCooldownDuration = 0L;
		private long prepareFireCooldownDuration = 0L;
		private long completeFireCooldownDuration = 0L;
		private long enableFireModeCooldownDuration = 0L;
		private long craftingDuration = 1000L;
		private String name;
		private float tradePrice = Float.NaN;
		private int tradeBundleQuantity = 1;
		private int tradeLevel = 0;
		private int rpm = 600;
		private long reloadCooldownTime;
		private String reloadAnimation;
		private int maxAmmoCapacity;
		private int maxAmmoPerReloadIteration = Integer.MAX_VALUE;
		private FireMode[] fireModes;
		private final Set<Supplier<AmmoItem>> compatibleAmmo = new LinkedHashSet<>();
		private Supplier<SoundEvent> fireSound;
		private float fireSoundVolume = 5.0F;
		private Supplier<SoundEvent> targetLockedSound;
		private Supplier<SoundEvent> targetStartLockingSound;
		private boolean isAimingEnabled = true;
		private double aimingCurveX = 0.0F;
		private double aimingCurveY = -0.07;
		private double aimingCurveZ = 0.3;
		private double aimingCurvePitch = -0.01;
		private double aimingCurveYaw = -0.01;
		private double aimingCurveRoll = -0.01;
		private double aimingZoom = 0.05;
		private double pipScopeZoom = 0.0F;
		private final List<Tuple<Long, AbstractProceduralAnimationController>> reloadEffectControllers;
		private String scopeOverlay;
		private String reticleOverlay;
		private String targetLockOverlay;
		private final List<PhasedReload> phasedReloads = new ArrayList<>();
		private final ConditionalAnimationProvider.Builder drawAnimationsBuilder =
			new ConditionalAnimationProvider.Builder();
		private final ConditionalAnimationProvider.Builder idleAnimationBuilder =
			new ConditionalAnimationProvider.Builder();
		private final ConditionalAnimationProvider.Builder inspectAnimationsBuilder =
			new ConditionalAnimationProvider.Builder();
		private final ConditionalAnimationProvider.Builder fireAnimationsBuilder =
			new ConditionalAnimationProvider.Builder();
		private int pelletCount = 0;
		private double pelletSpread = 1.0F;
		private double inaccuracy = 0.03;
		private double inaccuracyAiming = 0.0F;
		private double inaccuracySprinting = 0.1;
		private final List<GlowAnimationController.Builder> glowEffectBuilders = new ArrayList<>();
		private final List<RotationAnimationController.Builder> rotationEffectBuilders = new ArrayList<>();
		private final Map<FirePhase, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>> effectBuilders =
			new HashMap<>();
		private float bobbing = 1.0F;
		private float bobbingOnAim = 0.3F;
		private float bobbingRollMultiplier = 1.0F;
		private float modelScale = 1.0F;
		private AnimationType animationType;
		private String firstPersonFallbackAnimations;
		private String thirdPersonFallbackAnimations;
		private final List<Supplier<Attachment>> compatibleAttachments;
		private final List<String> compatibleAttachmentGroups;
		private final List<FeatureBuilder<?, ?>> featureBuilders;
		private final List<Supplier<Attachment>> defaultAttachments;
		@Nullable private Script mainScript;

		public Builder(ExtensionRegistry.Extension extension) {
			this.animationType = AnimationType.RIFLE;
			this.compatibleAttachments = new ArrayList<>();
			this.compatibleAttachmentGroups = new ArrayList<>();
			this.featureBuilders = new ArrayList<>();
			this.defaultAttachments = new ArrayList<>();
			this.reloadEffectControllers = new ArrayList<>();
			this.extension = extension;
		}
		public Builder() {
			this(new ExtensionRegistry.Extension("pointblank", Path.of("pointblank"), "pointblank"));
		}

		public String getName() {
			return this.name;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withHitscan(boolean hitscan) {
			this.hitscan = hitscan;
			return this;
		}

		public Builder withAdsSpeed(long adsSpeed) {
			this.adsSpeed = adsSpeed;
			return this;
		}

		public Builder withAnimationType(AnimationType animationType) {
			this.animationType = animationType;
			return this;
		}

		public Builder withFirstPersonFallbackAnimations(String firstPersonFallbackAnimations) {
			this.firstPersonFallbackAnimations = firstPersonFallbackAnimations;
			return this;
		}

		public Builder withThirdPersonFallbackAnimations(String thirdPersonFallbackAnimations) {
			this.thirdPersonFallbackAnimations = thirdPersonFallbackAnimations;
			return this;
		}

		@SafeVarargs
		public final Builder withDefaultAttachment(Supplier<? extends Attachment>... attachmentSuppliers) {
			for (Supplier<? extends Attachment> s : attachmentSuppliers) {
				Objects.requireNonNull(s);
				this.defaultAttachments.add(s::get);
			}

			return this;
		}

		@SafeVarargs
		public final Builder withCompatibleAttachment(Supplier<? extends Attachment>... attachmentSuppliers) {
			for (Supplier<? extends Attachment> s : attachmentSuppliers) {
				Objects.requireNonNull(s);
				this.compatibleAttachments.add(s::get);
			}

			return this;
		}

		public Builder withCompatibleAttachmentGroup(String... groups) {
			this.compatibleAttachmentGroups.addAll(Set.of(groups));
			return this;
		}

		public Builder withFeature(FeatureBuilder<?, ?> featureBuilder) {
			this.featureBuilders.add(featureBuilder);
			return this;
		}

		public Builder withModelScale(double modelScale) {
			this.modelScale = Mth.clamp((float)modelScale, 0.1F, 1.0F);
			return this;
		}

		public Builder withTradePrice(float price, int tradeBundleQuantity, int tradeLevel) {
			this.tradePrice = price;
			this.tradeLevel = tradeLevel;
			this.tradeBundleQuantity = tradeBundleQuantity;
			return this;
		}

		public Builder withTradePrice(float price, int tradeLevel) {
			return this.withTradePrice(price, 1, tradeLevel);
		}

		public Builder withMaxAmmoCapacity(int maxAmmoCapacity) {
			this.maxAmmoCapacity = maxAmmoCapacity;
			return this;
		}

		public Builder withMaxAmmoPerReloadIteration(int maxAmmoPerReloadIteration) {
			this.maxAmmoPerReloadIteration = maxAmmoPerReloadIteration;
			return this;
		}

		public Builder withRpm(int rpm) {
			this.rpm = rpm;
			return this;
		}

		public Builder withReloadAnimation(String reloadAnimation) {
			this.reloadAnimation = reloadAnimation;
			return this;
		}

		public Builder withReloadCooldownDuration(long reloadCooldownTime, TimeUnit timeUnit) {
			this.reloadCooldownTime = timeUnit.toMillis(reloadCooldownTime);
			return this;
		}

		public Builder withFireModes(FireMode... fireModes) {
			this.fireModes = fireModes;
			return this;
		}

		public Builder withCraftingDuration(int duration, TimeUnit timeUnit) {
			this.craftingDuration = timeUnit.toMillis(duration);
			return this;
		}

		@SafeVarargs
		public final Builder withCompatibleAmmo(Supplier<AmmoItem>... ammo) {
			Collections.addAll(this.compatibleAmmo, ammo);

			return this;
		}

		public final Builder withCompatibleAmmo(List<Supplier<AmmoItem>> ammo) {
			this.compatibleAmmo.addAll(ammo);
			return this;
		}

		public final Builder withTargetLock(int minTargetLockTime, TimeUnit timeUnit) {
			this.targetLockTimeTicks = timeUnit.toTicks(minTargetLockTime);
			return this;
		}

		public Builder withViewRecoilAmplitude(double amplitude) {
			this.viewRecoilAmplitude = amplitude;
			return this;
		}

		public Builder withShakeRecoilAmplitude(double amplitude) {
			this.shakeRecoilAmplitude = amplitude;
			return this;
		}

		public Builder withViewRecoilMaxPitch(int pitch) {
			this.viewRecoilMaxPitch = pitch;
			return this;
		}

		public Builder withViewRecoilDuration(int duration, TimeUnit timeUnit) {
			this.viewRecoilDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withShakeRecoilSpeed(double speed) {
			this.shakeRecoilSpeed = speed;
			return this;
		}

		public Builder withShakeDecay(double shakeDecay) {
			this.shakeDecay = shakeDecay;
			return this;
		}

		public Builder withShakeRecoilDuration(int duration, TimeUnit timeUnit) {
			this.shakeRecoilDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withGunRecoilInitialAmplitude(double amplitude) {
			this.gunRecoilInitialAmplitude = amplitude;
			return this;
		}

		public Builder withGunRecoilRateOfAmplitudeDecay(double decayRate) {
			this.gunRecoilRateOfAmplitudeDecay = decayRate;
			return this;
		}

		public Builder withGunRecoilInitialAngularFrequency(double frequency) {
			this.gunRecoilInitialAngularFrequency = frequency;
			return this;
		}

		public Builder withGunRecoilRateOfFrequencyIncrease(double rate) {
			this.gunRecoilRateOfFrequencyIncrease = rate;
			return this;
		}

		public Builder withGunRecoilPitchMultiplier(double gunRecoilPitchMultiplier) {
			this.gunRecoilPitchMultiplier = gunRecoilPitchMultiplier;
			return this;
		}

		public Builder withGunRecoilDuration(int duration, TimeUnit timeUnit) {
			this.gunRecoilDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withJumpMultiplier(double jumpMultiplier) {
			this.jumpMultiplier = jumpMultiplier;
			return this;
		}

		public Builder withShotsPerRecoil(int shotsPerRecoil) {
			this.shotsPerRecoil = shotsPerRecoil;
			return this;
		}

		public Builder withShotsPerTrace(int shotsPerTrace) {
			this.shotsPerTrace = shotsPerTrace;
			return this;
		}

		public Builder withIdleRandomizationDuration(int duration, TimeUnit timeUnit) {
			this.idleRandomizationDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withRecoilRandomizationDuration(int duration, TimeUnit timeUnit) {
			this.recoilRandomizationDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withFireSound(Supplier<SoundEvent> fireSound) {
			this.fireSound = fireSound;
			return this;
		}

		public Builder withFireSound(Supplier<SoundEvent> fireSound, float fireSoundVolume) {
			this.fireSound = fireSound;
			this.fireSoundVolume = fireSoundVolume;
			return this;
		}

		public Builder withFireSound(SoundEvent fireSound) {
			this.fireSound = () -> fireSound;
			return this;
		}

		public Builder withFireSound(SoundEvent fireSound, float fireSoundVolume) {
			this.fireSound = () -> fireSound;
			this.fireSoundVolume = fireSoundVolume;
			return this;
		}

		public Builder withReloadSound(Supplier<SoundEvent> reloadSound) {
			return this;
		}

		public Builder withReloadSound(SoundEvent reloadSound) {
			return this;
		}

		public Builder withReloadShakeEffect(
			long startTime,
			long duration,
			TimeUnit timeUnit,
			double initialAmplitude,
			double rateOfAmplitudeDecay,
			double initialAngularFrequency,
			double rateOfFrequencyIncrease) {
			this.reloadEffectControllers.add(new Tuple<>(
				timeUnit.toMillis(startTime),
				new ViewShakeAnimationController2(
					initialAmplitude,
					rateOfAmplitudeDecay,
					initialAngularFrequency,
					rateOfFrequencyIncrease,
					duration)));
			return this;
		}

		public Builder withAimingEnabled(boolean aimingEnabled) {
			this.isAimingEnabled = aimingEnabled;
			return this;
		}

		public Builder withAimingCurveX(double aimingCurveX) {
			this.aimingCurveX = aimingCurveX;
			return this;
		}

		public Builder withAimingCurveY(double aimingCurveY) {
			this.aimingCurveY = aimingCurveY;
			return this;
		}

		public Builder withAimingCurveZ(double aimingCurveZ) {
			this.aimingCurveZ = aimingCurveZ;
			return this;
		}

		public Builder withAimingCurvePitch(double aimingCurvePitch) {
			this.aimingCurvePitch = aimingCurvePitch;
			return this;
		}

		public Builder withAimingCurveYaw(double aimingCurveYaw) {
			this.aimingCurveYaw = aimingCurveYaw;
			return this;
		}

		public Builder withAimingCurveRoll(double aimingCurveRoll) {
			this.aimingCurveRoll = aimingCurveRoll;
			return this;
		}

		public Builder withAimingZoom(double aimingZoom) {
			this.aimingZoom = aimingZoom;
			return this;
		}

		public Builder withPipScopeZoom(double pipScopeZoom) {
			this.pipScopeZoom = pipScopeZoom;
			return this;
		}

		public Builder withScopeOverlay(String scopeOverlay) {
			this.scopeOverlay = scopeOverlay;
			return this;
		}

		public Builder withReticleOverlay(String reticleOverlay) {
			this.reticleOverlay = reticleOverlay;
			return this;
		}

		public Builder withTargetLockOverlay(String targetLockOverlay) {
			this.targetLockOverlay = targetLockOverlay;
			return this;
		}

		public Builder withTargetLockedSound(Supplier<SoundEvent> targetLockedSound) {
			this.targetLockedSound = targetLockedSound;
			return this;
		}

		public Builder withTargetLockedSound(SoundEvent targetLockedSound) {
			this.targetLockedSound = () -> targetLockedSound;
			return this;
		}

		public Builder withTargetStartLockingSound(Supplier<SoundEvent> targetStartLockingSound) {
			this.targetStartLockingSound = targetStartLockingSound;
			return this;
		}

		public Builder withTargetStartLockingSound(SoundEvent targetStartLockingSound) {
			this.targetStartLockingSound = () -> targetStartLockingSound;
			return this;
		}

		public Builder withReticleOverlay() {
			this.reticleOverlay = "textures/item/reticle.png";
			return this;
		}

		public Builder withPrepareFireCooldownDuration(int duration, TimeUnit timeUnit) {
			this.prepareFireCooldownDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withCompleteFireCooldownDuration(int duration, TimeUnit timeUnit) {
			this.completeFireCooldownDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withEnableFireModeCooldownDuration(int duration, TimeUnit timeUnit) {
			this.enableFireModeCooldownDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withPrepareIdleCooldownDuration(int duration, TimeUnit timeUnit) {
			this.prepareIdleCooldownDuration = timeUnit.toMillis(duration);
			return this;
		}

		public Builder withDrawCooldownDuration(int duration, TimeUnit timeUnit) {
			this.withDrawAnimation("animation.model.draw", (ctx) -> true, duration, timeUnit);
			return this;
		}

		public Builder withDrawAnimation(
			String animationName, Predicate<ConditionContext> predicate, int duration, TimeUnit timeUnit) {
			this.drawAnimationsBuilder.withAnimation(animationName, predicate, duration, timeUnit);
			return this;
		}

		public Builder withInspectCooldownDuration(int duration, TimeUnit timeUnit) {
			this.withInspectAnimation("animation.model.inspect", (ctx) -> true, duration, timeUnit);
			return this;
		}

		public Builder withInspectAnimation(
			String animationName, Predicate<ConditionContext> predicate, int duration, TimeUnit timeUnit) {
			this.inspectAnimationsBuilder.withAnimation(animationName, predicate, duration, timeUnit);
			return this;
		}

		public Builder withIdleAnimation(
			String animationName, Predicate<ConditionContext> predicate, int duration, TimeUnit timeUnit) {
			this.idleAnimationBuilder.withAnimation(animationName, predicate, duration, timeUnit);
			return this;
		}

		public Builder withFireAnimation(
			String animationName, Predicate<ConditionContext> predicate, int duration, TimeUnit timeUnit) {
			this.fireAnimationsBuilder.withAnimation(animationName, predicate, duration, timeUnit);
			return this;
		}

		public Builder withPhasedReload(PhasedReload phasedReload) {
			this.phasedReloads.add(phasedReload);
			return this;
		}

		public Builder withPhasedReload(ReloadPhase phase, long cooldownTime, String animationName) {
			this.phasedReloads.add(new PhasedReload(phase, cooldownTime, animationName));
			return this;
		}

		public Builder withPhasedReload(ReloadPhase phase, long cooldownTime, ReloadAnimation reloadAnimation) {
			this.phasedReloads.add(new PhasedReload(phase, cooldownTime, reloadAnimation));
			return this;
		}

		public Builder withPhasedReload(
			ReloadPhase phase,
			TriPredicate<LivingEntity, GunClientState, ItemStack> predicate,
			long cooldownTime,
			ReloadAnimation reloadAnimation) {
			this.phasedReloads.add(new PhasedReload(phase, predicate, cooldownTime, reloadAnimation));
			return this;
		}

		public Builder withPhasedReload(
			ReloadPhase phase,
			Predicate<ConditionContext> predicate,
			long cooldownTime,
			ReloadAnimation reloadAnimation) {
			this.phasedReloads.add(
				new PhasedReload(phase, predicate, cooldownTime, TimeUnit.MILLISECOND, reloadAnimation));
			return this;
		}

		public Builder withPelletCount(int pelletCount) {
			this.pelletCount = pelletCount;
			return this;
		}

		public Builder withPelletSpread(double pelletSpread) {
			this.pelletSpread = pelletSpread;
			return this;
		}

		public Builder withGunRandomizationAmplitude(double gunRandomizationAmplitude) {
			this.gunRandomizationAmplitude = gunRandomizationAmplitude;
			return this;
		}

		public Builder withBurstShots(int burstShots) {
			this.burstShots = burstShots;
			return this;
		}

		public Builder withInaccuracy(double inaccuracy) {
			this.inaccuracy = inaccuracy;
			return this;
		}

		public Builder withInaccuracyAiming(double inaccuracyAiming) {
			this.inaccuracyAiming = inaccuracyAiming;
			return this;
		}

		public Builder withInaccuracySprinting(double inaccuracySprinting) {
			this.inaccuracySprinting = inaccuracySprinting;
			return this;
		}

		public Builder withGlow(String glowingPartName) {
			return this.withGlow(glowingPartName, null);
		}

		public Builder withGlow(String glowingPartName, String textureName) {
			return this.withGlow(
				Collections.singleton(FirePhase.ANY), Collections.singleton(glowingPartName), textureName);
		}

		public Builder withGlow(Collection<FirePhase> firePhases, String glowingPartName) {
			return this.withGlow(firePhases, Collections.singleton(glowingPartName), null);
		}

		public Builder withGlow(Collection<FirePhase> firePhases, Collection<String> glowingPartNames, String texture) {
			GlowAnimationController.Builder builder =
				(new GlowAnimationController.Builder()).withFirePhases(firePhases);
			if (texture != null) {
				builder.withTexture(ResourceLocation.fromNamespaceAndPath("pointblank", texture));
			}

			builder.withGlowingPartNames(glowingPartNames);
			this.glowEffectBuilders.add(builder);
			return this;
		}

		public Builder withGlow(
			Collection<FirePhase> firePhases,
			String glowingPartName,
			String texture,
			SpriteAnimationType spriteAnimationType,
			int spriteRows,
			int spriteColumns,
			int spritesPerSecond,
			Direction... directions) {
			GlowAnimationController.Builder builder =
				(new GlowAnimationController.Builder()).withFirePhases(firePhases);
			if (texture != null) {
				builder.withTexture(ResourceLocation.fromNamespaceAndPath("pointblank", texture));
			}

			builder.withGlowingPartNames(Collections.singleton(glowingPartName));
			builder.withSprites(spriteRows, spriteColumns, spritesPerSecond, spriteAnimationType);
			builder.withDirections(directions);
			this.glowEffectBuilders.add(builder);
			return this;
		}

		public Builder
		withRotation(String phase, String modelPart, double rpm, double acceleration, double deceleration) {
			RotationAnimationController.Builder builder = (new RotationAnimationController.Builder())
															  .withPhase(phase)
															  .withModelPart(modelPart)
															  .withAccelerationRate(acceleration)
															  .withDecelerationRate(deceleration)
															  .withRotationsPerMinute(rpm);
			this.rotationEffectBuilders.add(builder);
			return this;
		}

		public Builder withRotation(
			RotationAnimationController.PhaseMapper phaseMapper,
			String modelPart,
			double rpm,
			double acceleration,
			double deceleration) {
			RotationAnimationController.Builder builder = (new RotationAnimationController.Builder())
															  .withPhaseMapper(phaseMapper)
															  .withModelPart(modelPart)
															  .withAccelerationRate(acceleration)
															  .withDecelerationRate(deceleration)
															  .withRotationsPerMinute(rpm);
			this.rotationEffectBuilders.add(builder);
			return this;
		}

		public Builder
		withEffect(FirePhase firePhase, Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilder) {
			List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> builders =
				this.effectBuilders.computeIfAbsent(firePhase, (k) -> new ArrayList<>());
			builders.add(effectBuilder);
			return this;
		}

		public Builder withBulletData(BulletData bulletData) {
			this.bulletData = bulletData;
			return this;
		}

		public Builder withBobbing(double bobbing) {
			this.bobbing = Mth.clamp((float)bobbing, 0.0F, 2.0F);
			return this;
		}

		public Builder withBobbingOnAim(double bobbingOnAim) {
			this.bobbingOnAim = Mth.clamp((float)bobbingOnAim, 0.0F, 2.0F);
			return this;
		}

		public Builder withBobbingRollMultiplier(double bobbingRollMultiplier) {
			this.bobbingRollMultiplier = Mth.clamp((float)bobbingRollMultiplier, 0.0F, 10.0F);
			return this;
		}

		public Builder withScript(Script script) {
			this.mainScript = script;
			return this;
		}

		public GunItem build() {
			return this.build("pointblank");
		}

		public GunItem build(String namespace) {
			return new GunItem(this, namespace);
		}

		public Builder withJsonObject(JsonObject obj) {
			super.withJsonObject(obj);
			Builder builder = this;
			this.withScript(JsonUtil.getJsonScript(obj));
			this.withAdsSpeed(JsonUtil.getJsonInt(obj, "adsSpeed", 400));
			this.withName(obj.getAsJsonPrimitive("name").getAsString());
			this.withAnimationType(
				(AnimationType)JsonUtil.getEnum(obj, "animationType", AnimationType.class, AnimationType.RIFLE, true));
			this.withFirstPersonFallbackAnimations(JsonUtil.getJsonString(obj, "firstPersonFallbackAnimations", null));
			this.withThirdPersonFallbackAnimations(JsonUtil.getJsonString(obj, "thirdPersonFallbackAnimations", null));
			this.withModelScale(JsonUtil.getJsonFloat(obj, "modelScale", 1.0F));
			this.withTradePrice(
				JsonUtil.getJsonFloat(obj, "tradePrice", Float.NaN),
				JsonUtil.getJsonInt(obj, "traceBundleQuantity", 1),
				JsonUtil.getJsonInt(obj, "tradeLevel", 0));
			JsonPrimitive jsonMaxAmmoCapacity = obj.getAsJsonPrimitive("maxAmmoCapacity");
			if (jsonMaxAmmoCapacity.isString() && "infinite".equalsIgnoreCase(jsonMaxAmmoCapacity.getAsString())) {
				this.withMaxAmmoCapacity(Integer.MAX_VALUE);
			} else {
				this.withMaxAmmoCapacity(obj.getAsJsonPrimitive("maxAmmoCapacity").getAsInt());
			}

			this.withCraftingDuration(JsonUtil.getJsonInt(obj, "craftingDuration", 1000), TimeUnit.MILLISECOND);
			this.withMaxAmmoPerReloadIteration(
				JsonUtil.getJsonInt(obj, "maxAmmoPerReloadIteration", Integer.MAX_VALUE));
			this.withAimingEnabled(JsonUtil.getJsonBoolean(obj, "aimingEnabled", true));
			this.withHitscan(JsonUtil.getJsonBoolean(obj, "hitscan", false));
			this.withTargetLock(JsonUtil.getJsonInt(obj, "minTargetLockTime", 0), TimeUnit.MILLISECOND);
			this.withRpm(JsonUtil.getJsonInt(obj, "rpm", 600));
			this.withPrepareIdleCooldownDuration(
				JsonUtil.getJsonInt(obj, "prepareIdleCooldownDuration", 0), TimeUnit.MILLISECOND);
			this.withViewRecoilMaxPitch(JsonUtil.getJsonInt(obj, "viewRecoilMaxPitch", 20));
			this.withViewRecoilDuration(JsonUtil.getJsonInt(obj, "viewRecoilDuration", 100), TimeUnit.MILLISECOND);
			this.withShakeRecoilDuration(JsonUtil.getJsonInt(obj, "shakeRecoilDuration", 400), TimeUnit.MILLISECOND);
			this.withGunRecoilDuration(JsonUtil.getJsonInt(obj, "gunRecoilDuration", 500), TimeUnit.MILLISECOND);
			this.withIdleRandomizationDuration(
				JsonUtil.getJsonInt(obj, "idleRandomizationDuration", 2500), TimeUnit.MILLISECOND);
			this.withRecoilRandomizationDuration(
				JsonUtil.getJsonInt(obj, "recoilRandomizationDuration", 250), TimeUnit.MILLISECOND);
			this.withBurstShots(JsonUtil.getJsonInt(obj, "burstShots", 3));
			this.withReloadCooldownDuration(JsonUtil.getJsonInt(obj, "reloadCooldownTime", 1000), TimeUnit.MILLISECOND);
			this.withPelletCount(JsonUtil.getJsonInt(obj, "pelletCount", 0));
			this.withPrepareFireCooldownDuration(
				JsonUtil.getJsonInt(obj, "prepareFireCooldownDuration", 0), TimeUnit.MILLISECOND);
			this.withCompleteFireCooldownDuration(
				JsonUtil.getJsonInt(obj, "completeFireCooldownDuration", 0), TimeUnit.MILLISECOND);
			this.withEnableFireModeCooldownDuration(
				JsonUtil.getJsonInt(obj, "enableFireModeCooldownDuration", 0), TimeUnit.MILLISECOND);
			this.withViewRecoilAmplitude(JsonUtil.getJsonDouble(obj, "viewRecoilAmplitude", 1.0F));
			this.withShakeRecoilAmplitude(JsonUtil.getJsonDouble(obj, "shakeRecoilAmplitude", 0.5F));
			this.withShakeRecoilSpeed(JsonUtil.getJsonDouble(obj, "shakeRecoilSpeed", 8.0F));
			this.withShakeDecay(JsonUtil.getJsonDouble(obj, "shakeDecay", 0.98));
			this.withGunRecoilInitialAmplitude(JsonUtil.getJsonDouble(obj, "gunRecoilInitialAmplitude", 0.3));
			this.withGunRecoilRateOfAmplitudeDecay(JsonUtil.getJsonDouble(obj, "gunRecoilRateOfAmplitudeDecay", 0.8));
			this.withGunRecoilInitialAngularFrequency(
				JsonUtil.getJsonDouble(obj, "gunRecoilInitialAngularFrequency", 1.0F));
			this.withGunRecoilRateOfFrequencyIncrease(
				JsonUtil.getJsonDouble(obj, "gunRecoilRateOfFrequencyIncrease", 0.05));
			this.withGunRecoilPitchMultiplier(JsonUtil.getJsonDouble(obj, "gunRecoilPitchMultiplier", 1.0F));
			this.withGunRandomizationAmplitude(JsonUtil.getJsonDouble(obj, "gunRandomizationAmplitude", 0.01));
			this.withAimingCurveX(JsonUtil.getJsonDouble(obj, "aimingCurveX", 0.0F));
			this.withAimingCurveY(JsonUtil.getJsonDouble(obj, "aimingCurveY", -0.07));
			this.withAimingCurveZ(JsonUtil.getJsonDouble(obj, "aimingCurveZ", 0.3));
			this.withAimingCurvePitch(JsonUtil.getJsonDouble(obj, "aimingCurvePitch", -0.01));
			this.withAimingCurveYaw(JsonUtil.getJsonDouble(obj, "aimingCurveYaw", -0.01));
			this.withAimingCurveRoll(JsonUtil.getJsonDouble(obj, "aimingCurveRoll", -0.01));
			this.withAimingZoom(JsonUtil.getJsonDouble(obj, "aimingZoom", 0.05));
			this.withPipScopeZoom(JsonUtil.getJsonDouble(obj, "pipScopeZoom", 0.0F));
			this.withShotsPerRecoil(JsonUtil.getJsonInt(obj, "shotsPerRecoil", 1));
			this.withShotsPerTrace(JsonUtil.getJsonInt(obj, "shotsPerTrace", 1));
			this.withPelletSpread(JsonUtil.getJsonDouble(obj, "pelletSpread", 1.0F));
			this.withInaccuracy(JsonUtil.getJsonDouble(obj, "inaccuracy", 0.03));
			this.withInaccuracyAiming(JsonUtil.getJsonDouble(obj, "inaccuracyAiming", 0.0F));
			this.withInaccuracySprinting(JsonUtil.getJsonDouble(obj, "inaccuracySprinting", 0.1));
			this.withJumpMultiplier(JsonUtil.getJsonDouble(obj, "jumpMultiplier", 1.0F));
			this.withScopeOverlay(
				obj.has("scopeOverlay") ? obj.getAsJsonPrimitive("scopeOverlay").getAsString() : null);
			this.withReticleOverlay(
				obj.has("reticleOverlay") ? obj.getAsJsonPrimitive("reticleOverlay").getAsString() : null);
			this.withTargetLockOverlay(
				obj.has("targetLockOverlay") ? obj.getAsJsonPrimitive("targetLockOverlay").getAsString() : null);
			JsonElement targetLockedSoundElem = obj.get("targetLockedSound");
			if (targetLockedSoundElem != null && !targetLockedSoundElem.isJsonNull()) {
				String targetLockedSoundName = targetLockedSoundElem.getAsString();
				this.withTargetLockedSound((() -> SoundRegistry.getSoundEvent(targetLockedSoundName)));
			}

			JsonElement targetStartLockingSoundElem = obj.get("targetStartLockingSound");
			if (targetStartLockingSoundElem != null && !targetStartLockingSoundElem.isJsonNull()) {
				String targetStargetLockingdSoundName = targetStartLockingSoundElem.getAsString();
				this.withTargetStartLockingSound((() -> SoundRegistry.getSoundEvent(targetStargetLockingdSoundName)));
			}

			List<String> fireModeNames = JsonUtil.getStrings(obj, "fireModes");
			this.withFireModes(fireModeNames.stream()
								   .map((n) -> FireMode.valueOf(n.toUpperCase(Locale.ROOT)))
								   .toArray(FireMode[] ::new));
			this.withBulletData(BulletData.fromJson(obj));

			for (JsonObject jsReloadShakeEffect : JsonUtil.getJsonObjects(obj, "reloadShakeEffects")) {
				builder.withReloadShakeEffect(
					jsReloadShakeEffect.getAsJsonPrimitive("start").getAsInt(),
					jsReloadShakeEffect.getAsJsonPrimitive("duration").getAsInt(),
					TimeUnit.MILLISECOND,
					JsonUtil.getJsonDouble(jsReloadShakeEffect, "initialAmplitude", 0.15),
					JsonUtil.getJsonDouble(jsReloadShakeEffect, "rateOfAmplitudeDecay", 0.3),
					JsonUtil.getJsonDouble(jsReloadShakeEffect, "initialAngularFrequency", 1.0F),
					JsonUtil.getJsonDouble(jsReloadShakeEffect, "rateOfFrequencyIncrease", 0.01));
			}

			if (hasFunction("overridePhasedReloads")) {
				List<PhasedReload> reloads = (List<PhasedReload>)invokeFunction("overridePhasedReloads", obj);
				for (PhasedReload reload : reloads)
					builder.withPhasedReload(reload);
			} else {
				for (JsonObject jsPhasedReload : JsonUtil.getJsonObjects(obj, "phasedReloads")) {
					List<ReloadShakeEffect> shakeEffects = new ArrayList<>();

					for (JsonObject jsReloadShakeEffect : JsonUtil.getJsonObjects(jsPhasedReload, "shakeEffects")) {
						shakeEffects.add(new ReloadShakeEffect(
							jsReloadShakeEffect.getAsJsonPrimitive("start").getAsLong(),
							jsReloadShakeEffect.getAsJsonPrimitive("duration").getAsLong(),
							TimeUnit.MILLISECOND,
							JsonUtil.getJsonDouble(jsReloadShakeEffect, "initialAmplitude", 0.15),
							JsonUtil.getJsonDouble(jsReloadShakeEffect, "rateOfAmplitudeDecay", 0.3),
							JsonUtil.getJsonDouble(jsReloadShakeEffect, "initialAngularFrequency", 1.0F),
							JsonUtil.getJsonDouble(jsReloadShakeEffect, "rateOfFrequencyIncrease", 0.01)));
					}

					Predicate<ConditionContext> condition = jsPhasedReload.has("condition")
																? Conditions.fromJson(jsPhasedReload.get("condition"))
																: (ctx) -> true;
					builder.withPhasedReload(new PhasedReload(
						ReloadPhase.valueOf(jsPhasedReload.getAsJsonPrimitive("phase").getAsString()),
						condition,
						jsPhasedReload.getAsJsonPrimitive("duration").getAsInt(),
						TimeUnit.MILLISECOND,
						new ReloadAnimation(
							jsPhasedReload.getAsJsonPrimitive("animation").getAsString(), shakeEffects)));
				}
			}

			JsonElement reloadAnimationElem = obj.get("reloadAnimation");
			String reloadAnimation = reloadAnimationElem != null ? reloadAnimationElem.getAsString() : null;
			builder.withReloadAnimation(reloadAnimation);
			float fireSoundVolume = JsonUtil.getJsonFloat(obj, "fireSoundVolume", 5.0F);
			JsonElement fireSoundElem = obj.get("fireSound");
			if (fireSoundElem != null && !fireSoundElem.isJsonNull()) {
				String fireSoundName = fireSoundElem.getAsString();
				builder.withFireSound((() -> SoundRegistry.getSoundEvent(fireSoundName)), fireSoundVolume);
			}

			JsonElement reloadSoundElem = obj.get("reloadSound");
			if (reloadSoundElem != null && !reloadSoundElem.isJsonNull()) {
				String reloadSoundName = reloadSoundElem.getAsString();
				builder.withReloadSound((() -> SoundRegistry.getSoundEvent(reloadSoundName)));
			}

			List<String> compatibleAmmoNames = JsonUtil.getStrings(obj, "compatibleAmmo");
			List<Supplier<AmmoItem>> compatibleAmmo = new ArrayList<>();

			for (String compatibleAmmoName : compatibleAmmoNames) {
				Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(compatibleAmmoName);
				if (ri != null) {
					compatibleAmmo.add((Supplier)ri);
				}
			}

			builder.withCompatibleAmmo(compatibleAmmo);

			for (JsonObject rotationEffect : JsonUtil.getJsonObjects(obj, "rotations")) {
				builder.withRotation(
					JsonUtil.getJsonString(rotationEffect, "phase", "fire"),
					JsonUtil.getJsonString(rotationEffect, "modelPart", null),
					JsonUtil.getJsonDouble(rotationEffect, "rpm", 180.0F),
					JsonUtil.getJsonDouble(rotationEffect, "acceleration", 1.0F),
					JsonUtil.getJsonDouble(rotationEffect, "deceleration", 5.0F));
			}

			for (JsonObject effect : JsonUtil.getJsonObjects(obj, "effects")) {
				FirePhase firePhase = (FirePhase)JsonUtil.getEnum(effect, "phase", FirePhase.class, null, true);
				String effectName = JsonUtil.getJsonString(effect, "name");
				Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier =
					() -> EffectRegistry.getEffectBuilderSupplier(effectName).get();
				builder.withEffect(firePhase, supplier);
			}

			builder.withBobbing(JsonUtil.getJsonDouble(obj, "bobbing", 1.0F));
			builder.withBobbingOnAim(JsonUtil.getJsonFloat(obj, "bobbingOnAim", 0.3F));
			builder.withBobbingRollMultiplier(JsonUtil.getJsonDouble(obj, "bobbingRollMultiplier", 1.0F));

			for (JsonObject glowingPart : JsonUtil.getJsonObjects(obj, "glowingParts")) {
				String partName = JsonUtil.getJsonString(glowingPart, "name");
				List<String> firePhaseNames = JsonUtil.getStrings(obj, "phases");
				List<FirePhase> firePhases =
					firePhaseNames.stream().map((n) -> FirePhase.valueOf(n.toUpperCase(Locale.ROOT))).toList();
				if (firePhases.isEmpty()) {
					firePhases = Collections.singletonList(FirePhase.ANY);
				}

				String textureName = JsonUtil.getJsonString(glowingPart, "texture", null);
				Direction direction =
					(Direction)JsonUtil.getEnum(glowingPart, "direction", Direction.class, null, true);
				JsonObject spritesObj = glowingPart.getAsJsonObject("sprites");
				if (spritesObj != null) {
					int rows = JsonUtil.getJsonInt(spritesObj, "rows", 1);
					int columns = JsonUtil.getJsonInt(spritesObj, "columns", 1);
					int fps = JsonUtil.getJsonInt(spritesObj, "fps", 60);
					SpriteAnimationType spriteAnimationType = (SpriteAnimationType)JsonUtil.getEnum(
						spritesObj, "type", SpriteAnimationType.class, SpriteAnimationType.LOOP, true);
					if (direction != null) {
						builder.withGlow(
							firePhases, partName, textureName, spriteAnimationType, rows, columns, fps, direction);
					} else {
						builder.withGlow(firePhases, partName, textureName, spriteAnimationType, rows, columns, fps);
					}
				} else {
					builder.withGlow(firePhases, Collections.singletonList(partName), textureName);
				}
			}

			for (String compatibleAttachmentName : JsonUtil.getStrings(obj, "compatibleAttachments")) {
				Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(compatibleAttachmentName);
				if (ri != null) {
					this.withCompatibleAttachment(() -> (Attachment)ri.get());
				}
			}

			List<String> compatibleAttachmentGroups = JsonUtil.getStrings(obj, "compatibleAttachmentGroups");
			this.compatibleAttachmentGroups.addAll(compatibleAttachmentGroups);

			for (JsonObject featureObj : JsonUtil.getJsonObjects(obj, "features")) {
				FeatureBuilder<?, ?> featureBuilder = Features.fromJson(featureObj);
				this.withFeature(featureBuilder);
			}

			for (String defaultAttachmentName : JsonUtil.getStrings(obj, "defaultAttachments")) {
				Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(defaultAttachmentName);
				if (ri != null) {
					this.withDefaultAttachment(() -> (Attachment)ri.get());
				}
			}

			for (JsonObject jsIdleAnimation : JsonUtil.getJsonObjects(obj, "idleAnimations")) {
				Predicate<ConditionContext> condition = jsIdleAnimation.has("condition")
															? Conditions.fromJson(jsIdleAnimation.get("condition"))
															: (ctx) -> true;
				builder.withIdleAnimation(
					JsonUtil.getJsonString(jsIdleAnimation, "name", "animation.model.idle"),
					condition,
					jsIdleAnimation.getAsJsonPrimitive("duration").getAsInt(),
					TimeUnit.MILLISECOND);
			}

			List<JsonObject> jsInspects = JsonUtil.getJsonObjects(obj, "inspectAnimations");

			for (JsonObject jsInspect : jsInspects) {
				Predicate<ConditionContext> condition =
					jsInspect.has("condition") ? Conditions.fromJson(jsInspect.get("condition")) : (ctx) -> true;
				builder.withInspectAnimation(
					JsonUtil.getJsonString(jsInspect, "name", "animation.model.inspect"),
					condition,
					jsInspect.getAsJsonPrimitive("duration").getAsInt(),
					TimeUnit.MILLISECOND);
			}

			if (jsInspects.isEmpty()) {
				builder.withInspectCooldownDuration(
					JsonUtil.getJsonInt(obj, "inspectCooldownDuration", 1000), TimeUnit.MILLISECOND);
			}

			List<JsonObject> jsDrawAnimations = JsonUtil.getJsonObjects(obj, "drawAnimations");

			for (JsonObject jsIdleAnimation : jsDrawAnimations) {
				Predicate<ConditionContext> condition = jsIdleAnimation.has("condition")
															? Conditions.fromJson(jsIdleAnimation.get("condition"))
															: (ctx) -> true;
				builder.withDrawAnimation(
					JsonUtil.getJsonString(jsIdleAnimation, "name", "animation.model.draw"),
					condition,
					jsIdleAnimation.getAsJsonPrimitive("duration").getAsInt(),
					TimeUnit.MILLISECOND);
			}

			if (jsDrawAnimations.isEmpty()) {
				builder.withDrawCooldownDuration(
					JsonUtil.getJsonInt(obj, "drawCooldownDuration", 500), TimeUnit.MILLISECOND);
			}

			for (JsonObject jsFireAnimation : JsonUtil.getJsonObjects(obj, "fireAnimations")) {
				Predicate<ConditionContext> condition = jsFireAnimation.has("condition")
															? Conditions.fromJson(jsFireAnimation.get("condition"))
															: (ctx) -> true;
				builder.withFireAnimation(
					JsonUtil.getJsonString(jsFireAnimation, "name", "animation.model.inspect"),
					condition,
					0,
					TimeUnit.MILLISECOND);
			}

			return this;
		}

		@Override
		public @org.jetbrains.annotations.Nullable Script getScript() {
			return mainScript;
		}
	}
}
