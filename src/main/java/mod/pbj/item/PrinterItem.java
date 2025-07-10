package mod.pbj.item;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PrinterItem extends BlockItem implements GeoItem {
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public PrinterItem(Block block, Properties properties) {
		super(block, properties);
	}

	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private GeoItemRenderer<PrinterItem> renderer = null;

			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (this.renderer == null) {
					this.renderer = new GeoItemRenderer<>(
						new DefaultedBlockGeoModel<>(new ResourceLocation("pointblank", "printer")));
				}

				return this.renderer;
			}
		});
	}

	public void registerControllers(ControllerRegistrar controllers) {}

	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
