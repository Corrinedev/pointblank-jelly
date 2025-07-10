package mod.pbj.client.render;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Random;
import mod.pbj.client.EntityRendererBuilder;
import mod.pbj.client.effect.AbstractEffect;
import mod.pbj.client.effect.Effect;
import mod.pbj.client.effect.Effect.BlendMode;
import mod.pbj.client.uv.LoopingSpriteUVProvider;
import mod.pbj.client.uv.PlayOnceSpriteUVProvider;
import mod.pbj.client.uv.RandomSpriteUVProvider;
import mod.pbj.client.uv.SpriteUVProvider;
import mod.pbj.client.uv.StaticSpriteUVProvider;
import mod.pbj.entity.ProjectileLike;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class SpriteEntityRenderer<T extends Entity & ProjectileLike> extends EntityRenderer<T> {
	private ResourceLocation texture;
	private RenderType renderType;
	private SpriteUVProvider spriteUVProvider;
	private float initialRoll;
	private boolean isGlowEnabled;
	private int brightness;
	private float rotationsPerSecond;
	private float width;
	private float height;

	public SpriteEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public void render(
		T projectile,
		float p_114657_,
		float partialTick,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int lightColor) {
		float progress = (float)((ProjectileLike)projectile).getElapsedTimeMillis() / 1000.0F;
		poseStack.pushPose();
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.getMainCamera();
		poseStack.mulPose(camera.rotation());
		float[] uv = this.spriteUVProvider.getSpriteUV(progress);
		if (uv != null) {
			VertexConsumer vertexConsumer = bufferSource.getBuffer(this.renderType);
			if (this.isGlowEnabled) {
				lightColor = 240;
			}

			float alpha = 1.0F;
			float spriteWidth = uv[2] - uv[0];
			float textureMinU = 0.0F;
			float textureMaxU = 1.0F;
			float minU = uv[0] + spriteWidth * textureMinU;
			float minV = uv[1];
			float maxU = uv[0] + spriteWidth * textureMaxU;
			float maxV = uv[3];
			Quaternionf rotation = new Quaternionf();
			float roll = this.initialRoll + this.rotationsPerSecond * 360.0F * progress;
			rotation.rotateZ(((float)Math.PI / 180F) * roll);
			poseStack.mulPose(rotation);
			Matrix4f matrix4f = poseStack.last().pose();
			float halfWidth = (this.width > 0.0F ? this.width : projectile.getBbWidth()) * 0.5F;
			float halfHeight = (this.height > 0.0F ? this.height : projectile.getBbHeight()) * 0.5F;

			for (int k = 0; k < this.brightness; ++k) {
				vertexConsumer.vertex(matrix4f, -halfWidth, halfHeight, 0.0F)
					.color(1.0F, 1.0F, 1.0F, alpha)
					.uv(maxU, minV)
					.uv2(lightColor)
					.endVertex();
				vertexConsumer.vertex(matrix4f, halfWidth, halfHeight, 0.0F)
					.color(1.0F, 1.0F, 1.0F, alpha)
					.uv(minU, minV)
					.uv2(lightColor)
					.endVertex();
				vertexConsumer.vertex(matrix4f, halfWidth, -halfHeight, 0.0F)
					.color(1.0F, 1.0F, 1.0F, alpha)
					.uv(minU, maxV)
					.uv2(lightColor)
					.endVertex();
				vertexConsumer.vertex(matrix4f, -halfWidth, -halfHeight, 0.0F)
					.color(1.0F, 1.0F, 1.0F, (float)this.brightness)
					.uv(maxU, maxV)
					.uv2(lightColor)
					.endVertex();
			}

			poseStack.popPose();
		}
	}

	public ResourceLocation getTextureLocation(Entity entity) {
		return this.texture;
	}

	private static final class EffectRenderType extends RenderType {
		private EffectRenderType(
			String renderTypeName,
			VertexFormat vertexFormat,
			VertexFormat.Mode p_173180_,
			int p_173181_,
			boolean p_173182_,
			boolean p_173183_,
			Runnable p_173184_,
			Runnable runnable) {
			super(renderTypeName, vertexFormat, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, runnable);
		}

		private static RenderType createRenderType(
			String name, ResourceLocation texture, Effect.BlendMode blendMode, boolean isDepthTestEnabled) {
			RenderStateShard.TransparencyStateShard transparencyShard;
			switch (blendMode) {
				case NORMAL -> transparencyShard = TRANSLUCENT_TRANSPARENCY;
				default -> transparencyShard = LIGHTNING_TRANSPARENCY;
			}

			return RenderType.create(
				name,
				DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
				Mode.QUADS,
				256,
				true,
				false,
				CompositeState.builder()
					.setTransparencyState(transparencyShard)
					.setDepthTestState(isDepthTestEnabled ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST)
					.setCullState(NO_CULL)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
					.setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
					.createCompositeState(false));
		}
	}

	public static class Builder<T extends Entity & ProjectileLike>
		implements EntityRendererBuilder<Builder<T>, T, EntityRenderer<T>> {
		private static long counter;
		private static final SpriteUVProvider DEFAULT_SPRITE_UV_PROVIDER;
		private static final String DEFAULT_NAME_PREFIX;
		private static Effect.BlendMode DEFAULT_BLEND_MODE;
		private static final int DEFAULT_BRIGHTNESS = 1;
		private static final float DEFAULT_NUM_ROTATIONS = 0.0F;
		private ResourceLocation texture;
		private Effect.BlendMode blendMode;
		private boolean isDepthTestEnabled;
		private boolean isGlowEnabled;
		private int brightness;
		private AbstractEffect.SpriteInfo spriteInfo;
		private float numRotations;
		private float width;
		private float height;

		public Builder() {
			++counter;
			this.blendMode = DEFAULT_BLEND_MODE;
			this.brightness = 1;
			this.numRotations = 0.0F;
			this.width = Float.NEGATIVE_INFINITY;
			this.height = Float.NEGATIVE_INFINITY;
		}

		public Builder<T> withSize(float width, float hight) {
			this.width = width;
			return this;
		}

		public Builder<T> withSize(float size) {
			this.width = size;
			this.height = size;
			return this;
		}

		public Builder<T> withTexture(ResourceLocation texture) {
			this.texture = texture;
			return this;
		}

		public Builder<T> withTexture(String textureName) {
			this.texture = new ResourceLocation("pointblank", textureName);
			return this;
		}

		public Builder<T> withBlendMode(Effect.BlendMode blendMode) {
			this.blendMode = blendMode;
			return this;
		}

		public Builder<T> withDepthTest(boolean isDepthTestEnabled) {
			this.isDepthTestEnabled = isDepthTestEnabled;
			return this;
		}

		public Builder<T> withGlow(boolean isGlowEnabled) {
			this.isGlowEnabled = isGlowEnabled;
			return this;
		}

		public Builder<T> withBrightness(int brightness) {
			this.brightness = brightness;
			return this;
		}

		public Builder<T>
		withSprites(int rows, int columns, int spritesPerSecond, AbstractEffect.SpriteAnimationType type) {
			this.spriteInfo = new AbstractEffect.SpriteInfo(rows, columns, spritesPerSecond, type);
			return this;
		}

		public Builder<T> withRotations(double numRotations) {
			this.numRotations = (float)numRotations;
			return this;
		}

		public Builder<T> withJsonObject(JsonObject obj) {
			return null;
		}

		public EntityRenderer<T> build(EntityRendererProvider.Context context) {
			SpriteEntityRenderer<T> renderer = new SpriteEntityRenderer<T>(context);
			renderer.width = this.width;
			renderer.height = this.height;
			renderer.initialRoll = (new Random()).nextFloat() * 360.0F;
			renderer.isGlowEnabled = this.isGlowEnabled;
			renderer.brightness = this.brightness;
			renderer.rotationsPerSecond = this.numRotations;
			renderer.texture = this.texture;
			String renderTypeName = DEFAULT_NAME_PREFIX + "." + counter;
			renderer.renderType = SpriteEntityRenderer.EffectRenderType.createRenderType(
				renderTypeName, this.texture, this.blendMode, this.isDepthTestEnabled);
			if (this.spriteInfo != null) {
				switch (this.spriteInfo.type()) {
					case STATIC:
						renderer.spriteUVProvider = StaticSpriteUVProvider.INSTANCE;
						break;
					case LOOP:
						renderer.spriteUVProvider = new LoopingSpriteUVProvider(
							this.spriteInfo.rows(),
							this.spriteInfo.columns(),
							this.spriteInfo.spritesPerSecond(),
							1000L);
						break;
					case RANDOM:
						renderer.spriteUVProvider = new RandomSpriteUVProvider(
							this.spriteInfo.rows(),
							this.spriteInfo.columns(),
							this.spriteInfo.spritesPerSecond(),
							1000L);
						break;
					case PLAY_ONCE:
						renderer.spriteUVProvider = new PlayOnceSpriteUVProvider(
							this.spriteInfo.rows(),
							this.spriteInfo.columns(),
							this.spriteInfo.spritesPerSecond(),
							1000L);
				}
			} else {
				renderer.spriteUVProvider = DEFAULT_SPRITE_UV_PROVIDER;
			}

			return renderer;
		}

		static {
			DEFAULT_SPRITE_UV_PROVIDER = StaticSpriteUVProvider.INSTANCE;
			DEFAULT_NAME_PREFIX = "pointblank:" + SpriteEntityRenderer.class.getSimpleName();
			DEFAULT_BLEND_MODE = BlendMode.NORMAL;
		}
	}
}
