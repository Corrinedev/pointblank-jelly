package mod.pbj.compat.iris;

import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.renderer.ShaderInstance;

public interface IrisAuxShaderProvider {
	ShaderInstance getPointblankAuxShader();

	ShaderInstance getPointblankAuxPlainShader();

	ShaderInstance getPointblankMaskShader();

	RenderTargets getPointblankRenderTargets();
}
