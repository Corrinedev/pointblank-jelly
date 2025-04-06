package com.vicmatskiv.pointblank.mixin;

import com.vicmatskiv.pointblank.client.ClientEventHandler;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({PostPass.class})
public class PostPassMixin {
   private static String EFFECT_NAME_PREFIX = "pointblank:";

   @Redirect(
      method = {"process"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/EffectInstance;apply()V"
)
   )
   private void onApplyEffect(EffectInstance effect) {
      String effectName = effect.m_172571_();
      if (effectName != null && effectName.startsWith(EFFECT_NAME_PREFIX)) {
         float progress = (float)ClientEventHandler.getPostPassEffectController().getProgress();
         effect.m_108960_("Progress").m_5985_(progress);
      }

      effect.m_108966_();
   }
}
