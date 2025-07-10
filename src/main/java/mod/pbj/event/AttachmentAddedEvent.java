package mod.pbj.event;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

public class AttachmentAddedEvent extends Event {
	private ItemStack parentStack;
	private ItemStack attachmentStack;

	public AttachmentAddedEvent(ItemStack parentStack, ItemStack attachmentStack) {
		this.parentStack = parentStack;
		this.attachmentStack = attachmentStack;
	}

	public ItemStack getParentStack() {
		return this.parentStack;
	}

	public ItemStack getAttachmentStack() {
		return this.attachmentStack;
	}
}
