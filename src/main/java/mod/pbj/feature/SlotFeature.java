package mod.pbj.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import mod.pbj.attachment.AttachmentHost;
import mod.pbj.attachment.Attachments;
import mod.pbj.item.AttachmentItem;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class SlotFeature extends ConditionalFeature {
	public int weight = 64;
	@Nullable public List<Either<Either<Supplier<Item>, TagKey<Item>>, Class<? extends Item>>> whitelist = null;

	public final Script script;

	public SlotFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> predicate,
		int weight,
		List<Either<Either<Supplier<Item>, TagKey<Item>>, Class<? extends Item>>> whitelist,
		Script script) {
		super(owner, predicate);
		this.script = script;
		this.weight = weight;
		this.whitelist = whitelist;
	}

	public static int getWeight(
		ItemStack itemStack, List<Either<Either<Supplier<Item>, TagKey<Item>>, Class<? extends Item>>> whitelist) {
		if (itemStack.isEmpty() || itemStack.getCount() == 0)
			return 99999;
		if (whitelist == null)
			return 64 / itemStack.getCount();
		if (matches(itemStack, whitelist))
			return 64 / itemStack.getCount();

		return -1;
	}

	public static boolean
	matches(ItemStack itemStack, List<Either<Either<Supplier<Item>, TagKey<Item>>, Class<? extends Item>>> whitelist) {
		if (whitelist == null || whitelist.isEmpty())
			return true;
		for (Either<Either<Supplier<Item>, TagKey<Item>>, Class<? extends Item>> entry : whitelist) {
			if (entry.left().isPresent()) {
				Either<Supplier<Item>, TagKey<Item>> itemOrTag = entry.left().get();
				if (itemOrTag.left().isPresent()) {
					Item item = itemOrTag.left().get().get();
					if (itemStack.is(item))
						return true;
				} else {
					TagKey<Item> tag = itemOrTag.right().get();
					if (itemStack.is(tag))
						return true;
				}
			} else {
				Class<? extends Item> itemClass = entry.right().get();
				if (itemClass.isInstance(itemStack.getItem()))
					return true;
			}
		}
		return false;
	}
	public @Nullable Script getScript() {
		return script;
	}

	public static class Builder implements FeatureBuilder<Builder, SlotFeature> {
		public int maxStackSize = 64;
		public Predicate<ConditionContext> predicate = (ctx) -> true;
		public Script script = null;
		public List<Either<Either<Supplier<Item>, TagKey<Item>>, Class<? extends Item>>> whitelist;

		public Builder withMaxStackSize(int maxStackSize) {
			this.maxStackSize = maxStackSize;
			return this;
		}

		public Builder withPredicate(Predicate<ConditionContext> predicate) {
			this.predicate = predicate;
			return this;
		}

		public Builder withScript(Script script) {
			this.script = script;
			return this;
		}

		public Builder withWhitelist(JsonArray list) {
			List<Either<Either<Supplier<Item>, TagKey<Item>>, Class<? extends Item>>> whitelist = new ArrayList<>();
			for (JsonElement jsonElement : list) {
				String element = jsonElement.getAsString();
				if (element.startsWith("class")) {
					String className = element.substring(6);
					try {
						Class<? extends Item> itemClass = (Class<? extends Item>) Class.forName(className);
						whitelist.add(Either.right(itemClass));
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(
							"Class not found: " + className + " | a Pointblank Slot feature has malformed whitelist!",
							e);
					}
					continue;
				}
				String itemName = element;
				if (itemName.startsWith("#")) {
					String tagName = itemName.substring(1);
					TagKey<Item> tagKey =
						TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), new ResourceLocation(tagName));
					whitelist.add(Either.left(Either.right(tagKey)));
					continue;
				}
				whitelist.add(
					Either.left(Either.left(() -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))));
			}
			this.whitelist = whitelist;
			return this;
		}

		@Override
		public Builder withJsonObject(JsonObject var1) {
			withScript(JsonUtil.getJsonScript(var1));
			withMaxStackSize(JsonUtil.getJsonInt(var1, "weight", 64));
			if (var1.has("condition"))
				this.withPredicate(Conditions.fromJson(var1.getAsJsonObject("condition")));
			if (var1.has("whitelist"))
				this.withWhitelist(var1.getAsJsonArray("whitelist"));
			return this;
		}

		@Override
		public SlotFeature build(FeatureProvider var1) {
			return new SlotFeature(var1, predicate, maxStackSize, whitelist, script);
		}
	}

	/**
	 * SlotHolder implements bundle-like logic for any item
	 * <p>
	 * - Tag is "items"
	 * <p>
	 * - Tag type is <code>ListTag</code>
	 */
	public interface SlotHolder {
		String TAG = "Items";
		default boolean
		stack(ItemStack pStack, ItemStack pOther, ClickAction pAction, Player pPlayer, SlotAccess pAccess) {
			if (pAction == ClickAction.PRIMARY && pOther.isEmpty())
				return false;
			if (pAction == ClickAction.SECONDARY && pOther.isEmpty()) {
				removeItem(pStack, pPlayer, pAccess);
				return true;
			}
			if (pOther.isEmpty())
				return false;
			return addItem(pStack, pOther);
		}
		default boolean addItem(ItemStack pStack, ItemStack toAdd) {
			var weight = 0;
			List<Either<Either<Supplier<Item>, TagKey<Item>>, Class<? extends Item>>> whitelist = new ArrayList<>();
			if (!pStack.getOrCreateTag().contains(TAG))
				pStack.getOrCreateTag().put(TAG, new ListTag());
			if (pStack.getItem() instanceof AttachmentHost host) {
				if (host.hasFeature(SlotFeature.class)) {
					SlotFeature selfFeature = host.getFeature(SlotFeature.class);
					weight += selfFeature.weight;
					if (selfFeature.whitelist != null) {
						whitelist.addAll(selfFeature.whitelist);
					}
				}
				for (ItemStack attachment : Attachments.getAttachments(pStack)) {
					if (((AttachmentItem)attachment.getItem()).hasFeature(SlotFeature.class)) {
						var feature = ((AttachmentItem)attachment.getItem()).getFeature(SlotFeature.class);
						weight += feature.weight;
						if (feature.whitelist != null) {
							whitelist.addAll(feature.whitelist);
						}
					}
				}
				if (getTotalWeight(pStack) >= weight)
					return false;
				if (!toAdd.isEmpty() && matches(toAdd, whitelist)) {
					int countToAdd = 0;
					int stackWeight = 0;
					for (; countToAdd < toAdd.getCount(); countToAdd++) {
						stackWeight += ((int)64 / toAdd.getMaxStackSize());
						if (getTotalWeight(pStack) + stackWeight > weight) {
							stackWeight -= ((int)64 / toAdd.getMaxStackSize());
							break;
						}
					}
					add(pStack, toAdd, countToAdd);
					toAdd.shrink(countToAdd);
					return true;
				} else
					return false;
			}
			return false;
		}

		default boolean removeItem(ItemStack pStack, Player entity, SlotAccess pSlot) {
			boolean allow = false;
			getContents(pStack).findFirst().ifPresent(item -> {
				var list = pStack.getOrCreateTag().get(TAG);
				if (list instanceof ListTag listTag) {
					listTag.remove(item.save(new CompoundTag()));
				} else
					return;
				int itemWeight = getWeight(item, getWhitelist(pStack));
				// pStack.getOrCreateTag().putInt("cachedWeight", getTotalWeight(pStack) - itemWeight);
				pSlot.set(item);
			});
			return allow;
		}

		default int getTotalWeight(ItemStack pStack) {
			return getContents(pStack).mapToInt(stack -> (64 / stack.getMaxStackSize()) * stack.getCount()).sum();
		}

		/**
		 * This method ignores checks to weight and directly adds items
		 * @param pStack
		 * @param toAdd
		 * @param countToAdd
		 */
		default void add(ItemStack pStack, ItemStack toAdd, int countToAdd) {
			var tag = pStack.getOrCreateTag().get(TAG);
			if (tag instanceof ListTag listTag) {
				listTag.add(toAdd.copyWithCount(countToAdd).save(new CompoundTag()));
			}
		}

		default int getMaxWeight(ItemStack pStack) {
			var weight = 0;
			if (this instanceof AttachmentHost host) {
				if (host.hasFeature(SlotFeature.class)) {
					SlotFeature selfFeature = host.getFeature(SlotFeature.class);
					weight += selfFeature.weight;
				}
				for (ItemStack attachment : Attachments.getAttachments(pStack)) {
					if (((AttachmentItem)attachment.getItem()).hasFeature(SlotFeature.class)) {
						var feature = ((AttachmentItem)attachment.getItem()).getFeature(SlotFeature.class);
						weight += feature.weight;
					}
				}
			}
			return weight;
		}

		default Stream<ItemStack> getContents(ItemStack pStack) {
			CompoundTag compoundtag = pStack.getTag();
			if (compoundtag == null) {
				return Stream.empty();
			} else {
				ListTag listtag = compoundtag.getList("Items", 10);
				return listtag.stream().map(CompoundTag.class ::cast).map(ItemStack::of);
			}
		}

		@Nullable
		default List<Either<Either<Supplier<Item>, TagKey<Item>>, Class<? extends Item>>>
		getWhitelist(ItemStack pStack) {
			List<Either<Either<Supplier<Item>, TagKey<Item>>, Class<? extends Item>>> whitelist = new ArrayList<>();
			if (this instanceof AttachmentHost host) {
				if (host.hasFeature(SlotFeature.class)) {
					SlotFeature selfFeature = host.getFeature(SlotFeature.class);
					if (selfFeature.whitelist != null) {
						whitelist.addAll(selfFeature.whitelist);
					}
				}
				for (ItemStack attachment : Attachments.getAttachments(pStack)) {
					if (((AttachmentItem)attachment.getItem()).hasFeature(SlotFeature.class)) {
						var feature = ((AttachmentItem)attachment.getItem()).getFeature(SlotFeature.class);
						if (feature.whitelist != null) {
							whitelist.addAll(feature.whitelist);
						}
					}
				}
			}
			return whitelist;
		}
	}
}
