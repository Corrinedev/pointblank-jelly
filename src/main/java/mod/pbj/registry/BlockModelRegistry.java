package mod.pbj.registry;

import java.util.function.Supplier;
import mod.pbj.block.entity.PrinterBlockEntity;
import mod.pbj.block.entity.PrinterBlockEntity.State;
import mod.pbj.block.entity.WorkstationBlockEntity;
import mod.pbj.client.model.BaseBlockModel;
import net.minecraft.resources.ResourceLocation;

public class BlockModelRegistry {
	public static Supplier<BaseBlockModel<WorkstationBlockEntity>> WORKSTATION_BLOCK_MODEL = ()
		-> (new BaseBlockModel.Builder<WorkstationBlockEntity>())
			   .withResource(new ResourceLocation("pointblank", "workstation"))
			   .build();
	public static Supplier<BaseBlockModel<PrinterBlockEntity>> PRINTER_BLOCK_MODEL = ()
		-> (new BaseBlockModel.Builder<PrinterBlockEntity>())
			   .withResource(new ResourceLocation("pointblank", "printer"))
			   .withGlow(
				   "glowy",
				   (e) -> {
					   boolean var10000;
					   if (e instanceof PrinterBlockEntity pbe) {
						   if (pbe.getState() != State.CLOSED) {
							   var10000 = true;
							   return var10000;
						   }
					   }

					   var10000 = false;
					   return var10000;
				   })
			   .withGlow(
				   "screendefault",
				   (e) -> {
					   boolean var10000;
					   if (e instanceof PrinterBlockEntity pbe) {
						   if (pbe.getState() != State.CLOSED) {
							   var10000 = true;
							   return var10000;
						   }
					   }

					   var10000 = false;
					   return var10000;
				   })
			   .withGlow(
				   "screenfinal",
				   (e) -> {
					   boolean var10000;
					   if (e instanceof PrinterBlockEntity pbe) {
						   if (pbe.getState() != State.CLOSED) {
							   var10000 = true;
							   return var10000;
						   }
					   }

					   var10000 = false;
					   return var10000;
				   })
			   .withGlow(
				   "screenfinal2",
				   (e) -> {
					   boolean var10000;
					   if (e instanceof PrinterBlockEntity pbe) {
						   if (pbe.getState() != State.CLOSED) {
							   var10000 = true;
							   return var10000;
						   }
					   }

					   var10000 = false;
					   return var10000;
				   })
			   .withGlow(
				   "screenfinal3",
				   (e) -> {
					   boolean var10000;
					   if (e instanceof PrinterBlockEntity pbe) {
						   if (pbe.getState() != State.CLOSED) {
							   var10000 = true;
							   return var10000;
						   }
					   }

					   var10000 = false;
					   return var10000;
				   })
			   .build();

	public BlockModelRegistry() {}
}
