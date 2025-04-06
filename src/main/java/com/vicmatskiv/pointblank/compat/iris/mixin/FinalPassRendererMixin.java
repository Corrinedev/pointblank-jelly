package com.vicmatskiv.pointblank.compat.iris.mixin;

import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.compat.iris.IriaAuxIndexHolder;
import com.vicmatskiv.pointblank.compat.iris.IrisShaderUtil;
import net.irisshaders.iris.pipeline.FinalPassRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({FinalPassRenderer.class})
public class FinalPassRendererMixin {
   @ModifyArg(
      method = {"createProgram"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/irisshaders/iris/gl/program/ProgramBuilder;begin(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/common/collect/ImmutableSet;)Lnet/irisshaders/iris/gl/program/ProgramBuilder;"
),
      index = 1,
      remap = false
   )
   private String modifyVertexShader(String vsh) {
      if (!Config.advancedIrisIntegrationEnabled) {
         return vsh;
      } else {
         Integer auxIndex = (Integer)IriaAuxIndexHolder.value.get();
         if (auxIndex == null) {
            return vsh;
         } else {
            String auxPointblankTexCoordVar = "auxPointblankTexCoord";
            return IrisShaderUtil.patchFinalPassVertexShader(vsh, auxPointblankTexCoordVar);
         }
      }
   }

   @ModifyArg(
      method = {"createProgram"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/irisshaders/iris/gl/program/ProgramBuilder;begin(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/common/collect/ImmutableSet;)Lnet/irisshaders/iris/gl/program/ProgramBuilder;"
),
      index = 3,
      remap = false
   )
   private String modifyFragmentShader(String fsh) {
      if (!Config.advancedIrisIntegrationEnabled) {
         return fsh;
      } else {
         Integer auxIndex = (Integer)IriaAuxIndexHolder.value.get();
         if (auxIndex == null) {
            return fsh;
         } else {
            String auxPointblankTexCoordVar = "auxPointblankTexCoord";
            return IrisShaderUtil.patchFinalPassFragmentShader(fsh, auxPointblankTexCoordVar, auxIndex);
         }
      }
   }
}
