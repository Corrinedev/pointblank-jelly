package mod.pbj.client.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import mod.pbj.client.effect.EffectLauncher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class TestCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("test")
				.then(Commands.literal("tracerTranslation")
					.then(Commands.argument("pos", Vec3Argument.vec3())
						.executes(TestCommand::executeTracerTranslation)
					)
				)
				.then(Commands.literal("tracerRotation")
					.then(Commands.argument("rot", Vec3Argument.vec3())
						.executes(TestCommand::executeTracerRotation)
					)
				)
		);
	}

	private static int executeTracerTranslation(CommandContext<CommandSourceStack> command) {
		if (command.getSource().getEntity() instanceof final Player player) {
			final Vec3 pos = Vec3Argument.getVec3(command, "pos");
			EffectLauncher.tracerTranslation = pos;
			player.sendSystemMessage(Component.literal("Set tracerTranslation to: " + pos));
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int executeTracerRotation(CommandContext<CommandSourceStack> command) {
		if (command.getSource().getEntity() instanceof final Player player) {
			final Vec3 rot = Vec3Argument.getVec3(command, "rot");
			EffectLauncher.tracerRotation = rot;
			player.sendSystemMessage(Component.literal("Set tracerRotation to: " + rot));
		}
		return Command.SINGLE_SUCCESS;
	}
}
