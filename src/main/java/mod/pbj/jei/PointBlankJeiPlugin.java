package mod.pbj.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mod.pbj.crafting.PointBlankRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class PointBlankJeiPlugin implements IModPlugin {
	public static final RecipeType<PointBlankRecipe> POINTBLANK_RECIPE_TYPE =
		RecipeType.create("pointblank", "pointblank", PointBlankRecipe.class);

	public ResourceLocation getPluginUid() {
		return new ResourceLocation("pointblank", "jei");
	}

	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new IRecipeCategory[] {new PointBlankRecipeCategory(helper)});
	}

	public void registerRecipes(IRecipeRegistration registration) {
		Minecraft mc = Minecraft.getInstance();
		registration.addRecipes(POINTBLANK_RECIPE_TYPE, PointBlankRecipe.getRecipes(mc.level));
	}
}
