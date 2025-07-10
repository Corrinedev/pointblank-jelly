package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class GunStateRequestPacket {
	protected UUID stateId;
	protected int slotIndex;
	protected int correlationId;

	public GunStateRequestPacket() {}

	public GunStateRequestPacket(UUID stateId, int slotIndex) {
		this.stateId = stateId;
		this.slotIndex = slotIndex;
	}

	public static <T extends GunStateRequestPacket> void encode(T packet, FriendlyByteBuf buffer) {
		buffer.writeLong(packet.stateId.getMostSignificantBits());
		buffer.writeLong(packet.stateId.getLeastSignificantBits());
		buffer.writeInt(packet.slotIndex);
		packet.doEncode(buffer);
	}

	protected void doEncode(FriendlyByteBuf buffer) {}

	protected static GunStateRequestPacket decodeHeader(FriendlyByteBuf buffer) {
		UUID stateId = new UUID(buffer.readLong(), buffer.readLong());
		int slotIndex = buffer.readInt();
		return new GunStateRequestPacket(stateId, slotIndex);
	}

	public static <T extends GunStateRequestPacket> void handle(T packet, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> packet.handleEnqueued(ctx));
		ctx.get().setPacketHandled(true);
	}

	protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<NetworkEvent.Context> ctx) {}
}
