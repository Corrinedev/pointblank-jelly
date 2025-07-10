package mod.pbj.item;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import mod.pbj.util.Tradeable;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WeaponManufacturingLicenseItem extends Item implements GeoItem, Tradeable {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public WeaponManufacturingLicenseItem(String resourceName) {
		super(new Properties());
	}

	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	public void registerControllers(ControllerRegistrar registry) {}

	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private GeoItemRenderer<WeaponManufacturingLicenseItem> renderer = null;

			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (this.renderer == null) {
					this.renderer =
						new GeoItemRenderer<>(new DefaultedBlockGeoModel<>(new ResourceLocation("pointblank", "wml")));
				}

				return this.renderer;
			}
		});
	}

	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {}

	public float getPrice() {
		return 100.0F;
	}

	public int getTradeLevel() {
		return 1;
	}

	public static class Builder extends ItemBuilder<Builder> {
		private String name;

		public Builder withJsonObject(JsonObject obj) {
			this.withName(obj.getAsJsonPrimitive("name").getAsString());
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public String getName() {
			return this.name;
		}

		public Item build() {
			return new WeaponManufacturingLicenseItem(this.name);
		}
	}
}
