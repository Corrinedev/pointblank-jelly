package mod.pbj.network;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.SimpleHitResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EffectRequestPacket {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private static final int MAX_DISTANCE_SQR = 22500;
	protected int playerEntityId;
	protected UUID gunStateId;
	protected UUID effectId;
	protected Vec3 startPosition;
	protected SimpleHitResult hitResult;
	protected boolean hasMuzzlePositionProvider;

	public EffectRequestPacket() {}

	public EffectRequestPacket(
		int playerEntityId,
		UUID gunStateId,
		UUID effectId,
		Vec3 startPosition,
		SimpleHitResult hitResult,
		boolean hasMuzzlePositionProvider) {
		this.playerEntityId = playerEntityId;
		this.gunStateId = gunStateId;
		this.effectId = effectId;
		this.startPosition = startPosition;
		this.hitResult = hitResult;
		this.hasMuzzlePositionProvider = hasMuzzlePositionProvider;
	}

	public static <T extends EffectRequestPacket> void encode(T packet, FriendlyByteBuf buffer) {
		buffer.writeInt(packet.playerEntityId);
		buffer.writeLong(packet.gunStateId.getMostSignificantBits());
		buffer.writeLong(packet.gunStateId.getLeastSignificantBits());
		buffer.writeLong(packet.effectId.getMostSignificantBits());
		buffer.writeLong(packet.effectId.getLeastSignificantBits());
		buffer.writeOptional(Optional.ofNullable(packet.startPosition), MiscUtil.VEC3_WRITER);
		buffer.writeOptional(Optional.ofNullable(packet.hitResult), SimpleHitResult.writer());
		buffer.writeBoolean(packet.hasMuzzlePositionProvider);
		packet.doEncode(buffer);
	}

	protected void doEncode(FriendlyByteBuf buffer) {}

	protected static EffectRequestPacket decode(FriendlyByteBuf buffer) {
		int playerEntityId = buffer.readInt();
		UUID gunStateId = new UUID(buffer.readLong(), buffer.readLong());
		UUID effectId = new UUID(buffer.readLong(), buffer.readLong());
		Vec3 startPosition = buffer.readOptional(MiscUtil.VEC3_READER).orElse(null);
		SimpleHitResult hitResult = buffer.readOptional(SimpleHitResult.reader()).orElse(null);
		boolean hasMuzzlePositionProvider = buffer.readBoolean();
		return new EffectRequestPacket(
			playerEntityId, gunStateId, effectId, startPosition, hitResult, hasMuzzlePositionProvider);
	}

	public static <T extends EffectRequestPacket> void handle(T packet, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> packet.handleEnqueued(ctx));
		ctx.get().setPacketHandled(true);
	}

	protected <T extends EffectRequestPacket> void handleEnqueued(Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer sender = ctx.get().getSender();
		LOGGER.debug("Received effect request {} from {}", this, sender);

		for (ServerPlayer player : ((ServerLevel)MiscUtil.getLevel(sender)).getPlayers((p) -> true)) {
			if (player.distanceToSqr(this.startPosition) < (double)22500.0F) {
				Network.networkChannel.send(
					PacketDistributor.PLAYER.with(() -> player),
					new EffectBroadcastPacket(
						this.playerEntityId,
						this.gunStateId,
						this.effectId,
						this.startPosition,
						this.hitResult,
						this.hasMuzzlePositionProvider));
			}
		}
	}
}
