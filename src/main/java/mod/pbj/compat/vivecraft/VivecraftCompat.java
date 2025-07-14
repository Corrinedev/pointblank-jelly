package mod.pbj.compat.vivecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.lang.reflect.InvocationTargetException;
import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract sealed class VivecraftCompat permits VivecraftCompatImpl {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private static VivecraftCompat instance;

	/**
	 * @return `null` if Vivecraft is not present or failed to load, otherwise
	 * a `VivecraftCompat` instance.
	 */
	public static VivecraftCompat getInstance() {
		if (instance == null) {
			final var compatModFileInfo = LoadingModList.get().getModFileById("vivecraft");

			if (compatModFileInfo == null)
				return null;

			try {
				final var compatImpl = Class.forName("mod.pbj.compat.vivecraft.VivecraftCompatImpl");
				instance = (VivecraftCompat)compatImpl.getDeclaredConstructor().newInstance();
				LOGGER.info("Compatibility with Vivecraft version {} enabled", compatModFileInfo.versionString());
			} catch (
				ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
				IllegalArgumentException | NoSuchMethodException | SecurityException | NoClassDefFoundError e) {
				LOGGER.error(
					"Vivecraft mod version {} detected, but compatibility could not be enabled. This is "
						+ "likely due to an outdated and/or incompatible version of the Player Animator mod. ",
					compatModFileInfo.versionString(),
					e);
			}
		}

		return instance;
	}

	public abstract RenderTarget getTelescopeRenderTarget();
	public abstract boolean isVrActive();
}
