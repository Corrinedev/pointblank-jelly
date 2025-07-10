package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.item.ThrowableLike;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class ThrowProjectileRequestPacket {
	protected UUID stateId;
	protected int slotIndex;

	public ThrowProjectileRequestPacket() {}

	public ThrowProjectileRequestPacket(UUID stateId, int slotIndex) {
		this.stateId = stateId;
		this.slotIndex = slotIndex;
	}

	public static <T extends ThrowProjectileRequestPacket> void encode(T packet, FriendlyByteBuf buffer) {
		buffer.writeLong(packet.stateId.getMostSignificantBits());
		buffer.writeLong(packet.stateId.getLeastSignificantBits());
		buffer.writeInt(packet.slotIndex);
	}

	public static void handle(ThrowProjectileRequestPacket packet, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> packet.handleEnqueued(ctx));
		ctx.get().setPacketHandled(true);
	}

	public static ThrowProjectileRequestPacket decode(FriendlyByteBuf buffer) {
		UUID stateId = new UUID(buffer.readLong(), buffer.readLong());
		int slotIndex = buffer.readInt();
		return new ThrowProjectileRequestPacket(stateId, slotIndex);
	}

	protected void handleEnqueued(Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		if (player != null) {
			ItemStack itemStack = player.getInventory().getItem(this.slotIndex);
			if (itemStack != null && itemStack.getItem() instanceof ThrowableLike) {
				((ThrowableLike)itemStack.getItem()).handleClientThrowRequest(player, this.stateId, this.slotIndex);
			} else {
				System.err.println("Mismatching item in slot " + this.slotIndex);
			}
		}
	}
}
