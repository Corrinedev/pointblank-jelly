package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.client.GunClientState;
import mod.pbj.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public class StopFireResponsePacket extends GunStateResponsePacket {
	public StopFireResponsePacket() {}

	public StopFireResponsePacket(UUID stateId, int slotIndex, int correlationId, boolean isSuccess) {
		super(stateId, slotIndex, correlationId, isSuccess);
	}

	protected void doEncode(FriendlyByteBuf buffer) {}

	public static StopFireResponsePacket decode(FriendlyByteBuf buffer) {
		GunStateResponsePacket header = GunStateResponsePacket.decodeHeader(buffer);
		return new StopFireResponsePacket(header.stateId, header.slotIndex, header.correlationId, header.isSuccess);
	}

	protected <T extends GunStateResponsePacket> void
	handleEnqueued(Supplier<Context> ctx, ItemStack itemStack, GunClientState gunClientState) {
		((GunItem)itemStack.getItem())
			.processStopServerFireResponse(this.stateId, this.correlationId, this.isSuccess, itemStack, gunClientState);
	}
}
