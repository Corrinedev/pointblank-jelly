package mod.pbj.registry;

import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.toml.TomlParser;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import mod.pbj.PointBlankJelly;
import mod.pbj.feature.*;
import mod.pbj.util.InternalFiles;
import net.minecraftforge.fml.loading.FMLPaths;

public class FeatureTypeRegistry {
	private static int featureTypeId;
	private static final Map<Class<? extends Feature>, Integer> typeToId = new HashMap<>();
	private static final Map<Integer, Class<? extends Feature>> idToType = new HashMap<>();
	public static final int AIMING_FEATURE = registerFeatureType(AimingFeature.class);
	public static final int PIP_FEATURE = registerFeatureType(PipFeature.class);
	public static final int RETICLE_FEATURE = registerFeatureType(ReticleFeature.class);
	public static final int MUZZLE_FLASH_FEATURE = registerFeatureType(MuzzleFlashFeature.class);
	public static final int GLOW_FEATURE = registerFeatureType(GlowFeature.class);
	public static final int VISIBILITY_FEATURE = registerFeatureType(PartVisibilityFeature.class);
	public static final int RECOIL_FEATURE = registerFeatureType(RecoilFeature.class);
	public static final int ACCURACY_FEATURE = registerFeatureType(AccuracyFeature.class);
	public static final int ACTIVE_MUZZLE_FEATURE = registerFeatureType(ActiveMuzzleFeature.class);
	public static final int AMMO_CAPACITY_FEATURE = registerFeatureType(AmmoCapacityFeature.class);
	public static final int DAMAGE_FEATURE = registerFeatureType(DamageFeature.class);
	public static final int SKIN_FEATURE = registerFeatureType(SkinFeature.class);
	public static final int SOUND_FEATURE = registerFeatureType(SoundFeature.class);
	public static final int FIRE_MODE_FEATURE = registerFeatureType(FireModeFeature.class);
	public static final int ADS_SPEED_FEATURE = registerFeatureType(AdsSpeedFeature.class);
	public static final int DEFENSE_FEATURE = registerFeatureType(DefenseFeature.class);
	public static final int SLOT_FEATURE = registerFeatureType(SlotFeature.class);

	private static int registerFeatureType(Class<? extends Feature> featureType) {
		++featureTypeId;
		if (typeToId.put(featureType, featureTypeId) != null) {
			throw new IllegalArgumentException("Duplicate feature type: " + featureType);
		} else {
			idToType.put(featureTypeId, featureType);
			return featureTypeId;
		}
	}

	public static Map<Integer, Class<? extends Feature>> getFeatureTypes() {
		return Collections.unmodifiableMap(idToType);
	}

	public static Class<? extends Feature> getFeatureType(int featureTypeId) {
		return idToType.get(featureTypeId);
	}

	public static int getFeatureTypeId(Class<? extends Feature> featureType) {
		Integer id = typeToId.get(featureType);
		if (id == null) {
			throw new IllegalArgumentException("Feature type not registered: " + featureType);
		} else {
			return id;
		}
	}

	public static void init() {
		var cfg = new TomlParser().parse(
			FMLPaths.CONFIGDIR.get().resolve("pointblank-common.toml").toFile(), FileNotFoundAction.READ_NOTHING);
		boolean allowBasePackOverwrite =
			cfg.get("allowBasePackOverwrite") != null && (boolean)cfg.get("allowBasePackOverwrite");
		if (!allowBasePackOverwrite) {
			try {
				InternalFiles.copyFolder(
					PointBlankJelly.class.getResource("/base_pack").toURI(),
					FMLPaths.GAMEDIR.get().resolve("pointblank").resolve("base_pack"));
				PointBlankJelly.LOGGER.info("Writing base_pack...");
			} catch (IOException | URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
