package mod.pbj.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import mod.pbj.Config;
import mod.pbj.Config.AutoReload;
import mod.pbj.Config.CrosshairType;
import mod.pbj.Enableable;
import mod.pbj.Nameable;
import mod.pbj.attachment.*;
import mod.pbj.client.controller.*;
import mod.pbj.client.gui.AttachmentManagerScreen;
import mod.pbj.client.gui.CraftingScreen;
import mod.pbj.client.gui.GunItemOverlay;
import mod.pbj.client.particle.EffectParticles;
import mod.pbj.client.render.BaseModelBlockRenderer;
import mod.pbj.client.render.CrosshairRenderer;
import mod.pbj.client.render.DefaultProjectileRenderer;
import mod.pbj.client.render.RenderUtil;
import mod.pbj.compat.playeranimator.PlayerAnimatorCompat;
import mod.pbj.compat.vivecraft.VivecraftCompat;
import mod.pbj.entity.EntityBuilder;
import mod.pbj.entity.ProjectileBulletEntity;
import mod.pbj.explosion.ExplosionEvent;
import mod.pbj.feature.AimingFeature;
import mod.pbj.feature.FeatureProvider;
import mod.pbj.item.*;
import mod.pbj.network.AimingChangeRequestPacket;
import mod.pbj.network.Network;
import mod.pbj.registry.*;
import mod.pbj.util.ClientUtil;
import mod.pbj.util.HitScan;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.UpDownCounter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Quaternionf;
import org.joml.Vector2ic;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.ClientUtils;

@OnlyIn(Dist.CLIENT)
public class ClientEventHandler {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	public static final Lazy<KeyMapping> RELOAD_KEY =
		Lazy.of(() -> new KeyMapping("key.pointblack.reload", Type.KEYSYM, 82, "key.categories.pointblank"));
	public static final Lazy<KeyMapping> FIRE_MODE_KEY =
		Lazy.of(() -> new KeyMapping("key.pointblack.firemode", Type.KEYSYM, 66, "key.categories.pointblank"));
	public static final Lazy<KeyMapping> INSPECT_KEY =
		Lazy.of(() -> new KeyMapping("key.pointblack.inspect", Type.KEYSYM, 73, "key.categories.pointblank"));
	public static final Lazy<KeyMapping> ATTACHMENT_KEY =
		Lazy.of(() -> new KeyMapping("key.pointblack.attachments", Type.KEYSYM, 89, "key.categories.pointblank"));
	public static final Lazy<KeyMapping> SCOPE_SWITCH_KEY =
		Lazy.of(() -> new KeyMapping("key.pointblack.scope_switch", Type.KEYSYM, 86, "key.categories.pointblank"));
	private static final ReentrantLock mainLoopLock = new ReentrantLock();
	private final InertiaController scopeInertiaController = new InertiaController(0.06, 0.2, 0.1);
	private final InertiaController inertiaController = new InertiaController(0.01, 0.1, 1.2217305F);
	public static InertiaController reticleInertiaController = new InertiaController(0.005, 0.05, 1.0F);
	private final GunJumpAnimationController jumpController =
		new GunJumpAnimationController(0.3, 0.8, 1.3, 0.05, 2000L);
	private final ViewShakeAnimationController2 sharedViewShakeController =
		new ViewShakeAnimationController2(0.15, 0.3, 1.0F, 0.01, 500L);
	private int currentInventorySlot;
	private int previousInventorySlot;
	private boolean inventorySlotChanged;
	private boolean currentSlotHasGun;
	private boolean currentSlotHasGunChanged;
	private boolean leftMouseButtonDown = false;
	private boolean rightMouseButtonDown = false;
	private float previousPlayerXRot;
	private float previousPlayerYRot;
	private float playerDeltaXRot;
	private float playerDeltaYRot;
	private double previousPlayerPosX;
	private double previousPlayerPosY;
	private double previousPlayerPosZ;
	private double playerDeltaX;
	private double playerDeltaY;
	private double playerDeltaZ;
	private static LivingEntity currentEntityLiving;
	private final RealtimeLinearEaser bobbingValue = new RealtimeLinearEaser(200L);
	private final RealtimeLinearEaser bobbingYawValue = new RealtimeLinearEaser(200L);
	private final RealtimeLinearEaser zoomValue = new RealtimeLinearEaser(200L);
	private final RealtimeLinearEaser crossHairExp = new RealtimeLinearEaser(100L);
	private final LockableTarget lockableTarget = new LockableTarget();
	private static final PostPassEffectController postPassEffectController = new PostPassEffectController(2000L);
	private static final ResourceLocation crossHairOverlay =
		new ResourceLocation("pointblank", "textures/gui/crosshair.png");
	private final FirstPersonWalkingAnimationHandler firstPersonWalkingAnimationHandler =
		new FirstPersonWalkingAnimationHandler();

	public ClientEventHandler() {
		this.startTicker();
	}

	private void startTicker() {
		GunStateTicker gunStateTicker = new GunStateTicker(this);
		Runtime.getRuntime().addShutdownHook(new Thread(gunStateTicker::shutdown));
		gunStateTicker.start();
	}

	@SubscribeEvent
	public void onWorldLoad(LevelEvent.Load event) {
		if (event.getLevel().isClientSide()) {
			this.currentInventorySlot = -1;
		}
	}

	@SubscribeEvent
	public void onExplosion(ExplosionEvent event) {
		double distanceToPlayer = event.getLocation().distanceTo(ClientUtils.getClientPlayer().position());
		ExplosionDescriptor descriptor = event.getExplosionDescriptor();
		distanceToPlayer = Mth.clamp(distanceToPlayer, 1.0F, Double.MAX_VALUE);
		double lambda = 0.2;
		double adjustedPower = (double)descriptor.power() * Math.exp(-lambda * distanceToPlayer);
		this.sharedViewShakeController.reset(adjustedPower, 0.5F, 1.5F, 0.01);
	}

	@SubscribeEvent
	public void onRenderTick(TickEvent.RenderTickEvent event) {
		Player player = ClientUtils.getClientPlayer();
		if (event.phase == Phase.START) {
			mainLoopLock.lock();
			if (player != null) {
				GunClientState state = GunClientState.getMainHeldState();
				if (state != null) {
					ItemStack gunStack = GunItem.getMainHeldGunItemStack(player);
					Minecraft mc = Minecraft.getInstance();
					state.renderTick(player, gunStack, event.renderTickTime);
					if (!mc.options.getCameraType().isFirstPerson()) {
						return;
					}

					GunItem gunItem = state.getGunItem();
					ItemDisplayContext itemDisplayContext = ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
					this.inertiaController.onRenderTick(
						player, state, gunStack, itemDisplayContext, event.renderTickTime);
					if (gunItem.getScopeOverlay() != null && state.isAiming() && state.isFiring()) {
						this.scopeInertiaController.setDynamicModifier(0.01F);
					} else {
						this.scopeInertiaController.setDynamicModifier(1.0F);
					}

					this.scopeInertiaController.onRenderTick(
						player, state, gunStack, itemDisplayContext, event.renderTickTime);
					this.jumpController.onRenderTick(player, state, gunStack, itemDisplayContext, event.renderTickTime);
					postPassEffectController.onRenderTick(
						player, state, gunStack, itemDisplayContext, event.renderTickTime);
					this.sharedViewShakeController.onRenderTick(
						player, state, gunStack, itemDisplayContext, event.renderTickTime);
				}
			}
		} else if (event.phase == Phase.END && mainLoopLock.isLocked()) {
			mainLoopLock.unlock();
		}
	}

	public static PostPassEffectController getPostPassEffectController() {
		return postPassEffectController;
	}

	private boolean autoReloadEnabled(Player player) {
		Config.AutoReload autoReload = Config.AUTO_RELOAD.get();
		return autoReload == AutoReload.ENABLED || autoReload == AutoReload.CREATIVE && player.isCreative() ||
			autoReload == AutoReload.SURVIVAL && !player.isCreative();
	}

	private static final VivecraftCompat vivecraft = VivecraftCompat.getInstance();

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (event.phase == Phase.START) {
			mainLoopLock.lock();
		} else if (event.phase == Phase.END) {
			if (player != null) {
				this.playerDeltaX = player.getX() - this.previousPlayerPosX;
				this.playerDeltaY = player.getY() - this.previousPlayerPosY;
				this.playerDeltaZ = player.getZ() - this.previousPlayerPosZ;
				this.previousPlayerPosX = player.getX();
				this.previousPlayerPosY = player.getY();
				this.previousPlayerPosZ = player.getZ();
				this.playerDeltaXRot = player.getXRot() - this.previousPlayerXRot;
				this.playerDeltaYRot = player.getYRot() - this.previousPlayerYRot;
				this.previousPlayerXRot = player.getXRot();
				this.previousPlayerYRot = player.getYRot();
				ItemStack heldItem = player.getMainHandItem();
				int activeSlot = player.getInventory().selected;
				if (activeSlot != this.currentInventorySlot) {
					this.inventorySlotChanged = true;
					this.previousInventorySlot = this.currentInventorySlot;
					this.currentInventorySlot = activeSlot;
				} else {
					this.inventorySlotChanged = false;
				}

				boolean updatedSlotHasGun = heldItem != null && heldItem.getItem() instanceof GunItem;
				if (updatedSlotHasGun != this.currentSlotHasGun) {
					this.currentSlotHasGun = updatedSlotHasGun;
					this.currentSlotHasGunChanged = true;
				} else {
					this.currentSlotHasGunChanged = false;
				}

				boolean var10000;
			label210: {
				Item rightMouseButtonDown = heldItem.getItem();
				if (rightMouseButtonDown instanceof Enableable e) {
					if (!e.isEnabled()) {
						var10000 = false;
						break label210;
					}
				}

				var10000 = true;
			}

				boolean isEnabled = var10000;
				if (isEnabled && this.autoReloadEnabled(player) && heldItem.getItem() instanceof GunItem &&
					!this.inventorySlotChanged) {
					LazyOptional<Integer> optionalAmmo =
						GunItem.getClientSideAmmo(player, heldItem, this.currentInventorySlot);
					optionalAmmo.ifPresent((ammo) -> {
						if (ammo <= 0) {
							((GunItem)heldItem.getItem()).tryReload(player, heldItem);
						}
					});
				}

				while (isEnabled && RELOAD_KEY.get().consumeClick() && !this.inventorySlotChanged) {
					if (heldItem.getItem() instanceof GunItem) {
						((GunItem)heldItem.getItem()).tryReload(player, heldItem);
					}
				}

				while (FIRE_MODE_KEY.get().consumeClick()) {
					GunClientState state = GunClientState.getMainHeldState();
					if (state != null) {
						state.tryChangeFireMode(player, player.getMainHandItem());
					}
				}

				while (INSPECT_KEY.get().consumeClick() && !this.inventorySlotChanged) {
					GunClientState state = GunClientState.getMainHeldState();
					if (state != null) {
						state.tryInspect(player, player.getMainHandItem());
					}
				}

				while (ATTACHMENT_KEY.get().consumeClick()) {
					if (heldItem != null && heldItem.getItem() instanceof AttachmentHost) {
						Attachments.tryAttachmentMode(player, heldItem);
					}
				}

				boolean leftMouseButtonDown = mc.options.keyAttack.isDown();
				if (leftMouseButtonDown && !this.leftMouseButtonDown) {
					this.leftMouseDown();
				} else if (!leftMouseButtonDown && this.leftMouseButtonDown) {
					this.leftMouseButtonRelease();
				}

				this.leftMouseButtonDown = leftMouseButtonDown;
				boolean rightMouseButtonDown = mc.options.keyUse.isDown();
				final var vrActive = vivecraft != null && vivecraft.isVrActive();
				// maybe make this configurable if requested
				if (!vrActive)
					if (rightMouseButtonDown && !this.rightMouseButtonDown) {
						this.rightMouseButtonDown();
					} else if (!rightMouseButtonDown && this.rightMouseButtonDown) {
						this.rightMouseButtonRelease();
					}

				GunClientState state = GunClientState.getMainHeldState();

				while (SCOPE_SWITCH_KEY.get().consumeClick()) {
					if (heldItem != null && heldItem.getItem() instanceof GunItem && state != null &&
						state.isAiming()) {
						Attachments.tryNextAttachment(player, heldItem, AttachmentCategory.SCOPE, AimingFeature.class);
					}
				}

				if (heldItem.getItem() instanceof LockableTarget.TargetLocker) {
					this.lockableTarget.setLocker((LockableTarget.TargetLocker)heldItem.getItem());
				} else {
					this.lockableTarget.setLocker(null);
				}

				if (state != null && state.isAiming() && state.isIdle() &&
					this.lockableTarget.getLockTimeTicks() > 0L) {
					HitResult hitResult = HitScan.getNearestObjectInCrosshair(
						player, 0.0F, 400.0F, (b) -> false, (b) -> false, new ArrayList<>());
					if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
						Entity hitScanEntity = ((EntityHitResult)hitResult).getEntity();
						if (MiscUtil.isProtected(hitScanEntity) || !this.lockableTarget.tryLock(hitScanEntity)) {
							this.lockableTarget.unlock(hitScanEntity);
						}
					} else {
						this.lockableTarget.unlock(null);
					}
				} else {
					this.lockableTarget.unlock(null);
				}

				if (this.inventorySlotChanged) {
					this.lockableTarget.unlock(null);
					this.inertiaController.reset(player);
					this.scopeInertiaController.reset();
					reticleInertiaController.reset();
					if (this.previousInventorySlot >= 0) {
					label228: {
						ItemStack previousStack = player.getInventory().getItem(this.previousInventorySlot);
						if (previousStack != null) {
							Item gun = previousStack.getItem();
							if (gun instanceof GunItem previousGunItem) {
								GunClientState previousState =
									GunClientState.getState(player, previousStack, this.previousInventorySlot, false);
								if (previousState != null) {
									previousState.tryDeactivate(player, previousStack);
									previousState.resetTotalUninterruptedFireTime();
								}

								AnimationController<GeoAnimatable> walkingController =
									previousGunItem.getGeoAnimationController("walking", previousStack);
								if (walkingController != null) {
									walkingController.tryTriggerAnimation("animation.model.standing");
								}
								break label228;
							}
						}

						if (previousStack != null && previousStack.getItem() instanceof ThrowableItem) {
							ThrowableClientState previousState =
								ThrowableClientState.getState(player, previousStack, this.previousInventorySlot, false);
							if (previousState != null) {
								previousState.tryDeactivate(player, previousStack);
							}
						}
					}
					}

					ItemStack mainHeldItem = player.getMainHandItem();
					if (state != null) {
						state.tryDraw(player, mainHeldItem);
						if (heldItem.getItem() instanceof GunItem) {
							this.firstPersonWalkingAnimationHandler.reset(player, heldItem);
						}
					} else if (mainHeldItem.getItem() instanceof ThrowableItem) {
						ThrowableClientState throwableState = ThrowableClientState.getMainHeldState();
						if (throwableState != null) {
							throwableState.tryDraw(player, mainHeldItem);
						}
					}
				}

				if (this.inventorySlotChanged || this.currentSlotHasGunChanged) {
					this.jumpController.reset();
					// always aim in vr
					// currentSlotHasGunChanged only lasts 1 tick, which the player has probably spent still in the
					// inventory. maybe figure out a way to make it aim when it's first rendered instead of ticked
					if (vrActive && heldItem.getItem() instanceof final GunItem gunItem && gunItem.isAimingEnabled())
						this.toggleAiming(player, true);
				}

				this.leftMouseButtonDown = leftMouseButtonDown;
				this.rightMouseButtonDown = rightMouseButtonDown;

				if (player.getMainHandItem().getItem() instanceof GunItem) {
					if (GunClientState.getMainHeldState() != null && GunClientState.getMainHeldState().isFiring()) {
						GunClientState.getMainHeldState().increaseTotalUninterruptedFireTime();
					} else if (GunClientState.getMainHeldState() != null) {
						GunClientState.getMainHeldState().decreaseTotalUninterruptedFireTime();
					}
				}
			}

			mainLoopLock.unlock();
		}
	}

	@SubscribeEvent
	public void onRenderLivingEvent(RenderLivingEvent.Pre<LivingEntity, EntityModel<LivingEntity>> e) {
		currentEntityLiving = e.getEntity();
		if (e.getEntity() instanceof Player player) {
			ItemStack itemStack = player.getMainHandItem();
			int activeSlot = player.getInventory().selected;
			if (itemStack != null && itemStack.getItem() instanceof GunItem &&
				!PlayerAnimatorCompat.getInstance().isEnabled()) {
				GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
				if (Config.thirdPersonArmPoseAlwaysOn ||
					gunClientState != null && (gunClientState.isAiming() || gunClientState.isFiring())) {
					LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> r = e.getRenderer();
					EntityModel<LivingEntity> model = r.getModel();
					if (model instanceof PlayerModel<?> playerModel) {
						playerModel.rightArmPose = ArmPose.BOW_AND_ARROW;
					}
				}
			}

			Minecraft mc = Minecraft.getInstance();
			this.handlePlayerFirstPersonMovement(player, itemStack);
			if (Config.thirdPersonAnimationsEnabled) {
				PlayerAnimatorCompat.getInstance().handlePlayerThirdPersonMovement(player, mc.getPartialTick());
			} else {
				PlayerAnimatorCompat.getInstance().clearAll(player);
			}
		}
	}

	public void onRenderLivingEvent(RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>> e) {
		currentEntityLiving = null;
	}

	public static LivingEntity getCurrentEntityLiving() {
		return currentEntityLiving;
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public void onRenderGameOverlay(RenderGuiEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			if (!(mc.screen instanceof AttachmentManagerScreen)) {
				ItemStack stack = mc.player.getMainHandItem();
				GuiGraphics guiGraphics = event.getGuiGraphics();
				if (stack != null && stack.getItem() instanceof GunItem) {
					GunItemOverlay.renderGunOverlay2(guiGraphics, stack);
				}
			}
		}
	}

	@SubscribeEvent
	public void onFovUpdate(ViewportEvent.ComputeFov event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null && !minecraft.player.getMainHandItem().isEmpty() &&
			minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
			ItemStack itemStack = minecraft.player.getMainHandItem();
			if (itemStack.getItem() instanceof GunItem) {
				int activeSlot = minecraft.player.getInventory().selected;
				GunClientState gunClientState = GunClientState.getState(minecraft.player, itemStack, activeSlot, false);
				if (gunClientState != null) {
					float aimingZoom = this.zoomValue.update(AimingFeature.getZoom(itemStack));
					BiDirectionalInterpolator aimingController =
						(BiDirectionalInterpolator)gunClientState.getAnimationController("aiming");
					float zoomAdj = aimingZoom * (float)aimingController.getValue();
					event.setFOV(
						event.getFOV() - event.getFOV() * (double)zoomAdj -
						(double)((float)this.sharedViewShakeController.getRoll()));
				}
			}
		}
	}

	@SubscribeEvent
	public void onRenderHandEvent(RenderHandEvent event) {
		ItemStack heldItem = event.getItemStack();
		if (event.getHand() == InteractionHand.MAIN_HAND) {
			Item gun = heldItem.getItem();
			if (gun instanceof GunItem gunItem) {
				int slot = ClientUtils.getClientPlayer().getInventory().findSlotMatchingItem(heldItem);
				Minecraft minecraft = Minecraft.getInstance();
				GunClientState gunClientState = GunClientState.getState(minecraft.player, heldItem, slot, false);
				if (gunClientState != null) {
					if (minecraft.options.bobView().get() && minecraft.getCameraEntity() instanceof Player player) {
						PoseStack poseStack = event.getPoseStack();
						float f = player.walkDist - player.walkDistO;
						float partialTick = event.getPartialTick();
						float walkDistance = -(player.walkDist + f * partialTick);
						float bobbing = Mth.lerp(partialTick, player.oBob, player.bob);
						poseStack.mulPose(Axis.XP.rotationDegrees(
							-Math.abs(Mth.cos(walkDistance * (float)Math.PI - 0.2F) * bobbing) * 5.0F));
						poseStack.mulPose(
							Axis.ZP.rotationDegrees(-Mth.sin(walkDistance * (float)Math.PI) * bobbing * 3.0F));
						poseStack.translate(
							-Mth.sin(walkDistance * (float)Math.PI) * bobbing * 0.5F,
							Math.abs(Mth.cos(walkDistance * (float)Math.PI) * bobbing),
							0.0F);
						float randomPitch = 0.0F;
						float randomYaw = 0.0F;
						float targetBobbing = 1.0F;
						float targetYaw;
						if (player.isSprinting()) {
							targetYaw = 10.0F;
							targetBobbing = gunItem.getBobbing();
						} else if (gunClientState.isAiming()) {
							targetBobbing = gunItem.getBobbingOnAim();
							float afc = AimingFeature.getViewBobbing(heldItem);
							targetYaw = 5.0F * afc;
							targetBobbing *= afc;
						} else {
							targetBobbing = gunItem.getBobbing();
							targetYaw = 5.0F;
						}

						if (!Config.firstPersonAnimationsEnabled) {
							bobbing *= this.bobbingValue.update(targetBobbing);
							float bobbingRoll = bobbing * 10.0F * gunItem.getBobbingRollMultiplier();
							float bobbingPitch = bobbing * 5.0F;
							float bobbingYaw = bobbing * this.bobbingYawValue.update(targetYaw);
							poseStack.translate(
								-Mth.sin(walkDistance * (float)Math.PI) * bobbing * 0.5F,
								(double)Math.abs(Mth.cos(walkDistance * (float)Math.PI) * bobbing) * 0.35 +
									(double)(randomPitch * 0.01F),
								randomYaw * 0.0F);
							poseStack.mulPose(
								Axis.ZP.rotationDegrees(Mth.sin(walkDistance * (float)Math.PI) * bobbingRoll));
							poseStack.mulPose(Axis.XP.rotationDegrees(
								Math.abs(Mth.cos(walkDistance * (float)Math.PI - 0.2F)) * bobbingPitch));
							poseStack.mulPose(
								Axis.YP.rotationDegrees(Mth.sin(walkDistance * (float)Math.PI) * bobbingYaw));
						}
					}

					if (Config.firstPersonAnimationsEnabled) {
						Player player = ClientUtil.getClientPlayer();
						this.handlePlayerFirstPersonMovement(player, player.getMainHandItem());
					}

					GunRandomizingAnimationController randomizerController =
						(GunRandomizingAnimationController)gunClientState.getAnimationController("randomizer");
					if (randomizerController != null) {
						if (this.inventorySlotChanged || this.currentSlotHasGunChanged) {
							randomizerController.reset();
						}

						double posX = randomizerController.getPosX();
						double posY = randomizerController.getPosY();
						double posZ = randomizerController.getPosZ();
						double roll = randomizerController.getRoll();
						double yaw = randomizerController.getYaw();
						double pitch = randomizerController.getPitch();
						PoseStack poseStack = event.getPoseStack();
						Quaternionf q = new Quaternionf(pitch, yaw, roll, 1.0F);
						poseStack.translate(posX, posY, posZ);
						poseStack.mulPose(q);
					}

					GunRecoilAnimationController recoilController =
						(GunRecoilAnimationController)gunClientState.getAnimationController("recoil2");
					if (recoilController != null) {
						if (this.inventorySlotChanged || this.currentSlotHasGunChanged) {
							recoilController.reset();
						}

						double posX = recoilController.getPosX();
						double posY = recoilController.getPosY();
						double posZ = recoilController.getPosZ();
						double roll = recoilController.getRoll();
						double pitch = recoilController.getPitch();
						PoseStack poseStack = event.getPoseStack();
						Quaternionf q = new Quaternionf(pitch, 0.0F, roll, 1.0F);
						poseStack.translate(posX, posY, posZ);
						poseStack.mulPose(q);
					}

					if (this.jumpController != null) {
						double jumpMultiplier = gunItem.getJumpMultiplier();
						double posX = this.jumpController.getPosX() * jumpMultiplier;
						double posY = this.jumpController.getPosY() * jumpMultiplier;
						double posZ = this.jumpController.getPosZ() * jumpMultiplier;
						double roll = this.jumpController.getRoll() * jumpMultiplier;
						double pitch = this.jumpController.getPitch() * jumpMultiplier;
						double yaw = this.jumpController.getYaw() * jumpMultiplier;
						PoseStack poseStack = event.getPoseStack();
						Quaternionf q = new Quaternionf(pitch, yaw, roll, 1.0F);
						poseStack.translate(posX, posY, posZ);
						poseStack.mulPose(q);
					}

					if (this.inertiaController != null) {
						double roll = this.inertiaController.getRoll();
						double yaw = 0.0F;
						PoseStack poseStack = event.getPoseStack();
						Quaternionf q = new Quaternionf(0.0F, yaw, roll, 1.0F);
						poseStack.mulPose(q);
					}
				}
			}
		}
	}

	private void handlePlayerFirstPersonMovement(Player player, ItemStack itemStack) {
		this.firstPersonWalkingAnimationHandler.handlePlayerFirstPersonMovement(player, itemStack);
	}

	@SubscribeEvent
	public void onClickEvent(InputEvent.InteractionKeyMappingTriggered event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		ItemStack heldItem = player.getMainHandItem();
		if (GunItem.getSelectedFireModeType(heldItem) == FireMode.MELEE)
			return;

		if (heldItem.getItem() instanceof GunItem || heldItem.getItem() instanceof ThrowableItem) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onPreRenderHandEvent(mod.pbj.event.RenderHandEvent.Pre event) {
		ItemStack itemStack = GunItem.getMainHeldGunItemStack(ClientUtils.getClientPlayer());
		if (itemStack != null) {
			Item item = itemStack.getItem();
			if (item instanceof Nameable gunItem) {
				BakedGeoModel model = AttachmentModelInfo.getModel(gunItem.getName());
				if (model != null) {
					GeoBone cameraBone = model.getBone("_camera_").orElse(null);
					if (cameraBone != null) {
						PoseStack poseStack = event.getPoseStack();
						if (cameraBone.getRotY() != 0.0F) {
							poseStack.mulPose(Axis.YP.rotation(-cameraBone.getRotY()));
						}

						if (cameraBone.getRotX() != 0.0F) {
							poseStack.mulPose(Axis.XP.rotation(-cameraBone.getRotX()));
						}

						if (cameraBone.getRotZ() != 0.0F) {
							poseStack.mulPose(Axis.ZP.rotation(-cameraBone.getRotZ()));
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.getCameraType().isFirstPerson()) {
			GunClientState state = GunClientState.getMainHeldState();
			if (state != null) {
				PryAnimationController shakeAnimationController =
					(PryAnimationController)state.getAnimationController("shake");
				if (shakeAnimationController != null) {
					float yawModifier = state.isAiming() ? 0.1F : 1.0F;
					float rollModifier = state.isAiming() ? 1.5F : 1.0F;
					event.setRoll(event.getRoll() + (float)shakeAnimationController.getRoll() * rollModifier);
					event.setYaw(event.getYaw() + (float)shakeAnimationController.getYaw() * yawModifier);
				}

				TimerController reloadTimer = (TimerController)state.getAnimationController("reloadTimer");
				if (reloadTimer != null && !reloadTimer.isDone()) {
					for (AbstractProceduralAnimationController activeHandler : reloadTimer.getActiveHandlers(
							 ClientUtils.getClientPlayer(), state, ClientUtils.getClientPlayer().getMainHandItem())) {
						event.setRoll(event.getRoll() + (float)activeHandler.getRoll());
						event.setPitch(event.getPitch() + (float)activeHandler.getPitch());
					}
				}

				if (this.sharedViewShakeController != null) {
					event.setRoll(event.getRoll() + (float)this.sharedViewShakeController.getRoll() * 0.5F);
				}

				if (this.inertiaController != null) {
					double roll = this.inertiaController.getYaw();
					double yaw = this.inertiaController.getYaw();
					event.setRoll(event.getRoll() + (float)roll * 15.0F);
					event.setYaw(event.getYaw() + (float)yaw * 15.0F);
				}
			}

			ItemStack itemStack = ClientUtil.getClientPlayer().getMainHandItem();
			Item var15 = itemStack.getItem();
			if (var15 instanceof Nameable gunItem) {
				BakedGeoModel model = AttachmentModelInfo.getModel(gunItem.getName());
				if (model != null) {
					GeoBone cameraBone = model.getBone("_camera_").orElse(null);
					if (cameraBone != null) {
						event.setPitch(event.getPitch() - cameraBone.getRotX() * (180F / (float)Math.PI));
						event.setYaw(event.getYaw() - cameraBone.getRotY() * (180F / (float)Math.PI));
						event.setRoll(event.getRoll() - cameraBone.getRotZ() * (180F / (float)Math.PI));
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			ItemStack itemStack = minecraft.player.getMainHandItem();
			if (itemStack.getItem() instanceof GunItem) {
				if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() && minecraft.screen instanceof
																				 AttachmentManagerScreen) {
					event.setCanceled(true);
				} else if (minecraft.options.getCameraType().isFirstPerson()) {
					int activeSlot = minecraft.player.getInventory().selected;
					GunClientState gunClientState =
						GunClientState.getState(minecraft.player, itemStack, activeSlot, false);
					if (gunClientState != null) {
						GunItem item = (GunItem)itemStack.getItem();
						ResourceLocation scopeOverlay = item.getScopeOverlay();
						if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
							final var shouldRender = Config.crosshairType == CrosshairType.DEFAULT &&
													 !gunClientState.isAiming() &&
													 (gunClientState.isFiring() || gunClientState.isIdle()) &&
													 (vivecraft == null || !vivecraft.isVrActive());

							if (shouldRender) {
								float crossHairExpansionRate =
									this.getCrosshairExpansionRatio(minecraft.player, gunClientState);
								double originalAspectRatio = 1.0F;
								int width = event.getWindow().getGuiScaledWidth();
								int height = event.getWindow().getGuiScaledHeight();
								double scaleFactor = 3.3;
								int renderWidth = (int)((double)width * scaleFactor);
								int renderHeight = (int)((double)renderWidth / originalAspectRatio);
								if (renderHeight > height) {
									renderHeight = (int)((double)height * scaleFactor);
									int var10000 = (int)((double)renderHeight * originalAspectRatio);
								}

								renderWidth = 80;
								renderHeight = 80;
								float posX = (float)(width - renderWidth) / 2.0F;
								float posY = (float)(height - renderHeight) / 2.0F;
								CrosshairRenderer.renderCrosshairOverlay3(
									event.getGuiGraphics(),
									event.getPartialTick(),
									crossHairOverlay,
									crossHairExpansionRate - 1.0F,
									posX,
									posY,
									renderWidth,
									renderHeight);
							}

							if (Config.crosshairType == CrosshairType.DEFAULT ||
								Config.crosshairType == CrosshairType.DISABLED) {
								event.setCanceled(true);
							}
						}

						if (gunClientState.isAiming()) {
							boolean pipZoomEnabled =
								MiscUtil.isGreaterThanZero(gunClientState.getGunItem().getPipScopeZoom());
							if (scopeOverlay != null && !pipZoomEnabled) {
								event.setCanceled(true);
							}

							if (!pipZoomEnabled || !Config.pipScopesEnabled) {
								BiDirectionalInterpolator aimingController =
									(BiDirectionalInterpolator)gunClientState.getAnimationController("aiming");
								double aimingProgress = aimingController.getValue();
								if (aimingController != null && scopeOverlay != null && !gunClientState.isReloading()) {
									this.renderTextureOverlay(
										item,
										gunClientState,
										event.getGuiGraphics(),
										event.getPartialTick(),
										item.getScopeOverlay(),
										event.getWindow().getGuiScaledWidth(),
										event.getWindow().getGuiScaledHeight(),
										(float)aimingProgress);
								}
							}
						}
					}
				}
			}
		}
	}

	protected void renderTextureOverlay(
		GunItem gunItem,
		GunClientState gunClientState,
		GuiGraphics guiGraphics,
		float partialTick,
		ResourceLocation textureLocation,
		int width,
		int height,
		float alpha) {
		double originalAspectRatio = 1.0F;
		double scaleFactor = 3.3;
		int renderWidth = (int)((double)width * scaleFactor);
		int renderHeight = (int)((double)renderWidth / originalAspectRatio);
		if (renderHeight > height) {
			renderHeight = (int)((double)height * scaleFactor);
			renderWidth = (int)((double)renderHeight * originalAspectRatio);
		}

		float posX = (float)(width - renderWidth) / 2.0F;
		float posY = (float)(height - renderHeight) / 2.0F;
		if (this.scopeInertiaController != null) {
			double yaw = this.scopeInertiaController.getYaw();
			double pitch = this.scopeInertiaController.getPitch();
			posX = (float)((double)posX - yaw * (double)5000.0F);
			posY = (float)((double)posY - pitch * (double)5000.0F);
		}

		GunRecoilAnimationController recoilController =
			(GunRecoilAnimationController)gunClientState.getAnimationController("recoil2");
		if (recoilController != null) {
			if (this.inventorySlotChanged || this.currentSlotHasGunChanged) {
				recoilController.reset();
			}

			double posZ1 = recoilController.getPosZ();
			posY = (float)((double)posY + posZ1 * (double)50.0F);
		}

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
		RenderUtil.blit(
			guiGraphics,
			textureLocation,
			posX,
			posY,
			-90,
			0.0F,
			0.0F,
			renderWidth,
			renderHeight,
			renderWidth,
			renderHeight);
		ResourceLocation targetLockOverlay = gunItem.getTargetLockOverlay();
		if (targetLockOverlay != null) {
			this.renderTargetLockOverlay(
				guiGraphics, partialTick, targetLockOverlay, posX, posY, renderWidth, renderHeight);
		}

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void renderTargetLockOverlay(
		GuiGraphics guiGraphics,
		float partialTick,
		ResourceLocation targetLockonOverlay,
		float posX,
		float posY,
		int renderWidth,
		int renderHeight) {
		UpDownCounter lockCounter = this.lockableTarget.getLockCounter();
		float targetLockCount =
			Mth.lerp(partialTick, (float)lockCounter.getPreviousValue(), (float)lockCounter.getCurrentValue()) /
			(float)lockCounter.getMaxValue();
		float lockProgress2 = 1.0F - targetLockCount;
		float lockRatio = 0.2F;
		int halfLockWidth = (int)((float)renderWidth * lockRatio * 0.5F);
		int halfLockHeight = (int)((float)renderHeight * lockRatio * 0.5F);
		float centerX = posX + (float)renderWidth * 0.5F;
		float centerY = posY + (float)renderHeight * 0.5F;
		float xOffset = (float)halfLockWidth * 0.4F * lockProgress2;
		float yOffset = (float)halfLockHeight * 0.4F * lockProgress2;
		float minU = 0.0F;
		float minV = 0.0F;
		float maxU = 0.5F;
		float maxV = 0.5F;
		float posXStart = centerX - (float)halfLockWidth - xOffset;
		float posXEnd = centerX - xOffset;
		float posYStart = centerY - (float)halfLockHeight - yOffset;
		float posYEnd = centerY - yOffset;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
		minU = centerX + xOffset;
		minV = centerX + (float)halfLockWidth + xOffset;
		maxU = centerY - (float)halfLockHeight - yOffset;
		maxV = centerY - yOffset;
		posXStart = 0.5F;
		posXEnd = 0.0F;
		posYStart = 1.0F;
		posYEnd = 0.5F;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, minU, minV, maxU, maxV, -90.0F, posXStart, posYStart, posXEnd, posYEnd);
		minU = centerX + xOffset;
		minV = centerX + (float)halfLockWidth + xOffset;
		maxU = centerY + yOffset;
		maxV = centerY + (float)halfLockHeight + yOffset;
		posXStart = 0.5F;
		posXEnd = 0.5F;
		posYStart = 1.0F;
		posYEnd = 1.0F;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, minU, minV, maxU, maxV, -90.0F, posXStart, posYStart, posXEnd, posYEnd);
		minU = centerX - (float)halfLockWidth - xOffset;
		minV = centerX - xOffset;
		maxU = centerY + yOffset;
		maxV = centerY + (float)halfLockHeight + yOffset;
		posXStart = 0.0F;
		posXEnd = 0.5F;
		posYStart = 0.5F;
		posYEnd = 1.0F;
		RenderUtil.blit(
			guiGraphics, targetLockonOverlay, minU, minV, maxU, maxV, -90.0F, posXStart, posYStart, posXEnd, posYEnd);
	}

	private void setTriggerOff(LocalPlayer player) {
		ItemStack heldItem = player.getMainHandItem();
		Item var5 = heldItem.getItem();
		if (var5 instanceof GunItem gunItem) {
			if (player.isSprinting()) {
				player.setSprinting(false);
			} else {
				gunItem.setTriggerOff(player, heldItem);
			}
		} else {
			var5 = heldItem.getItem();
			if (var5 instanceof ThrowableItem throwableItem) {
				throwableItem.setTriggerOff(player, heldItem);
			}
		}
	}

	private void leftMouseDown() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player != null) {
			this.tryFire(player);
		}
	}

	private void rightMouseButtonDown() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player != null) {
			MiscUtil.getMainHeldGun(player).ifPresent((item) -> {
				if (item.isAimingEnabled()) {
					this.toggleAiming(player, true);
				}
			});
		}
	}

	private void rightMouseButtonRelease() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		this.toggleAiming(player, false);
	}

	private void leftMouseButtonRelease() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		this.setTriggerOff(player);
	}

	public void tickMainHeldGun() {
		Player player = ClientUtils.getClientPlayer();
		if (player != null) {
			GunClientState state = GunClientState.getMainHeldState();
			if (state != null) {
				state.stateTick(player, player.getMainHandItem(), true);
				this.scopeInertiaController.onUpdateState(player, state);
				reticleInertiaController.onUpdateState(player, state);
				this.inertiaController.onUpdateState(player, state);
			}
		}
	}

	public static void runSyncTick(Runnable runnable) {
		mainLoopLock.lock();

		try {
			runnable.run();
		} catch (Exception e) {
			LOGGER.error("Client sync tick failed: {}", e);
		} finally {
			mainLoopLock.unlock();
		}
	}

	public static <T> T runSyncCompute(Supplier<T> resultSupplier) {
		mainLoopLock.lock();

		T result;
		try {
			result = resultSupplier.get();
		} catch (Exception e) {
			LOGGER.error("Run sync compute failed: {}", e);
			throw e;
		} finally {
			mainLoopLock.unlock();
		}

		return result;
	}

	private boolean toggleAiming(LocalPlayer player, boolean isAiming) {
		boolean toggled = false;
		ItemStack itemStack = player.getMainHandItem();
		if (itemStack != null && itemStack.getItem() instanceof GunItem) {
			int activeSlot = player.getInventory().selected;
			GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
			if (gunClientState != null) {
				toggled = gunClientState.isAiming() != isAiming;
				gunClientState.setAiming(isAiming);
				if (toggled) {
					Network.networkChannel.sendToServer(
						new AimingChangeRequestPacket(gunClientState.getId(), activeSlot, isAiming));
				}
			}
		}

		return toggled;
	}

	private boolean tryFire(LocalPlayer player) {
		boolean result = false;
		ItemStack heldItemStack = player.getMainHandItem();
		if (heldItemStack != null) {
			Item item = heldItemStack.getItem();
			if (item instanceof GunItem gunItem) {
				if (player.getOffhandItem() != heldItemStack && gunItem.isEnabled()) {
					long minTargetLockTime = gunItem.getTargetLockTimeTicks();
					if (minTargetLockTime == 0L || this.lockableTarget.getLockCounter().isAtMax()) {
						result = gunItem.tryFire(player, heldItemStack, this.lockableTarget.getTargetEntity());
					}

					return result;
				}
			}
		}

		if (heldItemStack != null) {
			Item var8 = heldItemStack.getItem();
			if (var8 instanceof ThrowableItem throwableItem) {
				if (player.getOffhandItem() != heldItemStack && throwableItem.isEnabled()) {
					result = throwableItem.tryThrow(player, heldItemStack, this.lockableTarget.getTargetEntity());
				}
			}
		}

		return result;
	}

	@SubscribeEvent
	public void onJump(LivingEvent.LivingJumpEvent event) {
		Level level = MiscUtil.getLevel(event.getEntity());
		if (level.isClientSide && event.getEntity() instanceof Player player) {
			ItemStack heldItem = player.getMainHandItem();
			if (player == ClientUtils.getClientPlayer() && heldItem.getItem() instanceof GunItem) {
				GunClientState state = GunClientState.getState(player, heldItem, this.currentInventorySlot, false);
				if (state != null) {
					state.jump(player, heldItem);
				}

				if (this.jumpController != null) {
					this.jumpController.onJumping(player, state, heldItem);
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.side == LogicalSide.CLIENT) {
			Player mainPlayer = ClientUtils.getClientPlayer();
			if (event.player != mainPlayer) {
				ItemStack itemStack = event.player.getMainHandItem();
				GunClientState state = GunClientState.getMainHeldState(event.player);
				if (state != null) {
					state.stateTick(event.player, itemStack, false);
				}
			}
		}

		ItemStack itemStack = event.player.getMainHandItem();
		if (itemStack != null && itemStack.getItem() instanceof GunItem) {
			if (GunItem.isAiming(itemStack)) {
				event.player.setSprinting(false);
			}

			if (MiscUtil.isClientSide(event.player)) {
				GunClientState state = GunClientState.getMainHeldState(event.player);
				if (state != null && !state.isIdle() && !state.isDrawing()) {
					event.player.setSprinting(false);
				}
			}
		}
	}

	@SubscribeEvent
	public void onRenderTooltip(RenderTooltipEvent.Pre event) {
		ItemStack itemStack = event.getItemStack();
		if (itemStack.getItem() instanceof AttachmentHost) {
			event.setCanceled(true);
			Minecraft mc = Minecraft.getInstance();
			GuiGraphics guiGraphics = event.getGraphics();
			List<ClientTooltipComponent> tooltipComponents = event.getComponents();
			int i = 0;
			int j = tooltipComponents.size() == 1 ? -2 : 0;

			for (ClientTooltipComponent clienttooltipcomponent : tooltipComponents) {
				int k = clienttooltipcomponent.getWidth(event.getFont());
				if (k > i) {
					i = k;
				}

				j += clienttooltipcomponent.getHeight();
			}

			Vector2ic vector2ic = event.getTooltipPositioner().positionTooltip(
				guiGraphics.guiWidth(), guiGraphics.guiHeight(), event.getX(), event.getY(), i, j);
			int l = vector2ic.x();
			int i1 = vector2ic.y();
			PoseStack poseStack = guiGraphics.pose();
			poseStack.pushPose();
			final int finalI = i;
			final int finalJ = j;
			guiGraphics.drawManaged(() -> {
				int background = 1343229968;
				int borderStart = 1342218495;
				int borderEnd = 1342197879;
				if (!(mc.screen instanceof AttachmentManagerScreen)) {
					background = -267382768;
				}

				TooltipRenderUtil.renderTooltipBackground(
					guiGraphics, l, i1, finalI, finalJ, 400, background, background, borderStart, borderEnd);
			});
			poseStack.translate(0.0F, 0.0F, 400.0F);
			int k1 = i1;

			for (int l1 = 0; l1 < tooltipComponents.size(); ++l1) {
				ClientTooltipComponent component = tooltipComponents.get(l1);
				component.renderText(event.getFont(), l, k1, poseStack.last().pose(), guiGraphics.bufferSource());
				k1 += component.getHeight() + (l1 == 0 ? 2 : 0);
			}

			k1 = i1;

			for (int k2 = 0; k2 < tooltipComponents.size(); ++k2) {
				ClientTooltipComponent component = tooltipComponents.get(k2);
				component.renderImage(event.getFont(), l, k1, guiGraphics);
				k1 += component.getHeight() + (k2 == 0 ? 2 : 0);
			}

			poseStack.popPose();
		}
	}

	@SubscribeEvent
	public void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
		ItemStack itemStack = event.getItemStack();
		Item item = itemStack.getItem();
		if (item instanceof FeatureProvider featureProvider) {
			List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
			List<Component> components = new ArrayList<>();

			if (item instanceof ArmorItem armor) {
				components.add(
					MutableComponent.create(Component.translatable("description.pointblank.armor").getContents())
						.append(String.format(" %s", armor.getDefense()))
						.withStyle(ChatFormatting.RED)
						.withStyle(ChatFormatting.ITALIC));
				if (armor.getToughness() > 0f)
					components.add(MutableComponent
									   .create(Component.translatable("description.pointblank.toughness").getContents())
									   .append(String.format(" %s", armor.getToughness()))
									   .withStyle(ChatFormatting.RED)
									   .withStyle(ChatFormatting.ITALIC));
				if (armor.getMaterial().getKnockbackResistance() > 0f)
					components.add(
						MutableComponent.create(Component.translatable("description.pointblank.kbres").getContents())
							.append(String.format(" %s", armor.getMaterial().getKnockbackResistance()))
							.withStyle(ChatFormatting.RED)
							.withStyle(ChatFormatting.ITALIC));
			}

			components.addAll(featureProvider.getDescriptionTooltipLines());
			components.addAll(featureProvider.getFeatureTooltipLines());
			if (item instanceof AttachmentHost attachmentHost) {
				Collection<Attachment> compatibleAttachments = attachmentHost.getCompatibleAttachments();
				if (!compatibleAttachments.isEmpty()) {
					if (Screen.hasShiftDown()) {
						components.addAll(attachmentHost.getCompatibleAttachmentTooltipLines(itemStack));
					} else {
						components.add(Component.empty());
						components.add(Component.translatable("message.pointblank.holdShiftForCompatibleAttachments")
										   .withStyle(ChatFormatting.ITALIC)
										   .withStyle(ChatFormatting.DARK_AQUA));
					}
				}
			}

			for (Component c : components) {
				elements.add(Either.left(c));
			}
		}
	}

	private float getCrosshairExpansionRatio(Player player, GunClientState gunClientState) {
		float inaccuracy = 2.0F;
		float baseExpansion = 1.0F;
		float speedMultiplier = 4.0F * inaccuracy;
		float rotationMultiplier = 0.05F * inaccuracy;
		float sprintMultiplier = 1.5F * inaccuracy;
		float jumpMultiplier = 1.2F * inaccuracy;
		float sneakMultiplier = 0.8F;
		float crouchMultiplier = 0.7F;
		float fireMultiplier = 2.0F * inaccuracy;
		double horizontalSpeed =
			Mth.sqrt((float)(this.playerDeltaX * this.playerDeltaX + this.playerDeltaY * this.playerDeltaY +
							 this.playerDeltaZ * this.playerDeltaZ));
		float expansionRatio = baseExpansion + (float)horizontalSpeed * speedMultiplier;
		if (player.isSprinting()) {
			expansionRatio *= sprintMultiplier;
		}

		if (!player.onGround()) {
			expansionRatio *= jumpMultiplier;
		}

		if (player.isVisuallyCrawling()) {
			expansionRatio *= sneakMultiplier;
		}

		if (player.isCrouching()) {
			expansionRatio *= crouchMultiplier;
		}

		float yawChangeRate = Math.abs(this.playerDeltaYRot);
		float pitchChangeRate = Math.abs(this.playerDeltaXRot);
		expansionRatio += yawChangeRate * rotationMultiplier;
		expansionRatio += pitchChangeRate * rotationMultiplier;
		if (gunClientState.getTotalUninterruptedShots() > 0) {
			expansionRatio *= fireMultiplier;
		}

		return Mth.clamp(this.crossHairExp.update(expansionRatio), 1.0F, 7.0F);
	}

	@EventBusSubscriber(modid = "pointblank", value = {Dist.CLIENT}, bus = Bus.MOD)
	public static class ModBusRegistration {
		public ModBusRegistration() {}

		@SubscribeEvent
		public static void registerKeybindings(RegisterKeyMappingsEvent event) {
			event.register(ClientEventHandler.RELOAD_KEY.get());
			event.register(ClientEventHandler.FIRE_MODE_KEY.get());
			event.register(ClientEventHandler.INSPECT_KEY.get());
			event.register(ClientEventHandler.ATTACHMENT_KEY.get());
			event.register(ClientEventHandler.SCOPE_SWITCH_KEY.get());
		}

		@SubscribeEvent
		public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
			event.registerBlockEntityRenderer(
				BlockEntityRegistry.WORKSTATION_BLOCK_ENTITY.get(),
				(context) -> new BaseModelBlockRenderer<>(BlockModelRegistry.WORKSTATION_BLOCK_MODEL.get()));
			event.registerBlockEntityRenderer(
				BlockEntityRegistry.PRINTER_BLOCK_ENTITY.get(),
				(context) -> new BaseModelBlockRenderer<>(BlockModelRegistry.PRINTER_BLOCK_MODEL.get()));
			event.registerEntityRenderer(ProjectileBulletEntity.TYPE, DefaultProjectileRenderer::new);

			for (Map.Entry<RegistryObject<EntityType<?>>, Supplier<EntityBuilder<?, ?>>> e :
				 EntityRegistry.getItemEntityBuilders().entrySet()) {
				Supplier<EntityBuilder<?, ?>> supplier = e.getValue();
				EntityBuilder<?, ?> builder = supplier.get();
				if (builder.hasRenderer()) {
					EntityType entityType = (e.getKey()).get();
					event.registerEntityRenderer(entityType, builder::createEntityRenderer);
				}
			}
		}

		@SubscribeEvent
		public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
			event.registerSpriteSet(
				ParticleRegistry.IMPACT_PARTICLE.get(), EffectParticles.EffectParticleProvider::new);
		}

		@SubscribeEvent
		public static void setupClient(FMLClientSetupEvent evt) {
			MenuScreens.register(MenuRegistry.ATTACHMENTS.get(), AttachmentManagerScreen::new);
			MenuScreens.register(MenuRegistry.CRAFTING.get(), CraftingScreen::new);
			PlayerAnimatorCompat.getInstance().registerAnimationTypes();
			ThirdPersonAnimationRegistry.init();
			// for (ExtensionRegistry.Extension extension : PointBlankJelly.instance.extensionRegistry.getExtensions())
			// {
			//    for (Map.Entry<String, Supplier<Script>> entry: extension.clientScripts.entrySet()) {
			//        entry.getValue().get().run();
			//    }
			// }
		}
	}
}
