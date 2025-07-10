package mod.pbj.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({GameRenderer.class})
public interface GameRendererAccessorMixin {
	@Accessor("mainCamera") Camera getMainCamera();

	@Accessor("lightTexture") LightTexture getLightTexture();

	@Mutable @Accessor("renderDistance") void setRenderDistance(float var1);

	@Invoker("getFov") double invokeGetFov(Camera var1, float var2, boolean var3);

	@Invoker("renderItemInHand") void invokeRenderItemInHand(PoseStack var1, Camera var2, float var3);
}
