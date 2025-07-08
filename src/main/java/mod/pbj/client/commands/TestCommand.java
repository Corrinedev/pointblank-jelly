package mod.pbj.client.commands;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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

	public static LiteralArgumentBuilder<CommandSourceStack> floatArgument(String name, Consumer<Float> valueSetter) {
		return Commands.literal(name).then(
			Commands.argument(name, FloatArgumentType.floatArg())
				.executes(command -> {
					final var value = command.getArgument(name, Float.class);
					valueSetter.accept(value);
					sendSystemMessage("Set " + name + " to " + value);
					return Command.SINGLE_SUCCESS;
				})
		);
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("test")
				// 12.75 is the limit before the game hangs, on my PC at least
				.then(floatArgument("zoomMultiplier", v -> zoomMultiplier = v))
		);
	}
}
