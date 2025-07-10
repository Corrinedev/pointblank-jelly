package mod.pbj.compat.iris.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class IrisCompatMixinPlugin implements IMixinConfigPlugin {
	public void onLoad(String s) {}

	public String getRefMapperConfig() {
		return null;
	}

	public boolean shouldApplyMixin(String s, String s1) {
		ModFileInfo modInfo = LoadingModList.get().getModFileById("oculus");
		if (modInfo == null) {
			modInfo = LoadingModList.get().getModFileById("iris");
		}

		return modInfo != null && !modInfo.versionString().startsWith("1.6.");
	}

	public void acceptTargets(Set<String> set, Set<String> set1) {}

	public List<String> getMixins() {
		return null;
	}

	public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}

	public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}
}
