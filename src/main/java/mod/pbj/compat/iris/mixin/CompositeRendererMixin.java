package mod.pbj.compat.iris.mixin;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.util.Set;
import java.util.function.Supplier;
import mod.pbj.Config;
import mod.pbj.compat.iris.IriaAuxIndexHolder;
import mod.pbj.compat.iris.IrisShaderUtil;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.pathways.CenterDepthSampler;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.BufferFlipper;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CompositeRenderer.class})
public class CompositeRendererMixin {
	@Inject(method = {"<init>"}, at = { @At("TAIL") })
	private void onInit(
		WorldRenderingPipeline pipeline,
		PackDirectives packDirectives,
		ProgramSource[] sources,
		ComputeSource[][] computes,
		RenderTargets renderTargets,
		ShaderStorageBufferHolder holder,
		TextureAccess noiseTexture,
		FrameUpdateNotifier updateNotifier,
		CenterDepthSampler centerDepthSampler,
		BufferFlipper bufferFlipper,
		Supplier<ShadowRenderTargets> shadowTargetsSupplier,
		TextureStage textureStage,
		Object2ObjectMap<String, TextureAccess> customTextureIds,
		Object2ObjectMap<String, TextureAccess> irisCustomTextures,
		Set<GlImage> customImages,
		ImmutableMap<Integer, Boolean> explicitPreFlips,
		CustomUniforms customUniforms,
		CallbackInfo ci) {
		if (textureStage == TextureStage.COMPOSITE_AND_FINAL) {
			if (!Config.advancedIrisIntegrationEnabled) {
				return;
			}

			int auxRenderTargetIndex = IrisShaderUtil.findAvailableRenderTarget(packDirectives, renderTargets);
			if (auxRenderTargetIndex > 0) {
				IriaAuxIndexHolder.value.set(auxRenderTargetIndex);
			} else {
				IriaAuxIndexHolder.value.remove();
			}
		}
	}
}
