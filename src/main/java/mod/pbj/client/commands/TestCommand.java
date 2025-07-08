package mod.pbj.client.commands;

import java.util.function.Consumer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

// low priority todo: integrate this with the config
public class TestCommand {
	public static float zoomMultiplier = 1;

	public static void sendSystemMessage(String msg) {
		final var player = Minecraft.getInstance().player;
		if (player == null) {
			System.err.println("[TestCommand.sendSystemMessage] player is null");
			return;
		}
		player.sendSystemMessage(Component.literal(msg));
	}

	public static <T> LiteralArgumentBuilder<CommandSourceStack> getSetArgument(Class<T> clazz, ArgumentType<T> type, String name, Consumer<T> valueSetter) {
		return Commands.literal(name).then(
			Commands.argument(name, type)
				.executes(command -> {
					final var value = command.getArgument(name, clazz);
					valueSetter.accept(value);
					sendSystemMessage("Set " + name + " to " + value);
					return Command.SINGLE_SUCCESS;
				})
		);
	}

	public static LiteralArgumentBuilder<CommandSourceStack> floatArgument(String name, Consumer<Float> valueSetter) {
		return getSetArgument(Float.class, FloatArgumentType.floatArg(), name, valueSetter);
	}

	public static LiteralArgumentBuilder<CommandSourceStack> boolArgument(String name, Consumer<Boolean> valueSetter) {
		return getSetArgument(Boolean.class, BoolArgumentType.bool(), name, valueSetter);
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("pbj")
				// do NOT set higher than 10, see AuxLevelRenderer for details
				.then(floatArgument("zoomMultiplier", v -> zoomMultiplier = v))
		);
	}
}
