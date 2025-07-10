package mod.pbj.registry;

import mod.pbj.block.entity.PrinterBlockEntity;
import mod.pbj.block.entity.WorkstationBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BlockEntityRegistry {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES;
	public static final RegistryObject<BlockEntityType<WorkstationBlockEntity>> WORKSTATION_BLOCK_ENTITY;
	public static final RegistryObject<BlockEntityType<PrinterBlockEntity>> PRINTER_BLOCK_ENTITY;

	static {
		BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "pointblank");
		WORKSTATION_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
			"workstation_block_entity",
			() -> Builder.of(WorkstationBlockEntity::new, BlockRegistry.WORKSTATION.get()).build(null));
		PRINTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
			"printer_block_entity", () -> Builder.of(PrinterBlockEntity::new, BlockRegistry.PRINTER.get()).build(null));
	}
}
