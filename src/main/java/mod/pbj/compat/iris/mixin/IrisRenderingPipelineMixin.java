package mod.pbj.compat.iris.mixin;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import mod.pbj.Config;
import mod.pbj.compat.iris.IriaAuxIndexHolder;
import mod.pbj.compat.iris.IrisAuxShaderProvider;
import mod.pbj.compat.iris.IrisShaderPropertiesProvider;
import mod.pbj.compat.iris.IrisShaderUtil;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({IrisRenderingPipeline.class})
public abstract class IrisRenderingPipelineMixin implements IrisAuxShaderProvider {
	@Shadow @Final private RenderTargets renderTargets;
	@Unique private ShaderInstance auxShader;
	@Unique private ShaderInstance auxPlainShader;
	@Unique private ShaderInstance maskShader;

	@Shadow protected abstract void destroyShaders();

	@Shadow
	protected abstract ShaderInstance createShader(String var1, Optional<ProgramSource> var2, ShaderKey var3)
		throws IOException;

	@Inject(method = {"<init>"}, at = { @At("TAIL") })
	private void onInit(ProgramSet programSet, CallbackInfo ci) {
		if (Config.advancedIrisIntegrationEnabled) {
			if (programSet instanceof IrisShaderPropertiesProvider) {
				IrisShaderPropertiesProvider aux = (IrisShaderPropertiesProvider)programSet;
				Integer auxIndex = (Integer)IriaAuxIndexHolder.value.get();
				if (auxIndex != null) {
					String fsh;
					String vsh;
					ProgramSource programSource;
					try {
						fsh = IrisShaderUtil.getResource("/iris_aux_shader.fsh");
						fsh = IrisShaderUtil.replaceRenderTargets(fsh, auxIndex);
						vsh = IrisShaderUtil.getResource("/iris_aux_shader.vsh");
						Objects.requireNonNull(aux);
						programSource = IrisShaderUtil.createProgramSource(
							"pointblank_iris_aux",
							vsh,
							fsh,
							programSet,
							aux::getPointblankShaderProperties,
							ShaderKey.TEXTURED.getProgram().getBlendModeOverride());
						this.auxShader =
							this.createShader(programSource.getName(), Optional.of(programSource), ShaderKey.TEXTURED);
					} catch (IOException var33) {
						this.destroyShaders();
						throw new RuntimeException(var33);
					} finally {
						IriaAuxIndexHolder.value.remove();
					}

					try {
						fsh = IrisShaderUtil.getResource("/iris_aux_shader_plain.fsh");
						fsh = IrisShaderUtil.replaceRenderTargets(fsh, auxIndex);
						vsh = IrisShaderUtil.getResource("/iris_aux_shader_plain.vsh");
						Objects.requireNonNull(aux);
						programSource = IrisShaderUtil.createProgramSource(
							"pointblank_iris_aux_plain",
							vsh,
							fsh,
							programSet,
							aux::getPointblankShaderProperties,
							ShaderKey.TEXTURED.getProgram().getBlendModeOverride());
						this.auxPlainShader =
							this.createShader(programSource.getName(), Optional.of(programSource), ShaderKey.TEXTURED);
					} catch (IOException var31) {
						this.destroyShaders();
						throw new RuntimeException(var31);
					} finally {
						IriaAuxIndexHolder.value.remove();
					}

					try {
						fsh = IrisShaderUtil.getResource("/iris_mask_shader.fsh");
						fsh = IrisShaderUtil.replaceRenderTargets(fsh, auxIndex);
						vsh = IrisShaderUtil.getResource("/iris_mask_shader.vsh");
						Objects.requireNonNull(aux);
						programSource = IrisShaderUtil.createProgramSource(
							"pointblank_iris_mask",
							vsh,
							fsh,
							programSet,
							aux::getPointblankShaderProperties,
							ShaderKey.TEXTURED_COLOR.getProgram().getBlendModeOverride());
						this.maskShader = this.createShader(
							programSource.getName(), Optional.of(programSource), ShaderKey.TEXTURED_COLOR);
					} catch (IOException var29) {
						this.destroyShaders();
						throw new RuntimeException(var29);
					} finally {
						IriaAuxIndexHolder.value.remove();
					}
				}
			}
		}
	}

	@Unique
	public ShaderInstance getPointblankAuxShader() {
		return this.auxShader;
	}

	@Unique
	public ShaderInstance getPointblankAuxPlainShader() {
		return this.auxPlainShader;
	}

	public ShaderInstance getPointblankMaskShader() {
		return this.maskShader;
	}

	public RenderTargets getPointblankRenderTargets() {
		return this.renderTargets;
	}
}
