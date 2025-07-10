package mod.pbj.util;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ClientUtil {
	public ClientUtil() {}

	public static Player getClientPlayer() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player;
	}

	public static boolean isFirstPerson(LivingEntity livingEntity) {
		Minecraft mc = Minecraft.getInstance();
		return livingEntity == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON;
	}

	public static boolean isFirstPerson() {
		Minecraft mc = Minecraft.getInstance();
		return mc.options.getCameraType() == CameraType.FIRST_PERSON;
	}
}
