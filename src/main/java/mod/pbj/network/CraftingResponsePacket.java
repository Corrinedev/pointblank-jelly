package mod.pbj.network;

import java.util.function.Supplier;
import mod.pbj.client.ClientEventHandler;
import mod.pbj.client.gui.CraftingScreen;
import mod.pbj.client.gui.NotificationToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

public class CraftingResponsePacket {
	private ItemStack itemStack;
	private CraftingResult result;
	private boolean isAddedToInventory;

	public CraftingResponsePacket(ItemStack itemStack, CraftingResult result, boolean isAddedToInventory) {
		this.itemStack = itemStack;
		this.result = result;
		this.isAddedToInventory = isAddedToInventory;
	}

	public CraftingResponsePacket() {}

	public static void encode(CraftingResponsePacket packet, FriendlyByteBuf buf) {
		buf.writeItemStack(packet.itemStack, false);
		buf.writeEnum(packet.result);
		buf.writeBoolean(packet.isAddedToInventory);
	}

	public static CraftingResponsePacket decode(FriendlyByteBuf buf) {
		return new CraftingResponsePacket(buf.readItem(), buf.readEnum(CraftingResult.class), buf.readBoolean());
	}

	public static void handle(CraftingResponsePacket packet, Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> ClientEventHandler.runSyncTick(() -> {
			Component message = null;
			switch (packet.result) {
				case COMPLETED:
					if (packet.isAddedToInventory) {
						message = Component.translatable("")
									  .append(packet.itemStack.getItem().getName(packet.itemStack))
									  .append(" ")
									  .append(Component.translatable("message.pointblank.added_to_the_inventory"))
									  .append("!");
					} else {
						message = Component.translatable("")
									  .append(packet.itemStack.getItem().getName(packet.itemStack))
									  .append(" ")
									  .append(Component.translatable("message.pointblank.dropped_on_the_gound"))
									  .append("!");
					}
					break;
				case FAILED:
					Minecraft mc = Minecraft.getInstance();
					Screen patt2905$temp = mc.screen;
					if (patt2905$temp instanceof CraftingScreen craftingScreen) {
						craftingScreen.cancelCrafting();
					}

					message = Component.translatable("message.pointblank.failed_to_craft");
					break;
				case CANCELLED:
					message = Component.translatable("")
								  .append(packet.itemStack.getItem().getName(packet.itemStack))
								  .append(" ")
								  .append(Component.translatable("message.pointblank.cancelled_to_craft"));
			}

			if (message != null) {
				notifyUser(message);
			}
		}));
		context.get().setPacketHandled(true);
	}

	@OnlyIn(Dist.CLIENT)
	private static void notifyUser(Component message) {
		Minecraft.getInstance().getToasts().addToast(new NotificationToast(message, 3000L));
	}

	public enum CraftingResult {
		COMPLETED,
		FAILED,
		CANCELLED;

		CraftingResult() {}
	}
}
