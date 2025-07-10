package mod.pbj.registry;

import mod.pbj.inventory.AttachmentContainerMenu;
import mod.pbj.inventory.CraftingContainerMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MenuType.MenuSupplier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuRegistry {
	public static final DeferredRegister<MenuType<?>> MENU_TYPES;
	public static final RegistryObject<MenuType<CraftingContainerMenu>> CRAFTING;
	public static final RegistryObject<MenuType<AttachmentContainerMenu>> ATTACHMENTS;

	private static <T extends AbstractContainerMenu>
		RegistryObject<MenuType<T>> register(String id, MenuSupplier<T> factory) {
		return MENU_TYPES.register(id, () -> new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS));
	}

	static {
		MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "pointblank");
		CRAFTING = register("crafting", CraftingContainerMenu::new);
		ATTACHMENTS = register("attachments", AttachmentContainerMenu::new);
	}
}
