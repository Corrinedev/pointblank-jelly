package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class StopFireRequestPacket extends GunStateRequestPacket {
	public StopFireRequestPacket() {}

	public StopFireRequestPacket(UUID stateId, int slotIndex) {
		super(stateId, slotIndex);
	}

	public static StopFireRequestPacket decode(FriendlyByteBuf buffer) {
		GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
		return new StopFireRequestPacket(header.stateId, header.slotIndex);
	}

	protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		if (player != null) {
			ItemStack itemStack = player.getInventory().getItem(this.slotIndex);
			if (itemStack != null && itemStack.getItem() instanceof GunItem) {
				((GunItem)itemStack.getItem())
					.handleClientStopFireRequest(player, this.stateId, this.slotIndex, this.correlationId);
			} else {
				System.err.println("Mismatching item in slot " + this.slotIndex);
			}
		}
	}
}
