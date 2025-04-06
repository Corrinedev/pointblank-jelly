package com.vicmatskiv.pointblank.client.render;

import java.util.ArrayDeque;
import java.util.Deque;

public enum RenderPass {
   MAIN_ITEM,
   HANDS,
   RETICLE,
   MUZZLE_FLASH,
   GLOW,
   ATTACHMENTS,
   PIP,
   PIP_MASK,
   PIP_OVERLAY;

   private static Deque<StackFrame> renderPassStack = new ArrayDeque();

   public static void push(RenderPass renderPass) {
      renderPassStack.addFirst(new StackFrame(renderPass));
   }

   public static RenderPass pop() {
      StackFrame stackFrame = (StackFrame)renderPassStack.removeFirst();
      return stackFrame != null ? stackFrame.renderPass : null;
   }

   public static RenderPass current() {
      StackFrame stackFrame = (StackFrame)renderPassStack.peekFirst();
      return stackFrame != null ? stackFrame.renderPass : null;
   }

   public static Object getEffectId() {
      StackFrame stackFrame = (StackFrame)renderPassStack.peekFirst();
      return stackFrame != null ? stackFrame.effectId : null;
   }

   public static void setEffectId(Object effectId) {
      StackFrame stackFrame = (StackFrame)renderPassStack.peekFirst();
      if (stackFrame == null) {
         throw new IllegalStateException("RenderPass stack is empty");
      } else {
         stackFrame.effectId = effectId;
      }
   }

   // $FF: synthetic method
   private static RenderPass[] $values() {
      return new RenderPass[]{MAIN_ITEM, HANDS, RETICLE, MUZZLE_FLASH, GLOW, ATTACHMENTS, PIP, PIP_MASK, PIP_OVERLAY};
   }

   private static class StackFrame {
      RenderPass renderPass;
      Object effectId;

      public StackFrame(RenderPass value) {
         this.renderPass = value;
      }
   }
}
