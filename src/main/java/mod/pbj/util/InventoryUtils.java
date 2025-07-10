package mod.pbj.util;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import mod.pbj.client.GunClientState;
import mod.pbj.crafting.PointBlankIngredient;
import mod.pbj.item.GunItem;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventoryUtils {
	public InventoryUtils() {}

	public static Tuple<ItemStack, GunClientState>
	getItemStackByStateId(Player player, UUID gunStateId, int slotIndex) {
		ItemStack targetStack = null;
		GunClientState targetGunState = null;
		ItemStack itemStack = player.getInventory().getItem(slotIndex);
		boolean isOffhand = player.getOffhandItem() == itemStack;
		if (itemStack.getItem() instanceof GunItem) {
			GunClientState gunClientState = GunClientState.getState(player, itemStack, slotIndex, isOffhand);
			if (gunClientState != null && Objects.equals(GunItem.getItemStackId(itemStack), gunStateId)) {
				targetStack = itemStack;
				targetGunState = gunClientState;
			}
		}

		return targetStack != null ? new Tuple<>(targetStack, targetGunState) : null;
	}

	public static boolean hasIngredient(Player player, PointBlankIngredient ingredient) {
		var itemStackStream = player.getInventory().items.stream();
		Objects.requireNonNull(ingredient);
		return itemStackStream.filter(ingredient::matches)
				   .collect(Collectors.summarizingInt(ItemStack::getCount))
				   .getSum() >= (long)ingredient.getCount();
	}

	public static boolean removeItem(Player player, Predicate<ItemStack> matchingPredicate, int count) {
		Inventory inventory = player.getInventory();
		int remainingCount = count;

		for (int i = 0; i < player.getInventory().items.size(); ++i) {
			ItemStack inventoryItem = inventory.items.get(i);
			if (matchingPredicate.test(inventoryItem)) {
				int availableCount = inventoryItem.getCount();
				if (availableCount <= remainingCount) {
					remainingCount -= availableCount;
					inventory.items.set(i, ItemStack.EMPTY);
				} else {
					inventoryItem.shrink(remainingCount);
					remainingCount = 0;
				}

				((ServerPlayer)inventory.player)
					.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, i, inventory.getItem(i)));
				if (remainingCount == 0) {
					break;
				}
			}
		}

		return remainingCount <= 0;
	}

	public static int addItem(Player player, Item item, int count) {
		Inventory inventory = player.getInventory();
		ItemStack stackToAdd = new ItemStack(item, 1);
		int remainingCount = count;
		if (stackToAdd.isStackable()) {
			for (int i = 0; i < inventory.items.size(); ++i) {
				ItemStack inventoryStack = inventory.items.get(i);
				if (inventoryStack.getItem() == item && inventoryStack.getCount() < inventoryStack.getMaxStackSize()) {
					int spaceLeft = inventoryStack.getMaxStackSize() - inventoryStack.getCount();
					int itemsToAdd = Math.min(spaceLeft, remainingCount);
					inventoryStack.grow(itemsToAdd);
					((ServerPlayer)inventory.player)
						.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, i, inventory.getItem(i)));
					remainingCount -= itemsToAdd;
					if (remainingCount == 0) {
						return 0;
					}
				}
			}
		}

		for (int i = 0; i < inventory.items.size(); ++i) {
			if (inventory.getItem(i).isEmpty()) {
				if (stackToAdd.isStackable()) {
					int stackSize = Math.min(stackToAdd.getMaxStackSize(), remainingCount);
					inventory.setItem(i, new ItemStack(item, stackSize));
					((ServerPlayer)inventory.player)
						.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, i, inventory.getItem(i)));
					remainingCount -= stackSize;
				} else {
					inventory.setItem(i, stackToAdd.copy());
					((ServerPlayer)inventory.player)
						.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, i, inventory.getItem(i)));
					--remainingCount;
				}

				if (remainingCount == 0) {
					return 0;
				}
			}
		}

		return remainingCount;
	}
}
