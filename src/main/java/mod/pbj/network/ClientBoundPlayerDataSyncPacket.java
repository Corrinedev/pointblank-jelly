package mod.pbj.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import mod.pbj.registry.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class ClientBoundPlayerDataSyncPacket {
	private CompoundTag playerData;
	private List<Integer> enabledItems;

	public ClientBoundPlayerDataSyncPacket(CompoundTag playerData, List<Integer> enabledItems) {
		this.playerData = playerData;
		this.enabledItems = enabledItems;
	}

	public ClientBoundPlayerDataSyncPacket() {}

	public static ClientBoundPlayerDataSyncPacket decode(FriendlyByteBuf buf) {
		ClientBoundPlayerDataSyncPacket packet = new ClientBoundPlayerDataSyncPacket();
		packet.playerData = buf.readNbt();
		int itemCount = buf.readInt();
		packet.enabledItems = new ArrayList<>();

		for (int i = 0; i < itemCount; ++i) {
			packet.enabledItems.add(buf.readInt());
		}

		return packet;
	}

	public static void encode(ClientBoundPlayerDataSyncPacket packet, FriendlyByteBuf buf) {
		buf.writeNbt(packet.playerData);
		buf.writeInt(packet.enabledItems.size());

		for (int itemId : packet.enabledItems) {
			buf.writeInt(itemId);
		}
	}

	public static void handle(ClientBoundPlayerDataSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			CompoundTag serverSlotMapping = packet.playerData.getCompound("pointblank:attachmentSlotMapping");
			if (serverSlotMapping != null) {
				mc.player.getPersistentData().put("pointblank:attachmentSlotMapping", serverSlotMapping);
			} else {
				mc.player.getPersistentData().remove(null);
			}

			ItemRegistry.ITEMS.syncEnabledItems(packet.enabledItems);
		});
		ctx.get().setPacketHandled(true);
	}

	public CompoundTag getPlayerData() {
		return this.playerData;
	}
}
