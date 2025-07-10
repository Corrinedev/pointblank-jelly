package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class HitScanFireRequestPacket extends GunStateRequestPacket {
	private FireModeInstance fireModeInstance;
	private boolean isAiming;
	private long seed;

	public HitScanFireRequestPacket() {}

	public HitScanFireRequestPacket(
		FireModeInstance fireModeInstance, UUID stateId, int slotIndex, boolean isAiming, long seed) {
		super(stateId, slotIndex);
		this.fireModeInstance = fireModeInstance;
		this.isAiming = isAiming;
		this.seed = seed;
	}

	public static HitScanFireRequestPacket decode(FriendlyByteBuf buffer) {
		GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
		FireModeInstance fireModeInstance = FireModeInstance.readFromBuf(buffer);
		boolean isAiming = buffer.readBoolean();
		long seed = buffer.readLong();
		return new HitScanFireRequestPacket(fireModeInstance, header.stateId, header.slotIndex, isAiming, seed);
	}

	protected void doEncode(FriendlyByteBuf buffer) {
		this.fireModeInstance.writeToBuf(buffer);
		buffer.writeBoolean(this.isAiming);
		buffer.writeLong(this.seed);
	}

	protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		if (player != null) {
			ItemStack itemStack = player.getInventory().getItem(this.slotIndex);
			if (itemStack != null && itemStack.getItem() instanceof GunItem) {
				((GunItem)itemStack.getItem())
					.handleClientHitScanFireRequest(
						player,
						this.fireModeInstance,
						this.stateId,
						this.slotIndex,
						this.correlationId,
						this.isAiming,
						this.seed);
			} else {
				System.err.println("Mismatching item in slot " + this.slotIndex);
			}
		}
	}
}
