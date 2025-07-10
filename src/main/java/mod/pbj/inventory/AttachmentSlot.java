package mod.pbj.inventory;

import mod.pbj.Nameable;
import mod.pbj.attachment.Attachment;
import mod.pbj.attachment.Attachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AttachmentSlot extends Slot implements Activatable, HierarchicalSlot {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private AttachmentContainerMenu menu;
	private boolean isActive;
	private SimpleAttachmentContainer container;
	private int slotIndexInContainer;
	private SimpleAttachmentContainer childContainer;
	private HierarchicalSlot parentSlot;

	public AttachmentSlot(
		Player player, AttachmentContainerMenu menu, SimpleAttachmentContainer container, int index, int x, int y) {
		super(container, index, x, y);
		this.slotIndexInContainer = index;
		this.container = container;
		this.menu = menu;
	}

	public SimpleAttachmentContainer getContainer() {
		return this.container;
	}

	public boolean isActive() {
		return this.isActive;
	}

	void clear() {
		this.childContainer = null;
		this.parentSlot = null;
	}

	public String getPath() {
		String parentPath;
		String var10000;
	label16: {
		parentPath = this.getParentSlot() != null ? this.getParentSlot().getPath() : "/";
		ItemStack itemStack = this.getItem();
		if (itemStack != null) {
			Item var5 = itemStack.getItem();
			if (var5 instanceof Nameable) {
				Nameable n = (Nameable)var5;
				var10000 = n.getName();
				break label16;
			}
		}

		var10000 = "?";
	}

		String name = var10000;
		return parentPath + "/" + name;
	}

	public HierarchicalSlot getParentSlot() {
		return this.parentSlot;
	}

	public void setParentSlot(HierarchicalSlot parentSlot) {
		this.parentSlot = parentSlot;
	}

	void setChildContainer(SimpleAttachmentContainer childContainer) {
		this.childContainer = childContainer;
	}

	public SimpleAttachmentContainer getChildContainer() {
		return this.childContainer;
	}

	public boolean mayPlace(ItemStack newAttachmentStack) {
		if (!this.isActive) {
			return false;
		} else if (!(newAttachmentStack.getItem() instanceof Attachment)) {
			return false;
		} else {
			ItemStack currentItemStack = this.getItem();
			if (currentItemStack != null && currentItemStack.getItem() instanceof Attachment) {
				return false;
			} else {
				VirtualInventory e = this.container.getVirtualInventory();
				return e != null ? e.mayPlace(newAttachmentStack, this) : false;
			}
		}
	}

	public int getMaxStackSize() {
		return 1;
	}

	public boolean mayPickup(Player player) {
		if (!this.isActive) {
			return false;
		} else {
			ItemStack currentItemStack = this.getItem();
			if (currentItemStack != null && !currentItemStack.isEmpty() && !player.isCreative()) {
				return !(currentItemStack.getItem() instanceof Attachment)
					? true
					: Attachments.isRemoveable(currentItemStack.getTag());
			} else {
				return true;
			}
		}
	}

	public void setActive(boolean isActive) {
		if (isActive != this.isActive) {
			LOGGER.debug(
				"Changing status for slot {} in container {} to {},",
				this.slotIndexInContainer,
				this.container,
				isActive);
		}

		this.isActive = isActive;
	}

	public void setByPlayer(ItemStack itemStack) {
		this.doSet(itemStack);
	}

	private void doSet(ItemStack itemStack) {
		LOGGER.debug(
			"Setting attachment slot {} for container {} to stack {} with tag {}",
			this.slotIndexInContainer,
			this.container,
			itemStack,
			itemStack.getTag());
		super.set(itemStack);
	}

	public void set(ItemStack itemStack) {
		this.doSet(itemStack);
	}

	public void setChanged() {}
}
