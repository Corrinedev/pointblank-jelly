package mod.pbj.script;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Arrays;
import java.util.function.Consumer;
import mod.pbj.PointBlankJelly;
import mod.pbj.network.ClientBoundScriptInvoker;
import mod.pbj.network.Network;
import mod.pbj.network.ServerBoundScriptInvoker;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.network.PacketDistributor;

public final class Scripts {
	public static void runFunctionOnServer(PacketContext context) {
		Network.networkChannel.sendToServer(new ServerBoundScriptInvoker(context));
	}

	public static void runFunctionOnClient(String functionName, ItemStack stack, ServerPlayer player) {
		Network.networkChannel.send(
			PacketDistributor.PLAYER.with(() -> player), new ClientBoundScriptInvoker(functionName, stack));
	}

	public static <E extends Event> void registerEvent(Consumer<E> event, Class<E> register, boolean forge) {
		if (forge)
			MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, register, event);
		else
			PointBlankJelly.modEventBus.addListener(EventPriority.NORMAL, true, register, event);
	}

	public static <E extends Event> void registerForgeEvent(Consumer<E> event, Class<E> register) {
		MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, register, event);
	}

	public static <E extends Event> void registerModEvent(Consumer<E> event, Class<E> register) {
		PointBlankJelly.modEventBus.addListener(EventPriority.NORMAL, true, register, event);
	}

	public record PacketContext(String staticMethod, Object... MethodArgs) {
		public static final Gson gson = new GsonBuilder().create();

		public JsonObject serialize() {
			JsonObject obj = new JsonObject();
			obj.addProperty("staticMethod", this.staticMethod);
			JsonArray args = new JsonArray();
			for (Object methodArg : MethodArgs) {
				if (Type.hasCodec(methodArg)) {
					JsonElement value = switch (methodArg.getClass().getSimpleName().toLowerCase()) {
						case "itemstack" ->
							ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, (ItemStack)methodArg).result().orElse(null);
						case "integer" -> Codec.INT.encodeStart(JsonOps.INSTANCE, (int)methodArg).result().orElse(null);
						case "long" -> Codec.LONG.encodeStart(JsonOps.INSTANCE, (long)methodArg).result().orElse(null);
						case "float" ->
							Codec.FLOAT.encodeStart(JsonOps.INSTANCE, (float)methodArg).result().orElse(null);
						case "double" ->
							Codec.DOUBLE.encodeStart(JsonOps.INSTANCE, (double)methodArg).result().orElse(null);
						case "boolean" ->
							Codec.BOOL.encodeStart(JsonOps.INSTANCE, (boolean)methodArg).result().orElse(null);
						case "compoundtag" ->
							CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, (CompoundTag)methodArg)
								.result()
								.orElse(null);
						case "jsonobject" -> (JsonObject) methodArg;
						case "jsonelement" -> (JsonElement) methodArg;
						case "jsonarray" -> (JsonArray) methodArg;
						case "jsonprimitive" -> (JsonPrimitive) methodArg;
						case "jsonnull" -> (JsonNull) methodArg;
						default -> null;
					};
					if (value == null && methodArg.getClass().getSimpleName().equalsIgnoreCase("player") ||
						methodArg.getClass().getSimpleName().equalsIgnoreCase("localplayer")) {
						var oj = new JsonObject();
						oj.addProperty("type", "player");
						args.add(oj);
					}
					if (value != null) {
						var argObj = new JsonObject();
						argObj.addProperty("type", methodArg.getClass().getSimpleName());
						argObj.add("value", value);
						args.add(argObj);
					}
				}
			}
			obj.add("arguments", args);
			PointBlankJelly.LOGGER.debug("Serialized PacketContext w/ JsonObject = {}", obj);
			return obj;
		}
		public static PacketContext deserialize(JsonObject obj) {
			var packetMedium = new Object() {
				String staticMethod = null;
				Object[] args = null;
			};
			packetMedium.staticMethod = obj.get("staticMethod").getAsString();
			if (obj.has("arguments")) {
				var argArr = obj.getAsJsonArray("arguments");
				packetMedium.args = new Object[argArr.size()];
				var i = 0;
				for (JsonElement arge : argArr) {
					JsonObject arg = arge.getAsJsonObject();
					Type type = Type.valueOf(arg.get("type").getAsString().toUpperCase());
					Object val = (type.codec).parse(JsonOps.INSTANCE, arg.get("value")).result().orElse(null);
					packetMedium.args[i] = val;
					i++;
				}
			}
			PacketContext ctx = new PacketContext(packetMedium.staticMethod, packetMedium.args);
			PointBlankJelly.LOGGER.debug("Deserialized PacketContext = {}", ctx.toString());
			return ctx;
		}

		@Override
		public String toString() {
			return "PacketContext{"
				+ "staticMethod='" + staticMethod + '\'' + ", MethodArgs=" + Arrays.toString(MethodArgs) + '}';
		}

		private enum Type {
			ITEMSTACK(ItemStack.CODEC),
			BOOLEAN(Codec.BOOL),
			INTEGER(Codec.INT),
			LONG(Codec.LONG),
			FLOAT(Codec.FLOAT),
			DOUBLE(Codec.DOUBLE),
			COMPOUNDTAG(CompoundTag.CODEC);

			public final Codec<?> codec;
			Type(Codec<?> type) {
				this.codec = type;
			}
			public static boolean hasCodec(Object obj) {
				for (Type value : Type.values()) {
					if (value.name().equalsIgnoreCase(obj.getClass().getSimpleName()))
						return true;
				}
				return false;
			}
		}
	}
}
