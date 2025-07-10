package mod.pbj.crafting;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mod.pbj.item.AmmoItem;
import mod.pbj.item.GunItem;
import mod.pbj.registry.ItemRegistry;
import mod.pbj.registry.RecipeTypeRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.Tags.Items;

public class PointBlankRecipeProvider extends RecipeProvider {
	public PointBlankRecipeProvider(PackOutput output) {
		super(output);
	}

	protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
		for (Map.Entry<String, Supplier<? extends Item>> e : ItemRegistry.ITEMS.getItemsByName().entrySet()) {
			Item item = (e.getValue()).get();
			int quantity = 0;
			PointBlankRecipeBuilder sampleBuilder =
				new PointBlankRecipeBuilder(RecipeTypeRegistry.DEFAULT_SERIALIZER.get(), item, quantity);
			if (item instanceof GunItem) {
				quantity = 1;
				sampleBuilder.requires(PointBlankIngredient.of(Items.INGOTS_GOLD, 1))
					.requires(PointBlankIngredient.of(Items.INGOTS_IRON, 3));
			} else if (item instanceof AmmoItem) {
				quantity = 30;
				sampleBuilder.requires(PointBlankIngredient.of(Items.INGOTS_COPPER, 1))
					.requires(PointBlankIngredient.of(Items.INGOTS_IRON, 1))
					.requires(PointBlankIngredient.of(Items.GUNPOWDER, 1));
			} else {
				quantity = 5;
				sampleBuilder.requires(PointBlankIngredient.of(Items.INGOTS_COPPER, 1))
					.requires(PointBlankIngredient.of(Items.INGOTS_IRON, 1))
					.requires(PointBlankIngredient.of(Items.GUNPOWDER, 1));
			}

			if (quantity > 0) {
				sampleBuilder.unlockedBy("has_iron", has(Items.INGOTS_IRON));
				sampleBuilder.save(consumer);
			}
		}
	}
}
