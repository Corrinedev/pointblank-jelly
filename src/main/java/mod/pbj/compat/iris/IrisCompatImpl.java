package mod.pbj.compat.iris;

import mod.pbj.Config;
import mod.pbj.client.render.RenderTypeProvider;
import net.irisshaders.iris.Iris;

class IrisCompatImpl extends IrisCompat {
	private boolean isRenderingShadows;
	private RenderTypeProvider renderTypeProvider = new IrisRenderTypeProvider();

	protected IrisCompatImpl() {}

	public boolean isIrisLoaded() {
		return true;
	}

	public boolean isShaderPackEnabled() {
		return Iris.getCurrentPack().isPresent();
	}

	public void onStartRenderShadows() {
		this.isRenderingShadows = true;
	}

	public void onEndRenderShadows() {
		this.isRenderingShadows = false;
	}

	public boolean isRenderingShadows() {
		return this.isRenderingShadows;
	}

	public RenderTypeProvider getRenderTypeProvider() {
		return this.renderTypeProvider;
	}

	public int getColorBalance() {
		return Config.pipScopeColorBalanceRed << 24 | Config.pipScopeColorBalanceGreen << 16 |
			Config.pipScopeColorBalanceBlue << 8 | 255;
	}
}
