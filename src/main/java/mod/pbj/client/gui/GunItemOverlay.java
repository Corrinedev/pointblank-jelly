package mod.pbj.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import mod.pbj.client.GunClientState;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class GunItemOverlay {
	private static final ResourceLocation OVERLAY_RESOURCE =
		new ResourceLocation("pointblank", "textures/gui/ammo.png");

	public GunItemOverlay() {}

	public static void renderGunOverlay(GuiGraphics guiGraphics, ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		int slotIndex = mc.player.getInventory().selected;
		GunClientState gunClientState = GunClientState.getState(mc.player, stack, slotIndex, false);
		if (gunClientState != null) {
			Component message = gunClientState.getCurrentMessage();
			int messageColor;
			if (message != null) {
				messageColor = 14548736;
			} else {
				FireModeInstance fireModeInstance = GunItem.getFireModeInstance(stack);
				int currentAmmo = gunClientState.getAmmoCount(fireModeInstance);
				GunItem gunItem = (GunItem)stack.getItem();
				int maxAmmo = gunItem.getMaxAmmoCapacity(stack, fireModeInstance);
				if (maxAmmo == Integer.MAX_VALUE) {
					message = Component.literal("∞");
				} else {
					message = Component.literal(String.format("%d/%d", currentAmmo, maxAmmo));
				}

				messageColor = 16776960;
			}

			Font font = mc.font;
			int screenX = mc.getWindow().getGuiScaledWidth();
			guiGraphics.drawString(font, message, screenX - font.width(message) - 10, 10, messageColor, false);
		}
	}

	public static void renderGunOverlay2(GuiGraphics guiGraphics, ItemStack stack) {
		int textureWidth = 160;
		int textureHeight = 32;
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		Minecraft mc = Minecraft.getInstance();
		FireModeInstance fireModeInstance = GunItem.getFireModeInstance(stack);
		if (fireModeInstance != null) {
			String fireModeDisplayName = fireModeInstance.getDisplayName().getString();
			int width = 9 + mc.font.width(fireModeDisplayName);
			int height = 22;
			int vOffset = mc.getWindow().getGuiScaledHeight() - height;
			int hOffset = (mc.getWindow().getGuiScaledWidth() >> 1) + 97;
			guiGraphics.blitNineSliced(
				OVERLAY_RESOURCE, hOffset, vOffset, width, height, 18, 4, textureWidth, textureHeight, 0, 0);
			guiGraphics.drawString(mc.font, fireModeDisplayName, hOffset + 5 + 1, vOffset + 7, 0, false);
			guiGraphics.drawString(mc.font, fireModeDisplayName, hOffset + 5 - 1, vOffset + 7, 0, false);
			guiGraphics.drawString(mc.font, fireModeDisplayName, hOffset + 5, vOffset + 7 + 1, 0, false);
			guiGraphics.drawString(mc.font, fireModeDisplayName, hOffset + 5, vOffset + 7 - 1, 0, false);
			guiGraphics.drawString(mc.font, fireModeDisplayName, hOffset + 5, vOffset + 7, 8040160, false);
			int slotIndex = mc.player.getInventory().selected;
			GunClientState gunClientState = GunClientState.getState(mc.player, stack, slotIndex, false);
			if (gunClientState != null) {
				int currentAmmo = gunClientState.getAmmoCount(fireModeInstance);
				GunItem gunItem = (GunItem)stack.getItem();
				int maxAmmo = gunItem.getMaxAmmoCapacity(stack, fireModeInstance);
				String counter;
				if (maxAmmo == Integer.MAX_VALUE) {
					counter = "∞";
				} else {
					counter = String.format("%d/%d", currentAmmo, maxAmmo);
				}

				Component message = gunClientState.getCurrentMessage();
				if (message != null) {
					counter = message.getString();
				}

				guiGraphics.drawString(mc.font, counter, hOffset + 5 + 1, vOffset - 5, 0, false);
				guiGraphics.drawString(mc.font, counter, hOffset + 5 - 1, vOffset - 5, 0, false);
				guiGraphics.drawString(mc.font, counter, hOffset + 5, vOffset - 5 + 1, 0, false);
				guiGraphics.drawString(mc.font, counter, hOffset + 5, vOffset - 5 - 1, 0, false);
				guiGraphics.drawString(mc.font, counter, hOffset + 5, vOffset - 5, -1, false);
			}
		}
	}
}
