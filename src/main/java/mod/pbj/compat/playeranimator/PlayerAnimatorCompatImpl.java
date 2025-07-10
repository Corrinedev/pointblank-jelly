package mod.pbj.compat.playeranimator;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import java.util.List;
import java.util.Objects;
import mod.pbj.Config;
import mod.pbj.client.GunClientState;
import mod.pbj.item.GunItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PlayerAnimatorCompatImpl extends PlayerAnimatorCompat {
	private static final int DEFAULT_FADE_OUT_TICKS = 8;
	private final PlayerAnimationRegistry<KeyframeAnimation> animationRegistry = new PlayerAnimationRegistryImpl();
	private boolean isClearRequired;

	protected PlayerAnimatorCompatImpl() {}

	public boolean isEnabled() {
		return Config.thirdPersonAnimationsEnabled;
	}

	public void onResourceManagerReload(ResourceManager resourceManager) {
		this.isClearRequired = true;
		this.animationRegistry.reload();
	}

	public void registerAnimationTypes() {
		for (PlayerAnimationPartGroup type : PlayerAnimationPartGroup.values()) {
			PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
				type.getLayerResource(), 42 + type.ordinal(), type.getAnimationFactory());
		}
	}

	public PlayerAnimationRegistry<KeyframeAnimation> getAnimationRegistry() {
		return this.animationRegistry;
	}

	public void playAnimation(Player player, String ownerId, String fallbackOwnerId, String animationName) {
		int dotIndex = animationName.lastIndexOf(".");
		String baseAnimationName = null;
		String partName = null;
		if (dotIndex > 0) {
			baseAnimationName = animationName.substring(0, dotIndex);
			partName = animationName.substring(dotIndex + 1);
		}

		if (partName != null && !partName.isEmpty()) {
			if (baseAnimationName != null && !baseAnimationName.isEmpty()) {
				PlayerAnimationPartGroup partGroup = PlayerAnimationPartGroup.fromName(partName);
				if (partGroup != null) {
					PlayerAnimationType newAnimationType = PlayerAnimationType.fromBaseAnimationName(baseAnimationName);
					if (newAnimationType != null) {
						ModifierLayer<IAnimation> animationLayer =
							getAnimationLayer(player, partGroup.getLayerResource());
						if (animationLayer != null) {
							PlayerAnimationType currentAnimationType = null;
							String currentAnimationName = getCurrentAnimationName(animationLayer);
							if (currentAnimationName != null) {
								dotIndex = currentAnimationName.lastIndexOf(".");
								String currentBaseAnimationName =
									dotIndex > 0 ? currentAnimationName.substring(0, dotIndex) : currentAnimationName;
								currentAnimationType =
									PlayerAnimationType.fromBaseAnimationName(currentBaseAnimationName);
							}

							if (currentAnimationType != newAnimationType || newAnimationType.isLooped()) {
								if (!animationLayer.isActive() ||
									!Objects.equals(animationName, currentAnimationName)) {
									AbstractFadeModifier fadeModifier;
									if (newAnimationType == PlayerAnimationType.IDLE) {
										fadeModifier = AbstractFadeModifier.standardFadeIn(2, Ease.LINEAR);
									} else if (
										currentAnimationType != null &&
										currentAnimationType != PlayerAnimationType.IDLE) {
										fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTEXPO);
									} else {
										fadeModifier = AbstractFadeModifier.standardFadeIn(2, Ease.LINEAR);
									}

									animationLayer.replaceAnimationWithFade(
										fadeModifier,
										this.getKeyframeAnimation(
											ownerId, fallbackOwnerId, newAnimationType, partGroup));
								}
							}
						}
					}
				}
			}
		}
	}

	protected void aux(Player player) {
		ModifierLayer<IAnimation> animationLayer =
			getAnimationLayer(player, PlayerAnimationPartGroup.AUX.getLayerResource());
		if (animationLayer != null) {
			AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(8, Ease.INOUTEXPO);
			animationLayer.replaceAnimationWithFade(
				fadeModifier, new KeyframeAnimationPlayer(PlayerAnimationRegistryImpl.AUX_ANIMATION));
		}
	}

	private static String getCurrentAnimationName(ModifierLayer<IAnimation> animationLayer) {
		IAnimation animation = animationLayer.getAnimation();
		String animationName = null;
		if (animation != null && animation instanceof KeyframeAnimationPlayer kap) {
			Object var5 = kap.getData().extraData.get("name");
			if (var5 instanceof String s) {
				animationName = s;
				if (s.charAt(0) == '"') {
					animationName = s.substring(1);
				}

				if (animationName.charAt(animationName.length() - 1) == '"') {
					animationName = animationName.substring(0, animationName.length() - 1);
				}
			}
		}

		return animationName;
	}

	public void stopAnimation(Player player, PlayerAnimationPartGroup group) {
		ModifierLayer<IAnimation> animationLayer = getAnimationLayer(player, group.getLayerResource());
		if (animationLayer != null) {
			AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(3, Ease.OUTCUBIC);
			animationLayer.replaceAnimationWithFade(fadeModifier, null);
		}
	}

	private KeyframeAnimationPlayer getKeyframeAnimation(
		String ownerId, String fallbackOwnerId, PlayerAnimationType animationType, PlayerAnimationPartGroup group) {
		List<PlayerAnimation<KeyframeAnimation>> playerAnimations =
			this.animationRegistry.getAnimations(ownerId, animationType);
		KeyframeAnimation keyframeAnimation = null;

		for (PlayerAnimation<KeyframeAnimation> playerAnimation : playerAnimations) {
			if (playerAnimation.group() == group) {
				keyframeAnimation = playerAnimation.keyframeAnimation();
				break;
			}
		}

		if (keyframeAnimation == null && fallbackOwnerId != null) {
			return this.getKeyframeAnimation(fallbackOwnerId, null, animationType, group);
		} else {
			return keyframeAnimation != null ? new KeyframeAnimationPlayer(keyframeAnimation) : null;
		}
	}

	private static ModifierLayer<IAnimation> getAnimationLayer(Player player, ResourceLocation location) {
		return (ModifierLayer<IAnimation>)PlayerAnimationAccess.getPlayerAssociatedData((AbstractClientPlayer)player)
			.get(location);
	}

	public void handlePlayerThirdPersonMovement(Player player, float partialTick) {
		ItemStack itemStack = player.getMainHandItem();
		if (!this.isClearRequired && itemStack != null) {
			Item item = itemStack.getItem();
			if (item instanceof GunItem gunItem) {
				double dmx = player.getX() - player.xo;
				double dmz = player.getZ() - player.zo;
				PlayerAnimationType upperBodyState = PlayerAnimationType.IDLE;
				PlayerAnimationType lowerBodyState = PlayerAnimationType.IDLE;
				float walkingSpeed = Mth.clamp(player.walkAnimation.speed(partialTick), 0.0F, 1.0F);
				if (!player.onGround()) {
					upperBodyState = PlayerAnimationType.OFF_GROUND;
					lowerBodyState = PlayerAnimationType.OFF_GROUND;
				} else if (
					player.onGround() && (dmx != (double)0.0F || dmz != (double)0.0F) && (double)walkingSpeed > 0.01) {
					Vec3 horizontalMovement = new Vec3(dmx, 0.0F, dmz);
					Vec3 viewVector = player.getViewVector(0.0F);
					Vec3 facingDir = viewVector.normalize();
					Vec3 movementDir = horizontalMovement.normalize();
					double dotProduct = movementDir.dot(facingDir);
					if (player.isSprinting() && !player.isCrouching()) {
						lowerBodyState = PlayerAnimationType.RUNNING;
					} else if (dotProduct < -0.4) {
						if (player.isCrouching()) {
							lowerBodyState = PlayerAnimationType.CROUCH_WALKING_BACKWARDS;
						} else {
							lowerBodyState = PlayerAnimationType.WALKING_BACKWARDS;
						}
					} else if (dotProduct > 0.4) {
						if (player.isCrouching()) {
							lowerBodyState = PlayerAnimationType.CROUCH_WALKING;
						} else {
							lowerBodyState = PlayerAnimationType.WALKING;
						}
					} else {
						double crossProduct = movementDir.cross(facingDir).y;
						if (crossProduct > (double)0.0F) {
							if (player.isCrouching()) {
								lowerBodyState = PlayerAnimationType.CROUCH_WALKING_RIGHT;
							} else {
								lowerBodyState = PlayerAnimationType.WALKING_RIGHT;
							}
						} else if (player.isCrouching()) {
							lowerBodyState = PlayerAnimationType.CROUCH_WALKING_LEFT;
						} else {
							lowerBodyState = PlayerAnimationType.WALKING_LEFT;
						}
					}
				} else if (player.isCrouching()) {
					lowerBodyState = PlayerAnimationType.CROUCHING;
				}

				GunClientState state = GunClientState.getMainHeldState(player);
				if (state != null) {
					if (state.isFiring()) {
						upperBodyState = PlayerAnimationType.FIRING;
					} else if (state.isReloading()) {
						upperBodyState = PlayerAnimationType.RELOADING;
					} else if (state.isAiming()) {
						upperBodyState = PlayerAnimationType.AIMING;
					} else {
						upperBodyState = PlayerAnimationType.IDLE;
					}
				}

				String ownerId = gunItem.getName();
				String fallbackOwnerId = gunItem.getThirdPersonFallbackAnimations();
				if (fallbackOwnerId == null) {
					fallbackOwnerId = gunItem.getAnimationType().getDefaultThirdPersonAnimation();
				}

				this.playEnsemble(player, ownerId, fallbackOwnerId, List.of(upperBodyState, lowerBodyState));
				return;
			}
		}

		this.clearAll(player);
	}

	public void clearAll(Player player) {
		this.stopAnimation(player, PlayerAnimationPartGroup.ARMS);
		this.stopAnimation(player, PlayerAnimationPartGroup.LEGS);
		this.stopAnimation(player, PlayerAnimationPartGroup.TORSO);
		this.stopAnimation(player, PlayerAnimationPartGroup.HEAD);
		this.stopAnimation(player, PlayerAnimationPartGroup.BODY);
		this.stopAnimation(player, PlayerAnimationPartGroup.AUX);
		this.isClearRequired = false;
	}
}
