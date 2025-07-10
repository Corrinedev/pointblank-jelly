package mod.pbj.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.Toast.Visibility;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NotificationToast implements Toast {
	public static final ResourceLocation BUTTON_RESOURCE =
		new ResourceLocation("pointblank", "textures/gui/buttons.png");
	private Component title;
	private long lastChanged;
	private boolean changed;
	private final int width;
	private long displayTime;

	public NotificationToast(Component title, long displayTime) {
		this.title = title;
		Minecraft mc = Minecraft.getInstance();
		this.width = Math.max(90, 30 + mc.font.width(title));
		this.displayTime = displayTime;
	}

	public int width() {
		return this.width;
	}

	public int height() {
		return 26;
	}

	public Toast.Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long currentTime) {
		if (this.changed) {
			this.lastChanged = currentTime;
			this.changed = false;
		}

		int textureWidth = 160;
		int textureHeight = 32;
		guiGraphics.blitNineSliced(
			BUTTON_RESOURCE, 0, 0, this.width(), this.height(), 18, 4, textureWidth, textureHeight, 0, 0);
		Minecraft minecraft = toastComponent.getMinecraft();
		guiGraphics.drawString(minecraft.font, this.title, 18, 9, -256, false);
		return (double)(currentTime - this.lastChanged) <
				(double)this.displayTime * toastComponent.getNotificationDisplayTimeMultiplier()
			? Visibility.SHOW
			: Visibility.HIDE;
	}
}
