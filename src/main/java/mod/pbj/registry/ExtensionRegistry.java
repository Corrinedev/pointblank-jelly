package mod.pbj.registry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cpw.mods.jarhandling.SecureJar;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import mod.pbj.InvalidExtensionComponentException;
import mod.pbj.InvalidExtensionException;
import mod.pbj.PointBlankJelly;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.compat.playeranimator.PlayerAnimationBuilder;
import mod.pbj.entity.EntityBuilder;
import mod.pbj.item.GunItem;
import mod.pbj.item.ItemBuilder;
import mod.pbj.script.ScriptParser;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.resource.DelegatingPackResources;
import net.minecraftforge.resource.PathPackResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExtensionRegistry {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private Pack extensionsPack;
	private List<Extension> extensions = new ArrayList<>();
	private final RepositorySource repositorySource = consumer -> {
		if (ExtensionRegistry.this.extensionsPack != null) {
			consumer.accept(ExtensionRegistry.this.extensionsPack);
		}
	};

	public ExtensionRegistry() {}

	public List<Extension> getExtensions() {
		return Collections.unmodifiableList(this.extensions);
	}

	public RepositorySource getRepositorySource() {
		return this.repositorySource;
	}

	public void discoverExtensions(PackType packType) {
		Path resourcePacksPath = FMLPaths.GAMEDIR.get().resolve("pointblank");
		this.extensions = scanExtensions(resourcePacksPath);
		List<PathPackResources> extensionPacks = new ArrayList<>();

		for (final Extension extension : this.extensions) {
			LOGGER.error("Adding extension: {}", extension);
			extensionPacks.add(new PathPackResources(extension.name, false, extension.path) {
				private final SecureJar secureJar;

				{
					this.secureJar = SecureJar.from(extension.path);
				}

				protected Path resolve(String... paths) {
					if (paths.length < 1) {
						throw new IllegalArgumentException("Missing path");
					} else {
						return this.secureJar.getPath(String.join("/", paths));
					}
				}

				public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
					return super.getResource(type, location);
				}

				public void listResources(
					PackType type, String namespace, String path, PackResources.ResourceOutput resourceOutput) {
					super.listResources(type, namespace, path, resourceOutput);
				}
			});
		}

		this.extensionsPack = Pack.readMetaAndCreate(
			"pointblank_resources",
			Component.literal("Pointblank Resources"),
			true,
			(id)
				-> new DelegatingPackResources(
					id,
					false,
					new PackMetadataSection(
						Component.translatable("pointblank.resources.modresources", extensionPacks.size()),
						SharedConstants.getCurrentVersion().getPackVersion(packType)),
					extensionPacks) {
				public boolean isHidden() {
					return true;
				}
			},
			packType,
			Position.BOTTOM,
			PackSource.DEFAULT);
	}

	public static void registerItemsFromExtensions(ExtensionRegistry registry, IEventBus modEventBus) {
		for (Extension extension : registry.getExtensions()) {
			List<Supplier<Item>> registeredExtItems = new ArrayList<>();
			ItemRegistry extItems = ItemRegistry.ITEMS;

			for (String soundName : extension.getSounds()) {
				Supplier<SoundEvent> registeredSound = SoundRegistry.register(soundName);
				extension.registeredExtSounds.put(soundName, registeredSound);
			}

			for (ItemBuilder<?> itemBuilder : extension.getItemBuilders()) {
				Supplier<Item> registeredExtItem = extItems.register(itemBuilder);
				registeredExtItems.add(registeredExtItem);
			}

			for (EffectBuilder<?, ?> effectBuilder : extension.getEffectBuilders()) {
				EffectRegistry.register(effectBuilder.getName(), () -> effectBuilder);
			}

			for (PlayerAnimationBuilder playerAnimationBuilder : extension.getPlayerAnimationBuilders()) {
				ThirdPersonAnimationRegistry.register(
					playerAnimationBuilder.getName(), playerAnimationBuilder.getReaderFactory());
			}

			Supplier<ItemStack> tabIconSupplier = () -> {
				Item item = null;
				if (extension.creativeTabIconItem != null) {
					Supplier<? extends Item> supplier =
						ItemRegistry.ITEMS.getItemsByName().get(extension.creativeTabIconItem);
					if (supplier != null) {
						item = supplier.get();
					}
				}

				if (item == null) {
					Optional<Supplier<Item>> firstGunItem =
						registeredExtItems.stream().filter((ro) -> ro.get() instanceof GunItem).findFirst();
					if (firstGunItem.isPresent()) {
						item = (Item)((Supplier<?>)firstGunItem.get()).get();
					} else {
						item = Items.BARRIER;
					}

					if (item == null) {
						item = AmmoRegistry.AMMOCREATIVE.get();
					}
				}

				return new ItemStack(item != null ? item : Items.AIR);
			};
			ItemRegistry.TABS.register(
				extension.getName(),
				()
					-> CreativeModeTab.builder()
						   .title(Component.translatable("itemGroup." + extension.getName() + ".items"))
						   .icon(tabIconSupplier)
						   .displayItems((enabledFeatures, entries) -> {
							   CreativeModeTab.Output output = new CreativeModeTab.Output() {
								   public void accept(
									   ItemStack itemStack, CreativeModeTab.TabVisibility tabVisibility) {
									   entries.accept(itemStack, tabVisibility);
								   }

								   public void accept(ItemLike itemLike) {
									   if (itemLike != null) {
										   entries.accept(itemLike);
									   }
								   }
							   };

							   for (Supplier<Item> registeredExtensionItem : registeredExtItems) {
								   output.accept(registeredExtensionItem.get());
							   }
						   })
						   .build());
		}
	}
	@Nullable
	public static Object getScript(ResourceLocation scriptName) {
		System.out.println("Getting Script of " + scriptName.getNamespace() + " with name " + scriptName.getPath());

		return ScriptParser.SCRIPTCACHE.get(scriptName);
	}

	private static List<Extension> scanExtensions(Path extensionsPath) {
		List<Extension> extensions = new ArrayList<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					try {
						Extension extension = ExtensionRegistry.Extension.fromPath(entry);
						if (extension != null) {
							extensions.add(extension);
						}
					} catch (Exception re) {
						throw new InvalidExtensionException(entry, re);
					}
				} else if (entry.toString().endsWith(".zip")) {
					try {
						Extension extension = ExtensionRegistry.Extension.fromZipPath(entry);
						if (extension != null) {
							extensions.add(extension);
						}
					} catch (Exception re) {
						throw new InvalidExtensionException(entry, re);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return extensions;
	}

	private static Set<String> getSoundEventNames(Path pathToJson) {
		Set<String> soundEventNames = new HashSet<>();

		try {
			try (BufferedReader reader = Files.newBufferedReader(pathToJson)) {
				JsonElement element = (new Gson()).fromJson(reader, JsonElement.class);
				if (element != null && element.isJsonObject()) {
					JsonObject jsonObject = element.getAsJsonObject();

					soundEventNames.addAll(jsonObject.keySet());
				} else {
					LOGGER.error("Content of the sounds file {} is not an object", pathToJson);
				}
			}

			return soundEventNames;
		} catch (IOException e) {
			LOGGER.error("Failed to load json file {}", pathToJson, e);
			throw new RuntimeException("Failed to load file " + pathToJson + ". Error: " + e);
		}
	}

	private static Set<String> getSoundEventNamesFromZip(ZipFile zipFile, ZipEntry entry) {
		Set<String> soundEventNames;

		try {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)))) {
				JsonObject jsonObject = (new Gson()).fromJson(reader, JsonObject.class);

				soundEventNames = new HashSet<>(jsonObject.keySet());
			}

			return soundEventNames;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static class Extension {
		private final String name;
		private Path path;
		private final String creativeTabIconItem;
		private List<ItemBuilder<?>> itemBuilders;
		private List<EffectBuilder<?, ?>> effectBuilders;
		private List<EntityBuilder<?, ?>> entityBuilders;
		private List<PlayerAnimationBuilder> playerAnimationBuilders;
		private Set<String> sounds;
		private Map<String, Supplier<SoundEvent>> registeredExtSounds;
		public Map<String, Supplier<Object>> clientScripts = new HashMap<>();

		public Extension(String name, Path path, String creativeTabIconItem) {
			this.name = name;
			this.path = path;
			this.creativeTabIconItem = creativeTabIconItem;
		}

		List<ItemBuilder<?>> getItemBuilders() {
			return Collections.unmodifiableList(this.itemBuilders);
		}

		List<EffectBuilder<?, ?>> getEffectBuilders() {
			return this.effectBuilders;
		}

		List<EntityBuilder<?, ?>> getEntityBuilders() {
			return this.entityBuilders;
		}

		Set<String> getSounds() {
			return Collections.unmodifiableSet(this.sounds);
		}

		List<PlayerAnimationBuilder> getPlayerAnimationBuilders() {
			return this.playerAnimationBuilders;
		}

		public String getName() {
			return this.name;
		}

		public String getCreativeTabIconItem() {
			return this.creativeTabIconItem;
		}

		static Extension fromPath(Path extPath) throws InvalidExtensionComponentException {
			Path extDescriptorPath = extPath.resolve("ext.json");
			if (Files.exists(extDescriptorPath) && Files.isRegularFile(extDescriptorPath)) {
				try {
					Extension var27;
					try (BufferedReader reader = Files.newBufferedReader(extDescriptorPath)) {
						Extension extension = (new Gson()).fromJson(reader, Extension.class);
						extension.clientScripts = new HashMap<>();
						extension.path = extPath;
						extension.itemBuilders = new ArrayList<>();
						extension.effectBuilders = new ArrayList<>();
						extension.entityBuilders = new ArrayList<>();
						extension.registeredExtSounds = new HashMap<>();
						extension.playerAnimationBuilders = new ArrayList<>();
						Path namespacePath = extPath.resolve("assets").resolve("pointblank");
						Path itemsPath = namespacePath.resolve("items");
						Path scriptsPath = namespacePath.resolve("scripts");
						Path clientScriptsPath = scriptsPath.resolve("client");

						if (Files.exists(scriptsPath) && Files.isDirectory(scriptsPath)) {
							try (DirectoryStream<Path> scriptFiles = Files.newDirectoryStream(scriptsPath, "*.js")) {
								for (Path scriptFile : scriptFiles) {
									if (scriptFile.getParent().endsWith("scripts")) {
										String scriptName = scriptFile.getFileName().toString().replace(".js", "");
										ScriptParser.cacheScript(
											scriptFile,
											ResourceLocation.fromNamespaceAndPath(extension.name, scriptName));
										PointBlankJelly.LOGGER.debug(
											"Loaded script: {} from extension: {}", scriptName, extension.name);
									}
								}
							}
						}

						if (Files.exists(clientScriptsPath) && Files.isDirectory(clientScriptsPath)) {
							try (
								DirectoryStream<Path> scriptFiles =
									Files.newDirectoryStream(clientScriptsPath, "*.js")) {
								for (Path scriptFile : scriptFiles) {
									String scriptName = scriptFile.getFileName().toString().replace(".js", "");
									extension.clientScripts.put(scriptName, () -> ScriptParser.getScript(scriptFile));
									PointBlankJelly.LOGGER.debug(
										"Loaded client script: {} from extension: {}", scriptName, extension.name);
								}
							}
						}

						if (Files.exists(itemsPath) && Files.isDirectory(itemsPath)) {
							try (DirectoryStream<Path> itemFiles = Files.newDirectoryStream(itemsPath, "*.json")) {
								for (Path itemFile : itemFiles) {
									extension.itemBuilders.add(ItemBuilder.fromPath(itemFile, extension));
								}
							}
						}

						Path effectsPath = namespacePath.resolve("effects");
						if (Files.exists(effectsPath) && Files.isDirectory(effectsPath)) {
							try (DirectoryStream<Path> effectFiles = Files.newDirectoryStream(effectsPath, "*.json")) {
								for (Path effectFile : effectFiles) {
									extension.effectBuilders.add(EffectBuilder.fromPath(effectFile));
								}
							}
						}

						Path soundsPath = namespacePath.resolve("sounds.json");
						if (Files.exists(soundsPath) && Files.isRegularFile(soundsPath)) {
							extension.sounds = ExtensionRegistry.getSoundEventNames(soundsPath);
						} else {
							extension.sounds = Collections.emptySet();
						}

						Path playerAnimationPath = namespacePath.resolve("animations").resolve("player");
						if (Files.exists(playerAnimationPath) && Files.isDirectory(playerAnimationPath)) {
							try (
								DirectoryStream<Path> playerAnimationFiles =
									Files.newDirectoryStream(playerAnimationPath, "*.animation.json")) {
								for (Path playerAnimationFile : playerAnimationFiles) {
									extension.playerAnimationBuilders.add(
										PlayerAnimationBuilder.fromPath(playerAnimationFile));
								}
							}
						}

						var27 = extension;
					}

					return var27;
				} catch (IOException e) {
					ExtensionRegistry.LOGGER.error("Failed to load extension from path {}. Error: {}", extPath, e);
					return null;
				}
			} else {
				return null;
			}
		}

		static Extension fromZipPath(Path zipPath) throws InvalidExtensionComponentException {
			try {
				Extension var7;
				try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
					ZipEntry extDescriptorEntry = zipFile.getEntry("ext.json");
					if (extDescriptorEntry == null) {
						return null;
					}

					try (
						BufferedReader reader =
							new BufferedReader(new InputStreamReader(zipFile.getInputStream(extDescriptorEntry)))) {
						Extension extension = (new Gson()).fromJson(reader, Extension.class);
						extension.path = zipPath;
						extension.clientScripts = new HashMap<>();
						extension.itemBuilders = new ArrayList<>();
						extension.effectBuilders = new ArrayList<>();
						extension.entityBuilders = new ArrayList<>();
						extension.registeredExtSounds = new HashMap<>();
						extension.playerAnimationBuilders = new ArrayList<>();
						Enumeration<? extends ZipEntry> entries = zipFile.entries();
						Path scriptsPath = zipPath.resolve("assets/pointblank/scripts");

						while (entries.hasMoreElements()) {
							ZipEntry entry = entries.nextElement();
							if (entry.getName().startsWith("assets/pointblank/scripts/") &&
								entry.getName().endsWith(".js")) {
								try (
									BufferedReader scriptreader =
										new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)))) {
									// extension.scripts.put(entry.getName().split("assets/pointblank/scripts/")[1],
									// ScriptParser.getScript(scriptreader));
									ScriptParser.cacheScript(
										scriptreader,
										ResourceLocation.fromNamespaceAndPath(
											extension.name, entry.getName().replace("assets/pointblank/scripts/", "")));
								}
							} else if (
								entry.getName().startsWith("assets/pointblank/scripts/client/") &&
								entry.getName().endsWith(".js")) {
								try (
									BufferedReader scriptreader =
										new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)))) {
									// extension.scripts.put(entry.getName().split("assets/pointblank/scripts/")[1],
									// ScriptParser.getScript(scriptreader)); ScriptParser.cacheScript(scriptreader,
									// ResourceLocation.fromNamespaceAndPath(extension.name,
									// entry.getName().replace("assets/pointblank/scripts/client/", "")));
									extension.clientScripts.put(
										entry.getName().replace("assets/pointblank/scripts/client/", ""),
										()
											-> ScriptParser.getScript(
												scriptreader,
												entry.getName().replace("assets/pointblank/scripts/client/", "")));
								}
							} else if (
								entry.getName().startsWith("assets/pointblank/items/") &&
								entry.getName().endsWith(".json")) {
								extension.itemBuilders.add(ItemBuilder.fromZipEntry(zipFile, entry, extension));
							} else if (
								entry.getName().startsWith("assets/pointblank/effects/") &&
								entry.getName().endsWith(".json")) {
								extension.effectBuilders.add(EffectBuilder.fromZipEntry(zipFile, entry));
							} else if (
								entry.getName().startsWith("assets/pointblank/entities/") &&
								entry.getName().endsWith(".json")) {
								extension.entityBuilders.add(EntityBuilder.fromZipEntry(zipFile, entry));
							} else if (
								entry.getName().startsWith("assets/pointblank/animations/player/") &&
								entry.getName().endsWith(".animation.json")) {
								extension.playerAnimationBuilders.add(
									PlayerAnimationBuilder.fromZipEntry(zipFile, entry));
							}
						}

						ZipEntry soundsEntry = zipFile.getEntry("assets/pointblank/sounds.json");
						if (soundsEntry != null) {
							extension.sounds = ExtensionRegistry.getSoundEventNamesFromZip(zipFile, soundsEntry);
						}

						var7 = extension;
					}
				}

				return var7;
			} catch (IOException e) {
				ExtensionRegistry.LOGGER.error("Failed to load extension from ZIP {}. Error: {}", zipPath, e);
				return null;
			}
		}

		@Override
		public String toString() {
			return "Extension{"
				+ "name='" + name + '\'' + ", path=" + path + '}';
		}
	}
}
