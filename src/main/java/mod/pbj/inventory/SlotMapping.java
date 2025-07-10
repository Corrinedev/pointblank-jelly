package mod.pbj.inventory;

import java.util.HashMap;
import java.util.Map;
import mod.pbj.attachment.AttachmentCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SlotMapping {
	private static final int CURRENT_VERSION = 1;
	private static final long EXPIRATION_DURATION = 604800000L;
	public static final String TAG_SLOT_MAPPING = "pointblank:attachmentSlotMapping";
	private static final Logger logger = LogManager.getLogger("pointblank");
	private final Map<String, Map<Integer, AttachmentCategory>> mapping = new HashMap<>();
	private final Player player;

	static SlotMapping getOrCreate(Player player) {
		SlotMapping slotMapping = new SlotMapping(player);
		slotMapping.loadSlotMapping();
		return slotMapping;
	}

	SlotMapping(Player player) {
		this.player = player;
	}

	Map<Integer, AttachmentCategory> getOrCreateSlotMapping(VirtualInventory virtualInventory) {
		String key = virtualInventory.getPath();
		Map<Integer, AttachmentCategory> result;
		if (key == null) {
			return null;
		} else {
			result = this.mapping.get(key);
			if (result != null && !virtualInventory.isValidSlotMapping(result)) {
				logger.debug(
					"Invalid slot mapping found for key {} in inventory {}. Here it is: {}",
					key,
					virtualInventory,
					result);
				result = null;
			}

			if (result == null) {
				logger.debug("Creating a slot mapping for key {} in inventory {}", key, virtualInventory);
				result = virtualInventory.createSlotMapping(key);
				if (key != null) {
					this.mapping.put(key, result);
				}

				this.saveSlotMapping();
				logger.debug(
					"Created a slot mapping for key {} in inventory {}. Here it is: {}", key, virtualInventory, result);
			}

			return result;
		}
	}

	Map<Integer, AttachmentCategory> getStackSlotMapping(VirtualInventory virtualInventory) {
		return this.mapping.get(virtualInventory.getPath());
	}

	void saveSlotMapping() {
		CompoundTag persistentData = this.player.getPersistentData();
		CompoundTag slotMappingTag = this.serializeSlotMapping();
		persistentData.put("pointblank:attachmentSlotMapping", slotMappingTag);
	}

	private void loadSlotMapping() {
		CompoundTag persistentData = this.player.getPersistentData();
		CompoundTag slotMappingTag = persistentData.getCompound("pointblank:attachmentSlotMapping");
		if (slotMappingTag != null) {
			this.deserializeStackIdSlotMapping(slotMappingTag);
		}
	}

	CompoundTag serializeSlotMapping() {
		CompoundTag rootTag = new CompoundTag();
		long currentTime = System.currentTimeMillis();
		rootTag.putInt("Version", 1);
		rootTag.putLong("Timestamp", currentTime);
		ListTag mappingsList = new ListTag();

		for (Map.Entry<String, Map<Integer, AttachmentCategory>> entry : this.mapping.entrySet()) {
			CompoundTag mappingTag = new CompoundTag();
			mappingTag.putString("id", entry.getKey());
			mappingTag.putLong("CreationTime", currentTime);
			CompoundTag slotMappingsTag = new CompoundTag();

			for (Map.Entry<Integer, AttachmentCategory> slotEntry : (entry.getValue()).entrySet()) {
				slotMappingsTag.putString(slotEntry.getKey().toString(), slotEntry.getValue().getName());
			}

			mappingTag.put("SlotMappings", slotMappingsTag);
			mappingsList.add(mappingTag);
		}

		rootTag.put("Mappings", mappingsList);
		return rootTag;
	}

	void deserializeStackIdSlotMapping(CompoundTag rootTag) {
		int storedVersion = rootTag.getInt("Version");
		long storedTimestamp = rootTag.getLong("Timestamp");
		long currentTime = System.currentTimeMillis();
		if (storedVersion == 1 && currentTime - storedTimestamp <= 604800000L) {
			this.mapping.clear();
			ListTag mappingsList = rootTag.getList("Mappings", 10);

			for (int i = 0; i < mappingsList.size(); ++i) {
				CompoundTag mappingTag = mappingsList.getCompound(i);
				long creationTime = mappingTag.getLong("CreationTime");
				if (currentTime - creationTime <= 604800000L) {
					String stackId = mappingTag.getString("id");
					CompoundTag slotMappingsTag = mappingTag.getCompound("SlotMappings");
					Map<Integer, AttachmentCategory> slotMappings = new HashMap<>();

					for (String slotKey : slotMappingsTag.getAllKeys()) {
						String slotValue = slotMappingsTag.getString(slotKey);
						slotMappings.put(Integer.parseInt(slotKey), AttachmentCategory.fromString(slotValue));
					}

					this.mapping.put(stackId, slotMappings);
				}
			}

		} else {
			logger.warn("Skipping loading of outdated or expired slot mappings.");
		}
	}
}
