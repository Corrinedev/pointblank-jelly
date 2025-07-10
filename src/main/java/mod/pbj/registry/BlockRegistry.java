package mod.pbj.registry;

import mod.pbj.block.PrinterBlock;
import mod.pbj.block.WorkstationBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockRegistry {
	public static final DeferredRegister<Block> BLOCKS;
	public static final RegistryObject<Block> WORKSTATION;
	public static final RegistryObject<Block> PRINTER;

	static {
		BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "pointblank");
		WORKSTATION = BLOCKS.register("workstation", WorkstationBlock::new);
		PRINTER = BLOCKS.register("printer", PrinterBlock::new);
	}
}
