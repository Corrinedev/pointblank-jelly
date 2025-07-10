package mod.pbj.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.pbj.Config;
import mod.pbj.compat.iris.IrisCompat;
import mod.pbj.feature.PipFeature;
import mod.pbj.item.GunItem;
import mod.pbj.mixin.GameRendererAccessorMixin;
import mod.pbj.mixin.MinecraftAccessorMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.vivecraft.api_beta.client.VivecraftClientAPI;
import org.vivecraft.client_vr.ClientDataHolderVR;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.ClientUtils;

public class AuxLevelRenderer {
	private final RenderTarget renderTarget;
	private int textureWidth;
	private int textureHeight;
	private final long frameCount = 0L;
	private boolean isRendering;
	private boolean isRenderingPip;
	private double fov;
	private double cullFrustrumFov;
	private boolean isStencilEnabled;

	public AuxLevelRenderer(int textureWidth, int textureHeight) {
		this.renderTarget = new TextureTarget(textureWidth, textureHeight, true, Minecraft.ON_OSX);
		this.renderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}

	public RenderTarget getRenderTarget() {
		// if (vivecraftClient.currentPass == org.vivecraft.client_vr.render.RenderPass.SCOPER)
		return vivecraftClient.vrRenderer.telescopeFramebufferR;
		// return this.renderTarget;
	}

	public boolean isRenderingPip() {
		return this.isRenderingPip;
	}

	public double getFov() {
		return this.fov;
	}

	public double getCullFrustrumFov() {
		return this.cullFrustrumFov;
	}

	private static final VivecraftClientAPI vivecraft = VivecraftClientAPI.getInstance();
	private static final ClientDataHolderVR vivecraftClient = ClientDataHolderVR.getInstance();

	public void renderToTarget(float partialTick, long time, float zoom) {
		final var mc = Minecraft.getInstance();

		final var shouldRender = !mc.noRender && mc.gameMode != null && mc.player != null && Config.pipScopesEnabled &&
								 this.frameCount % (long)Config.pipScopeRefreshFrame == 0L &&
								 vivecraft.isVanillaRenderPass();

		if (!shouldRender)
			return;

		this.isRenderingPip = true;
		double d0 = ((GameRendererAccessorMixin)mc.gameRenderer)
						.invokeGetFov(mc.gameRenderer.getMainCamera(), partialTick, true);
		this.fov = d0 * (double)(1.0F - zoom);
		this.cullFrustrumFov = 110.0F;
		RenderTarget origTarget = mc.getMainRenderTarget();
		MinecraftAccessorMixin mm = (MinecraftAccessorMixin)mc;
		int[] viewport = new int[4];
		GL11.glGetIntegerv(2978, viewport);
		int originalWidth = mc.getWindow().getWidth();
		int originalHeight = mc.getWindow().getHeight();
		if (!origTarget.isStencilEnabled()) {
			Player player = ClientUtils.getClientPlayer();
			if (player != null) {
				ItemStack itemStack = GunItem.getMainHeldGunItemStack(player);
				if (itemStack != null && PipFeature.getMaskTexture(itemStack) != null) {
					origTarget.enableStencil();
				}
			}
		}

		if (this.renderTarget.width != origTarget.width || this.renderTarget.height != origTarget.height ||
			this.isStencilEnabled != origTarget.isStencilEnabled()) {
			this.renderTarget.resize(origTarget.width, origTarget.height, true);
			this.textureWidth = originalWidth;
			this.textureHeight = originalHeight;
			if (origTarget.isStencilEnabled()) {
				this.renderTarget.enableStencil();
			}
		}

		this.isStencilEnabled = origTarget.isStencilEnabled();
		mc.getMainRenderTarget().unbindWrite();
		mc.getMainRenderTarget().clear(false);
		mm.setMainRenderTarget(this.renderTarget);
		this.renderTarget.bindWrite(true);

		try {
			mc.gameRenderer.setPanoramicMode(true);
			mc.gameRenderer.setRenderBlockOutline(false);
			mc.gameRenderer.setRenderHand(false);
			RenderSystem.clear(0, Minecraft.ON_OSX);
			this.renderTarget.clear(false);
			this.renderTarget.bindWrite(false);
			mc.gameRenderer.renderLevel(partialTick, time + 10000L, new PoseStack());
		} finally {
			mc.gameRenderer.setPanoramicMode(false);
			mc.gameRenderer.setRenderBlockOutline(true);
			mc.gameRenderer.setRenderHand(true);
			mc.getMainRenderTarget().unbindWrite();
			mm.setMainRenderTarget(origTarget);
			RenderSystem.clear(0, Minecraft.ON_OSX);
			mc.getMainRenderTarget().clear(true);
			mc.getMainRenderTarget().bindWrite(true);
			this.isRenderingPip = false;
		}

		IrisCompat irisCompat = IrisCompat.getInstance();
		if (irisCompat.isIrisLoaded() && irisCompat.isShaderPackEnabled()) {
			GL11.glDepthMask(true);
			GL11.glClear(17664);
			if (ClientUtils.getLevel().dimension() != Level.NETHER) {
				GL11.glDepthMask(false);
			}
		}
	}

	public boolean isRendering() {
		return this.isRendering;
	}

	public void renderToBuffer(
		PoseStack poseStack, GeoQuad quad, VertexConsumer buffer, float red, float green, float blue, float alpha) {
		Matrix4f poseState = poseStack.last().pose();
		float[][] texUV = new float[][] {{1.0F, 1.0F}, {0.0F, 1.0F}, {0.0F, 0.0F}, {1.0F, 0.0F}};

		for (int i = 0; i < 4; ++i) {
			GeoVertex v = quad.vertices()[i];
			buffer.vertex(poseState, v.position().x, v.position().y, v.position().z)
				.uv(texUV[i][0], texUV[i][1])
				.color(red, green, blue, alpha)
				.endVertex();
		}
	}

	public void renderToBuffer(PoseStack poseStack, GeoQuad quad, VertexConsumer buffer, int packedLight) {
		Matrix4f poseState = poseStack.last().pose();
		float aspectRatio = Mth.clamp((float)this.textureHeight / (float)this.textureWidth, 0.0F, 1.0F);
		float arH = (1.0F - aspectRatio) * 0.5F;
		float minU = 0.0F + arH;
		float maxU = 1.0F - arH;
		float minV = 0.0F;
		float maxV = 1.0F;
		float[][] texUV = new float[][] {{maxU, maxV}, {minU, maxV}, {minU, minV}, {maxU, minV}};
		IrisCompat irisCompat = IrisCompat.getInstance();
		int red;
		int green;
		int blue;
		int alpha;
		if (irisCompat.isShaderPackEnabled()) {
			int colorBalance = irisCompat.getColorBalance();
			red = colorBalance >> 24 & 255;
			green = colorBalance >> 16 & 255;
			blue = colorBalance >> 8 & 255;
			alpha = colorBalance & 255;
		} else {
			alpha = 255;
			blue = 255;
			green = 255;
			red = 255;
		}

		for (int i = 0; i < 4; ++i) {
			GeoVertex v = quad.vertices()[i];
			buffer.vertex(poseState, v.position().x, v.position().y, v.position().z)
				.uv(texUV[i][0], texUV[i][1])
				.color(red, green, blue, alpha)
				.endVertex();
		}
	}
}
