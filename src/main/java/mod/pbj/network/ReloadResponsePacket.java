package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.client.GunClientState;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public class ReloadResponsePacket extends GunStateResponsePacket {
	private int ammo;
	private FireModeInstance fireModeInstance;

	public ReloadResponsePacket() {}

	public ReloadResponsePacket(
		UUID stateId,
		int slotIndex,
		int correlationId,
		boolean isSuccess,
		int ammo,
		FireModeInstance fireModeInstance) {
		super(stateId, slotIndex, correlationId, isSuccess);
		this.ammo = ammo;
		this.fireModeInstance = fireModeInstance;
	}

	protected void doEncode(FriendlyByteBuf buffer) {
		buffer.writeInt(this.ammo);
		this.fireModeInstance.writeToBuf(buffer);
	}

	public static ReloadResponsePacket decode(FriendlyByteBuf buffer) {
		GunStateResponsePacket header = GunStateResponsePacket.decodeHeader(buffer);
		int ammo = buffer.readInt();
		FireModeInstance fireMode = FireModeInstance.readFromBuf(buffer);
		return new ReloadResponsePacket(
			header.stateId, header.slotIndex, header.correlationId, header.isSuccess, ammo, fireMode);
	}

	protected <T extends GunStateResponsePacket> void
	handleEnqueued(Supplier<Context> ctx, ItemStack itemStack, GunClientState gunClientState) {
		((GunItem)itemStack.getItem())
			.processServerReloadResponse(
				this.correlationId, this.isSuccess, itemStack, gunClientState, this.ammo, this.fireModeInstance);
	}
}
