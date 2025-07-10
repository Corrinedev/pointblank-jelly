package mod.pbj.network;

import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.util.ClientUtils;

public record SpawnParticlePacket(ParticleType<?> particleType, double x, double y, double z, int count) {
	public static void encode(SpawnParticlePacket packet, FriendlyByteBuf buf) {
		buf.writeResourceLocation(ForgeRegistries.PARTICLE_TYPES.getKey(packet.particleType));
		buf.writeDouble(packet.x);
		buf.writeDouble(packet.y);
		buf.writeDouble(packet.z);
		buf.writeInt(packet.count);
	}

	public static SpawnParticlePacket decode(FriendlyByteBuf buf) {
		return new SpawnParticlePacket(
			ForgeRegistries.PARTICLE_TYPES.getValue(buf.readResourceLocation()),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readInt());
	}

	public static void handle(SpawnParticlePacket packet, Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			Level level = ClientUtils.getLevel();

			for (int i = 0; i < packet.count; ++i) {
				double offsetX = Math.random() * (double)0.5F - (double)0.25F;
				double offsetY = Math.random() * (double)0.5F - (double)0.25F;
				double offsetZ = Math.random() * (double)0.5F - (double)0.25F;
				double x = packet.x + offsetX;
				double y = packet.y + offsetY;
				double z = packet.z + offsetZ;
				level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0F, 0.0F, 0.0F);
			}
		});
		context.get().setPacketHandled(true);
	}
}
