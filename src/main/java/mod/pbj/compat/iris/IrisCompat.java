package mod.pbj.compat.iris;

import mod.pbj.client.render.RenderTypeProvider;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;

public abstract class IrisCompat {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static IrisCompat instance;

   public static IrisCompat getInstance() {
      if (instance == null) {
         ModFileInfo shadersModFileInfo = LoadingModList.get().getModFileById("oculus");
         if (shadersModFileInfo == null) {
            shadersModFileInfo = LoadingModList.get().getModFileById("iris");
         }

         if (shadersModFileInfo != null) {
            String irisClassName;
            if (shadersModFileInfo.versionString().startsWith("1.6.")) {
               irisClassName = "mod.pbj.compat.iris.IrisCompatImpl16";
            } else {
               irisClassName = "mod.pbj.compat.iris.IrisCompatImpl";
            }

            try {
               Class<?> irisClass = Class.forName(irisClassName);
               instance = (IrisCompat)irisClass.getDeclaredConstructor().newInstance();
               LOGGER.info("Compatibility with Oculus/Iris version {} enabled", shadersModFileInfo.versionString());
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | NoClassDefFoundError var3) {
               LOGGER.error("Oculus mod version {} detected, but compatibility could not be enabled. This is likely due to an outdated and/or incompatible version of the Oculus mod. Please ensure you have Oculus version 1.7.0 or later installed.", shadersModFileInfo.versionString(), var3);
            }
         }

         if (instance == null) {
            instance = new IrisCompat() {
            };
         }
      }

      return instance;
   }

   public boolean isIrisLoaded() {
      return false;
   }

   public boolean isShaderPackEnabled() {
      return false;
   }

   public boolean isRenderingShadows() {
      return false;
   }

   public void onStartRenderShadows() {
      throw new UnsupportedOperationException();
   }

   public void onEndRenderShadows() {
      throw new UnsupportedOperationException();
   }

   public RenderTypeProvider getRenderTypeProvider() {
      throw new UnsupportedOperationException();
   }

   public int getColorBalance() {
      return -1;
   }
}
