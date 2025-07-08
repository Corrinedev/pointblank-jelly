package mod.pbj;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@EventBusSubscriber(
   modid = "pointblank",
   bus = Bus.MOD
)
public class Config {
   private static final Builder BUILDER = new Builder();
   public static final ForgeConfigSpec SPEC;

   public static final ConfigValue<AutoReload> AUTO_RELOAD;
   private static final DoubleValue SCOPE_AIMING_MOUSE_SENSITIVITY;
   private static final BooleanValue RESET_AUTO_FIRE_PITCH_ENABLED;
   private static final DoubleValue KNOCKBACK;
   private static final BooleanValue PARTICLE_EFFECTS_ENABLED;
   private static final BooleanValue OVERWRITE_DISABLED;
   private static final IntValue ARMS_DEALER_HOUSE_WEIGHT;
   private static final DoubleValue EMERALD_EXCHANGE_RATE;
   private static final BooleanValue THIRD_PERSON_ARM_POSE_ALWAYS_ON;
   private static final IntValue PIP_SCOPE_REFRESH_FRAME;
   private static final BooleanValue PIP_SCOPES_ENABLED;
   private static final IntValue PIP_SCOPE_COLOR_BALANCE_RED;
   private static final IntValue PIP_SCOPE_COLOR_BALANCE_GREEN;
   private static final IntValue PIP_SCOPE_COLOR_BALANCE_BLUE;
   private static final BooleanValue CUSTOM_SHADERS_ENABLED;
   private static final BooleanValue EXPLOSION_DESTROY_BLOCKS_ENABLED;
   private static final DoubleValue ITEM_DROP_CHANCE;
   private static final IntValue MAX_ITEM_DROP_COUNT;
   private static final BooleanValue BULLETS_BREAK_GLASS_ENABLED;
   private static final DoubleValue HITSCAN_DAMAGE_MODIFIER;
   private static final DoubleValue HEADSHOT_DAMAGE_MODIFIER;
   private static final ConfigValue<CrosshairType> CROSSHAIR_TYPE;
   private static final BooleanValue GORE_ENABLED;
   private static final BooleanValue ADVANCE_IRIS_INTEGRATION_ENABLED;
   private static final BooleanValue FIRST_PERSON_ANIMATIONS_ENABLED;
   private static final BooleanValue THIRD_PERSON_ANIMATIONS_ENABLED;
   public static final BooleanValue PLAYERHEADSHOTS;
   public static final BooleanValue MOBHEADSHOTS;
   public static final IntValue IFRAMES;
   public static final ConfigValue<ReticleRenderType> PIP_OVERLAY_RENDER_TYPE;
   public static final BooleanValue HANDS_ENABLED;
   public static final BooleanValue RETICLES_ENABLED;

   public static AutoReload autoReload;
   public static double scopeAimingMouseSensitivity;
   public static boolean resetAutoFirePitchEnabled;
   public static double knockback;
   public static boolean particleEffectsEnabled;
   public static int armsDealerHouseWeight;
   public static double emeraldExchangeRate;
   public static boolean thirdPersonArmPoseAlwaysOn;
   public static boolean pipScopesEnabled;
   public static int pipScopeRefreshFrame;
   public static int pipScopeColorBalanceRed;
   public static int pipScopeColorBalanceGreen;
   public static int pipScopeColorBalanceBlue;
   public static int iframes;
   public static boolean customShadersEnabled;
   public static boolean explosionDestroyBlocksEnabled;
   public static double itemDropChance;
   public static int maxItemDropCount;
   public static boolean bulletsBreakGlassEnabled;
   public static double hitscanDamageModifier;
   public static double headshotDamageModifier;
   public static CrosshairType crosshairType;
   public static boolean goreEnabled;
   public static boolean overwriteDisabled;
   public static boolean advancedIrisIntegrationEnabled;
   public static boolean firstPersonAnimationsEnabled;
   public static boolean thirdPersonAnimationsEnabled;
   public static boolean playerHeadshots;
   public static boolean mobHeadshots;

   @SubscribeEvent
   static void onLoad(ModConfigEvent event) {
      autoReload = AUTO_RELOAD.get();
      scopeAimingMouseSensitivity = SCOPE_AIMING_MOUSE_SENSITIVITY.get();
      resetAutoFirePitchEnabled = RESET_AUTO_FIRE_PITCH_ENABLED.get();
      knockback = KNOCKBACK.get();
      particleEffectsEnabled = PARTICLE_EFFECTS_ENABLED.get();
      overwriteDisabled = OVERWRITE_DISABLED.get();
      armsDealerHouseWeight = ARMS_DEALER_HOUSE_WEIGHT.get();
      emeraldExchangeRate = EMERALD_EXCHANGE_RATE.get();
      thirdPersonArmPoseAlwaysOn = THIRD_PERSON_ARM_POSE_ALWAYS_ON.get();
      pipScopeRefreshFrame = PIP_SCOPE_REFRESH_FRAME.get();
      pipScopesEnabled = PIP_SCOPES_ENABLED.get();
      pipScopeColorBalanceRed = PIP_SCOPE_COLOR_BALANCE_RED.get();
      pipScopeColorBalanceGreen = PIP_SCOPE_COLOR_BALANCE_GREEN.get();
      pipScopeColorBalanceBlue = PIP_SCOPE_COLOR_BALANCE_BLUE.get();
      customShadersEnabled = CUSTOM_SHADERS_ENABLED.get();
      explosionDestroyBlocksEnabled = EXPLOSION_DESTROY_BLOCKS_ENABLED.get();
      itemDropChance = ITEM_DROP_CHANCE.get();
      maxItemDropCount = MAX_ITEM_DROP_COUNT.get();
      bulletsBreakGlassEnabled = BULLETS_BREAK_GLASS_ENABLED.get();
      hitscanDamageModifier = HITSCAN_DAMAGE_MODIFIER.get();
      headshotDamageModifier = HEADSHOT_DAMAGE_MODIFIER.get();
      crosshairType = CROSSHAIR_TYPE.get();
      goreEnabled = GORE_ENABLED.get();
      advancedIrisIntegrationEnabled = ADVANCE_IRIS_INTEGRATION_ENABLED.get();
      firstPersonAnimationsEnabled = FIRST_PERSON_ANIMATIONS_ENABLED.get();
      thirdPersonAnimationsEnabled = THIRD_PERSON_ANIMATIONS_ENABLED.get();
      playerHeadshots = PLAYERHEADSHOTS.get();
      mobHeadshots = MOBHEADSHOTS.get();
      iframes = IFRAMES.get();
   }

   static {
      AUTO_RELOAD = BUILDER.comment("Enables auto-reloading of guns").defineEnum("autoReloadEnabled", AutoReload.CREATIVE);
      SCOPE_AIMING_MOUSE_SENSITIVITY = BUILDER.comment("Adjusts mouse sensitivity when using scopes").defineInRange("scopeAimingMouseSensitivity", 0.4D, 0.01D, 0.9D);
      RESET_AUTO_FIRE_PITCH_ENABLED = BUILDER.comment("Toggle to reset player pitch to original after auto fire").define("resetAutoFirePitchEnabled", true);
      KNOCKBACK = BUILDER.comment("Adjusts the knockback force applied to entities hit by gunfire, with higher values causing greater knockback distance.").defineInRange("knockback", 1.0D, 0.1D, 2.0D);
      PARTICLE_EFFECTS_ENABLED = BUILDER.comment("Enables particle effects").define("particleEffectsEnabled", true);
      OVERWRITE_DISABLED = BUILDER.comment("Disables overwriting of the base content pack").define("allowBasePackOverwrite", false);
      ARMS_DEALER_HOUSE_WEIGHT = BUILDER.comment("Sets the likelihood of an 'Arms Dealer' house appearing in new villages, with higher values increasing frequency and lower values making it rarer.").defineInRange("armsDealerHouse", 10, 0, 20);
      EMERALD_EXCHANGE_RATE = BUILDER.comment("Set the exchange rate to determine how many in-game price units are equivalent to one emerald.").defineInRange("emeraldExchangeRate", 100.0D, 1.0D, 1000.0D);
      THIRD_PERSON_ARM_POSE_ALWAYS_ON = BUILDER.comment("Controls whether the player's arm pose is permanently set to the aiming/firing position in third-person view, regardless of their current action with a gun.").define("thirdPersonArmPoseAlwaysOn", true);
      PIP_SCOPE_REFRESH_FRAME = BUILDER.comment("Specifies how often the \"picture-in-picture\" scope updates, with 1 being every frame, 2 for every other frame, etc. A higher number may improve performance.").defineInRange("pipScopeRefreshRate", 2, 0, 5);
      PIP_SCOPES_ENABLED = BUILDER.comment("Enables pip scopes").define("pipScopesEnabled", true);
      PIP_SCOPE_COLOR_BALANCE_RED = BUILDER.comment("Sets pip scope red color balance. This options works for shaders only.").defineInRange("pipScopeColorBalanceRed", 90, 0, 255);
      PIP_SCOPE_COLOR_BALANCE_GREEN = BUILDER.comment("Sets pip scope green color balance. This options works for shaders only.").defineInRange("pipScopeColorBalanceGreen", 105, 0, 255);
      PIP_SCOPE_COLOR_BALANCE_BLUE = BUILDER.comment("Sets pip scope blue color balance. This options works for shaders only.").defineInRange("pipScopeColorBalanceBlue", 110, 0, 255);
      CUSTOM_SHADERS_ENABLED = BUILDER.comment("Enables custom shaders").define("customShadersEnabled", true);
      EXPLOSION_DESTROY_BLOCKS_ENABLED = BUILDER.comment("Enables explosions to destroy blocks").define("explosionDestroyBlocksEnabled", true);
      ITEM_DROP_CHANCE = BUILDER.comment("Sets the probability of dropping some item(s) by monster mobs.").defineInRange("itemDropChance", 0.35D, 0.0D, 1.0D);
      MAX_ITEM_DROP_COUNT = BUILDER.comment("Sets the max count of items to be dropped by a monster mob.").defineInRange("maxItemDropCount", 7, 2, 64);
      BULLETS_BREAK_GLASS_ENABLED = BUILDER.comment("Enables bullets breaking glass").define("bulletsBreakGlassEnabled", true);
      HITSCAN_DAMAGE_MODIFIER = BUILDER.comment("Modifier to adjust damage dealt by hitscan weapons.").defineInRange("hitscanDamageModifier", 1.0D, 0.1D, 10.0D);
      HEADSHOT_DAMAGE_MODIFIER = BUILDER.comment("Modifier to adjust damage dealt by headshot.").defineInRange("headshotDamageModifier", 3.0D, 1.0D, 10.0D);
      IFRAMES = BUILDER.comment("Time in ticks before a player can be damaged again by a gun.").defineInRange("iframes", 1, 0, 20);
      CROSSHAIR_TYPE = BUILDER.comment("Sets the crosshair type.").defineEnum("crosshair", CrosshairType.DEFAULT);
      GORE_ENABLED = BUILDER.comment("Enables gore effects").define("goreEnabled", true);
      ADVANCE_IRIS_INTEGRATION_ENABLED = BUILDER.comment("Enables advanced integraiton with Iris shaders").define("advancedIrisIntegrationEnabled", true);
      FIRST_PERSON_ANIMATIONS_ENABLED = BUILDER.comment("Enables advanced first person animations").define("firstPersonAnimationsEnabled", true);
      THIRD_PERSON_ANIMATIONS_ENABLED = BUILDER.comment("Enables advanced third person animations").define("thirdPersonAnimationsEnabled", true);
      PLAYERHEADSHOTS = BUILDER.comment("Enables player headshots, recommended to disable if using FirstAid").define("playerHeadshots", true);
      MOBHEADSHOTS = BUILDER.comment("Enables mob headshots").define("playerHeadshots", true);
	  PIP_OVERLAY_RENDER_TYPE = BUILDER
	     .comment("How to render pip overlays (PARALLAX, NON_PARALLAX, or DISABLED)")
		 .defineEnum("pipOverlayRenderType", ReticleRenderType.PARALLAX);
	  HANDS_ENABLED = BUILDER.comment("Whether to render hands holding gun").define("handsEnabled", true);
	  RETICLES_ENABLED = BUILDER.comment("Whether to render reticles on red-dot sights/scopes").define("reticlesEnabled", true);

      SPEC = BUILDER.build();
   }

   public enum AutoReload {
      CREATIVE,
      SURVIVAL,
      ENABLED,
      DISABLED
   }

   public enum CrosshairType {
      DEFAULT,
      VANILLA,
      DISABLED
      }
	
	public enum ReticleRenderType {
		DISABLED,
		NON_PARALLAX,
		PARALLAX
	}
}
