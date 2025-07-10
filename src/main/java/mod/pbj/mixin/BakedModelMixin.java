package mod.pbj.mixin;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.client.model.SeparateTransformsModel.Baked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Baked.class})
public interface BakedModelMixin {
	@Accessor(value = "baseModel", remap = false) BakedModel getBaseModel();
}
