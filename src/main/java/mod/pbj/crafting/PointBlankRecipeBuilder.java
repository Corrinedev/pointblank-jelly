package mod.pbj.crafting;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

public class PointBlankRecipeBuilder implements RecipeBuilder {
	private final Item result;
	private final int count;
	private final List<PointBlankIngredient> ingredients = Lists.newArrayList();
	private final Advancement.Builder advancement = Builder.recipeAdvancement();
	@Nullable private String group;
	private RecipeSerializer<?> serializer;
	private RecipeCategory category;

	public PointBlankRecipeBuilder(RecipeSerializer<?> serializer, ItemLike item, int count) {
		this.category = RecipeCategory.COMBAT;
		this.serializer = serializer;
		this.result = item.asItem();
		this.count = count;
	}

	public PointBlankRecipeBuilder requires(TagKey<Item> tagKey, int count) {
		return this.requires(PointBlankIngredient.of(tagKey, count));
	}

	public PointBlankRecipeBuilder requires(ItemLike item, int count) {
		return this.requires(PointBlankIngredient.of(item, count));
	}

	public PointBlankRecipeBuilder requires(PointBlankIngredient ingredient) {
		this.ingredients.add(ingredient);
		return this;
	}

	public PointBlankRecipeBuilder unlockedBy(String p_126197_, CriterionTriggerInstance p_126198_) {
		this.advancement.addCriterion(p_126197_, p_126198_);
		return this;
	}

	public PointBlankRecipeBuilder group(@Nullable String p_126195_) {
		this.group = p_126195_;
		return this;
	}

	public Item getResult() {
		return this.result;
	}

	public void save(Consumer<FinishedRecipe> consumer, ResourceLocation recipeId) {
		this.ensureValid(recipeId);
		this.advancement.parent(ROOT_RECIPE_ADVANCEMENT)
			.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
			.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(recipeId))
			.requirements(RequirementsStrategy.OR);
		consumer.accept(new Result(
			this.serializer,
			recipeId,
			this.result,
			this.count,
			this.group == null ? "" : this.group,
			this.ingredients,
			this.advancement,
			recipeId.withPrefix("recipes/" + this.category.getFolderName() + "/")));
	}

	private void ensureValid(ResourceLocation p_126208_) {
		if (this.advancement.getCriteria().isEmpty()) {
			throw new IllegalStateException("No way of obtaining recipe " + p_126208_);
		}
	}

	public static class Result implements FinishedRecipe {
		private final ResourceLocation id;
		private final Item result;
		private final int count;
		private final String group;
		private final List<PointBlankIngredient> ingredients;
		private final Advancement.Builder advancement;
		private final ResourceLocation advancementId;
		private RecipeSerializer<?> serializer;

		public Result(
			RecipeSerializer<?> serializer,
			ResourceLocation recipeId,
			Item resultItem,
			int count,
			String group,
			List<PointBlankIngredient> ingredients,
			Advancement.Builder advancementBuilder,
			ResourceLocation advancementId) {
			this.serializer = serializer;
			this.id = recipeId;
			this.result = resultItem;
			this.count = count;
			this.group = group;
			this.ingredients = ingredients;
			this.advancement = advancementBuilder;
			this.advancementId = advancementId;
		}

		public void serializeRecipeData(JsonObject jsonObject) {
			if (!this.group.isEmpty()) {
				jsonObject.addProperty("group", this.group);
			}

			JsonArray jsonarray = new JsonArray();

			for (PointBlankIngredient ingredient : this.ingredients) {
				jsonarray.add(ingredient.toJson());
			}

			jsonObject.add("ingredients", jsonarray);
			JsonObject jsonobject = new JsonObject();
			jsonobject.addProperty("item", BuiltInRegistries.ITEM.getKey(this.result).toString());
			if (this.count > 1) {
				jsonobject.addProperty("count", this.count);
			}

			jsonObject.add("result", jsonobject);
		}

		public RecipeSerializer<?> getType() {
			return this.serializer;
		}

		public ResourceLocation getId() {
			return this.id;
		}

		@Nullable
		public JsonObject serializeAdvancement() {
			return this.advancement.serializeToJson();
		}

		@Nullable
		public ResourceLocation getAdvancementId() {
			return this.advancementId;
		}
	}
}
