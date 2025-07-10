package mod.pbj.network;

import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class ProjectileFireRequestPacket extends GunStateRequestPacket {
	private FireModeInstance fireModeInstance;
	private boolean isAiming;
	private double posX;
	private double posY;
	private double posZ;
	private double directionX;
	private double directionY;
	private double directionZ;
	private int targetEntityId;
	private long seed;

	public ProjectileFireRequestPacket() {}

	public ProjectileFireRequestPacket(
		FireModeInstance fireModeInstance,
		UUID stateId,
		int slotIndex,
		boolean isAiming,
		double posX,
		double posY,
		double posZ,
		double directionX,
		double directionY,
		double directionZ,
		int targetEntityId,
		long seed) {
		super(stateId, slotIndex);
		this.fireModeInstance = fireModeInstance;
		this.isAiming = isAiming;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		this.directionX = directionX;
		this.directionY = directionY;
		this.directionZ = directionZ;
		this.targetEntityId = targetEntityId;
		this.seed = seed;
	}

	public static ProjectileFireRequestPacket decode(FriendlyByteBuf buffer) {
		GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
		FireModeInstance fireMode = FireModeInstance.readFromBuf(buffer);
		boolean isAiming = buffer.readBoolean();
		double posX = buffer.readDouble();
		double posY = buffer.readDouble();
		double posZ = buffer.readDouble();
		double directionX = buffer.readDouble();
		double directionY = buffer.readDouble();
		double directionZ = buffer.readDouble();
		int targetEntityId = buffer.readInt();
		long seed = buffer.readLong();
		return new ProjectileFireRequestPacket(
			fireMode,
			header.stateId,
			header.slotIndex,
			isAiming,
			posX,
			posY,
			posZ,
			directionX,
			directionY,
			directionZ,
			targetEntityId,
			seed);
	}

	protected void doEncode(FriendlyByteBuf buffer) {
		this.fireModeInstance.writeToBuf(buffer);
		buffer.writeBoolean(this.isAiming);
		buffer.writeDouble(this.posX);
		buffer.writeDouble(this.posY);
		buffer.writeDouble(this.posZ);
		buffer.writeDouble(this.directionX);
		buffer.writeDouble(this.directionY);
		buffer.writeDouble(this.directionZ);
		buffer.writeInt(this.targetEntityId);
		buffer.writeLong(this.seed);
	}

	protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer player = ctx.get().getSender();
		if (player != null) {
			ItemStack itemStack = player.getInventory().getItem(this.slotIndex);
			if (itemStack != null && itemStack.getItem() instanceof GunItem) {
				((GunItem)itemStack.getItem())
					.handleClientProjectileFireRequest(
						player,
						this.fireModeInstance,
						this.stateId,
						this.slotIndex,
						this.correlationId,
						this.isAiming,
						this.posX,
						this.posY,
						this.posZ,
						this.directionX,
						this.directionY,
						this.directionZ,
						this.targetEntityId,
						this.seed);
			} else {
				System.err.println("Mismatching item in slot " + this.slotIndex);
			}
		}
	}
}
