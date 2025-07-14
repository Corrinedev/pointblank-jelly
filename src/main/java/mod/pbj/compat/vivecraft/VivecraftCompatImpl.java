package mod.pbj.compat.vivecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.vivecraft.api_beta.client.VivecraftClientAPI;
import org.vivecraft.client_vr.ClientDataHolderVR;

final class VivecraftCompatImpl extends VivecraftCompat {
	private static final VivecraftClientAPI clientApi = VivecraftClientAPI.getInstance();
	private static final ClientDataHolderVR dh = ClientDataHolderVR.getInstance();

	@Override
	public RenderTarget getTelescopeRenderTarget() {
		return dh.vrRenderer.telescopeFramebufferR;
	}

	@Override
	public boolean isVrActive() {
		return clientApi.isVrActive();
	}
}
