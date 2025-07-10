package mod.pbj.network;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.Config;
import mod.pbj.client.ClientEventHandler;
import mod.pbj.client.GunClientState;
import mod.pbj.client.GunStatePoseProvider;
import mod.pbj.client.GunStatePoseProvider.PoseContext;
import mod.pbj.client.PoseProvider;
import mod.pbj.client.PositionProvider;
import mod.pbj.client.VertexConsumers;
import mod.pbj.client.effect.Effect;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.registry.EffectRegistry;
import mod.pbj.util.SimpleHitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.util.ClientUtils;

public class EffectBroadcastPacket {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	protected int playerEntityId;
	protected UUID gunStateId;
	protected UUID effectId;
	protected Vec3 startPosition;
	protected SimpleHitResult hitResult;
	protected boolean hasMuzzlePositionProvider;

	public EffectBroadcastPacket() {}

	public EffectBroadcastPacket(
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

	public static <T extends EffectBroadcastPacket> void encode(T packet, FriendlyByteBuf buffer) {
		buffer.writeInt(packet.playerEntityId);
		buffer.writeLong(packet.gunStateId.getMostSignificantBits());
		buffer.writeLong(packet.gunStateId.getLeastSignificantBits());
		buffer.writeLong(packet.effectId.getMostSignificantBits());
		buffer.writeLong(packet.effectId.getLeastSignificantBits());
		buffer.writeDouble(packet.startPosition.x);
		buffer.writeDouble(packet.startPosition.y);
		buffer.writeDouble(packet.startPosition.z);
		buffer.writeOptional(Optional.ofNullable(packet.hitResult), SimpleHitResult.writer());
		buffer.writeBoolean(packet.hasMuzzlePositionProvider);
		packet.doEncode(buffer);
	}

	protected void doEncode(FriendlyByteBuf buffer) {}

	protected static EffectBroadcastPacket decode(FriendlyByteBuf buffer) {
		int playerEntityId = buffer.readInt();
		UUID gunStateId = new UUID(buffer.readLong(), buffer.readLong());
		UUID effectId = new UUID(buffer.readLong(), buffer.readLong());
		Vec3 startPosition = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		SimpleHitResult hitResult = buffer.readOptional(SimpleHitResult.reader()).orElse(null);
		boolean hasMuzzlePositionProvider = buffer.readBoolean();
		return new EffectBroadcastPacket(
			playerEntityId, gunStateId, effectId, startPosition, hitResult, hasMuzzlePositionProvider);
	}

	public static <T extends EffectBroadcastPacket> void handle(T packet, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> ClientEventHandler.runSyncTick(() -> {
			if (Config.particleEffectsEnabled) {
				launchEffect(packet);
			}
		}));
		ctx.get().setPacketHandled(true);
	}

	private static <T extends EffectBroadcastPacket> void launchEffect(T packet) {
		Minecraft mc = Minecraft.getInstance();
		Player clientPlayer = ClientUtils.getClientPlayer();
		Entity effectOwnerEntity = mc.level.getEntity(packet.playerEntityId);
		if (effectOwnerEntity instanceof Player effectOwnerPlayer) {
			if (effectOwnerPlayer == clientPlayer) {
			}

			Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilderSupplier =
				EffectRegistry.getEffectBuilderSupplier(packet.effectId);
			if (effectBuilderSupplier != null) {
				EffectBuilder<? extends EffectBuilder<?, ?>, ?> effectBuilder = effectBuilderSupplier.get();
				HitResult hitResult = packet.hitResult;
				double distanceToTarget;
				if (hitResult != null) {
					distanceToTarget = hitResult.getLocation().distanceTo(packet.startPosition);
				} else {
					distanceToTarget = 400.0F;
				}

				GunClientState gunState = GunClientState.getState(packet.gunStateId);
				PositionProvider positionProvider = null;
				PoseProvider poseProvider = null;
				if (packet.hasMuzzlePositionProvider && gunState != null) {
					positionProvider = ()
						-> GunStatePoseProvider.getInstance().getPositionAndDirection(
							gunState, PoseContext.THIRD_PERSON_MUZZLE);
					poseProvider =
						() -> GunStatePoseProvider.getInstance().getPose(gunState, PoseContext.THIRD_PERSON_MUZZLE);
				}

				EffectBuilder.Context effectBuilderContext =
					(new EffectBuilder.Context())
						.withGunState(gunState)
						.withStartPosition(packet.startPosition)
						.withDistance((float)distanceToTarget)
						.withRandomization(0.0F)
						.withVertexConsumerTransformer(VertexConsumers.PARTICLE)
						.withPositionProvider(positionProvider)
						.withPoseProvider(poseProvider)
						.withHitResult(packet.hitResult);
				Effect effect = effectBuilder.build(effectBuilderContext);
				LOGGER.debug("Launching effect {}", effect);
				effect.launch(effectOwnerPlayer);
			}
		}
	}
}
