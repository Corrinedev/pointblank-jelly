package mod.pbj.entity;

import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;

public class ItemsAndEmeraldsToItems implements VillagerTrades.ItemListing {
	private final ItemStack fromItem;
	private final int fromCount;
	private final ItemStack toItem;
	private final int toCount;
	private final int maxUses;
	private final int villagerXp;
	private final float priceMultiplier;

	public ItemsAndEmeraldsToItems(
		ItemLike fromItem, int fromCount, Item toItem, int toCount, int maxUses, int villagerXp) {
		this.fromItem = new ItemStack(fromItem);
		this.fromCount = fromCount;
		this.toItem = new ItemStack(toItem);
		this.toCount = toCount;
		this.maxUses = maxUses;
		this.villagerXp = villagerXp;
		this.priceMultiplier = 0.05F;
	}

	@Nullable
	public MerchantOffer getOffer(Entity entity, RandomSource randomSource) {
		return new MerchantOffer(
			new ItemStack(this.fromItem.getItem(), this.fromCount),
			new ItemStack(this.toItem.getItem(), this.toCount),
			this.maxUses,
			this.villagerXp,
			this.priceMultiplier);
	}
}
