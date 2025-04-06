package com.vicmatskiv.pointblank.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry.ChannelBuilder;
import net.minecraftforge.network.simple.SimpleChannel;

public class Network {
   public static SimpleChannel networkChannel;
   public static final String PROTOCOL_VERSION = "1";

   public static void setupNetworkChannel() {
      networkChannel = ChannelBuilder.named(new ResourceLocation("pointblank", "pointblank")).networkProtocolVersion(() -> {
         return "1";
      }).clientAcceptedVersions("1"::equals).serverAcceptedVersions("1"::equals).simpleChannel();
      networkChannel.registerMessage(1, MainHeldSimplifiedStateSyncRequest.class, GunStateRequestPacket::encode, MainHeldSimplifiedStateSyncRequest::decode, GunStateRequestPacket::handle);
      networkChannel.registerMessage(2, MainHeldSimplifiedStateBroadcastPacket.class, MainHeldSimplifiedStateBroadcastPacket::encode, MainHeldSimplifiedStateBroadcastPacket::decode, MainHeldSimplifiedStateBroadcastPacket::handle);
      networkChannel.registerMessage(3, HitScanFireRequestPacket.class, GunStateRequestPacket::encode, HitScanFireRequestPacket::decode, GunStateRequestPacket::handle);
      networkChannel.registerMessage(4, HitScanFireResponsePacket.class, GunStateResponsePacket::encode, HitScanFireResponsePacket::decode, HitScanFireResponsePacket::handle);
      networkChannel.registerMessage(5, ProjectileFireRequestPacket.class, GunStateRequestPacket::encode, ProjectileFireRequestPacket::decode, GunStateRequestPacket::handle);
      networkChannel.registerMessage(7, ReloadRequestPacket.class, GunStateRequestPacket::encode, ReloadRequestPacket::decode, GunStateRequestPacket::handle);
      networkChannel.registerMessage(8, ReloadResponsePacket.class, GunStateResponsePacket::encode, ReloadResponsePacket::decode, GunStateResponsePacket::handle);
      networkChannel.registerMessage(9, FireModeRequestPacket.class, GunStateRequestPacket::encode, FireModeRequestPacket::decode, GunStateRequestPacket::handle);
      networkChannel.registerMessage(10, FireModeResponsePacket.class, GunStateResponsePacket::encode, FireModeResponsePacket::decode, GunStateResponsePacket::handle);
      networkChannel.registerMessage(11, SpawnParticlePacket.class, SpawnParticlePacket::encode, SpawnParticlePacket::decode, SpawnParticlePacket::handle);
      networkChannel.registerMessage(12, CustomClientBoundExplosionPacket.class, CustomClientBoundExplosionPacket::encode, CustomClientBoundExplosionPacket::decode, CustomClientBoundExplosionPacket::handle);
      networkChannel.registerMessage(13, EffectRequestPacket.class, EffectRequestPacket::encode, EffectRequestPacket::decode, EffectRequestPacket::handle);
      networkChannel.registerMessage(14, AimingChangeRequestPacket.class, GunStateRequestPacket::encode, AimingChangeRequestPacket::decode, GunStateRequestPacket::handle);
      networkChannel.registerMessage(15, CraftingRequestPacket.class, CraftingRequestPacket::encode, CraftingRequestPacket::decode, CraftingRequestPacket::handle);
      networkChannel.registerMessage(16, CraftingResponsePacket.class, CraftingResponsePacket::encode, CraftingResponsePacket::decode, CraftingResponsePacket::handle);
      networkChannel.registerMessage(17, ServerBoundOpenScreenPacket.class, ServerBoundOpenScreenPacket::encode, ServerBoundOpenScreenPacket::decode, ServerBoundOpenScreenPacket::handle);
      networkChannel.registerMessage(18, ServerBoundNextAttachmentPacket.class, ServerBoundNextAttachmentPacket::encode, ServerBoundNextAttachmentPacket::decode, ServerBoundNextAttachmentPacket::handle);
      networkChannel.registerMessage(19, ClientBoundPlayerDataSyncPacket.class, ClientBoundPlayerDataSyncPacket::encode, ClientBoundPlayerDataSyncPacket::decode, ClientBoundPlayerDataSyncPacket::handle);
      networkChannel.registerMessage(20, EffectBroadcastPacket.class, EffectBroadcastPacket::encode, EffectBroadcastPacket::decode, EffectBroadcastPacket::handle);
      networkChannel.registerMessage(21, ThrowProjectileRequestPacket.class, ThrowProjectileRequestPacket::encode, ThrowProjectileRequestPacket::decode, ThrowProjectileRequestPacket::handle);
   }
}
