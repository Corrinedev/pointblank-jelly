//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AimingChangeRequestPacket extends GunStateRequestPacket {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private boolean isAimingEnabled;

	public AimingChangeRequestPacket() {}

	public AimingChangeRequestPacket(UUID stateId, int slotIndex, boolean isAimingEnabled) {
		super(stateId, slotIndex);
		this.isAimingEnabled = isAimingEnabled;
	}

	public static AimingChangeRequestPacket decode(FriendlyByteBuf buffer) {
		GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
		return new AimingChangeRequestPacket(header.stateId, header.slotIndex, buffer.readBoolean());
	}

	protected void doEncode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(this.isAimingEnabled);
	}

	protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		if (player != null) {
			ItemStack itemStack = player.getInventory().getItem(this.slotIndex);
			if (itemStack != null) {
				Item var5 = itemStack.getItem();
				if (var5 instanceof GunItem gunItem) {
					gunItem.handleAimingChangeRequest(
						player, itemStack, this.stateId, this.slotIndex, this.isAimingEnabled);
					return;
				}
			}

			LOGGER.warn("Mismatching item in slot {}", this.slotIndex);
		}
	}
}
