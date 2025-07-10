package mod.pbj.inventory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mod.pbj.Nameable;
import mod.pbj.attachment.Attachment;
import mod.pbj.attachment.AttachmentCategory;
import mod.pbj.attachment.AttachmentHost;
import mod.pbj.attachment.Attachments;
import mod.pbj.event.AttachmentAddedEvent;
import mod.pbj.event.AttachmentRemovedEvent;
import mod.pbj.util.MiscUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VirtualInventory implements ContainerListener {
	private static final Logger logger = LogManager.getLogger("pointblank");
	protected AttachmentCategory category;
	protected ItemStack itemStack;
	protected VirtualInventory parent;
	protected Map<AttachmentCategory, VirtualInventory> elements;
	protected Player owner;

	private VirtualInventory(Player owner, AttachmentCategory category, VirtualInventory parent, ItemStack itemStack) {
		this.owner = owner;
		this.category = category;
		this.parent = parent;
		this.itemStack = itemStack != null ? itemStack : ItemStack.EMPTY;
		this.elements = new LinkedHashMap<>();
	}

	public ItemStack getItemStack() {
		return this.itemStack;
	}

	private void addElement(VirtualInventory e) {
		AttachmentCategory category = e.category;
		if (category == null) {
			logger.error("Adding an element without a category");
		}

		this.elements.put(e.category, e);
	}

	public Map<AttachmentCategory, VirtualInventory> getElements() {
		return Collections.unmodifiableMap(this.elements);
	}

	public VirtualInventory getElement(AttachmentCategory category) {
		return this.elements.get(category);
	}

	private Collection<AttachmentCategory> getCategories() {
		return this.elements.keySet();
	}

	public boolean mayPlace(ItemStack attachmentStack, AttachmentSlot attachmentSlot) {
		if (attachmentSlot.getSlotIndex() > this.elements.size()) {
			return false;
		} else if (!this.isCompatibleAttachment(attachmentStack)) {
			return false;
		} else {
			AttachmentCategory attachmentCategory = ((Attachment)attachmentStack.getItem()).getCategory();
			return !this.hasAttachmentInCategory(attachmentCategory);
		}
	}

	public boolean hasAttachments() {
		return this.elements.values().stream().anyMatch((e) -> e.itemStack != null && !e.itemStack.isEmpty());
	}

	private boolean hasAttachmentInCategory(AttachmentCategory category) {
		VirtualInventory e = this.elements.get(category);
		return e != null && e.itemStack != null && !e.itemStack.isEmpty();
	}

	private boolean isCompatibleAttachment(ItemStack attachmentStack) {
		Item var3 = attachmentStack.getItem();
		boolean var10000;
		if (var3 instanceof Attachment attachment) {
			if (((AttachmentHost)this.itemStack.getItem()).getCompatibleAttachments().contains(attachment)) {
				var10000 = true;
				return var10000;
			}
		}

		var10000 = false;
		return var10000;
	}

	public boolean mayPickup(Player player) {
		return false;
	}

	String getPath() {
		if (this.parent != null) {
			Item var2 = this.itemStack.getItem();
			if (var2 instanceof Nameable nameable) {
				String var3 = this.parent.getPath();
				return var3 + "/" + nameable.getName();
			} else {
				String var10000 = this.parent.getPath();
				return var10000 + "/" + this.itemStack.getItem();
			}
		} else {
			return MiscUtil.getItemStackId(this.itemStack).toString();
		}
	}

	private ItemStack getRootStack() {
		return this.parent != null ? this.parent.getRootStack() : this.itemStack;
	}

	public void containerChanged(Container c) {
		if (c instanceof SimpleAttachmentContainer container) {
			AttachmentContainerMenu menu = container.getMenu();
			logger.debug(
				"Virtual inventory {} handling changes in container {}, stack tags {}",
				this,
				container,
				this.itemStack.getTag());
			SlotMapping slotMapping = menu.getSlotMapping();
			Map<Integer, AttachmentCategory> stackSlotMapping = slotMapping.getStackSlotMapping(this);
			if (stackSlotMapping == null) {
				logger.warn(
					"Slot mapping not found for container {}, stack tags {}",
					System.identityHashCode(container),
					this.itemStack.getTag());
			} else {
				List<Attachment> removedItems = Attachments.removeAllAttachments(this.itemStack);

				for (int i = 1; i < container.getContainerSize(); ++i) {
					ItemStack slotStack = container.getItem(i);
					if (!slotStack.isEmpty()) {
						Item category = slotStack.getItem();
						if (category instanceof Attachment a1) {
							logger.debug(
								"Adding attachment '{}' from slot {} with tag {} to stack '{}'",
								slotStack,
								i,
								slotStack.getTag(),
								this.itemStack);
							Attachments.addAttachment(this.itemStack, slotStack, true);
							if (!removedItems.contains(a1)) {
								AttachmentAddedEvent event = new AttachmentAddedEvent(this.itemStack, slotStack);
								MinecraftForge.EVENT_BUS.post(event);
								logger.debug(
									"Added new attachment '{}' from slot {} with tag {} to stack '{}' with tag {} "
										+ "and path {}",
									slotStack,
									i,
									slotStack.getTag(),
									this.itemStack,
									this.itemStack.getTag());
							} else {
								logger.debug(
									"Re-added existing attachment '{}' from slot {} with tag {} to stack '{}' with "
										+ "tag {}",
									slotStack,
									i,
									slotStack.getTag(),
									this.itemStack,
									this.itemStack.getTag());
							}

							stackSlotMapping.put(i, a1.getCategory());
							VirtualInventory childInventory = createInventory(this.owner, this, slotStack);
							if (childInventory != null) {
								this.elements.put(a1.getCategory(), childInventory);
							}
							continue;
						}
					}

					AttachmentCategory category = stackSlotMapping.remove(i);
					if (category != null) {
						logger.debug(
							"Removing attachment '{}' from slot {}, with tag {} from stack '{}'",
							slotStack,
							i,
							slotStack.getTag(),
							this.itemStack);
						VirtualInventory e = this.elements.get(category);
						if (e != null) {
							AttachmentRemovedEvent event =
								new AttachmentRemovedEvent(this.owner, this.getRootStack(), this.itemStack, slotStack);
							MinecraftForge.EVENT_BUS.post(event);
							e.itemStack = ItemStack.EMPTY;
							e.elements.clear();
						}
					}
				}

				slotMapping.saveSlotMapping();
				if (this.parent != null) {
					this.parent.onContentChange();
				}

				menu.updateAttachmentSlots();
				menu.broadcastChanges();
				logger.debug(
					"Virtual inventory {} handled changes for container {}, stack tags {}",
					this,
					container,
					this.itemStack.getTag());
			}
		}
	}

	private void onContentChange() {
		logger.debug("Updating tag content for {}, tag: {}", this, this.itemStack.getTag());
		this.updateTag();
		logger.debug("Updating tag content for {}, tag: {}", this, this.itemStack.getTag());
		if (this.parent != null) {
			this.parent.updateTag();
		}
	}

	private CompoundTag updateTag() {
		if (this.itemStack == null) {
			return null;
		} else if (this.itemStack.isEmpty()) {
			logger.error("Virtual inventory {} attempted to update empty stack {}", this, this.itemStack);
			return null;
		} else {
			CompoundTag tag = this.itemStack.getOrCreateTag();
			Item item = this.itemStack.getItem();
			if (item instanceof Attachment attachment) {
				String attachmentId = ForgeRegistries.ITEMS.getKey(attachment.asItem()).toString();
				tag.putString("id", attachmentId);
				boolean isRemovable = !tag.contains("rmv", 99) || tag.getBoolean("rmv");
				tag.putBoolean("rmv", isRemovable);
			}

			ListTag nestedAttachments = tag.getList("as", 10);
			nestedAttachments.clear();

			for (Map.Entry<AttachmentCategory, VirtualInventory> e : this.elements.entrySet()) {
				CompoundTag nestedAttachmentTag = e.getValue().updateTag();
				if (nestedAttachmentTag != null) {
					nestedAttachments.add(nestedAttachmentTag);
				}
			}

			tag.put("as", nestedAttachments);
			return tag;
		}
	}

	Map<Integer, AttachmentCategory> createSlotMapping(String stackId) {
		Map<Integer, AttachmentCategory> mapping = new HashMap<>();
		int i = 1;

		for (AttachmentCategory category : this.getCategories()) {
			VirtualInventory e = this.getElement(category);
			ItemStack itemStack = e.getItemStack();
			if (itemStack != null && !itemStack.isEmpty()) {
				mapping.put(i, category);
			}

			++i;
		}

		return mapping;
	}

	boolean isValidSlotMapping(Map<Integer, AttachmentCategory> mapping) {
		return mapping.isEmpty() ? this.elements.isEmpty() : this.elements.keySet().containsAll(mapping.values());
	}

	public String toString() {
		return String.format(
			"{vi: %d, path: %s, elements: %s}", System.identityHashCode(this), this.getPath(), this.elements);
	}

	private static VirtualInventory
	createInventory(Player owner, VirtualInventory parentInventory, ItemStack currentStack) {
		AttachmentCategory currentCategory = null;
		if (currentStack != null) {
			Item currentTag = currentStack.getItem();
			if (currentTag instanceof Attachment attachment) {
				currentCategory = attachment.getCategory();
			}
		}

		VirtualInventory currentInventory = new VirtualInventory(owner, currentCategory, parentInventory, currentStack);
		if (currentStack != null) {
			CompoundTag currentTag = currentStack.getOrCreateTag();
			Item attachmentStacks = currentStack.getItem();
			if (attachmentStacks instanceof AttachmentHost attachmentHost) {
				HashMap<AttachmentCategory, ItemStack> var17 = new HashMap<>();
				if (currentTag.contains("as", 9)) {
					ListTag attachmentsList = currentTag.getList("as", 10);

					for (int i = 0; i < attachmentsList.size(); ++i) {
						CompoundTag attachmentTag = attachmentsList.getCompound(i);
						String itemId = attachmentTag.getString("id");
						Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
						if (item instanceof Attachment attachment) {
							ItemStack attachmentItemStack = new ItemStack(item);
							attachmentItemStack.setTag(attachmentTag);
							var17.put(attachment.getCategory(), attachmentItemStack);
						}
					}
				}

				for (AttachmentCategory category : attachmentHost.getCompatibleAttachmentCategories()) {
					ItemStack attachmentStack = var17.get(category);
					VirtualInventory nestedInventory;
					if (attachmentStack == null) {
						nestedInventory = new VirtualInventory(owner, category, currentInventory, null);
					} else {
						nestedInventory = createInventory(owner, currentInventory, attachmentStack);
					}

					if (nestedInventory != null) {
						currentInventory.addElement(nestedInventory);
					}
				}

				logger.debug(
					"Created {} with stack: {}, tag {}, elements: {}",
					currentInventory,
					currentStack,
					currentStack.getTag(),
					currentInventory.elements);
			}
		}
		return currentInventory;
	}

	public static VirtualInventory createInventory(Player owner, ItemStack mainStack) {
		return createInventory(owner, null, mainStack);
	}
}
