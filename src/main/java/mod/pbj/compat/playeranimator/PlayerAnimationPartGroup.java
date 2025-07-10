package mod.pbj.compat.playeranimator;

import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import mod.pbj.item.GunItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.util.ClientUtils;

public enum PlayerAnimationPartGroup {
	ARMS("arms", (p) -> new ModifierLayer<>()),
	LEGS("legs", (p) -> new ModifierLayer<>()),
	TORSO("torso", (p) -> new ModifierLayer<>()),
	HEAD("head", (p) -> new ModifierLayer<>()),
	BODY("body", (p) -> new ModifierLayer<>()),
	AUX("aux", (p) -> new ModifierLayer<>(null, new AdjustmentModifier((bodyPart) -> correctRotation(p, bodyPart))));

	private final String name;
	private final ResourceLocation resourceLocation;
	private final PlayerAnimationFactory playerAnimationFactory;

	private static final Set<String> ROTATED_PARTS = Set.of("head", "leftArm", "rightArm", "body");

	PlayerAnimationPartGroup(String name, PlayerAnimationFactory playerAnimationFactory) {
		this.name = name;
		this.playerAnimationFactory = playerAnimationFactory;
		this.resourceLocation = new ResourceLocation("pointblank", this.name().toLowerCase(Locale.ROOT));
	}

	String getGroupName() {
		return this.name;
	}

	ResourceLocation getLayerResource() {
		return this.resourceLocation;
	}

	public static PlayerAnimationPartGroup fromName(String name) {
		return valueOf(name.toUpperCase(Locale.ROOT));
	}

	public PlayerAnimationFactory getAnimationFactory() {
		return this.playerAnimationFactory;
	}

	private static Optional<AdjustmentModifier.PartModifier> correctRotation(Player player, String partName) {
		Minecraft mc = Minecraft.getInstance();
		Player mainPlayer = ClientUtils.getClientPlayer();

		if (player == mainPlayer && mc.options.getCameraType() != CameraType.FIRST_PERSON && mc.screen == null &&
			mainPlayer.getMainHandItem().getItem() instanceof GunItem && ROTATED_PARTS.contains(partName)) {
			float partialTicks = mc.getPartialTick();
			float pitch = player.getViewXRot(partialTicks) * ((float)Math.PI / 180F);

			// Yaw com interpolação suave
			float yawDelta =
				Mth.rotLerp(partialTicks, player.yHeadRotO - player.yBodyRotO, player.yHeadRot - player.yBodyRot);
			float yaw = -yawDelta * ((float)Math.PI / 180F); // Eixo Y invertido

			if (partName.equals("body")) {
				return Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(0.0F, yaw, 0.0F), Vec3f.ZERO));
			}

			if (partName.equals("head")) {
				return Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(pitch, 0.0F, 0.0F), Vec3f.ZERO));
			}

			if (partName.equals("leftArm") || partName.equals("rightArm")) {
				return Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(pitch, 0.0F, 0.0F), Vec3f.ZERO));
			}

			return Optional.empty();
		}

		return Optional.empty();
	}
}
