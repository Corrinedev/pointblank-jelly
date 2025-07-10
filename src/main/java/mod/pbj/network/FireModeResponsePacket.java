package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.client.GunClientState;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class FireModeResponsePacket extends GunStateResponsePacket {
	private FireModeInstance fireModeInstance;

	public FireModeResponsePacket() {}

	public FireModeResponsePacket(
		UUID stateId, int slotIndex, int correlationId, boolean isSuccess, FireModeInstance fireModeInstance) {
		super(stateId, slotIndex, correlationId, isSuccess);
		this.fireModeInstance = fireModeInstance;
	}

	protected void doEncode(FriendlyByteBuf buffer) {
		this.fireModeInstance.writeToBuf(buffer);
	}

	public static FireModeResponsePacket decode(FriendlyByteBuf buffer) {
		GunStateResponsePacket header = GunStateResponsePacket.decodeHeader(buffer);
		FireModeInstance fireModeInstance = FireModeInstance.readFromBuf(buffer);
		return new FireModeResponsePacket(
			header.stateId, header.slotIndex, header.correlationId, header.isSuccess, fireModeInstance);
	}

	protected <T extends GunStateResponsePacket> void
	handleEnqueued(Supplier<NetworkEvent.Context> ctx, ItemStack itemStack, GunClientState gunClientState) {
		((GunItem)itemStack.getItem())
			.processServerFireModeResponse(
				this.stateId, this.correlationId, this.isSuccess, itemStack, gunClientState, this.fireModeInstance);
	}
}
