package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.registry.FeatureTypeRegistry;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class ServerBoundNextAttachmentPacket {
   private AttachmentCategory category;
   private Class<? extends Feature> featureType;

   public ServerBoundNextAttachmentPacket(AttachmentCategory category, Class<? extends Feature> featureType) {
      this.category = category;
      this.featureType = featureType;
   }

   public ServerBoundNextAttachmentPacket() {
   }

   public static void encode(ServerBoundNextAttachmentPacket packet, FriendlyByteBuf buf) {
      buf.writeInt(packet.category.ordinal());
      buf.writeInt(FeatureTypeRegistry.getFeatureTypeId(packet.featureType));
   }

   public static ServerBoundNextAttachmentPacket decode(FriendlyByteBuf buf) {
      return new ServerBoundNextAttachmentPacket(AttachmentCategory.fromOrdinal(buf.readInt()), FeatureTypeRegistry.getFeatureType(buf.readInt()));
   }

   public static void handle(ServerBoundNextAttachmentPacket packet, Supplier<NetworkEvent.Context> context) {
      context.get().enqueueWork(() -> {
         ServerPlayer player = context.get().getSender();
         ItemStack mainHeldItem = player.getMainHandItem();
         if (mainHeldItem != null) {
            Attachments.selectNextAttachment(mainHeldItem, packet.category, packet.featureType);
         }

      });
      context.get().setPacketHandled(true);
   }
}
