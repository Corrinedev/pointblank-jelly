package mod.pbj.attachment;

import com.mojang.datafixers.util.Pair;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import mod.pbj.feature.AimingFeature;
import mod.pbj.feature.Feature;
import mod.pbj.feature.FeatureProvider;
import mod.pbj.network.Network;
import mod.pbj.network.ServerBoundNextAttachmentPacket;
import mod.pbj.network.ServerBoundOpenScreenPacket;
import mod.pbj.network.ServerBoundOpenScreenPacket.ScreenType;
import mod.pbj.util.LRUCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Attachments {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private static final LRUCache<CompoundTag, NavigableMap<AttachmentCategory, String>> selectedAttachmentsCache =
		new LRUCache<>(100);
	private static final LRUCache<CompoundTag, NavigableMap<String, ItemStack>> tagAttachmentsCache =
		new LRUCache<>(100);
	private static final LRUCache<CompoundTag, NavigableMap<String, List<ItemStack>>> tagAttachmentGroupsCache =
		new LRUCache<>(100);
	private static final LRUCache<CompoundTag, NavigableMap<String, ItemStack>> recursiveAttachmentsCache =
		new LRUCache<>(100);
	public static final String ROOT_PATH = "/";

	public Attachments() {}

	public static void ensureValidAttachmentsSelected(ItemStack itemStack) {
		ensureValidAttachmentSelected(itemStack, AttachmentCategory.SCOPE, AimingFeature.class);
	}

	private static void ensureValidAttachmentSelected(
		ItemStack itemStack, AttachmentCategory category, Class<? extends Feature> featureType) {
		boolean isValid = false;
		Pair<String, ItemStack> selected = getSelectedAttachment(itemStack, category);
		if (selected != null) {
			Item item = selected.getSecond().getItem();
			if (item instanceof FeatureProvider fp) {
				Feature feature = fp.getFeature(featureType);
				if (feature != null) {
					isValid = feature.isEnabled(selected.getSecond());
				}
			}
		}

		if (!isValid) {
			selectNextAttachment(itemStack, category, featureType);
		}
	}

	public static Collection<ItemStack> getAttachments(ItemStack itemStack) {
		return getAttachments(itemStack, false).values();
	}

	public static Pair<String, ItemStack> getSelectedAttachment(ItemStack rootItemStack, AttachmentCategory category) {
		NavigableMap<AttachmentCategory, String> selectedAttachments = getSelectedAttachments(rootItemStack);
		String selectedAttachmentPath = selectedAttachments.get(category);
		if (selectedAttachmentPath == null) {
			return null;
		} else if ("/".equals(selectedAttachmentPath)) {
			return Pair.of(selectedAttachmentPath, rootItemStack);
		} else {
			NavigableMap<String, ItemStack> allAttachments = getAttachments(rootItemStack, true);
			ItemStack attachmentStack = allAttachments.get(selectedAttachmentPath);
			return attachmentStack == null ? Pair.of(selectedAttachmentPath, ItemStack.EMPTY)
										   : Pair.of(selectedAttachmentPath, attachmentStack);
		}
	}

	public static NavigableMap<AttachmentCategory, String> getSelectedAttachments(ItemStack itemStack) {
		return itemStack != null && itemStack.getItem() instanceof AttachmentHost && itemStack.getTag() != null
			? selectedAttachmentsCache.computeIfAbsent(itemStack.getTag(), Attachments::getSelectedAttachments)
			: Collections.emptyNavigableMap();
	}

	private static NavigableMap<AttachmentCategory, String> getSelectedAttachments(CompoundTag tag) {
		if (tag != null && tag.contains("sa")) {
			CompoundTag attachmentsTag = tag.getCompound("sa");
			NavigableMap<AttachmentCategory, String> attachments = new TreeMap<>();

			for (AttachmentCategory category : AttachmentCategory.values()) {
				if (attachmentsTag.contains(category.getName())) {
					attachments.put(category, attachmentsTag.getString(category.getName()));
				}
			}

			return attachments;
		} else {
			return Collections.emptyNavigableMap();
		}
	}

	private static void saveSelectedAttachments(ItemStack rootItemStack, Map<AttachmentCategory, String> attachments) {
		CompoundTag nbt = rootItemStack.getOrCreateTag();
		CompoundTag attachmentsTag = new CompoundTag();

		for (Map.Entry<AttachmentCategory, String> entry : attachments.entrySet()) {
			attachmentsTag.putString(entry.getKey().getName(), entry.getValue());
		}

		nbt.put("sa", attachmentsTag);
	}

	public static Pair<String, ItemStack>
	selectNextAttachment(ItemStack rootItemStack, AttachmentCategory category, Class<? extends Feature> featureType) {
		Item itema = rootItemStack.getItem();
		if (itema instanceof AttachmentHost attachmentHost) {
			if (attachmentHost.getCompatibleAttachments().isEmpty()) {
				return null;
			} else {
				NavigableMap<String, ItemStack> attachmentsForCategory =
					getAttachmentsForCategory(rootItemStack, category);
				NavigableMap<AttachmentCategory, String> selectedAttachments = getSelectedAttachments(rootItemStack);
				String activeAttachmentPath = selectedAttachments.get(category);
				Map.Entry<String, ItemStack> next = null;
				if (activeAttachmentPath != null) {
					for (Map.Entry<String, ItemStack> e :
						 attachmentsForCategory.tailMap(activeAttachmentPath, false).entrySet()) {
						Item item = e.getValue().getItem();
						if (item instanceof FeatureProvider fp) {
							Feature feature = fp.getFeature(featureType);
							if (feature != null && feature.isEnabled(e.getValue())) {
								next = e;
								break;
							}
						}
					}
				}

				if (next == null) {
					Item var16 = rootItemStack.getItem();
					if (var16 instanceof FeatureProvider fp) {
						Feature feature = fp.getFeature(featureType);
						if (feature != null && feature.isEnabled(rootItemStack)) {
							next = new AbstractMap.SimpleImmutableEntry<>("/", rootItemStack);
						}
					}

					if (next == null) {
						for (Map.Entry<String, ItemStack> e : attachmentsForCategory.entrySet()) {
							Item var21 = e.getValue().getItem();
							if (var21 instanceof FeatureProvider fp) {
								Feature feature = fp.getFeature(featureType);
								if (feature != null && feature.isEnabled(e.getValue())) {
									next = e;
									break;
								}
							}
						}
					}
				}

				selectedAttachments = new TreeMap<>(selectedAttachments);
				if (next != null) {
					selectedAttachments.put(category, next.getKey());
				} else {
					selectedAttachments.remove(category);
				}

				saveSelectedAttachments(rootItemStack, selectedAttachments);
				return next != null ? Pair.of(next.getKey(), next.getValue()) : null;
			}
		} else {
			return null;
		}
	}

	public static NavigableMap<String, ItemStack>
	getAttachmentsForCategory(ItemStack itemStack, AttachmentCategory category) {
		NavigableMap<String, ItemStack> attachments = getAttachments(itemStack, true);
		NavigableMap<String, ItemStack> attachmentsForCategory = new TreeMap<>();

		for (Map.Entry<String, ItemStack> e : attachments.entrySet()) {
			ItemStack attachmentStack = e.getValue();
			Item var8 = attachmentStack.getItem();
			if (var8 instanceof Attachment attachment) {
				if (Objects.equals(category, attachment.getCategory())) {
					attachmentsForCategory.put(e.getKey(), e.getValue());
				}
			}
		}

		return Collections.unmodifiableNavigableMap(attachmentsForCategory);
	}

	public static NavigableMap<String, List<ItemStack>> getAttachmentGroups(ItemStack itemStack) {
		return itemStack != null && itemStack.getItem() instanceof AttachmentHost && itemStack.getTag() != null
			? tagAttachmentGroupsCache.computeIfAbsent(itemStack.getTag(), Attachments::getAttachmentGroups)
			: Collections.emptyNavigableMap();
	}

	private static NavigableMap<String, List<ItemStack>> getAttachmentGroups(CompoundTag tag) {
		NavigableMap<String, ItemStack> attachments = getAttachments(tag, "/", false, new TreeMap<>());
		NavigableMap<String, List<ItemStack>> attachmentGroups = new TreeMap<>();

		for (Map.Entry<String, ItemStack> e : attachments.entrySet()) {
			for (String group : ((Attachment)e.getValue().getItem()).getGroups()) {
				List<ItemStack> attachmentList = attachmentGroups.computeIfAbsent(group, (g) -> new ArrayList<>());
				attachmentList.add(e.getValue());
			}
		}

		return attachmentGroups;
	}

	public static NavigableMap<String, ItemStack> getAttachments(ItemStack itemStack, boolean recursive) {
		if (itemStack != null && itemStack.getItem() instanceof AttachmentHost && itemStack.getTag() != null) {
			return recursive ? recursiveAttachmentsCache.computeIfAbsent(
								   itemStack.getTag(), (tag) -> getAttachments(tag, "/", true, new TreeMap<>()))
							 : tagAttachmentsCache.computeIfAbsent(
								   itemStack.getTag(), (tag) -> getAttachments(tag, "/", false, new TreeMap<>()));
		} else {
			return Collections.emptyNavigableMap();
		}
	}

	static NavigableMap<String, ItemStack> getAttachments(
		CompoundTag itemTag, String parentPath, boolean recursive, NavigableMap<String, ItemStack> attachmentStacks) {
		if (itemTag != null && itemTag.contains("as", 9)) {
			ListTag attachmentsList = itemTag.getList("as", 10);

			for (int i = 0; i < attachmentsList.size(); ++i) {
				CompoundTag attachmentTag = attachmentsList.getCompound(i);
				String attachmentItemId = attachmentTag.getString("id");
				Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(attachmentItemId));
				if (item instanceof Attachment attachment) {
					ItemStack attachmentStack = new ItemStack(item);
					attachmentStack.setTag(attachmentTag);
					String path = parentPath + "/" + attachment.getName();
					attachmentStacks.put(path, attachmentStack);
					if (recursive && item instanceof AttachmentHost) {
						getAttachments(attachmentTag, path, recursive, attachmentStacks);
					}
				}
			}
		}

		return attachmentStacks;
	}

	public static CompoundTag addAttachment(ItemStack containerStack, ItemStack attachmentStack, boolean isRemoveable) {
		String attachmentId = ForgeRegistries.ITEMS.getKey(attachmentStack.getItem()).toString();
		CompoundTag containerTag = containerStack.getOrCreateTag();
		ListTag attachmentsList = containerTag.getList("as", 10);
		CompoundTag attachmentTag = attachmentStack.hasTag() ? attachmentStack.getTag().copy() : new CompoundTag();
		initAttachmentTag(attachmentTag, attachmentId, isRemoveable);
		attachmentsList.add(attachmentTag);
		containerTag.put("as", attachmentsList);
		return attachmentTag;
	}

	public static List<Attachment> removeAllAttachments(ItemStack containerStack) {
		CompoundTag containerTag = containerStack.getOrCreateTag();
		ListTag attachmentsList1 = containerTag.getList("as", 10);
		ListTag removedList = attachmentsList1.copy();
		attachmentsList1.clear();
		List<Attachment> removedItems = new ArrayList<>();

		for (int i = 0; i < removedList.size(); ++i) {
			CompoundTag attachmentTag = removedList.getCompound(i);
			String attachmentItemId = attachmentTag.getString("id");
			Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(attachmentItemId));
			if (item instanceof Attachment attachment) {
				removedItems.add(attachment);
			}
		}

		return removedItems;
	}

	public static void initAttachmentTag(CompoundTag attachmentTag, String attachmentId, boolean isRemoveable) {
		attachmentTag.putString("id", attachmentId);
		attachmentTag.putBoolean("rmv", isRemoveable);
	}

	public static boolean isRemoveable(CompoundTag containerTag) {
		return true;
	}

	public static void tryAttachmentMode(Player player, ItemStack heldItem) {
		Network.networkChannel.sendToServer(new ServerBoundOpenScreenPacket(ScreenType.ATTACHMENTS));
	}

	public static void tryNextAttachment(
		Player player, ItemStack heldItem, AttachmentCategory category, Class<? extends Feature> feature) {
		Network.networkChannel.sendToServer(new ServerBoundNextAttachmentPacket(category, feature));
	}
}
