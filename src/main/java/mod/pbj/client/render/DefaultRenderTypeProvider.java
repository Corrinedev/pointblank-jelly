package mod.pbj.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import mod.pbj.client.ClientSystem;
import mod.pbj.compat.iris.IrisCompat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public class DefaultRenderTypeProvider extends RenderStateShard implements RenderTypeProvider {
	private static DefaultRenderTypeProvider instance;
	private static Optional<ShaderInstance> posTexColorShader;
	private static Optional<ShaderInstance> auxShader;
	private final Function<Boolean, RenderType> pipRenderTypes =
		Util.memoize(DefaultRenderTypeProvider::createPipRenderType);
	private final Function<ResourceLocation, RenderType> pipOverlayRenderTypes =
		Util.memoize((texture) -> createPipOverlayRenderType(texture, false));
	private final Function<ResourceLocation, RenderType> pipOverlayRenderTypesMasked =
		Util.memoize((texture) -> createPipOverlayRenderType(texture, true));
	private final Function<ResourceLocation, RenderType> pipMaskRenderTypes =
		Util.memoize(DefaultRenderTypeProvider::createPipMaskRenderType);
	private final Function<ResourceLocation, RenderType> muzzleFlashRenderTypes =
		Util.memoize(DefaultRenderTypeProvider::createMuzzleFlashRenderType);
	private final Function<ResourceLocation, RenderType> reticleRenderTypes =
		Util.memoize((texture) -> createReticleRenderType(texture));
	private final Function<ResourceLocation, RenderType> reticleRenderTypesWithParallax = Util.memoize((texture) -> {
		ClientSystem var10001 = ClientSystem.getInstance();
		Objects.requireNonNull(var10001);
		return createReticleRenderType(texture, var10001::getTexColorShaderInstance);
	});
	private final Function<ResourceLocation, RenderType> glowEntityRenderTypes =
		Util.memoize(GlowEntityRenderType::createRenderType);
	private static final PipTextureStateShard PIP_TEXTURE_STATE_SHARD = new PipTextureStateShard();

	public static DefaultRenderTypeProvider getInstance() {
		if (instance == null) {
			instance = new DefaultRenderTypeProvider();
		}

		return instance;
	}

	public DefaultRenderTypeProvider() {
		super(null, null, null);
	}

	public RenderType getPipRenderType(boolean isMasked) {
		return this.pipRenderTypes.apply(isMasked);
	}

	public RenderType getPipOverlayRenderType(ResourceLocation texture, boolean isMasked) {
		return isMasked ? this.pipOverlayRenderTypesMasked.apply(texture) : this.pipOverlayRenderTypes.apply(texture);
	}

	public RenderType getPipMaskRenderType(ResourceLocation texture) {
		return this.pipMaskRenderTypes.apply(texture);
	}

	public RenderType getGlowRenderType(ResourceLocation texture) {
		return RenderType.entityTranslucentEmissive(texture, true);
	}

	public RenderType getMuzzleFlashRenderType(ResourceLocation texture) {
		return this.muzzleFlashRenderTypes.apply(texture);
	}

	public RenderType getReticleRenderType(ResourceLocation texture, boolean isParallaxEnabled) {
		return isParallaxEnabled ? this.reticleRenderTypesWithParallax.apply(texture)
								 : this.reticleRenderTypes.apply(texture);
	}

	public RenderType getGlowBlockEntityRenderType(ResourceLocation texture) {
		return IrisCompat.getInstance().isIrisLoaded() ? RenderType.entityTranslucent(texture)
													   : this.glowEntityRenderTypes.apply(texture);
	}

	private static ShaderInstance getPosTexColorShader() {
		if (posTexColorShader != null) {
			return posTexColorShader.orElse(null);
		} else {
			String shaderName = "pointblank_position_tex_color";

			try {
				Minecraft mc = Minecraft.getInstance();
				posTexColorShader = Optional.of(
					new ShaderInstance(mc.getResourceManager(), shaderName, DefaultVertexFormat.POSITION_TEX_COLOR));
			} catch (Exception e) {
				e.printStackTrace();
				posTexColorShader = Optional.empty();
			}

			return posTexColorShader.get();
		}
	}

	private static ShaderInstance getAuxShader() {
		if (auxShader != null) {
			return auxShader.orElse(null);
		} else {
			String shaderName = "pointblank_aux";

			try {
				Minecraft mc = Minecraft.getInstance();
				auxShader = Optional.of(
					new ShaderInstance(mc.getResourceManager(), shaderName, DefaultVertexFormat.POSITION_TEX_COLOR));
			} catch (Exception e) {
				e.printStackTrace();
				auxShader = Optional.empty();
			}

			return auxShader.get();
		}
	}

	private static RenderType createPipRenderType(boolean isMasked) {
		Runnable setup = isMasked ? SETUP_STENCIL_RENDER : () -> {};
		Runnable clear = isMasked ? CLEAR_STENCIL_RENDER : () -> {};
		return RenderTypeProvider.wrapRenderType(
			RenderType.create(
				"pointblank:pip",
				DefaultVertexFormat.POSITION_TEX_COLOR,
				Mode.QUADS,
				256,
				true,
				false,
				CompositeState.builder()
					.setShaderState(new RenderStateShard.ShaderStateShard(DefaultRenderTypeProvider::getAuxShader))
					.setTextureState(PIP_TEXTURE_STATE_SHARD)
					.setTransparencyState(NO_TRANSPARENCY)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setLightmapState(NO_LIGHTMAP)
					.setOverlayState(NO_OVERLAY)
					.setCullState(NO_CULL)
					.createCompositeState(true)),
			setup,
			clear);
	}

	private static RenderType createPipMaskRenderType(ResourceLocation maskTexture) {
		return RenderTypeProvider.wrapRenderType(
			RenderType.CompositeRenderType.create(
				"pointblank:pip_mask",
				DefaultVertexFormat.POSITION_TEX_COLOR,
				Mode.QUADS,
				2097152,
				true,
				false,
				CompositeState.builder()
					.setTransparencyState(NO_TRANSPARENCY)
					.setTextureState(new RenderStateShard.TextureStateShard(maskTexture, false, false))
					.setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
					.createCompositeState(false)),
			RenderTypeProvider.SETUP_STENCIL_MASK_RENDER,
			RenderTypeProvider.CLEAR_STENCIL_MASK_RENDER);
	}

	private static RenderType createPipOverlayRenderType(ResourceLocation overlayTexture, boolean isMasked) {
		Runnable setup = isMasked ? SETUP_STENCIL_RENDER : () -> {};
		Runnable clear = isMasked ? CLEAR_STENCIL_RENDER : () -> {};
		return RenderTypeProvider.wrapRenderType(
			RenderType.create(
				"pointblank:pip_overlay_" + isMasked,
				DefaultVertexFormat.POSITION_TEX_COLOR,
				Mode.QUADS,
				256,
				true,
				false,
				CompositeState.builder()
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setCullState(NO_CULL)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setLightmapState(NO_LIGHTMAP)
					.setOverlayState(NO_OVERLAY)
					.setTextureState(new RenderStateShard.TextureStateShard(overlayTexture, false, false))
					.setShaderState(
						new RenderStateShard.ShaderStateShard(DefaultRenderTypeProvider::getPosTexColorShader))
					.createCompositeState(false)),
			setup,
			clear);
	}

	private static RenderType createMuzzleFlashRenderType(ResourceLocation texture) {
		VertexFormat var10001 = DefaultVertexFormat.POSITION_COLOR_TEX;
		VertexFormat.Mode var10002 = Mode.QUADS;
		RenderType.CompositeState.CompositeStateBuilder var10006 =
			CompositeState.builder()
				.setTransparencyState(LIGHTNING_TRANSPARENCY)
				.setCullState(NO_CULL)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false));
		ClientSystem var10009 = ClientSystem.getInstance();
		Objects.requireNonNull(var10009);
		return RenderType.create(
			"pointblank:muzzle_flash",
			var10001,
			var10002,
			256,
			true,
			false,
			var10006.setShaderState(new RenderStateShard.ShaderStateShard(var10009::getColorTexLightmapShaderInstance))
				.createCompositeState(false));
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
			"pointblank:reticle_parallax",
			var10001,
			var10002,
			256,
			true,
			false,
			var10006.setShaderState(new RenderStateShard.ShaderStateShard(shaderSupplier)).createCompositeState(false));
	}

	private static RenderType createReticleRenderType(ResourceLocation reticleResource) {
		VertexFormat var10001 = DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;
		VertexFormat.Mode var10002 = Mode.QUADS;
		RenderType.CompositeState.CompositeStateBuilder var10006 =
			CompositeState.builder()
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setCullState(NO_CULL)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setTextureState(new RenderStateShard.TextureStateShard(reticleResource, false, false));
		ClientSystem var10009 = ClientSystem.getInstance();
		Objects.requireNonNull(var10009);
		return RenderType.create(
			"pointblank:reticle",
			var10001,
			var10002,
			256,
			true,
			false,
			var10006.setShaderState(new RenderStateShard.ShaderStateShard(var10009::getColorTexLightmapShaderInstance))
				.createCompositeState(false));
	}

	private static class PipTextureStateShard extends RenderStateShard.EmptyTextureStateShard {
		public PipTextureStateShard() {
			super(() -> {
				int textureId = ClientSystem.getInstance().getAuxLevelRenderer().getRenderTarget().getColorTextureId();
				RenderSystem.setShaderTexture(0, textureId);
			}, () -> {});
		}
	}

	private static final class GlowEntityRenderType extends RenderType {
		public GlowEntityRenderType(
			String renderTypeName,
			VertexFormat p_173179_,
			VertexFormat.Mode p_173180_,
			int p_173181_,
			boolean p_173182_,
			boolean p_173183_,
			Runnable p_173184_,
			Runnable p_173185_) {
			super(renderTypeName, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
		}

		private static RenderType createRenderType(ResourceLocation texture) {
			return RenderType.create(
				"pointblank:glowy_entity_block",
				DefaultVertexFormat.POSITION_COLOR_TEX,
				Mode.QUADS,
				256,
				true,
				false,
				CompositeState.builder()
					.setTransparencyState(LIGHTNING_TRANSPARENCY)
					.setCullState(NO_CULL)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
					.setShaderState(RenderStateShard.POSITION_COLOR_TEX_SHADER)
					.createCompositeState(false));
		}
	}
}
