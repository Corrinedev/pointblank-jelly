package mod.pbj;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import mod.pbj.client.ClientEventHandler;
import mod.pbj.client.GunClientState;
import mod.pbj.crafting.PointBlankRecipeProvider;
import mod.pbj.entity.ItemsAndEmeraldsToItems;
import mod.pbj.event.AttachmentRemovedEvent;
import mod.pbj.feature.math.FeatureVariables;
import mod.pbj.item.AmmoItem;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import mod.pbj.item.ThrowableItem;
import mod.pbj.network.ClientBoundPlayerDataSyncPacket;
import mod.pbj.network.Network;
import mod.pbj.registry.*;
import mod.pbj.script.ScriptParser;
import mod.pbj.util.InventoryUtils;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.ServerTaskScheduler;
import mod.pbj.util.Tradeable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.GeckoLib;
import software.bernie.geckolib.core.molang.LazyVariable;
import software.bernie.geckolib.core.molang.MolangParser;

import static mod.pbj.Constants.*;

@Mod("pointblank")
public class PointBlankJelly {
	public static final Logger LOGGER = LogManager.getLogger("pointblank");
	public ExtensionRegistry extensionRegistry;
	private static final ServerTaskScheduler scheduler = new ServerTaskScheduler();
	private final Random random = new Random();
	public static PointBlankJelly instance;
	public static IEventBus modEventBus;
	public PointBlankJelly() {
		instance = this;
		modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		LOGGER.info("Loading mod {}", "pointblank");
		ModLoadingContext.get().registerConfig(Type.COMMON, Config.SPEC);
		GeckoLib.initialize();

		registerMolang();
		FeatureVariables.createGlobal();
		FeatureTypeRegistry.init();
		this.extensionRegistry = new ExtensionRegistry();
		Dist side = FMLLoader.getDist();
		this.extensionRegistry.discoverExtensions(side.isClient() ? PackType.CLIENT_RESOURCES : PackType.SERVER_DATA);
		ExtensionRegistry.registerItemsFromExtensions(this.extensionRegistry, modEventBus);
		ParticleRegistry.PARTICLES.register(modEventBus);
		SoundRegistry.SOUNDS.register(modEventBus);
		ItemRegistry.ITEMS.register(modEventBus);
		ItemRegistry.TABS.register(modEventBus);
		BlockEntityRegistry.BLOCK_ENTITY_TYPES.register(modEventBus);
		BlockRegistry.BLOCKS.register(modEventBus);
		VillagerRegistry.POI_TYPES.register(modEventBus);
		VillagerRegistry.PROFESSIONS.register(modEventBus);
		EntityRegistry.ENTITIES.register(modEventBus);
		MenuRegistry.MENU_TYPES.register(modEventBus);
		RecipeTypeRegistry.RECIPE_TYPES.register(modEventBus);
		RecipeTypeRegistry.RECIPE_SERIALIZERS.register(modEventBus);
		ItemRegistry.ITEMS.complete();

		ScriptParser.runScripts(modEventBus);
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(this::clientSetup);
		modEventBus.addListener(this::loadComplete);
		modEventBus.addListener(this::onAddPackFinder);
		modEventBus.addListener(this::onGatherData);
		MinecraftForge.EVENT_BUS.register(this);
		Network.setupNetworkChannel();
	}

	public static void registerMolang() {
		MolangParser.INSTANCE.register(new LazyVariable(AMMO, 0));
		MolangParser.INSTANCE.register(new LazyVariable(FIREMODE, 0));
		MolangParser.INSTANCE.register(new LazyVariable(FIRETICKS, 0));
		MolangParser.INSTANCE.register(new LazyVariable(TOTALSHOTS, 0));
		MolangParser.INSTANCE.register(new LazyVariable(AIMING, 0));
		MolangParser.INSTANCE.register(new LazyVariable(HEADBOB, 0));
		MolangParser.INSTANCE.register(new LazyVariable(HEADROTX, 0));
		MolangParser.INSTANCE.register(new LazyVariable(HEADROTY, 0));
		MolangParser.INSTANCE.register(new LazyVariable(CROUCHING, 0));
		MolangParser.INSTANCE.register(new LazyVariable(CRAWLING, 0));
	}

	public static <E extends Event> void registerEvent(Consumer<E> event) {
		modEventBus.addListener(event);
	}

	public static ServerTaskScheduler getTaskScheduler() {
		return scheduler;
	}

	private void commonSetup(FMLCommonSetupEvent event) {}

	private void onGatherData(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();
		generator.addProvider(event.includeServer(), new PointBlankRecipeProvider(output));
	}

	@SubscribeEvent
	public void onSwapHands(LivingSwapItemsEvent.Hands event) {
		ItemStack toOffhand = event.getItemSwappedToOffHand();
		if (toOffhand != null &&
			(toOffhand.getItem() instanceof GunItem || toOffhand.getItem() instanceof ThrowableItem)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onExplosionStart(ExplosionEvent.Start explosionStart) {}

	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) {
		MinecraftServer server = event.getServer();
		server.addTickable(scheduler);
	}

	@SubscribeEvent
	public void onServerAboutToStartEvent(ServerAboutToStartEvent event) {
		VillagerRegistry.registerStructures(event.getServer());
	}

	private void clientSetup(FMLClientSetupEvent event) {
		MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
	}

	private void onAddPackFinder(AddPackFindersEvent event) {
		event.addRepositorySource(this.extensionRegistry.getRepositorySource());
	}

	private void loadComplete(FMLLoadCompleteEvent event) {}

	@SubscribeEvent
	public void onVillagerTradesEvent(VillagerTradesEvent event) {
		if (event.getType() == VillagerRegistry.ARMS_DEALER_PROFESSION.get()) {
			for (Map.Entry<String, Supplier<? extends Item>> e : ItemRegistry.ITEMS.getItemsByName().entrySet()) {
				Item item = e.getValue().get();
				if (item instanceof Tradeable tradeableItem) {
					float price = tradeableItem.getPrice();
					int tradeLevel = tradeableItem.getTradeLevel();
					if (!Float.isNaN(price) && tradeLevel >= 1 && tradeLevel <= 5) {
						List<VillagerTrades.ItemListing> levelTrades = event.getTrades().get(tradeLevel);
						if (levelTrades != null) {
							int emeraldCount = (int)Math.round((double)price / Config.emeraldExchangeRate);
							if (emeraldCount > 0) {
								ItemsAndEmeraldsToItems trade = new ItemsAndEmeraldsToItems(
									Items.EMERALD, emeraldCount, item, tradeableItem.getBundleQuantity(), 16, 1);
								levelTrades.add(trade);
							}
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onLivingDrops(LivingDropsEvent event) {
		float dropChance = (float)Config.itemDropChance;
		if (!MiscUtil.isNearlyZero(dropChance)) {
			float randomNumber = this.random.nextFloat();
			if (event.getEntity() instanceof Monster && randomNumber < dropChance) {
				ItemStack itemStackToDrop = new ItemStack(
					MiscItemRegistry.GUNMETAL_NUGGET.get(), this.random.nextInt(1, Config.maxItemDropCount));
				ItemEntity drop = new ItemEntity(
					event.getEntity().level(),
					event.getEntity().getX(),
					event.getEntity().getY(),
					event.getEntity().getZ(),
					itemStackToDrop);
				event.getDrops().add(drop);
			}
		}
	}
	private static ItemStack currentHeldItem = ItemStack.EMPTY;
	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		// Cancel sprint
		ItemStack itemStack = event.player.getMainHandItem();
		if (itemStack != null && itemStack.getItem() instanceof GunItem && GunItem.isAiming(itemStack)) {
			event.player.setSprinting(false);
		}

		if (itemStack != null) {
			if (itemStack != currentHeldItem) {
				if (currentHeldItem.getItem() instanceof GunItem gun) {
					if (GunClientState.getState(GunItem.getItemStackId(currentHeldItem)).isReloading()) {
						gun.cancelReload(currentHeldItem);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onHarvestCheck(PlayerEvent.HarvestCheck check) {
		if (check.getTargetBlock().is(BlockRegistry.PRINTER.get())) {
			Player player = check.getEntity();
			ItemStack heldItem = player.getMainHandItem();
			if (heldItem != null && heldItem.getItem() instanceof PickaxeItem) {
				check.setCanHarvest(true);
			}
		}
	}

	@SubscribeEvent
	public void onEquipmentChangeEvent(LivingEquipmentChangeEvent event) {
		if (event.getTo() != null &&
			(event.getTo().getItem() instanceof GunItem || event.getTo().getItem() instanceof ThrowableItem)) {
			LivingEntity copyEntity = event.getEntity();
			if (copyEntity instanceof Player player) {
				if (event.getSlot() == EquipmentSlot.OFFHAND) {
					ItemStack copy = event.getTo().copy();
					player.getInventory().offhand.clear();
					Vec3 playerPos = event.getEntity().getPosition(0.0F);
					Containers.dropItemStack(
						MiscUtil.getLevel(event.getEntity()), playerPos.x, playerPos.y, playerPos.z, copy);
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getEntity();
		Level level = MiscUtil.getLevel(player);
		if (!level.isClientSide) {
			Network.networkChannel.send(
				PacketDistributor.PLAYER.with(() -> (ServerPlayer)player),
				new ClientBoundPlayerDataSyncPacket(
					player.getPersistentData(),
					ItemRegistry.ITEMS.getItemsByName()
						.values()
						.stream()
						.filter(Objects::nonNull)
						.map((v) -> BuiltInRegistries.ITEM.getId(v.get()))
						.collect(Collectors.toList())));
		}
	}

	@SubscribeEvent
	public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		Player player = event.getEntity();
		Level level = MiscUtil.getLevel(player);
		if (!level.isClientSide) {
			Network.networkChannel.send(
				PacketDistributor.PLAYER.with(() -> (ServerPlayer)player),
				new ClientBoundPlayerDataSyncPacket(
					player.getPersistentData(),
					ItemRegistry.ITEMS.getItemsByName()
						.values()
						.stream()
						.filter(Objects::nonNull)
						.map((v) -> BuiltInRegistries.ITEM.getId(v.get()))
						.collect(Collectors.toList())));
		}
	}

	@SubscribeEvent
	public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		Player player = event.getEntity();
		Level level = MiscUtil.getLevel(player);
		if (!level.isClientSide) {
			Network.networkChannel.send(
				PacketDistributor.PLAYER.with(() -> (ServerPlayer)player),
				new ClientBoundPlayerDataSyncPacket(
					player.getPersistentData(),
					ItemRegistry.ITEMS.getItemsByName()
						.values()
						.stream()
						.filter(Objects::nonNull)
						.map((v) -> BuiltInRegistries.ITEM.getId(v.get()))
						.collect(Collectors.toList())));
		}
	}

	@SubscribeEvent
	public void onAttachmentRemoved(AttachmentRemovedEvent event) {
		ItemStack rootStack = event.getRootStack();
		Item playerItem = rootStack.getItem();
		if (playerItem instanceof GunItem gunItem) {
			Player player = event.getPlayer();
			if (player != null && !MiscUtil.isClientSide(player)) {
				for (FireModeInstance fireModeInstance : gunItem.getMainFireModes()) {
					int currentFireModeAmmo = GunItem.getAmmo(rootStack, fireModeInstance);
					int maxAmmoCapacity = gunItem.getMaxAmmoCapacity(rootStack, fireModeInstance);
					int delta = Math.min(currentFireModeAmmo - maxAmmoCapacity, 1728);
					if (delta > 0) {
						List<AmmoItem> actualAmmo = fireModeInstance.getActualAmmo();
						AmmoItem ammoToRemove = null;
						if (actualAmmo.size() == 1) {
							ammoToRemove = actualAmmo.get(0);
						} else if (actualAmmo.size() > 1) {
							AmmoItem creativeAmmo = AmmoRegistry.AMMOCREATIVE.get();
							if (actualAmmo.contains(creativeAmmo) && player.isCreative()) {
								ammoToRemove = creativeAmmo;
							} else {
								ammoToRemove =
									actualAmmo.stream().filter((a) -> a != creativeAmmo).findAny().orElse(null);
							}
						}

						if (ammoToRemove != null) {
							GunItem.setAmmo(rootStack, fireModeInstance, maxAmmoCapacity);

							int dropCount;
							for (int remainingCount = InventoryUtils.addItem(player, ammoToRemove, delta);
								 remainingCount > 0;
								 remainingCount -= dropCount) {
								dropCount = Math.min(ammoToRemove.getMaxStackSize(), remainingCount);
								Containers.dropItemStack(
									MiscUtil.getLevel(event.getPlayer()),
									player.getX() + (double)1.25F,
									player.getY() + (double)1.25F,
									player.getZ(),
									new ItemStack(ammoToRemove, dropCount));
							}
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onLivingDeath(LivingDeathEvent event) {
		// Temporary fix, causes crash on dedicated servers

		// DamageSource damageSource = event.getSource();
		// if (damageSource != null) {
		//    Entity entity = damageSource.getEntity();
		//    if (entity instanceof Player player) {
		//        if (!event.getEntity().level().isClientSide) {
		//          GunClientState state = GunClientState.getMainHeldState(player);
		//          if (state != null) {
		//             LivingEntity targetEntity = event.getEntity();
		//
		//            for(Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effect :
		//            EffectRegistry.getEntityDeathEffects(targetEntity)) {
		//               Vec3 targetPos = targetEntity.getBoundingBox().getCenter();
		//               EffectLauncher.broadcast(effect, player, state, targetEntity, new SimpleHitResult(targetPos,
		//               net.minecraft.world.phys.HitResult.Type.ENTITY, Direction.DOWN, targetEntity.getId()));
		//            }
		//         }
		//      }
		//   }
		//}
	}
}
