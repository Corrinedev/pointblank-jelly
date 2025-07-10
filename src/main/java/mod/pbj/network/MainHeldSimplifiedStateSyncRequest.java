package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.client.GunClientState;
import mod.pbj.client.GunClientState.FireState;
import mod.pbj.util.MiscUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class MainHeldSimplifiedStateSyncRequest extends GunStateRequestPacket {
	private static final int MAX_STATE_SYNC_DISTANCE_SQR = 10000;
	private GunClientState.FireState simplifiedState;

	public MainHeldSimplifiedStateSyncRequest() {}

	public MainHeldSimplifiedStateSyncRequest(UUID stateId, GunClientState.FireState simplifiedState) {
		super(stateId, 0);
		this.simplifiedState = simplifiedState;
	}

	protected void doEncode(FriendlyByteBuf buffer) {
		buffer.writeInt(this.simplifiedState.ordinal());
	}

	public static MainHeldSimplifiedStateSyncRequest decode(FriendlyByteBuf buffer) {
		GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
		GunClientState.FireState state = FireState.values()[buffer.readInt()];
		return new MainHeldSimplifiedStateSyncRequest(header.stateId, state);
	}

	protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer sender = ctx.get().getSender();

		for (ServerPlayer player : ((ServerLevel)MiscUtil.getLevel(sender)).getPlayers((p) -> true)) {
			if (player != sender && player.distanceToSqr(player.position()) < (double)10000.0F) {
				Network.networkChannel.send(
					PacketDistributor.PLAYER.with(() -> player),
					new MainHeldSimplifiedStateBroadcastPacket(sender, this.stateId, this.simplifiedState));
			}
		}
	}
}
