package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class ReloadRequestPacket extends GunStateRequestPacket {
	private FireModeInstance fireModeInstance;

	public ReloadRequestPacket() {}

	public ReloadRequestPacket(UUID stateId, int slotIndex, FireModeInstance fireModeInstance) {
		super(stateId, slotIndex);
		this.fireModeInstance = fireModeInstance;
	}

	protected void doEncode(FriendlyByteBuf buffer) {
		this.fireModeInstance.writeToBuf(buffer);
	}

	public static ReloadRequestPacket decode(FriendlyByteBuf buffer) {
		GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
		FireModeInstance fireModeInstance = FireModeInstance.readFromBuf(buffer);
		return new ReloadRequestPacket(header.stateId, header.slotIndex, fireModeInstance);
	}

	protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		if (player != null) {
			ItemStack itemStack = player.getInventory().getItem(this.slotIndex);
			if (itemStack != null && itemStack.getItem() instanceof GunItem) {
				((GunItem)itemStack.getItem())
					.handleClientReloadRequest(player, itemStack, this.stateId, this.slotIndex, this.fireModeInstance);
			} else {
				System.err.println("Mismatching item in slot " + this.slotIndex);
			}
		}
	}
}
