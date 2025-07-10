package mod.pbj.network;

import java.util.function.Supplier;
import mod.pbj.item.ScriptHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class ClientBoundScriptInvoker {
	private String function;
	private ItemStack stack;

	public ClientBoundScriptInvoker(String function, ItemStack stack) {
		this.function = function;
		this.stack = stack;
	}

	public ClientBoundScriptInvoker() {}

	public static void encode(ClientBoundScriptInvoker packet, FriendlyByteBuf buf) {
		buf.writeUtf(packet.function);
		buf.writeItemStack(packet.stack, false);
	}

	public static ClientBoundScriptInvoker decode(FriendlyByteBuf buf) {
		return new ClientBoundScriptInvoker(buf.readUtf(), buf.readItem());
	}

	public static void handle(ClientBoundScriptInvoker packet, Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			LocalPlayer player = Minecraft.getInstance().player;
			if (packet.stack.getItem() instanceof ScriptHolder scriptHolder)
				scriptHolder.invokeFunction(packet.function, player, packet.stack);
		});
		context.get().setPacketHandled(true);
	}
}
