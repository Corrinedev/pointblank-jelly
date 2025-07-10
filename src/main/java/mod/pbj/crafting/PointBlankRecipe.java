package mod.pbj.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import mod.pbj.item.GunItem;
import mod.pbj.registry.RecipeTypeRegistry;
import mod.pbj.util.InventoryUtils;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags.Items;

public class PointBlankRecipe implements Recipe<Container> {
	private static final int MAX_INGREDIENTS = 10;
	private static final Function<Level, Map<Item, PointBlankRecipe>> levelRecipesByItem = Util.memoize(
		(level)
			-> level.getRecipeManager()
				   .getAllRecipesFor(RecipeTypeRegistry.DEFAULT_RECIPE_TYPE.get())
				   .stream()
				   .collect(Collectors.toMap(PointBlankRecipe::getItem, (r) -> r)));
	private static final Function<Level, Map<ResourceLocation, PointBlankRecipe>> levelRecipesById = Util.memoize(
		(level)
			-> level.getRecipeManager()
				   .getAllRecipesFor(RecipeTypeRegistry.DEFAULT_RECIPE_TYPE.get())
				   .stream()
				   .collect(Collectors.toMap(PointBlankRecipe::getId, (r) -> r)));
	private static final int MAX_SIZE = 10;
	private final ResourceLocation id;
	private final String group;
	private final ItemStack result;
	private final ItemStack initializedItemStack;
	private final List<PointBlankIngredient> ingredients;
	private static final NonNullList<Ingredient> defaultIngredients;

	public static PointBlankRecipe getRecipe(Level level, Item item) {
		return (levelRecipesByItem.apply(level)).get(item);
	}

	public static PointBlankRecipe getRecipe(Level level, ResourceLocation recipeId) {
		return (levelRecipesById.apply(level)).get(recipeId);
	}

	public static List<PointBlankRecipe> getRecipes(Level level) {
		return new ArrayList<>(levelRecipesById.apply(level).values());
	}

	public PointBlankRecipe(
		ResourceLocation recipeId, String group, ItemStack resultItemStack, List<PointBlankIngredient> ingredients) {
		this.id = recipeId;
		this.group = group;
		this.result = resultItemStack.copy();
		this.initializedItemStack = resultItemStack.copy();
		if (this.initializedItemStack.getItem() instanceof GunItem) {
			GunItem.initStackForCrafting(this.initializedItemStack);
		}

		if (ingredients != null && !ingredients.isEmpty()) {
			if (ingredients.size() > 10) {
				throw new IllegalArgumentException(
					"Recipe ingredients for item " + resultItemStack + " exceed maximum allowed count of 10");
			} else {
				this.ingredients = ingredients;
			}
		} else {
			throw new IllegalArgumentException("Recipe ingredients are not set for item " + resultItemStack);
		}
	}

	public ResourceLocation getId() {
		return this.id;
	}

	public RecipeSerializer<?> getSerializer() {
		return RecipeTypeRegistry.DEFAULT_SERIALIZER.get();
	}

	public String getGroup() {
		return this.group;
	}

	private Item getItem() {
		return this.result.getItem();
	}

	public ItemStack getResultItem(RegistryAccess registryAccess) {
		return this.result.copy();
	}

	public ItemStack getInitializedStack() {
		return this.initializedItemStack;
	}

	public NonNullList<Ingredient> getIngredients() {
		return defaultIngredients;
	}

	public List<PointBlankIngredient> getPointBlankIngredients() {
		return this.ingredients;
	}

	public boolean matches(Container container, Level level) {
		throw new UnsupportedOperationException("Implement me!");
	}

	public boolean canBeCrafted(Player player) {
		return this.ingredients.stream().anyMatch((ingredient) -> InventoryUtils.hasIngredient(player, ingredient));
	}

	public void removeIngredients(Player player) {
		this.ingredients.stream().forEach((ingredient) -> {
			Objects.requireNonNull(ingredient);
			InventoryUtils.removeItem(player, ingredient::matches, ingredient.getCount());
		});
	}

	public ItemStack assemble(CraftingContainer craftingContainer, RegistryAccess registryAccess) {
		return this.result.copy();
	}

	public boolean canCraftInDimensions(int m, int n) {
		return false;
	}

	public ItemStack assemble(Container container, RegistryAccess registryAccess) {
		throw new UnsupportedOperationException("Implement me!");
	}

	public RecipeType<?> getType() {
		return RecipeTypeRegistry.DEFAULT_RECIPE_TYPE.get();
	}

	static {
		defaultIngredients = NonNullList.of(Ingredient.of(Items.INGOTS));
	}

	public static class Serializer implements RecipeSerializer<PointBlankRecipe> {
		public Serializer() {}

		public PointBlankRecipe fromJson(ResourceLocation recipeId, JsonObject jsonObject) {
			String s = GsonHelper.getAsString(jsonObject, "group", "");
			NonNullList<PointBlankIngredient> ingredients =
				itemsFromJson(GsonHelper.getAsJsonArray(jsonObject, "ingredients"));
			if (ingredients.isEmpty()) {
				throw new JsonParseException("No ingredients for the recipe");
			} else if (ingredients.size() > 10) {
				throw new JsonParseException("Too many ingredients for the recipe. The maximum is 10");
			} else {
				ItemStack itemstack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(jsonObject, "result"));
				return new PointBlankRecipe(recipeId, s, itemstack, ingredients);
			}
		}

		private static NonNullList<PointBlankIngredient> itemsFromJson(JsonArray jsonArray) {
			NonNullList<PointBlankIngredient> ingredients = NonNullList.create();

			for (int i = 0; i < jsonArray.size(); ++i) {
				PointBlankIngredient ingredient = PointBlankIngredient.fromJson(jsonArray.get(i));
				ingredients.add(ingredient);
			}

			return ingredients;
		}

		public PointBlankRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf byteBuf) {
			String recipeGroup = byteBuf.readUtf();
			int ingredientSize = byteBuf.readVarInt();
			NonNullList<PointBlankIngredient> ingredients = NonNullList.create();

			for (int j = 0; j < ingredientSize; ++j) {
				ingredients.add(PointBlankIngredient.fromNetwork(byteBuf));
			}

			ItemStack recipeResult = byteBuf.readItem();
			return new PointBlankRecipe(recipeId, recipeGroup, recipeResult, ingredients);
		}

		public void toNetwork(FriendlyByteBuf byteBuf, PointBlankRecipe recipe) {
			byteBuf.writeUtf(recipe.group);
			byteBuf.writeVarInt(recipe.ingredients.size());

			for (PointBlankIngredient ingredient : recipe.ingredients) {
				ingredient.toNetwork(byteBuf);
			}

			byteBuf.writeItem(recipe.result);
		}
	}
}
