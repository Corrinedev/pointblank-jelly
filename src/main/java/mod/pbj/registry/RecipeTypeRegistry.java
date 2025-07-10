package mod.pbj.registry;

import mod.pbj.crafting.PointBlankRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RecipeTypeRegistry {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS;
	public static final RegistryObject<PointBlankRecipe.Serializer> DEFAULT_SERIALIZER;
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES;
	public static final RegistryObject<RecipeType<PointBlankRecipe>> DEFAULT_RECIPE_TYPE;

	private static <T extends Recipe<?>> RegistryObject<RecipeType<T>> create(String name) {
		return RECIPE_TYPES.register(name, () -> new RecipeType<>() {
			public String toString() {
				return name;
			}
		});
	}

	static {
		RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "pointblank");
		DEFAULT_SERIALIZER = RECIPE_SERIALIZERS.register("default", PointBlankRecipe.Serializer::new);
		RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "pointblank");
		DEFAULT_RECIPE_TYPE = create("default");
	}
}
