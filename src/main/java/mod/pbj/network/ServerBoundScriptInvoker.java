package mod.pbj.network;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.function.Supplier;
import mod.pbj.script.Script;
import mod.pbj.script.ScriptParser;
import mod.pbj.script.Scripts;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

public class ServerBoundScriptInvoker {
	private Scripts.PacketContext context;

	public ServerBoundScriptInvoker(Scripts.PacketContext function) {
		this.context = function;
	}

	public ServerBoundScriptInvoker() {}

	public static void encode(ServerBoundScriptInvoker packet, FriendlyByteBuf buf) {
		buf.writeUtf(packet.context.serialize().toString(), Integer.MAX_VALUE);
	}

	public static ServerBoundScriptInvoker decode(FriendlyByteBuf buf) {
		return new ServerBoundScriptInvoker(
			Scripts.PacketContext.deserialize(Scripts.PacketContext.gson.fromJson(buf.readUtf(), JsonObject.class)));
	}

	public static void handle(ServerBoundScriptInvoker packet, Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			String scriptName = packet.context.staticMethod().split(":")[0];
			String methodName = packet.context.staticMethod().split(":")[1];
			Script script = ScriptParser.SCRIPTCACHE.get(ResourceLocation.fromNamespaceAndPath("_static", scriptName));
			final Object[] finalArgs =
				Arrays.copyOf(packet.context.MethodArgs(), packet.context.MethodArgs().length + 1);
			finalArgs[finalArgs.length - 1] = context.get();
			script.invokeMethod(methodName, finalArgs);
		});
		context.get().setPacketHandled(true);
	}
}
