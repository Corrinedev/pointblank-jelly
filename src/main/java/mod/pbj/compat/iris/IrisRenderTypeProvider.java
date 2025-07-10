package mod.pbj.compat.iris;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import mod.pbj.client.ClientSystem;
import mod.pbj.client.render.Flushable;
import mod.pbj.client.render.RenderTypeProvider;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class IrisRenderTypeProvider extends RenderStateShard implements RenderTypeProvider {
	private final Function<Boolean, RenderType> pipRenderTypes = Util.memoize(this::createPipRenderType);
	private final Function<ResourceLocation, RenderType> pipMaskRenderTypes =
		Util.memoize((texture) -> this.createPipMaskRenderType(getIrisTexture(texture), this::getPipMaskShader));
	private final Function<ResourceLocation, RenderType> pipOverlayRenderTypes = Util.memoize(
		(texture)
			-> createPipOverlayRenderType(getIrisTexture(texture), GameRenderer::getPositionTexColorShader, false));
	private final Function<ResourceLocation, RenderType> pipOverlayMaskedRenderTypes =
		Util.memoize((texture) -> createPipOverlayRenderType(getIrisTexture(texture), this::getPipOverlayShader, true));
	private final Function<ResourceLocation, RenderType> reticleRenderTypes =
		Util.memoize((texture) -> createReticleRenderType(getIrisTexture(texture)));
	private final Function<ResourceLocation, RenderType> reticleRenderTypesWithParallax = Util.memoize(
		(texture) -> createReticleRenderType(getIrisTexture(texture), GameRenderer::getPositionTexColorShader));
	private final Function<ResourceLocation, RenderType> glowRenderTypes =
		Util.memoize((texture) -> IrisRenderTypeProvider.GlowRenderType.createRenderType(getIrisTexture(texture)));

	public IrisRenderTypeProvider() {
		super(null, null, null);
	}

	public MultiBufferSource wrapBufferSource(MultiBufferSource source) {
		return new WrappedBufferSource(source);
	}

	public RenderType getPipRenderType(boolean isMasked) {
		return this.pipRenderTypes.apply(isMasked);
	}

	private ShaderInstance getPipShader() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipeline().orElse(null);
		ShaderInstance shader = null;
		if (pipeline instanceof IrisAuxShaderProvider auxShaderProvider) {
			shader = auxShaderProvider.getPointblankAuxShader();
		}

		if (shader == null) {
			shader = GameRenderer.getPositionTexColorShader();
		}

		return shader;
	}

	private ShaderInstance getPipOverlayShader() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipeline().orElse(null);
		ShaderInstance shader = null;
		if (pipeline instanceof IrisAuxShaderProvider auxShaderProvider) {
			shader = auxShaderProvider.getPointblankAuxPlainShader();
		}

		if (shader == null) {
			shader = GameRenderer.getPositionTexColorShader();
		}

		return shader;
	}

	private ShaderInstance getPipMaskShader() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipeline().orElse(null);
		ShaderInstance shader = null;
		if (pipeline instanceof IrisAuxShaderProvider auxShaderProvider) {
			shader = auxShaderProvider.getPointblankMaskShader();
		}

		if (shader == null) {
			shader = GameRenderer.getPositionTexColorShader();
		}

		return shader;
	}

	public RenderType getPipOverlayRenderType(ResourceLocation texture, boolean isMasked) {
		return isMasked ? this.pipOverlayMaskedRenderTypes.apply(texture) : this.pipOverlayRenderTypes.apply(texture);
	}

	public RenderType getPipMaskRenderType(ResourceLocation texture) {
		return this.pipMaskRenderTypes.apply(texture);
	}

	public RenderType getGlowRenderType(ResourceLocation texture) {
		return this.glowRenderTypes.apply(texture);
	}

	public RenderType getGlowBlockEntityRenderType(ResourceLocation texture) {
		return RenderType.entityTranslucent(texture);
	}

	public RenderType getMuzzleFlashRenderType(ResourceLocation texture) {
		return RenderType.entityTranslucentEmissive(texture);
	}

	public RenderType getReticleRenderType(ResourceLocation texture, boolean isParallaxEnabled) {
		return isParallaxEnabled ? this.reticleRenderTypesWithParallax.apply(texture)
								 : this.reticleRenderTypes.apply(texture);
	}

	private static ResourceLocation getIrisTexture(ResourceLocation originalTexture) {
		Minecraft mc = Minecraft.getInstance();
		ResourceManager resourceManager = mc.getResourceManager();
		String path = originalTexture.getPath();
		if (path.endsWith(".png")) {
			String modifiedPath = path.replace(".png", "_iris.png");
			ResourceLocation irisTexture = new ResourceLocation(originalTexture.getNamespace(), modifiedPath);

			try {
				resourceManager.getResourceOrThrow(irisTexture);
				return irisTexture;
			} catch (FileNotFoundException ignored) {
			}
		}

		return originalTexture;
	}

	private static RenderType
	createReticleRenderType(ResourceLocation texture, Supplier<ShaderInstance> shaderSupplier) {
		VertexFormat var10001 = DefaultVertexFormat.POSITION_TEX_COLOR;
		VertexFormat.Mode var10002 = Mode.QUADS;
		RenderType.CompositeState.CompositeStateBuilder var10006 =
			CompositeState.builder()
				.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
				.setCullState(NO_CULL)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setLightmapState(NO_LIGHTMAP)
				.setOverlayState(NO_OVERLAY)
				.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false));
		Objects.requireNonNull(shaderSupplier);
		return RenderType.create(
			"pointblank:reticle_iris_with_parallax",
			var10001,
			var10002,
			256,
			true,
			false,
			var10006.setShaderState(new RenderStateShard.ShaderStateShard(shaderSupplier)).createCompositeState(false));
	}

	private static RenderType createReticleRenderType(ResourceLocation texture) {
		RenderType.CompositeState compositeState =
			CompositeState.builder()
				.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
				.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setCullState(NO_CULL)
				.createCompositeState(true);
		return RenderType.create(
			"pointblank:reticle_iris",
			DefaultVertexFormat.POSITION_COLOR_TEX,
			Mode.QUADS,
			256,
			true,
			false,
			compositeState);
	}

	private RenderType createPipRenderType(boolean isMasked) {
		RenderType.CompositeState compositeState =
			CompositeState.builder()
				.setShaderState(new RenderStateShard.ShaderStateShard(this::getPipShader))
				.setTextureState(new PipTextureStateShard())
				.setTransparencyState(NO_TRANSPARENCY)
				.setLightmapState(NO_LIGHTMAP)
				.setOverlayState(NO_OVERLAY)
				.createCompositeState(true);
		Runnable setup = isMasked ? SETUP_STENCIL_RENDER : () -> {};
		Runnable clear = isMasked ? CLEAR_STENCIL_RENDER : () -> {};
		return RenderTypeProvider.wrapRenderType(
			RenderType.create(
				"pointblank:pip_iris_" + isMasked,
				DefaultVertexFormat.POSITION_TEX_COLOR,
				Mode.QUADS,
				256,
				true,
				false,
				compositeState),
			setup,
			clear);
	}

	private RenderType createPipMaskRenderType(ResourceLocation maskTexture, Supplier<ShaderInstance> shaderSupplier) {
		VertexFormat var10001 = DefaultVertexFormat.POSITION_TEX_COLOR;
		VertexFormat.Mode var10002 = Mode.QUADS;
		RenderType.CompositeState.CompositeStateBuilder var10006 =
			CompositeState.builder()
				.setDepthTestState(NO_DEPTH_TEST)
				.setLightmapState(NO_LIGHTMAP)
				.setOverlayState(NO_OVERLAY)
				.setTransparencyState(NO_TRANSPARENCY)
				.setTextureState(new RenderStateShard.TextureStateShard(maskTexture, false, false));
		Objects.requireNonNull(shaderSupplier);
		return RenderTypeProvider.wrapRenderType(
			RenderType.create(
				"pointblank:pip_mask_iris",
				var10001,
				var10002,
				256,
				true,
				false,
				var10006.setShaderState(new RenderStateShard.ShaderStateShard(shaderSupplier))
					.createCompositeState(false)),
			RenderTypeProvider.SETUP_STENCIL_MASK_RENDER,
			RenderTypeProvider.CLEAR_STENCIL_MASK_RENDER);
	}

	private static RenderType createPipOverlayRenderType(
		ResourceLocation overlayTexture, Supplier<ShaderInstance> shaderSupplier, boolean isMasked) {
		Runnable setup = isMasked ? SETUP_STENCIL_RENDER : () -> {};
		Runnable clear = isMasked ? CLEAR_STENCIL_RENDER : () -> {};
		String var10000 = "pointblank:pip_overlay_iris_" + isMasked;
		VertexFormat var10001 = DefaultVertexFormat.POSITION_TEX_COLOR;
		VertexFormat.Mode var10002 = Mode.QUADS;
		RenderType.CompositeState.CompositeStateBuilder var10006 =
			CompositeState.builder()
				.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
				.setCullState(NO_CULL)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setLightmapState(NO_LIGHTMAP)
				.setOverlayState(NO_OVERLAY)
				.setTextureState(new RenderStateShard.TextureStateShard(overlayTexture, false, false));
		Objects.requireNonNull(shaderSupplier);
		return RenderTypeProvider.wrapRenderType(
			RenderType.create(
				var10000,
				var10001,
				var10002,
				256,
				true,
				false,
				var10006.setShaderState(new RenderStateShard.ShaderStateShard(shaderSupplier))
					.createCompositeState(false)),
			setup,
			clear);
	}

	public float getReticleBrightness() {
		return 0.6F;
	}

	public float getGlowBrightness() {
		return 0.6F;
	}

	private record WrappedBufferSource(MultiBufferSource delegate) implements MultiBufferSource, Flushable {
		public void flush() {
			if (this.delegate instanceof FullyBufferedMultiBufferSource fbmbs) {
				fbmbs.endBatch();
			}
		}

		public VertexConsumer getBuffer(RenderType renderType) {
			return renderType == RenderTypeProvider.NO_RENDER_TYPE ? null : this.delegate.getBuffer(renderType);
		}
	}

	private static class PipTextureStateShard extends RenderStateShard.EmptyTextureStateShard {
		public PipTextureStateShard() {
			super(() -> {
				int textureId = ClientSystem.getInstance().getAuxLevelRenderer().getRenderTarget().getColorTextureId();
				RenderSystem.setShaderTexture(0, textureId);
			}, () -> {});
		}
	}

	private static final class GlowRenderType extends RenderType {
		public GlowRenderType(
			String renderTypeName,
			VertexFormat vertexFormat,
			VertexFormat.Mode mode,
			int p_173181_,
			boolean p_173182_,
			boolean p_173183_,
			Runnable p_173184_,
			Runnable p_173185_) {
			super(renderTypeName, vertexFormat, mode, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
		}

		private static RenderType createRenderType(ResourceLocation glowTexture) {
			return RenderType.create(
				"pointblank:glow_iris",
				DefaultVertexFormat.NEW_ENTITY,
				Mode.QUADS,
				256,
				true,
				false,
				CompositeState.builder()
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setCullState(NO_CULL)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(NO_OVERLAY)
					.setTextureState(new RenderStateShard.TextureStateShard(glowTexture, false, false))
					.setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
					.createCompositeState(false));
		}
	}
}
