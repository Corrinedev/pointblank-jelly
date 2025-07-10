package mod.pbj.registry;

import java.util.function.Consumer;
import java.util.function.Supplier;
import mod.pbj.client.render.DefaultModelRenderer;
import mod.pbj.item.MiscItem;
import mod.pbj.item.PrinterItem;
import mod.pbj.item.WorkstationItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;

public class MiscItemRegistry {
	public static final Supplier<Item> PROCESSOR;
	public static final Supplier<Item> GUNINTERNALS;
	public static final Supplier<Item> MOTOR;
	public static final Supplier<Item> GUNMETAL_MESH;
	public static final Supplier<Item> GUNMETAL_INGOT;
	public static final Supplier<Item> GUNMETAL_NUGGET;
	public static final Supplier<BlockItem> WORKSTATION;
	public static final Supplier<BlockItem> PRINTER;

	public static void init() {}

	public static void registerTabItems(Consumer<ItemLike> entries) {
		entries.accept(PROCESSOR.get());
		entries.accept(GUNINTERNALS.get());
		entries.accept(MOTOR.get());
		entries.accept(GUNMETAL_MESH.get());
		entries.accept(GUNMETAL_INGOT.get());
		entries.accept(GUNMETAL_NUGGET.get());
		entries.accept(WORKSTATION.get());
		entries.accept(PRINTER.get());
	}

	static {
		PROCESSOR = ItemRegistry.ITEMS.register((new MiscItem.MiscItemBuilder())
													.withName("processor")
													.withRenderer(() -> new DefaultModelRenderer("processor"))
													.withTradePrice(100.0D, 1));
		GUNINTERNALS = ItemRegistry.ITEMS.register((new MiscItem.MiscItemBuilder())
													   .withName("guninternals")
													   .withRenderer(() -> new DefaultModelRenderer("guninternals"))
													   .withTradePrice(500.0D, 2));
		MOTOR = ItemRegistry.ITEMS.register((new MiscItem.MiscItemBuilder())
												.withName("motor")
												.withRenderer(() -> new DefaultModelRenderer("motor"))
												.withTradePrice(30000.0D, 2));
		GUNMETAL_MESH = ItemRegistry.itemRegistry.register("gunmetal_mesh", () -> new Item(new Properties()));
		GUNMETAL_INGOT = ItemRegistry.itemRegistry.register("gunmetal_ingot", () -> new Item(new Properties()));
		GUNMETAL_NUGGET = ItemRegistry.itemRegistry.register("gunmetal_nugget", () -> new Item(new Properties()));
		WORKSTATION = ItemRegistry.itemRegistry.register(
			"workstation", () -> new WorkstationItem(BlockRegistry.WORKSTATION.get(), new Properties()));
		PRINTER = ItemRegistry.itemRegistry.register(
			"printer", () -> new PrinterItem(BlockRegistry.PRINTER.get(), new Properties()));
	}
}
