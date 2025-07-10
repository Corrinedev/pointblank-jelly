package mod.pbj.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mod.pbj.util.JsonUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public interface PointBlankIngredient {
	List<ItemStack> getItemStacks();

	JsonElement toJson();

	int getCount();

	default boolean matches(ItemStack itemStack) {
		return itemStack != null &&
			this.getItemStacks().stream().anyMatch((ingredientStack) -> ingredientStack.is(itemStack.getItem()));
	}

	void toNetwork(FriendlyByteBuf var1);

	static PointBlankIngredient of(TagKey<Item> tagKey, int count) {
		return new TagIngredient(tagKey, count);
	}

	static PointBlankIngredient of(ItemLike itemLike, int count) {
		return new ItemIngredient(new ItemStack(itemLike, count));
	}

	static PointBlankIngredient fromNetwork(FriendlyByteBuf byteBuf) {
		boolean isItem = byteBuf.readBoolean();
		return isItem ? PointBlankIngredient.ItemIngredient.fromNetwork(byteBuf)
					  : PointBlankIngredient.TagIngredient.fromNetwork(byteBuf);
	}

	static PointBlankIngredient fromJson(JsonElement jsonElement) {
		if (jsonElement != null && !jsonElement.isJsonNull() && jsonElement.isJsonObject()) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			if (jsonObject.has("item") && jsonObject.has("tag")) {
				throw new JsonParseException("An ingredient entry is either a tag or an item, not both");
			} else if (jsonObject.has("item")) {
				return PointBlankIngredient.ItemIngredient.fromJson(jsonObject);
			} else if (jsonObject.has("tag")) {
				return PointBlankIngredient.TagIngredient.fromJson(jsonObject);
			} else {
				throw new JsonParseException("An ingredient entry needs either a tag or an item");
			}
		} else {
			throw new JsonSyntaxException("Invalid json ingredient element: " + jsonElement);
		}
	}

	class ItemIngredient implements PointBlankIngredient {
		private final ItemStack itemStack;

		public ItemIngredient(ItemStack itemStack) {
			this.itemStack = itemStack;
		}

		public int getCount() {
			return this.itemStack.getCount();
		}

		public List<ItemStack> getItemStacks() {
			return Collections.singletonList(this.itemStack);
		}

		public JsonElement toJson() {
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("item", BuiltInRegistries.ITEM.getKey(this.itemStack.getItem()).toString());
			jsonObject.addProperty("count", this.itemStack.getCount());
			return jsonObject;
		}

		public static PointBlankIngredient fromJson(JsonObject jsonObject) {
			String itemLocation = JsonUtil.getJsonString(jsonObject, "item", null);
			if (itemLocation == null) {
				throw new JsonSyntaxException("Missing ingredient tag");
			} else {
				Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemLocation));
				int count = JsonUtil.getJsonInt(jsonObject, "count", 1);
				return new ItemIngredient(new ItemStack(item, count));
			}
		}

		public final void toNetwork(FriendlyByteBuf byteBuf) {
			byteBuf.writeBoolean(true);
			byteBuf.writeItem(this.itemStack);
		}

		public static PointBlankIngredient fromNetwork(FriendlyByteBuf byteBuf) {
			return new ItemIngredient(byteBuf.readItem());
		}
	}

	class TagIngredient implements PointBlankIngredient {
		private final TagKey<Item> tagKey;
		private final int count;
		private List<ItemStack> itemStacks;

		private TagIngredient(TagKey<Item> tagKey, int count) {
			this.tagKey = tagKey;
			this.count = count;
		}

		public int getCount() {
			return this.count;
		}

		public List<ItemStack> getItemStacks() {
			if (this.itemStacks == null) {
				List<ItemStack> itemStacks = new ArrayList<>();

				for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(this.tagKey)) {
					itemStacks.add(new ItemStack(holder.value(), this.count));
				}

				this.itemStacks = Collections.unmodifiableList(itemStacks);
			}

			return this.itemStacks;
		}

		public JsonElement toJson() {
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("tag", this.tagKey.location().toString());
			jsonObject.addProperty("count", this.count);
			return jsonObject;
		}

		public static PointBlankIngredient fromJson(JsonObject jsonObject) {
			String tagResourceLocation = JsonUtil.getJsonString(jsonObject, "tag", null);
			if (tagResourceLocation == null) {
				throw new JsonSyntaxException("Missing ingredient tag");
			} else {
				TagKey<Item> tagKey = TagKey.create(Registries.ITEM, new ResourceLocation(tagResourceLocation));
				int count = JsonUtil.getJsonInt(jsonObject, "count", 1);
				return new TagIngredient(tagKey, count);
			}
		}

		public final void toNetwork(FriendlyByteBuf byteBuf) {
			byteBuf.writeBoolean(false);
			byteBuf.writeResourceLocation(this.tagKey.location());
			byteBuf.writeInt(this.count);
		}

		public static PointBlankIngredient fromNetwork(FriendlyByteBuf byteBuf) {
			TagKey<Item> tagKey = TagKey.create(Registries.ITEM, byteBuf.readResourceLocation());
			int count = byteBuf.readInt();
			return new TagIngredient(tagKey, count);
		}
	}
}
