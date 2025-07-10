package mod.pbj.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SearchSlot extends Slot {
	private int slotIndex;
	private boolean isHighlightable;

	public SearchSlot(Container container, int slotIndex, int posX, int posY, boolean isHighlightable) {
		super(container, slotIndex, posX, posY);
		this.slotIndex = slotIndex;
		this.isHighlightable = isHighlightable;
	}

	public boolean isHighlightable() {
		return this.isHighlightable;
	}

	public boolean mayPickup(Player player) {
		return false;
	}

	public boolean mayPlace(ItemStack itemStack) {
		return false;
	}

	public String toString() {
		return String.format(
			"SearchSlot {index: %d, container: %s}", this.slotIndex, System.identityHashCode(this.container));
	}
}
