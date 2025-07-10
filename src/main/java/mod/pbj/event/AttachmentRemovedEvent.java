package mod.pbj.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

public class AttachmentRemovedEvent extends Event {
	private ItemStack rootStack;
	private ItemStack parentStack;
	private ItemStack attachmentStack;
	private Player player;

	public AttachmentRemovedEvent(
		Player player, ItemStack rootStack, ItemStack parentStack, ItemStack attachmentStack) {
		this.player = player;
		this.rootStack = rootStack;
		this.parentStack = parentStack;
		this.attachmentStack = attachmentStack;
	}

	public Player getPlayer() {
		return this.player;
	}

	public ItemStack getRootStack() {
		return this.rootStack;
	}

	public ItemStack getParentStack() {
		return this.parentStack;
	}

	public ItemStack getAttachmentStack() {
		return this.attachmentStack;
	}
}
