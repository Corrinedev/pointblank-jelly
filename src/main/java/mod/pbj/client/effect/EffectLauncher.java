package mod.pbj.client.effect;

import mod.pbj.Config;
import mod.pbj.client.GunClientState;
import mod.pbj.client.GunStateListener;
import mod.pbj.client.GunStatePoseProvider;
import mod.pbj.client.PoseProvider;
import mod.pbj.client.PositionProvider;
import mod.pbj.client.VertexConsumers;
import mod.pbj.client.GunStatePoseProvider.PoseContext;
import mod.pbj.feature.Features;
import mod.pbj.item.GunItem;
import mod.pbj.item.GunItem.FirePhase;
import mod.pbj.network.EffectBroadcastPacket;
import mod.pbj.network.Network;
import mod.pbj.registry.EffectRegistry;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.SimpleHitResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.util.ClientUtils;

public class EffectLauncher implements GunStateListener {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static final int MAX_DISTANCE_SQR = 22500;
   private final Map<GunItem.FirePhase, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>> effectBuilders;

   public static Vec3 tracerTranslation = Vec3.ZERO;
   public static Vec3 tracerRotation = Vec3.ZERO;

   public EffectLauncher(Map<GunItem.FirePhase, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>> effectBuilders) {
      this.effectBuilders = effectBuilders;
   }

   public void onPrepareFiring(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
      this.applyPhaseEffects(FirePhase.PREPARING, player, gunClientState, itemStack, null, 0.0F, false);
   }

   public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
      this.applyPhaseEffects(FirePhase.FIRING, player, state, itemStack, null, 0.0F, false);
   }

   public void onCompleteFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
      this.applyPhaseEffects(FirePhase.COMPLETETING, player, state, itemStack, null, 0.0F, false);
   }

   public void onHitScanTargetAcquired(LivingEntity player, GunClientState state, ItemStack itemStack, HitResult hitResult) {
      this.applyPhaseEffects(FirePhase.HIT_SCAN_ACQUIRED, player, state, itemStack, hitResult, 0.0F, false);
   }

   public void onHitScanTargetConfirmed(LivingEntity player, GunClientState state, ItemStack itemStack, HitResult hitResult, float damage) {
      this.applyPhaseEffects(FirePhase.HIT_TARGET, player, state, itemStack, hitResult, damage, false);
   }

   private static List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> getGlobalEffectBuilders(GunItem.FirePhase phase, LivingEntity player, GunClientState gunClientState, ItemStack itemStack, HitResult hitResult, boolean thirdPersonOnly) {
      if (phase == FirePhase.HIT_TARGET && hitResult.getType() == Type.ENTITY && hitResult instanceof SimpleHitResult simpleHitResult) {
         int entityId = simpleHitResult.getEntityId();
         if (entityId > 0) {
            Entity entity = ClientUtils.getLevel().getEntity(entityId);
            if (entity instanceof LivingEntity) {
               return EffectRegistry.getEntityHitEffects(entity);
            }
         }
      }

      return Collections.emptyList();
   }

   @OnlyIn(Dist.CLIENT)
   private void applyPhaseEffects(GunItem.FirePhase phase, LivingEntity player, GunClientState gunClientState, ItemStack itemStack, HitResult hitResult, float damage, boolean thirdPersonOnly) {
      if (Config.particleEffectsEnabled) {
         Item item = itemStack.getItem();
         if (item instanceof GunItem) {
            List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> mainPhaseEffectBuilders = this.effectBuilders.computeIfAbsent(phase, (k) -> new ArrayList<>());
            List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> featurePhaseEffectBuilders = Features.getEnabledPhaseEffects(itemStack, phase);
            List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> phaseEffectBuilders = new ArrayList<>(mainPhaseEffectBuilders);
            phaseEffectBuilders.addAll(featurePhaseEffectBuilders);
            phaseEffectBuilders.addAll(getGlobalEffectBuilders(phase, player, gunClientState, itemStack, hitResult, thirdPersonOnly));
            if (!phaseEffectBuilders.isEmpty()) {
               Minecraft mc = Minecraft.getInstance();
               float maxDistance = Math.min((float)(mc.options.getEffectiveRenderDistance() * 16), 200.0F);
               if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                  thirdPersonOnly = true;
               }

               float distanceToTarget = 0.0F;
               if (hitResult != null) {
                  distanceToTarget = Mth.clamp((float)Math.sqrt(hitResult.distanceTo(player)) - 0.5F, 0.0F, maxDistance);
               } else {
                  distanceToTarget = maxDistance;
               }

               GunStatePoseProvider gunStatePoseProvider = GunStatePoseProvider.getInstance();
               Vec3 startPosition = null;
               if (!thirdPersonOnly) {
                  startPosition = gunStatePoseProvider.getPosition(gunClientState, PoseContext.FIRST_PERSON_MUZZLE);
                  if (startPosition == null) {
                     startPosition = gunStatePoseProvider.getPosition(gunClientState, PoseContext.FIRST_PERSON_MUZZLE_FLASH);
                  }

                  if (startPosition == null) {
                     startPosition = gunStatePoseProvider.getPosition(gunClientState, PoseContext.THIRD_PERSON_MUZZLE);
                  }
               }

               if (startPosition == null) {
                  startPosition = gunStatePoseProvider.getPosition(gunClientState, PoseContext.THIRD_PERSON_MUZZLE_FLASH);
               }

               for(Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier : phaseEffectBuilders) {
                  EffectBuilder<? extends EffectBuilder<?, ?>, ?> builder = supplier.get();
                  if (!builder.getCompatiblePhases().contains(phase)) {
                     throw new IllegalStateException("Effect builder " + builder + " is not compatible with phase '" + phase + "'. Check how you construct item: " + item.getName(itemStack));
                  }

                  boolean isEffectAttached = builder.isEffectAttached();
                  PoseProvider poseProvider = null;
                  PositionProvider positionProvider = null;
                  if (isEffectAttached) {
                     if (!thirdPersonOnly && gunStatePoseProvider.getPose(gunClientState, PoseContext.FIRST_PERSON_MUZZLE_FLASH) != null) {
                        poseProvider = () -> gunStatePoseProvider.getPose(gunClientState, PoseContext.FIRST_PERSON_MUZZLE_FLASH);
                     } else if (!thirdPersonOnly && gunStatePoseProvider.getPose(gunClientState, PoseContext.FIRST_PERSON_MUZZLE) != null) {
                        poseProvider = () -> gunStatePoseProvider.getPose(gunClientState, PoseContext.FIRST_PERSON_MUZZLE);
                     } else if (gunStatePoseProvider.getPose(gunClientState, PoseContext.THIRD_PERSON_MUZZLE_FLASH) != null) {
                        poseProvider = () -> gunStatePoseProvider.getPose(gunClientState, PoseContext.THIRD_PERSON_MUZZLE_FLASH);
                     } else if (gunStatePoseProvider.getPose(gunClientState, PoseContext.THIRD_PERSON_MUZZLE) != null) {
                        poseProvider = () -> gunStatePoseProvider.getPose(gunClientState, PoseContext.THIRD_PERSON_MUZZLE);
                     }

                     if (!thirdPersonOnly && gunStatePoseProvider.getPositionAndDirection(gunClientState, PoseContext.FIRST_PERSON_MUZZLE_FLASH) != null) {
                        positionProvider = () -> gunStatePoseProvider.getPositionAndDirection(gunClientState, PoseContext.FIRST_PERSON_MUZZLE_FLASH);
                     } else if (!thirdPersonOnly && gunStatePoseProvider.getPositionAndDirection(gunClientState, PoseContext.FIRST_PERSON_MUZZLE) != null) {
                        positionProvider = () -> gunStatePoseProvider.getPositionAndDirection(gunClientState, PoseContext.FIRST_PERSON_MUZZLE);
                     }
                  } else {
                     if (!thirdPersonOnly && gunStatePoseProvider.getPose(gunClientState, PoseContext.FIRST_PERSON_MUZZLE) != null) {
                        poseProvider = () -> gunStatePoseProvider.getPose(gunClientState, PoseContext.FIRST_PERSON_MUZZLE);
                     } else if (!thirdPersonOnly && gunStatePoseProvider.getPose(gunClientState, PoseContext.FIRST_PERSON_MUZZLE_FLASH) != null) {
                        poseProvider = () -> gunStatePoseProvider.getPose(gunClientState, PoseContext.FIRST_PERSON_MUZZLE_FLASH);
                     }

                     if (!thirdPersonOnly && gunStatePoseProvider.getPositionAndDirection(gunClientState, PoseContext.FIRST_PERSON_MUZZLE) != null) {
                        positionProvider = () -> gunStatePoseProvider.getPositionAndDirection(gunClientState, PoseContext.FIRST_PERSON_MUZZLE);
                     } else if (!thirdPersonOnly && gunStatePoseProvider.getPositionAndDirection(gunClientState, PoseContext.FIRST_PERSON_MUZZLE_FLASH) != null) {
                        positionProvider = () -> gunStatePoseProvider.getPositionAndDirection(gunClientState, PoseContext.FIRST_PERSON_MUZZLE_FLASH);
                     }
                  }

                  EffectBuilder.Context effectBuilderContext = (new EffectBuilder.Context()).withGunState(gunClientState).withStartPosition(startPosition).withDistance(distanceToTarget).withRandomization(0.0F).withVertexConsumerTransformer(VertexConsumers.PARTICLE).withPoseProvider(poseProvider).withPositionProvider(positionProvider).withDamage(damage).withHitResult(hitResult);
                  Effect effect = builder.build(effectBuilderContext);
				  LOGGER.debug("[EffectLauncher.applyPhaseEffects] Launching effect '{}'", effect.getName());
                  effect.launch(player);
               }

            }
         }
      }
   }

   public static void broadcast(Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier, Player sourcePlayer, GunClientState state, LivingEntity targetEntity, SimpleHitResult hitResult) {
      Level level = MiscUtil.getLevel(sourcePlayer);
      if (!level.isClientSide) {
         Vec3 targetPos = targetEntity.getBoundingBox().getCenter();

         for(ServerPlayer nearbyPlayer : ((ServerLevel)level).getPlayers((p) -> true)) {
            if (nearbyPlayer.distanceToSqr(targetPos) < (double)22500.0F) {
               Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> nearbyPlayer), new EffectBroadcastPacket(sourcePlayer.getId(), state.getId(), EffectRegistry.getEffectId(effectSupplier.get().getName()), targetPos, hitResult, false));
            }
         }

      }
   }
}
