package mod.pbj.inventory;

import java.util.Collections;
import java.util.List;
import mod.pbj.crafting.PointBlankIngredient;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class IngredientSlot extends Slot {
	private static final int SHOW_ROLLING_INGREDIENT_DURATION_MILLIS = 800;
	private int slotIndex;
	private PointBlankIngredient ingredient;
	boolean isIngredientAvailable;

	public IngredientSlot(Container container, int slotIndex, int posX, int posY) {
		super(container, slotIndex, posX, posY);
		this.slotIndex = slotIndex;
	}

	public boolean isHighlightable() {
		return false;
	}

	public void setIngredient(PointBlankIngredient ingredient, boolean isIngredientAvailable) {
		this.ingredient = ingredient;
		this.isIngredientAvailable = isIngredientAvailable;
		List<ItemStack> itemStacks = ingredient != null ? ingredient.getItemStacks() : Collections.emptyList();
		int itemStackCount = itemStacks.size();
		ItemStack currentStack;
		if (itemStackCount == 1) {
			currentStack = (ItemStack)itemStacks.get(0);
		} else if (itemStackCount > 0) {
			int index = (int)((long)((double)System.currentTimeMillis() / (double)800.0F) % (long)itemStackCount);
			currentStack = (ItemStack)itemStacks.get(index);
		} else {
			currentStack = ItemStack.EMPTY;
		}

		this.set(currentStack);
	}

	public PointBlankIngredient getIngredient() {
		return this.ingredient;
	}

	public boolean mayPickup(Player player) {
		return false;
	}

	public boolean mayPlace(ItemStack itemStack) {
		return false;
	}

	public String toString() {
		return String.format(
			"IngredientSlot {index: %d, container: %s}", this.slotIndex, System.identityHashCode(this.container));
	}

	public boolean isIngredientAvailable() {
		return this.isIngredientAvailable;
	}
}
