package com.vicmatskiv.pointblank.registry;

import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.feature.AccuracyFeature;
import com.vicmatskiv.pointblank.feature.ActiveMuzzleFeature;
import com.vicmatskiv.pointblank.feature.AimingFeature;
import com.vicmatskiv.pointblank.feature.FireModeFeature;
import com.vicmatskiv.pointblank.feature.MuzzleFlashFeature;
import com.vicmatskiv.pointblank.feature.PartVisibilityFeature;
import com.vicmatskiv.pointblank.feature.PipFeature;
import com.vicmatskiv.pointblank.feature.RecoilFeature;
import com.vicmatskiv.pointblank.feature.ReticleFeature;
import com.vicmatskiv.pointblank.feature.SkinFeature;
import com.vicmatskiv.pointblank.feature.SoundFeature;
import com.vicmatskiv.pointblank.item.AnimationProvider;
import com.vicmatskiv.pointblank.item.AttachmentItem;
import com.vicmatskiv.pointblank.item.ConditionalAnimationProvider;
import com.vicmatskiv.pointblank.item.FireMode;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.item.ItemBuilder;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

public class AttachmentRegistry {
   public static final Supplier<AttachmentItem> MOA;
   public static final Supplier<AttachmentItem> DELTA;
   public static final Supplier<AttachmentItem> MOA_HG;
   public static final Supplier<AttachmentItem> OPERATORREFLEX;
   public static final Supplier<AttachmentItem> HOLOGRAPHIC558;
   public static final Supplier<AttachmentItem> HOLOGRAPHIC;
   public static final Supplier<AttachmentItem> HOLOGRAPHIC_EM;
   public static final Supplier<AttachmentItem> AIMPOINT;
   public static final Supplier<AttachmentItem> AIMPOINT_T2;
   public static final Supplier<AttachmentItem> SRS;
   public static final Supplier<AttachmentItem> RSPEC;
   public static final Supplier<AttachmentItem> ACOG;
   public static final Supplier<AttachmentItem> SPECTER;
   public static final Supplier<AttachmentItem> HI_RED_ZOOM;
   public static final Supplier<AttachmentItem> HI_RED;
   public static final Supplier<AttachmentItem> HAMR;
   public static final Supplier<AttachmentItem> EAGLESCOPE;
   public static final Supplier<AttachmentItem> SPEARSCOPE;
   public static final Supplier<AttachmentItem> SPEARSCOPE_BLACK;
   public static final Supplier<AttachmentItem> HAWKSCOPE;
   public static final Supplier<AttachmentItem> WOLFSCOPE;
   public static final Supplier<AttachmentItem> DRAKESCOPE;
   public static final Supplier<AttachmentItem> PRECISIONSCOPE;
   public static final Supplier<AttachmentItem> CANTED_RAIL;
   public static final Supplier<AttachmentItem> AR_SUPPRESSOR;
   public static final Supplier<AttachmentItem> AR_SUPPRESSOR_TAN;
   public static final Supplier<AttachmentItem> XM7_SUPPRESSOR;
   public static final Supplier<AttachmentItem> AK_SUPPRESSOR;
   public static final Supplier<AttachmentItem> SCARL_ISS;
   public static final Supplier<AttachmentItem> SMG_SUPPRESSOR;
   public static final Supplier<AttachmentItem> RF_SUPPRESSOR;
   public static final Supplier<AttachmentItem> HP_SUPPRESSOR;
   public static final Supplier<AttachmentItem> SG_SUPPRESSOR;
   public static final Supplier<AttachmentItem> SMG_MUZZLEBRAKE;
   public static final Supplier<AttachmentItem> P30L_COMPENSATOR;
   public static final Supplier<AttachmentItem> AR_MUZZLEBRAKE;
   public static final Supplier<AttachmentItem> FOREGRIP;
   public static final Supplier<AttachmentItem> FOREGRIP_TAN;
   public static final Supplier<AttachmentItem> SHORTGRIP;
   public static final Supplier<AttachmentItem> STUBBYGRIPTAN;
   public static final Supplier<AttachmentItem> STUBBYGRIP;
   public static final Supplier<AttachmentItem> HERAGRIP;
   public static final Supplier<AttachmentItem> AK_ROMGRIP;
   public static final Supplier<AttachmentItem> M9_STOCK;
   public static final Supplier<AttachmentItem> GLOCK_STOCK;
   public static final Supplier<AttachmentItem> M203LAUNCHER;
   public static final Supplier<AttachmentItem> GP25;
   public static final Supplier<AttachmentItem> FN40LAUNCHER;
   public static final Supplier<AttachmentItem> M870MODSHOTGUN;
   public static final Supplier<AttachmentItem> ULG99CANNON;
   public static final Supplier<AttachmentItem> M16A3KIT;
   public static final Supplier<AttachmentItem> STAR15MOD3RECEIVER;
   public static final Supplier<AttachmentItem> M4SOPMODII_BARTENDER;
   public static final Supplier<AttachmentItem> M4A1_ASIIMOV;
   public static final Supplier<AttachmentItem> L96A1_HYPERBEAST;

   public static void init() {
   }

   public static void registerTabItems(Consumer<ItemLike> entries) {
      entries.accept((ItemLike)MOA.get());
      entries.accept((ItemLike)DELTA.get());
      entries.accept((ItemLike)MOA_HG.get());
      entries.accept((ItemLike)OPERATORREFLEX.get());
      entries.accept((ItemLike)HOLOGRAPHIC.get());
      entries.accept((ItemLike)HOLOGRAPHIC_EM.get());
      entries.accept((ItemLike)HOLOGRAPHIC558.get());
      entries.accept((ItemLike)AIMPOINT.get());
      entries.accept((ItemLike)AIMPOINT_T2.get());
      entries.accept((ItemLike)SRS.get());
      entries.accept((ItemLike)RSPEC.get());
      entries.accept((ItemLike)ACOG.get());
      entries.accept((ItemLike)SPECTER.get());
      entries.accept((ItemLike)HAMR.get());
      entries.accept((ItemLike)HI_RED.get());
      entries.accept((ItemLike)HI_RED_ZOOM.get());
      entries.accept((ItemLike)EAGLESCOPE.get());
      entries.accept((ItemLike)SPEARSCOPE.get());
      entries.accept((ItemLike)SPEARSCOPE_BLACK.get());
      entries.accept((ItemLike)HAWKSCOPE.get());
      entries.accept((ItemLike)WOLFSCOPE.get());
      entries.accept((ItemLike)DRAKESCOPE.get());
      entries.accept((ItemLike)PRECISIONSCOPE.get());
      entries.accept((ItemLike)CANTED_RAIL.get());
      entries.accept((ItemLike)AR_MUZZLEBRAKE.get());
      entries.accept((ItemLike)SMG_MUZZLEBRAKE.get());
      entries.accept((ItemLike)P30L_COMPENSATOR.get());
      entries.accept((ItemLike)AR_SUPPRESSOR.get());
      entries.accept((ItemLike)AR_SUPPRESSOR_TAN.get());
      entries.accept((ItemLike)XM7_SUPPRESSOR.get());
      entries.accept((ItemLike)AK_SUPPRESSOR.get());
      entries.accept((ItemLike)SMG_SUPPRESSOR.get());
      entries.accept((ItemLike)RF_SUPPRESSOR.get());
      entries.accept((ItemLike)HP_SUPPRESSOR.get());
      entries.accept((ItemLike)SG_SUPPRESSOR.get());
      entries.accept((ItemLike)FOREGRIP.get());
      entries.accept((ItemLike)FOREGRIP_TAN.get());
      entries.accept((ItemLike)SHORTGRIP.get());
      entries.accept((ItemLike)STUBBYGRIPTAN.get());
      entries.accept((ItemLike)STUBBYGRIP.get());
      entries.accept((ItemLike)HERAGRIP.get());
      entries.accept((ItemLike)AK_ROMGRIP.get());
      entries.accept((ItemLike)M9_STOCK.get());
      entries.accept((ItemLike)GLOCK_STOCK.get());
      entries.accept((ItemLike)M16A3KIT.get());
      entries.accept((ItemLike)STAR15MOD3RECEIVER.get());
      entries.accept((ItemLike)SCARL_ISS.get());
      entries.accept((ItemLike)M203LAUNCHER.get());
      entries.accept((ItemLike)GP25.get());
      entries.accept((ItemLike)FN40LAUNCHER.get());
      entries.accept((ItemLike)M870MODSHOTGUN.get());
      entries.accept((ItemLike)ULG99CANNON.get());
      entries.accept((ItemLike)M4SOPMODII_BARTENDER.get());
      entries.accept((ItemLike)M4A1_ASIIMOV.get());
      entries.accept((ItemLike)L96A1_HYPERBEAST.get());
   }

   static {
      MOA = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("moa").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "sopmod_group", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights", "sub_sight").withFeature((new ReticleFeature.Builder()).withTexture("textures/item/reticledot_new.png").withParallaxEnabled(true).withMaxAngularOffset(2.5F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      DELTA = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("delta").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "sopmod_group", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights", "sub_sight").withFeature((new ReticleFeature.Builder()).withTexture("textures/item/reticledot_new.png").withParallaxEnabled(true).withMaxAngularOffset(5.0F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      MOA_HG = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("moa_hg").withCategory(AttachmentCategory.SCOPE).withGroup("hg_sights").withFeature((new ReticleFeature.Builder()).withTexture("textures/item/reticledot_new.png").withParallaxEnabled(true).withMaxAngularOffset(2.5F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      OPERATORREFLEX = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("operatorreflex").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights", "sub_sight").withFeature((new ReticleFeature.Builder()).withTexture("textures/item/reticledot_new.png").withParallaxEnabled(true).withMaxAngularOffset(5.0F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      HOLOGRAPHIC558 = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("holographic558").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights", "sub_sight").withFeature((new ReticleFeature.Builder()).withParallaxEnabled(true).withMaxAngularOffset(5.0F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      HOLOGRAPHIC = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("holographic").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights", "sub_sight").withFeature((new ReticleFeature.Builder()).withParallaxEnabled(true).withMaxAngularOffset(5.0F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      HOLOGRAPHIC_EM = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("holographic_em").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights", "sub_sight").withFeature((new ReticleFeature.Builder()).withParallaxEnabled(true).withMaxAngularOffset(5.0F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      AIMPOINT = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("aimpoint").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "sopmod_group", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights", "sub_sight").withFeature((new ReticleFeature.Builder()).withTexture("textures/item/reticledot_new.png").withParallaxEnabled(true).withMaxAngularOffset(5.0F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      AIMPOINT_T2 = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("aimpoint_t2").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights", "sub_sight").withFeature((new ReticleFeature.Builder()).withTexture("textures/item/reticledot_new.png").withParallaxEnabled(true).withMaxAngularOffset(5.0F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      SRS = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("srs").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights", "sub_sight").withFeature((new ReticleFeature.Builder()).withTexture("textures/item/reticledot_new.png").withParallaxEnabled(true).withMaxAngularOffset(5.0F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      RSPEC = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("rspec").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights", "sub_sight").withFeature((new ReticleFeature.Builder()).withTexture("textures/item/reticledot_new.png").withParallaxEnabled(true).withMaxAngularOffset(5.0F)).withFeature((new AimingFeature.Builder()).withZoom(0.3D)));
      ACOG = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("acog").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "sopmod_group", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/acog_pip.png").withParallaxEnabled(true).withZoom(0.8D)).withFeature((new AimingFeature.Builder()).withZoom(0.45D)).withCompatibleAttachmentGroup("sub_sight"));
      SPECTER = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("specter").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "sopmod_group", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/starscope_pip.png").withParallaxEnabled(true).withZoom(0.8D)).withFeature((new AimingFeature.Builder()).withZoom(0.45D)).withCompatibleAttachmentGroup("sub_sight"));
      HI_RED_ZOOM = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("hi_red_zoom").withCategory(AttachmentCategory.SCOPE).withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/hi_redscope2_pip.png").withMaskTexture("textures/gui/hi_red_mask.png").withParallaxEnabled(true).withZoom(0.8D)).withFeature((new AimingFeature.Builder()).withZoom(0.65D)));
      HI_RED = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("hi_red").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "sopmod_group", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/hi_redscope_pip.png").withMaskTexture("textures/gui/hi_red_mask.png").withParallaxEnabled(true).withZoom(0.6D)).withFeature((new AimingFeature.Builder()).withZoom(0.6D)).withCompatibleAttachment(HI_RED_ZOOM));
      HAMR = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("hamr").withCategory(AttachmentCategory.SCOPE).withGroup("sights_group", "ar_sightsandscopes", "sopmod_group", "m16_sightsandscopes", "deagle_sights", "snipers_sights", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/starscope_pip.png").withParallaxEnabled(true).withZoom(0.8D)).withFeature((new AimingFeature.Builder()).withZoom(0.45D).withViewBobbing(0.5D)).withFeature((new PartVisibilityFeature.Builder()).withShownPart("shroud", Conditions.hasAttachment(DELTA))).withCompatibleAttachmentGroup("sub_sight").withDefaultAttachment(DELTA));
      EAGLESCOPE = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("eaglescope").withCategory(AttachmentCategory.SCOPE).withGroup("ar_sightsandscopes", "sopmod_group", "m16_sightsandscopes", "snipers_sights", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/starscope_pip.png").withParallaxEnabled(true).withZoom(0.85D)).withFeature((new AimingFeature.Builder()).withViewBobbing(0.4000000059604645D).withZoom(0.57D)).withCompatibleAttachmentGroup("sights_group"));
      SPEARSCOPE = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("spear").withCategory(AttachmentCategory.SCOPE).withGroup("ar_sightsandscopes", "sopmod_group", "m16_sightsandscopes", "snipers_sights", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/starscope_pip.png").withParallaxEnabled(true).withZoom(0.8D)).withFeature((new AimingFeature.Builder()).withZoom(0.5D)));
      SPEARSCOPE_BLACK = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("spearblack").withCategory(AttachmentCategory.SCOPE).withGroup("ar_sightsandscopes", "sopmod_group", "m16_sightsandscopes", "snipers_sights", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/starscope_pip.png").withParallaxEnabled(true).withZoom(0.8D)).withFeature((new AimingFeature.Builder()).withZoom(0.5D)));
      HAWKSCOPE = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("hawk_scope").withCategory(AttachmentCategory.SCOPE).withGroup("snipers_sights", "m16_sightsandscopes", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/l96scope_pip.png").withParallaxEnabled(true).withZoom(0.83D)).withFeature((new AimingFeature.Builder()).withZoom(0.6D)).withCompatibleAttachmentGroup("sights_group"));
      WOLFSCOPE = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("wolf_scope").withCategory(AttachmentCategory.SCOPE).withGroup("snipers_sights", "m16_sightsandscopes", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/scope_pip.png").withParallaxEnabled(true).withZoom(0.84D)).withFeature((new AimingFeature.Builder()).withZoom(0.6D)));
      DRAKESCOPE = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("drake_scope").withCategory(AttachmentCategory.SCOPE).withGroup("snipers_sights", "m16_sightsandscopes", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/scope_pip.png").withParallaxEnabled(true).withZoom(0.84D)).withFeature((new AimingFeature.Builder()).withZoom(0.6D)));
      PRECISIONSCOPE = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("precision_scope").withCategory(AttachmentCategory.SCOPE).withGroup("snipers_sights", "m16_sightsandscopes", "sl8_sights").withFeature((new PipFeature.Builder()).withOverlayTexture("textures/gui/scope_pip.png").withParallaxEnabled(true).withZoom(0.81D)).withFeature((new AimingFeature.Builder()).withZoom(0.6D)).withCompatibleAttachmentGroup("sights_group"));
      CANTED_RAIL = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("cantedrail").withCategory(AttachmentCategory.RAIL).withGroup("sopmod_group", "m16_sightsandscopes").withCompatibleAttachmentGroup("sights_group"));
      AR_SUPPRESSOR = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("ar_suppressor").withDescription("description.pointblank.supressesWeapon").withCategory(AttachmentCategory.MUZZLE).withGroup("ar_muzzle", "ar_suppressors").withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH_SMALL)).withFeature((new RecoilFeature.Builder()).withRecoilModifier(1.2D)));
      AR_SUPPRESSOR_TAN = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("ar_suppressor_tan").withDescription("description.pointblank.supressesWeapon").withCategory(AttachmentCategory.MUZZLE).withGroup("ar_muzzle", "ar_suppressors").withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH_SMALL)).withFeature((new RecoilFeature.Builder()).withRecoilModifier(1.2D)));
      XM7_SUPPRESSOR = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("xm7_suppressor").withDescription("description.pointblank.supressesWeapon").withCategory(AttachmentCategory.MUZZLE).withGroup("xm7_muzzle").withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH_SMALL)).withFeature((new RecoilFeature.Builder()).withRecoilModifier(1.2D)));
      AK_SUPPRESSOR = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("ak_suppressor").withDescription("description.pointblank.supressesWeapon").withCategory(AttachmentCategory.MUZZLE).withGroup("ak_muzzle").withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH_SMALL)).withFeature((new RecoilFeature.Builder()).withRecoilModifier(1.2D)));
      SCARL_ISS = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("scarl_iss").withDescription("description.pointblank.supressesWeapon").withCategory(AttachmentCategory.MUZZLE).withGroup("ar_suppressors").withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH_SMALL)).withFeature((new RecoilFeature.Builder()).withRecoilModifier(1.2D)));
      SMG_SUPPRESSOR = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("smg_suppressor").withDescription("description.pointblank.supressesWeapon").withCategory(AttachmentCategory.MUZZLE).withGroup("smg_muzzle", "smg_suppressors").withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH_SMALL)).withFeature((new RecoilFeature.Builder()).withRecoilModifier(1.2D)));
      RF_SUPPRESSOR = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("rf_suppressor").withDescription("description.pointblank.supressesWeapon").withCategory(AttachmentCategory.MUZZLE).withGroup("rf_muzzle", "rf_suppressors").withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH)).withFeature((new RecoilFeature.Builder()).withRecoilModifier(1.2D)));
      HP_SUPPRESSOR = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("hp_suppressor").withDescription("description.pointblank.supressesWeapon").withCategory(AttachmentCategory.MUZZLE).withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH)).withFeature((new RecoilFeature.Builder()).withRecoilModifier(1.2D)));
      SG_SUPPRESSOR = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("sg_suppressor").withDescription("description.pointblank.supressesWeapon").withCategory(AttachmentCategory.MUZZLE).withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH)).withFeature((new RecoilFeature.Builder()).withRecoilModifier(1.2D)));
      SMG_MUZZLEBRAKE = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("smg_muzzlebrake").withCategory(AttachmentCategory.MUZZLE).withGroup("smg_muzzle").withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.8D)));
      P30L_COMPENSATOR = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("p30l_compensator").withCategory(AttachmentCategory.MUZZLE).withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.8D)));
      AR_MUZZLEBRAKE = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("ar_muzzlebrake").withCategory(AttachmentCategory.MUZZLE).withGroup("ar_muzzle", "xm7_muzzle", "ak_muzzle").withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.8D)));
      FOREGRIP = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("foregrip").withCategory(AttachmentCategory.UNDERBARREL).withGroup("underbarrel", "underbarrel_extra").withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.6D)));
      FOREGRIP_TAN = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("foregrip_tan").withCategory(AttachmentCategory.UNDERBARREL).withGroup("underbarrel", "underbarrel_extra").withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.6D)));
      SHORTGRIP = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("shortgrip").withCategory(AttachmentCategory.UNDERBARREL).withGroup("underbarrel", "underbarrel_extra").withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.75D)));
      STUBBYGRIPTAN = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("stubbygriptan").withCategory(AttachmentCategory.UNDERBARREL).withGroup("underbarrel", "underbarrel_extra").withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.75D)));
      STUBBYGRIP = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("stubbygrip").withCategory(AttachmentCategory.UNDERBARREL).withGroup("underbarrel", "underbarrel_extra").withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.75D)));
      HERAGRIP = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("heragrip").withCategory(AttachmentCategory.UNDERBARREL).withGroup("underbarrel_2", "underbarrel_extra").withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.6D)));
      AK_ROMGRIP = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("ak_romgrip").withCategory(AttachmentCategory.UNDERBARREL).withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.75D)));
      M9_STOCK = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("m9_stock").withCategory(AttachmentCategory.STOCK).withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.5D)).withFeature((new AccuracyFeature.Builder()).withAccuracyModifier(3.0D)));
      GLOCK_STOCK = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("glockstock").withCategory(AttachmentCategory.STOCK).withFeature((new RecoilFeature.Builder()).withRecoilModifier(0.5D)).withFeature((new AccuracyFeature.Builder()).withAccuracyModifier(3.0D)));
      M203LAUNCHER = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("m203launcher").withCategory(AttachmentCategory.UNDERBARREL).withGroup("underbarrel_extra").withFeature((new FireModeFeature.Builder()).withFireMode("grenade_launcher", FireMode.SINGLE, Component.m_237115_("label.pointblank.fireMode.grenade"), AmmoRegistry.GRENADE40MM, 1, 127, 0.0D, false, "animation.model.firegrenade", new FireModeInstance.ViewShakeDescriptor(350L, 2.0D, 2.0D))).withFeature((new SoundFeature.Builder()).withCondition(Conditions.selectedFireMode("grenade_launcher")).withFireSound(SoundRegistry.MGL_SHOOT, 6.0D)).withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle().negate()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.LAUNCHER_MUZZLE)).withFeature((new PartVisibilityFeature.Builder()).withShownPart("launchermain", Conditions.isGunOnGround())).withFeature((new ActiveMuzzleFeature.Builder()).withCondition(Conditions.selectedFireMode("grenade_launcher"))).withFeature((new RecoilFeature.Builder()).withCondition(Conditions.selectedFireMode("grenade_launcher")).withRecoilModifier(1.1D)));
      GP25 = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("gp25").withCategory(AttachmentCategory.UNDERBARREL).withFeature((new FireModeFeature.Builder()).withFireMode("grenade_launcher", FireMode.SINGLE, Component.m_237115_("label.pointblank.fireMode.grenade"), AmmoRegistry.GRENADE40MM, 1, 127, 0.0D, false, "animation.model.firegrenade", new FireModeInstance.ViewShakeDescriptor(350L, 2.0D, 2.0D))).withFeature((new SoundFeature.Builder()).withCondition(Conditions.selectedFireMode("grenade_launcher")).withFireSound(SoundRegistry.MGL_SHOOT, 6.0D)).withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle().negate()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.LAUNCHER_MUZZLE)).withFeature((new PartVisibilityFeature.Builder()).withShownPart("launchermain", Conditions.isGunOnGround())).withFeature((new ActiveMuzzleFeature.Builder()).withCondition(Conditions.selectedFireMode("grenade_launcher"))).withFeature((new RecoilFeature.Builder()).withCondition(Conditions.selectedFireMode("grenade_launcher")).withRecoilModifier(1.1D)));
      FN40LAUNCHER = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("fn40").withCategory(AttachmentCategory.UNDERBARREL).withFeature((new FireModeFeature.Builder()).withFireMode("grenade_launcher", FireMode.SINGLE, Component.m_237115_("label.pointblank.fireMode.grenade"), AmmoRegistry.GRENADE40MM, 1, 127, 0.0D, false, "animation.model.firegrenade", new FireModeInstance.ViewShakeDescriptor(350L, 2.0D, 2.0D))).withFeature((new SoundFeature.Builder()).withCondition(Conditions.selectedFireMode("grenade_launcher")).withFireSound(SoundRegistry.MGL_SHOOT, 6.0D)).withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle().negate()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.LAUNCHER_MUZZLE)).withFeature((new PartVisibilityFeature.Builder()).withShownPart("launchermain", Conditions.isGunOnGround())).withFeature((new ActiveMuzzleFeature.Builder()).withCondition(Conditions.selectedFireMode("grenade_launcher"))).withFeature((new RecoilFeature.Builder()).withCondition(Conditions.selectedFireMode("grenade_launcher")).withRecoilModifier(1.1D)));
      M870MODSHOTGUN = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("m870modshotgun").withCategory(AttachmentCategory.UNDERBARREL).withGroup("underbarrel_extra").withCompatibleAttachment(SG_SUPPRESSOR).withFeature((new FireModeFeature.Builder()).withFireMode((new FireModeFeature.FireModeDescriptor.Builder()).withName("shotgun").withType(FireMode.SINGLE).withDisplayName(Component.m_237115_("label.pointblank.fireMode.shotgun")).withIsUsingDefaultMuzzle(false).withAmmoSupplier(AmmoRegistry.AMMO12GAUGE).withMaxAmmoCapacity(8).withRpm(300).withDamage(0.8D).withPelletSpread(0.1D).withPelletCount(30).withFireAnimationProvider(new AnimationProvider.Simple("animation.model.fireshotgun")).withViewShakeDescriptor(new FireModeInstance.ViewShakeDescriptor(350L, 2.0D, 2.0D)).build())).withFeature((new SoundFeature.Builder()).withFireSound(SoundRegistry.M870, 6.0D, Conditions.selectedFireMode("shotgun").and(Conditions.doesNotHaveAttachmentAtPathPrefix("//m870modshotgun/sg_suppressor"))).withFireSound(SoundRegistry.SPAS12_SILENCED, 1.0D, Conditions.selectedFireMode("shotgun").and(Conditions.hasAttachmentAtPathPrefix("//m870modshotgun/sg_suppressor")))).withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle().negate()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH)).withFeature((new PartVisibilityFeature.Builder()).withShownPart("shotgunmain", Conditions.isGunOnGround())).withFeature((new ActiveMuzzleFeature.Builder()).withCondition(Conditions.selectedFireMode("shotgun"))));
      ULG99CANNON = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("ulg99cannon").withCategory(AttachmentCategory.UNDERBARREL).withGroup("underbarrel_extra").withFeature((new FireModeFeature.Builder()).withFireMode((new FireModeFeature.FireModeDescriptor.Builder()).withName("laser_cannon").withType(FireMode.SINGLE).withDisplayName(Component.m_237115_("label.pointblank.fireMode.laser")).withIsUsingDefaultMuzzle(false).withAmmoSupplier(AmmoRegistry.AMMOLASERCHARGE).withMaxAmmoCapacity(10).withRpm(80).withDamage(25.0D).withFireAnimationProvider(new AnimationProvider.Simple("animation.model.firelaser")).withPrepareFireAnimationProvider((new ConditionalAnimationProvider.Builder()).withAnimation("animation.model.preparefire", (ctx) -> {
         return true;
      }, 570L, TimeUnit.MILLISECOND).build()).withViewShakeDescriptor(new FireModeInstance.ViewShakeDescriptor(350L, 2.0D, 2.0D)).build())).withFeature((new SoundFeature.Builder()).withCondition(Conditions.selectedFireMode("laser_cannon")).withFireSound(SoundRegistry.LASER, 6.0D)).withFeature((new MuzzleFlashFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle().negate()).withEffect(GunItem.FirePhase.HIT_SCAN_ACQUIRED, EffectRegistry.LASER)).withFeature((new PartVisibilityFeature.Builder()).withShownPart("lasermain", Conditions.isGunOnGround())).withFeature((new ActiveMuzzleFeature.Builder()).withCondition(Conditions.selectedFireMode("laser_cannon"))));
      M16A3KIT = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("m16a3kit").withDescription("description.pointblank.allowsSightsetc").withCategory(AttachmentCategory.SCOPE).withCompatibleAttachmentGroup("m16_sightsandscopes").withCompatibleAttachmentGroup("underbarrel").withCompatibleAttachmentGroup("underbarrel_2").withCompatibleAttachment(M203LAUNCHER).withCompatibleAttachment(ULG99CANNON).withCompatibleAttachment(M870MODSHOTGUN).withFeature((new AimingFeature.Builder()).withCondition(Conditions.doesNotHaveAttachmentInCategory(AttachmentCategory.SCOPE)).withZoom(0.25D)).withFeature((new PartVisibilityFeature.Builder()).withHiddenPart("carryhandle", Conditions.hasAttachmentGroup("m16_sightsandscopes"))));
      STAR15MOD3RECEIVER = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("star15mod3receiver").withCategory("receiver").withFeature((new FireModeFeature.Builder()).withFireMode((new FireModeFeature.FireModeDescriptor.Builder()).withName("automatic").withType(FireMode.AUTOMATIC).withDisplayName(Component.m_237115_("label.pointblank.fireMode.automatic")).build())));
      M4SOPMODII_BARTENDER = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("m4sopmodii_bartender").withCategory(AttachmentCategory.SKIN).withGroup("m4sopmodii_skins").withFeature((new SkinFeature.Builder()).withTexture("textures/item/skin_sopmod_bartender.png")));
      M4A1_ASIIMOV = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("m4a1_asiimov").withCategory(AttachmentCategory.SKIN).withGroup("m4a1_skins").withFeature((new SkinFeature.Builder()).withTexture("textures/item/skin_m4a1_asiimov.png")));
      L96A1_HYPERBEAST = ItemRegistry.ITEMS.register((ItemBuilder)(new AttachmentItem.Builder()).withName("l96a1_hyperbeast").withCategory(AttachmentCategory.SKIN).withGroup("l96a1_skins").withFeature((new SkinFeature.Builder()).withTexture("textures/item/skin_l96a1_hyperbeast.png")));
   }
}
