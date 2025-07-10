package mod.pbj.inventory;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleAttachmentContainer implements Container, StackedContentsCompatible {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private int containerIndex;
	private final int size;
	private final NonNullList<ItemStack> items;
	private List<ContainerListener> listeners;
	private AttachmentContainerMenu menu;
	private VirtualInventory virtualInventory;
	private SimpleAttachmentContainer parentContainer;

	public SimpleAttachmentContainer(int containerIndex, AttachmentContainerMenu menu, int size) {
		this.containerIndex = containerIndex;
		this.menu = menu;
		this.size = size;
		this.items = NonNullList.withSize(size, ItemStack.EMPTY);
	}

	public SimpleAttachmentContainer(ItemStack... itemStacks) {
		this.size = itemStacks.length;
		this.items = NonNullList.of(ItemStack.EMPTY, itemStacks);
	}

	public int getContainerIndex() {
		return this.containerIndex;
	}

	void setParentContainer(SimpleAttachmentContainer parentContainer) {
		this.parentContainer = parentContainer;
	}

	public SimpleAttachmentContainer getParentContainer() {
		return this.parentContainer;
	}

	void setVirtualInventory(VirtualInventory virtualInventory) {
		this.virtualInventory = virtualInventory;
	}

	public VirtualInventory getVirtualInventory() {
		return this.virtualInventory;
	}

	public void addListener(ContainerListener listener) {
		if (this.listeners == null) {
			this.listeners = Lists.newArrayList();
		}

		this.listeners.add(listener);
	}

	public void removeListener(ContainerListener listeners) {
		if (this.listeners != null) {
			this.listeners.remove(listeners);
		}
	}

	public void removeAllListeners() {
		this.listeners = null;
	}

	public ItemStack getItem(int index) {
		return index >= 0 && index < this.items.size() ? this.items.get(index) : ItemStack.EMPTY;
	}

	public List<ItemStack> removeAllItems() {
		List<ItemStack> list =
			this.items.stream().filter((itemStack) -> !itemStack.isEmpty()).collect(Collectors.toList());
		this.clearContent();
		return list;
	}

	public ItemStack removeItem(int p_19159_, int p_19160_) {
		ItemStack itemstack = ContainerHelper.removeItem(this.items, p_19159_, p_19160_);
		if (!itemstack.isEmpty()) {
			this.setChanged();
		}

		return itemstack;
	}

	public ItemStack removeItemType(Item item, int p_19172_) {
		ItemStack itemstack = new ItemStack(item, 0);

		for (int i = this.size - 1; i >= 0; --i) {
			ItemStack itemstack1 = this.getItem(i);
			if (itemstack1.getItem().equals(item)) {
				int j = p_19172_ - itemstack.getCount();
				ItemStack itemstack2 = itemstack1.split(j);
				itemstack.grow(itemstack2.getCount());
				if (itemstack.getCount() == p_19172_) {
					break;
				}
			}
		}

		if (!itemstack.isEmpty()) {
			this.setChanged();
		}

		return itemstack;
	}

	public ItemStack addItem(ItemStack itemStack) {
		if (itemStack.isEmpty()) {
			return ItemStack.EMPTY;
		} else {
			ItemStack itemStackCopy = itemStack.copy();
			this.moveItemToOccupiedSlotsWithSameType(itemStackCopy);
			if (itemStackCopy.isEmpty()) {
				return ItemStack.EMPTY;
			} else {
				this.moveItemToEmptySlots(itemStackCopy);
				return itemStackCopy.isEmpty() ? ItemStack.EMPTY : itemStackCopy;
			}
		}
	}

	public boolean canAddItem(ItemStack item) {
		boolean flag = false;

		for (ItemStack itemstack : this.items) {
			if (itemstack.isEmpty() ||
				ItemStack.isSameItemSameTags(itemstack, item) && itemstack.getCount() < itemstack.getMaxStackSize()) {
				flag = true;
				break;
			}
		}

		return flag;
	}

	public ItemStack removeItemNoUpdate(int itemIndex) {
		ItemStack itemstack = this.items.get(itemIndex);
		if (itemstack.isEmpty()) {
			return ItemStack.EMPTY;
		} else {
			this.items.set(itemIndex, ItemStack.EMPTY);
			return itemstack;
		}
	}

	public void setItem(int index, ItemStack itemStack) {
		LOGGER.debug(
			"Setting item {} in container {} to stack {} with tag {}", index, this, itemStack, itemStack.getTag());
		this.items.set(index, itemStack);
		if (!itemStack.isEmpty() && itemStack.getCount() > this.getMaxStackSize()) {
			itemStack.setCount(this.getMaxStackSize());
		}

		this.setChanged();
	}

	public int getContainerSize() {
		return this.size;
	}

	public boolean isEmpty() {
		for (ItemStack itemstack : this.items) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	public void setChanged() {
		if (this.listeners != null) {
			for (ContainerListener containerlistener : this.listeners) {
				containerlistener.containerChanged(this);
			}
		}
	}

	public boolean stillValid(Player p_19167_) {
		return true;
	}

	public void clearContent() {
		this.items.clear();
		this.setChanged();
	}

	public void fillStackedContents(StackedContents p_19169_) {
		for (ItemStack itemstack : this.items) {
			p_19169_.accountStack(itemstack);
		}
	}

	public String toString() {
		return String.format(
			"{Container #%d id: %d, items: %s }", this.containerIndex, System.identityHashCode(this), this.items);
	}

	private void moveItemToEmptySlots(ItemStack p_19190_) {
		for (int i = 0; i < this.size; ++i) {
			ItemStack itemstack = this.getItem(i);
			if (itemstack.isEmpty()) {
				this.setItem(i, p_19190_.copyAndClear());
				return;
			}
		}
	}

	private void moveItemToOccupiedSlotsWithSameType(ItemStack p_19192_) {
		for (int i = 0; i < this.size; ++i) {
			ItemStack itemstack = this.getItem(i);
			if (ItemStack.isSameItemSameTags(itemstack, p_19192_)) {
				this.moveItemsBetweenStacks(p_19192_, itemstack);
				if (p_19192_.isEmpty()) {
					return;
				}
			}
		}
	}

	private void moveItemsBetweenStacks(ItemStack fromStack, ItemStack toStack) {
		int i = Math.min(this.getMaxStackSize(), toStack.getMaxStackSize());
		int j = Math.min(fromStack.getCount(), i - toStack.getCount());
		if (j > 0) {
			toStack.grow(j);
			fromStack.shrink(j);
			this.setChanged();
		}
	}

	public void fromTag(ListTag tag) {
		this.clearContent();

		for (int i = 0; i < tag.size(); ++i) {
			ItemStack itemstack = ItemStack.of(tag.getCompound(i));
			if (!itemstack.isEmpty()) {
				this.addItem(itemstack);
			}
		}
	}

	public ListTag createTag() {
		ListTag listtag = new ListTag();

		for (int i = 0; i < this.getContainerSize(); ++i) {
			ItemStack itemstack = this.getItem(i);
			if (!itemstack.isEmpty()) {
				listtag.add(itemstack.save(new CompoundTag()));
			}
		}

		return listtag;
	}

	AttachmentContainerMenu getMenu() {
		return this.menu;
	}

	public static int getContainerStartIndex(SimpleAttachmentContainer[] attachmentContainers, int containerIndex) {
		int startIndex = 0;

		for (int i = 0; i < containerIndex; ++i) {
			SimpleAttachmentContainer container = attachmentContainers[i];
			startIndex += container.getContainerSize();
		}

		return startIndex;
	}
}
