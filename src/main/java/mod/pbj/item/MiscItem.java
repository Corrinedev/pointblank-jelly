package mod.pbj.item;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mod.pbj.Nameable;
import mod.pbj.client.render.DefaultModelRenderer;
import mod.pbj.crafting.Craftable;
import mod.pbj.registry.ExtensionRegistry;
import mod.pbj.util.JsonUtil;
import mod.pbj.util.TimeUnit;
import mod.pbj.util.Tradeable;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.loading.FMLLoader;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MiscItem extends Item implements Nameable, Tradeable, Craftable {
	private final String name;
	private final float tradePrice;
	private final int tradeBundleQuantity;
	private final int tradeLevel;
	private final long craftingDuration;

	public MiscItem(String name, float tradePrice, int tradeBundleQuantity, int tradeLevel, long craftingDuration) {
		super(new Item.Properties());
		this.name = name;
		this.tradePrice = tradePrice;
		this.tradeLevel = tradeLevel;
		this.tradeBundleQuantity = tradeBundleQuantity;
		this.craftingDuration = craftingDuration;
	}

	public String getName() {
		return this.name;
	}

	public float getPrice() {
		return this.tradePrice;
	}

	public int getTradeLevel() {
		return this.tradeLevel;
	}

	public int getBundleQuantity() {
		return this.tradeBundleQuantity;
	}

	public long getCraftingDuration() {
		return this.craftingDuration;
	}

	public static class MiscItemBuilder extends ItemBuilder<MiscItemBuilder> implements Nameable {
		private static final float DEFAULT_PRICE = Float.NaN;
		private static final int DEFAULT_TRADE_LEVEL = 0;
		private static final int DEFAULT_TRADE_BUNDLE_QUANTITY = 1;
		private static final int DEFAULT_CRAFTING_DURATION = 500;
		private String name;
		private Supplier<Object> rendererBuilder;
		private float tradePrice = Float.NaN;
		private int tradeBundleQuantity = 1;
		private int tradeLevel = 0;
		private long craftingDuration = 500L;

		public MiscItemBuilder(ExtensionRegistry.Extension extension) {
			this.extension = extension;
		}

		public MiscItemBuilder() {
			this.extension = new ExtensionRegistry.Extension("pointblank", Path.of("pointblank"), "pointblank");
		}

		public MiscItemBuilder withName(String name) {
			this.name = name;
			return this;
		}

		public MiscItemBuilder withRenderer(Supplier<Object> rendererBuilder) {
			this.rendererBuilder = rendererBuilder;
			return this;
		}

		public MiscItemBuilder withTradePrice(double price, int tradeBundleQuantity, int tradeLevel) {
			this.tradePrice = (float)price;
			this.tradeLevel = tradeLevel;
			this.tradeBundleQuantity = tradeBundleQuantity;
			return this;
		}

		public MiscItemBuilder withTradePrice(double price, int tradeLevel) {
			return this.withTradePrice(price, 1, tradeLevel);
		}

		public MiscItemBuilder withCraftingDuration(int duration, TimeUnit timeUnit) {
			this.craftingDuration = timeUnit.toMillis(duration);
			return this;
		}

		public MiscItemBuilder withJsonObject(JsonObject obj) {
			this.withName(JsonUtil.getJsonString(obj, "name"));
			this.withTradePrice(
				JsonUtil.getJsonFloat(obj, "tradePrice", Float.NaN),
				JsonUtil.getJsonInt(obj, "traceBundleQuantity", 1),
				JsonUtil.getJsonInt(obj, "tradeLevel", 0));
			this.withCraftingDuration(JsonUtil.getJsonInt(obj, "craftingDuration", 500), TimeUnit.MILLISECOND);
			JsonObject rendererObj = obj.getAsJsonObject("renderer");
			Dist side = FMLLoader.getDist();
			if (side.isClient() && rendererObj != null) {
				String rendererType = JsonUtil.getJsonString(rendererObj, "type");
				if (rendererType.equalsIgnoreCase("model")) {
					this.withRenderer(() -> new DefaultModelRenderer(this.name));
				}
			}

			return this;
		}

		public String getName() {
			return this.name;
		}

		public Item build() {
			return this.rendererBuilder != null
				? new MiscModelItem(
					  this.name,
					  this.rendererBuilder,
					  this.tradePrice,
					  this.tradeBundleQuantity,
					  this.tradeLevel,
					  this.craftingDuration)
				: new MiscItem(
					  this.name, this.tradePrice, this.tradeBundleQuantity, this.tradeLevel, this.craftingDuration);
		}
	}

	private static class MiscModelItem extends MiscItem implements GeoItem {
		private final Supplier<Object> rendererSupplier;
		private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

		public MiscModelItem(
			String name,
			Supplier<Object> rendererSupplier,
			float tradePrice,
			int tradeBundleQuantity,
			int tradeLevel,
			long craftingDuration) {
			super(name, tradePrice, tradeBundleQuantity, tradeLevel, craftingDuration);
			this.rendererSupplier = rendererSupplier;
		}

		public void registerControllers(AnimatableManager.ControllerRegistrar registry) {}

		public AnimatableInstanceCache getAnimatableInstanceCache() {
			return this.cache;
		}

		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				private BlockEntityWithoutLevelRenderer renderer;

				public BlockEntityWithoutLevelRenderer getCustomRenderer() {
					if (this.renderer == null) {
						this.renderer = (BlockEntityWithoutLevelRenderer)MiscModelItem.this.rendererSupplier.get();
					}

					return this.renderer;
				}
			});
		}
	}
}
