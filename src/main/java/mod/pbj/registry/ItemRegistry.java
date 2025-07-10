package mod.pbj.registry;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mod.pbj.Enableable;
import mod.pbj.config.ConfigManager;
import mod.pbj.config.Configurable;
import mod.pbj.entity.EntityBuilderProvider;
import mod.pbj.item.AttachmentItem;
import mod.pbj.item.ItemBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ItemRegistry {
	private final Map<String, Supplier<? extends Item>> itemsByName = new LinkedHashMap<>();
	private final Map<String, List<Supplier<? extends Item>>> itemsByGroup = new LinkedHashMap<>();
	private final ConfigManager.Builder gunConfigBuilder = new ConfigManager.Builder();
	private final List<DeferredRegistration> deferredRegistrations = new ArrayList<>();
	public static final ItemRegistry ITEMS = new ItemRegistry();
	public static final DeferredRegister<Item> itemRegistry;
	public static final DeferredRegister<CreativeModeTab> TABS;
	public static final Supplier<CreativeModeTab> POINTBLANK_TAB;

	public ItemRegistry() {}

	public Map<String, Supplier<? extends Item>> getItemsByName() {
		return Collections.unmodifiableMap(this.itemsByName);
	}

	public List<Supplier<? extends Item>> getAttachmentsForGroup(String group) {
		List<Supplier<? extends Item>> groupAttachments = this.itemsByGroup.get(group);
		return groupAttachments != null ? Collections.unmodifiableList(groupAttachments) : Collections.emptyList();
	}

	public <I extends Item> Supplier<I> register(ItemBuilder<?> itemBuilder) {
		String name = itemBuilder.getName();
		if (itemBuilder instanceof Configurable configurable) {
			configurable.configure(this.gunConfigBuilder);
		}

		DeferredRegistration ro = new DeferredRegistration(itemBuilder);
		this.deferredRegistrations.add(ro);
		var itemNameMap = this.itemsByName;
		Objects.requireNonNull(ro);
		itemNameMap.put(name, ro::get);
		if (itemBuilder instanceof AttachmentItem.Builder attachmentBuilder) {
			for (String group : attachmentBuilder.getGroups()) {
				List<Supplier<? extends Item>> groupMembers =
					this.itemsByGroup.computeIfAbsent(group, (g) -> new ArrayList<>());
				Objects.requireNonNull(ro);
				groupMembers.add(ro::get);
			}
		}

		EntityBuilderProvider entityBuilderProvider = itemBuilder.getEntityBuilderProvider();
		if (entityBuilderProvider != null) {
			EntityRegistry.registerItemEntity(name, entityBuilderProvider::getEntityBuilder);
		}

		return () -> (I)ro.get(); // TODO: check if this is correct
	}

	public <I extends Item> Supplier<I> getDeferredRegisteredObject(String name) {
		return () -> {
			Supplier<?> registryObject = this.itemsByName.get(name);
			return registryObject != null ? (I)registryObject.get() : null;
		};
	}

	public void register(IEventBus modEventBus) {
		itemRegistry.register(modEventBus);
	}

	public void syncEnabledItems(List<Integer> itemIds) {}

	public void complete() {
		this.gunConfigBuilder.build();
		Iterator<DeferredRegistration> it = this.deferredRegistrations.iterator();

		while (it.hasNext()) {
			DeferredRegistration dr = it.next();
			var itemBuilder = dr.itemBuilder;
			if (itemBuilder instanceof Enableable e) {
				if (!e.isEnabled()) {
					it.remove();
					dr.resolve(false);
					continue;
				}
			}

			dr.resolve(true);
		}
	}

	static {
		itemRegistry = DeferredRegister.create(ForgeRegistries.ITEMS, "pointblank");
		TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "pointblank");
		MiscItemRegistry.init();
		AmmoRegistry.init();
		// AttachmentRegistry.init();
		// GunRegistry.init();
		POINTBLANK_TAB = TABS.register(
			"pointblank",
			()
				-> CreativeModeTab.builder()
					   .title(Component.translatable("itemGroup.pointblank.items"))
					   .icon(
						   ()
							   -> new ItemStack(
								   BlockRegistry.PRINTER.get() != null ? BlockRegistry.PRINTER.get() : Items.AIR))
					   .displayItems((enabledFeatures, entries) -> {
						   Consumer<ItemLike> output = itemLike -> {
							   if (itemLike != null && itemLike != Items.AIR) {
								   if (itemLike instanceof Enableable e) {
									   if (!e.isEnabled()) {
										   return;
									   }
								   }

								   entries.accept(itemLike);
							   }
						   };
						   // GunRegistry.registerTabItems(output);
						   // AttachmentRegistry.registerTabItems(output);
						   AmmoRegistry.registerTabItems(output);
						   MiscItemRegistry.registerTabItems(output);
					   })
					   .build());
	}

	private static class DeferredRegistration {
		private Supplier<Item> supplier;
		private final ItemBuilder<?> itemBuilder;

		DeferredRegistration(ItemBuilder<?> itemBuilder) {
			this.itemBuilder = itemBuilder;
		}

		void resolve(boolean register) {
			String name = this.itemBuilder.getName();
			if (register) {
				RegistryObject<Item> ro = ItemRegistry.itemRegistry.register(name, this.itemBuilder::build);
				this.supplier = () -> (Item)ro.orElse(null);
			} else {
				this.supplier = () -> null;
			}
		}

		public Item get() {
			return this.supplier.get();
		}
	}
}
