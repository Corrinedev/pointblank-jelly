package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.client.ClientEventHandler;
import mod.pbj.client.GunClientState;
import mod.pbj.item.GunItem;
import mod.pbj.util.InventoryUtils;
import mod.pbj.util.SimpleHitResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import software.bernie.geckolib.util.ClientUtils;

public class HitScanFireResponsePacket extends GunStateResponsePacket {
	private SimpleHitResult hitResult;
	private int ownerEntityId;
	private float damage;

	public HitScanFireResponsePacket() {}

	public HitScanFireResponsePacket(
		int ownerEntityId, UUID stateId, int slotIndex, int correlationId, SimpleHitResult hitResult, float damage) {
		super(stateId, slotIndex, correlationId, true);
		this.ownerEntityId = ownerEntityId;
		this.hitResult = hitResult;
		this.damage = damage;
	}

	protected void doEncode(FriendlyByteBuf buffer) {
		buffer.writeInt(this.ownerEntityId);
		SimpleHitResult.writer().accept(buffer, this.hitResult);
		buffer.writeFloat(this.damage);
	}

	public static HitScanFireResponsePacket decode(FriendlyByteBuf buffer) {
		GunStateResponsePacket header = GunStateResponsePacket.decodeHeader(buffer);
		int ownerEntityId = buffer.readInt();
		SimpleHitResult hitResult = SimpleHitResult.reader().apply(buffer);
		float damage = buffer.readFloat();
		return new HitScanFireResponsePacket(
			ownerEntityId, header.stateId, header.slotIndex, header.correlationId, hitResult, damage);
	}

	public static void handle(HitScanFireResponsePacket packet, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> ClientEventHandler.runSyncTick(() -> {
			Level level = ClientUtils.getLevel();
			Entity entity = level.getEntity(packet.ownerEntityId);
			if (entity instanceof Player player) {
				Tuple<ItemStack, GunClientState> targetTuple = packet.getItemStackAndState(packet, player);
				if (targetTuple != null) {
					packet.handleEnqueued(player, targetTuple.getA(), targetTuple.getB());
				}
			}
		}));
		ctx.get().setPacketHandled(true);
	}

	protected <T extends GunStateResponsePacket> Tuple<ItemStack, GunClientState>
	getItemStackAndState(T packet, Entity entity) {
		Player clientPlayer = ClientUtils.getClientPlayer();
		if (entity instanceof Player player) {
			return InventoryUtils.getItemStackByStateId(
				player, packet.stateId, clientPlayer == player ? packet.slotIndex : 0);
		} else {
			return null;
		}
	}

	protected <T extends GunStateResponsePacket> void
	handleEnqueued(Player player, ItemStack itemStack, GunClientState gunClientState) {
		Item var5 = itemStack.getItem();
		if (var5 instanceof GunItem gunItem) {
			gunItem.processServerHitScanFireResponse(
				player, this.stateId, itemStack, gunClientState, this.hitResult, this.damage);
		}
	}
}
