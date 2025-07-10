//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import mod.pbj.Config;
import mod.pbj.client.render.AuxLevelRenderer;
import mod.pbj.compat.playeranimator.PlayerAnimatorCompat;
import mod.pbj.feature.PipFeature;
import mod.pbj.item.GunItem;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.ReloadableMemoize;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.util.ClientUtils;

public class ClientSystem {
	private static ClientSystem instance;
	private final AuxLevelRenderer auxLevelRenderer;
	private final ShaderInstance texColorShaderInstance;
	private final ShaderInstance colorTexLightmapShaderInstance;
	private final List<ResourceManagerReloadListener> resourceManagerReloadListeners;

	public static ClientSystem getInstance() {
		if (RenderSystem.isOnRenderThreadOrInit()) {
			if (instance == null) {
				instance = new ClientSystem();
			}
		} else {
			throw new IllegalStateException("getInstance() called from wrong thread");
		}

		return instance;
	}

	private ClientSystem() {
		Minecraft mc = Minecraft.getInstance();
		RenderTarget mainRenderTarget = mc.getMainRenderTarget();
		this.auxLevelRenderer = new AuxLevelRenderer(mainRenderTarget.width, mainRenderTarget.height);
		String texColorShaderName = "pointblank_position_tex_color";

		try {
			this.texColorShaderInstance =
				new ShaderInstance(mc.getResourceManager(), texColorShaderName, DefaultVertexFormat.POSITION_TEX_COLOR);
		} catch (Exception exception) {
			throw new IllegalStateException("could not preload shader " + texColorShaderName, exception);
		}

		String colorTexLightmapShaderName = "pointblank_position_color_tex_lightmap";

		try {
			this.colorTexLightmapShaderInstance = new ShaderInstance(
				mc.getResourceManager(), colorTexLightmapShaderName, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
		} catch (Exception exception) {
			throw new IllegalStateException("could not preload shader " + colorTexLightmapShaderName, exception);
		}

		this.resourceManagerReloadListeners = new ArrayList<>();
		ResourceManager var6 = Minecraft.getInstance().getResourceManager();
		if (var6 instanceof ReloadableResourceManager rrm) {
			rrm.registerReloadListener(this.resourceManagerReloadListener());
		}

		this.addReloadListener(PlayerAnimatorCompat.getInstance());
	}

	private ResourceManagerReloadListener resourceManagerReloadListener() {
		return this::onResourceManagerReload;
	}

	private void onResourceManagerReload(ResourceManager resourceManager) {
		for (ResourceManagerReloadListener reloadListener : this.resourceManagerReloadListeners) {
			reloadListener.onResourceManagerReload(resourceManager);
		}
	}

	public void addReloadListener(ResourceManagerReloadListener listener) {
		this.resourceManagerReloadListeners.add(listener);
	}

	public void removeReloadListener(ResourceManagerReloadListener listener) {
		this.resourceManagerReloadListeners.remove(listener);
	}

	public <T, U, R> BiFunction<T, U, R> createReloadableMemoize(BiFunction<T, U, R> f) {
		ReloadableMemoize<T, U, R> reloadable = new ReloadableMemoize<>(f);
		this.resourceManagerReloadListeners.add(reloadable);
		return reloadable.getMemoizedFunction();
	}

	public AuxLevelRenderer getAuxLevelRenderer() {
		return this.auxLevelRenderer;
	}

	public ShaderInstance getTexColorShaderInstance() {
		return Config.customShadersEnabled ? this.texColorShaderInstance : GameRenderer.getPositionTexColorShader();
	}

	public ShaderInstance getColorTexLightmapShaderInstance() {
		return Config.customShadersEnabled ? this.colorTexLightmapShaderInstance
										   : GameRenderer.getPositionColorTexLightmapShader();
	}

	public void renderAux(GunClientState state, float partialTick, long time) {
		Minecraft mc = Minecraft.getInstance();
		Optional<Float> pipZoom = PipFeature.getZoom(mc.player.getMainHandItem());
		if (pipZoom.isPresent() && state.isAiming()) {
			getInstance().getAuxLevelRenderer().renderToTarget(partialTick, time, pipZoom.get());
		}
	}

	public static double modifyMouseSensitivity(double originalValue) {
		GunClientState state = GunClientState.getMainHeldState();
		ItemStack mainHeldItem = ClientUtils.getClientPlayer().getMainHandItem();
		if (mainHeldItem != null) {
			Item var5 = mainHeldItem.getItem();
			if (var5 instanceof GunItem gunItem) {
				if (state != null && state.isAiming() &&
					(gunItem.getScopeOverlay() != null || MiscUtil.isGreaterThanZero(gunItem.getPipScopeZoom()) ||
					 ClientEventHandler.runSyncCompute(() -> PipFeature.getZoom(mainHeldItem).isPresent()))) {
					return originalValue * Config.scopeAimingMouseSensitivity;
				}
			}
		}

		return originalValue;
	}
}
