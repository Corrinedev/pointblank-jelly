package mod.pbj.network;

import java.util.function.Supplier;
import mod.pbj.attachment.AttachmentHost;
import mod.pbj.inventory.AttachmentContainerMenu;
import mod.pbj.inventory.CraftingContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

public class ServerBoundOpenScreenPacket {
	private ScreenType screenType;

	public ServerBoundOpenScreenPacket(ScreenType sreenType) {
		this.screenType = sreenType;
	}

	public ServerBoundOpenScreenPacket() {}

	public static void encode(ServerBoundOpenScreenPacket packet, FriendlyByteBuf buf) {
		buf.writeEnum(packet.screenType);
	}

	public static ServerBoundOpenScreenPacket decode(FriendlyByteBuf buf) {
		return new ServerBoundOpenScreenPacket(buf.readEnum(ScreenType.class));
	}

	public static void handle(ServerBoundOpenScreenPacket packet, Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			switch (packet.screenType) {
				case CRAFTING:
					NetworkHooks.openScreen(
						player,
						new SimpleMenuProvider(
							(windowId, playerInventory, p)
								-> new CraftingContainerMenu(windowId, playerInventory),
							Component.translatable("screen.pointblank.crafting")));
					break;
				case ATTACHMENTS:
					ItemStack heldItem = player.getMainHandItem();
					if (player != null && heldItem.getItem() instanceof AttachmentHost) {
						NetworkHooks.openScreen(
							player,
							new SimpleMenuProvider(
								(windowId, playerInventory, p)
									-> new AttachmentContainerMenu(windowId, playerInventory, heldItem),
								Component.translatable("screen.pointblank.attachments")));
					}
			}
		});
		context.get().setPacketHandled(true);
	}

	public enum ScreenType {
		ATTACHMENTS,
		CRAFTING;

		ScreenType() {}
	}
}
