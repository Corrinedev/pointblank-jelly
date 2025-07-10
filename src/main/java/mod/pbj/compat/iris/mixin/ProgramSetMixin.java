package mod.pbj.compat.iris.mixin;

import java.util.function.Function;
import mod.pbj.compat.iris.IrisShaderPropertiesProvider;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ProgramSet.class})
public class ProgramSetMixin implements IrisShaderPropertiesProvider {
	@Unique ShaderProperties shaderProperties;

	@Inject(method = {"<init>"}, at = { @At("TAIL") })
	private void onInit(
		AbsolutePackPath directory,
		Function<AbsolutePackPath, String> sourceProvider,
		ShaderProperties shaderProperties,
		ShaderPack pack,
		CallbackInfo ci) {
		this.shaderProperties = shaderProperties;
	}

	public ShaderProperties getPointblankShaderProperties() {
		return this.shaderProperties;
	}
}
