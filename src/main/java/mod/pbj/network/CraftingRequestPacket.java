package mod.pbj.network;

import java.util.function.Supplier;
import mod.pbj.block.entity.PrinterBlockEntity;
import mod.pbj.inventory.CraftingContainerMenu;
import mod.pbj.network.CraftingResponsePacket.CraftingResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class CraftingRequestPacket {
	private RequestType requestType;
	private ResourceLocation recipeId;

	public CraftingRequestPacket(RequestType requestType, ResourceLocation recipeId) {
		this.requestType = requestType;
		this.recipeId = recipeId;
	}

	public CraftingRequestPacket() {}

	public static void encode(CraftingRequestPacket packet, FriendlyByteBuf buf) {
		buf.writeEnum(packet.requestType);
		buf.writeResourceLocation(packet.recipeId);
	}

	public static CraftingRequestPacket decode(FriendlyByteBuf buf) {
		return new CraftingRequestPacket(buf.readEnum(RequestType.class), buf.readResourceLocation());
	}

	public static void handle(CraftingRequestPacket packet, Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			AbstractContainerMenu patt1720$temp = player.containerMenu;
			if (patt1720$temp instanceof CraftingContainerMenu containerMenu) {
				PrinterBlockEntity craftingBlockEntity = containerMenu.getWorkstationBlockEntity();
				if (packet.requestType == CraftingRequestPacket.RequestType.START_CRAFTING) {
					PrinterBlockEntity.CraftingEventHandler eventHandler =
						new PrinterBlockEntity.CraftingEventHandler() {
							public void onCraftingCompleted(
								Player player, ItemStack craftingItemStack, boolean isAddedToInventory) {
								Network.networkChannel.send(
									PacketDistributor.PLAYER.with(() -> (ServerPlayer)player),
									new CraftingResponsePacket(
										craftingItemStack, CraftingResult.COMPLETED, isAddedToInventory));
							}

							public void onCraftingFailed(
								Player player, ItemStack craftingItemStack, Exception craftingException) {
								Network.networkChannel.send(
									PacketDistributor.PLAYER.with(() -> (ServerPlayer)player),
									new CraftingResponsePacket(ItemStack.EMPTY, CraftingResult.FAILED, false));
							}
						};
					if (!craftingBlockEntity.tryCrafting(player, packet.recipeId, eventHandler)) {
						Network.networkChannel.send(
							PacketDistributor.PLAYER.with(() -> player),
							new CraftingResponsePacket(ItemStack.EMPTY, CraftingResult.FAILED, false));
					}
				} else if (packet.requestType == CraftingRequestPacket.RequestType.CANCEL_CRAFTING) {
					craftingBlockEntity.cancelCrafting(player, packet.recipeId);
				}
			}
		});
		context.get().setPacketHandled(true);
	}

	public enum RequestType {
		START_CRAFTING,
		CANCEL_CRAFTING;

		RequestType() {}
	}
}
