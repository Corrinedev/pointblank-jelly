package mod.pbj.client.gui;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import mod.pbj.attachment.Attachment;
import mod.pbj.attachment.AttachmentHost;
import mod.pbj.event.AttachmentAddedEvent;
import mod.pbj.event.AttachmentRemovedEvent;
import mod.pbj.inventory.AttachmentContainerMenu;
import mod.pbj.inventory.AttachmentSlot;
import mod.pbj.inventory.HierarchicalSlot;
import mod.pbj.inventory.SimpleAttachmentContainer;
import mod.pbj.registry.SoundRegistry;
import mod.pbj.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import software.bernie.geckolib.util.ClientUtils;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public class AttachmentManagerScreen extends AbstractContainerScreen<AttachmentContainerMenu> {
   private static final ResourceLocation GUI_TEXTURES = new ResourceLocation("pointblank", "textures/gui/attachments4.png");
   private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("pointblank", "textures/gui/blueprint-background-2.png");
   protected static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");
   private static final Vector4f COLOR_GREEN = new Vector4f(0.0F, 1.0F, 0.0F, 1.0F);
   private final Inventory playerInventory;
   private final MouseInteractionHandler mouseInteractionHandler;
   private final int inventoryWidth = 176;
   private final int inventoryHeight = 90;
   private final int slotWidth = 18;
   private final int slotHeight = 18;
   private final int slotRightPadding = 4;
   private final int headerBottomPadding = 2;
   private final AttachmentContainerMenu menu;
   private String selectedAttachmentPath;
   private final Queue<AttachmentHighlightEvent> attachmentEventQueue = new ArrayDeque<>();

   public AttachmentManagerScreen(AttachmentContainerMenu menu, Inventory playerInventory, Component titleIn) {
      super(menu, playerInventory, titleIn);
      this.menu = menu;
      this.playerInventory = playerInventory;
      this.imageHeight = 250;
      this.imageWidth = 370;
      this.mouseInteractionHandler = new MouseInteractionHandler(this::isMouseInScreen, 0.5F, 2.0F, 0.1F);
      MinecraftForge.EVENT_BUS.register(this);
   }

   protected void init() {
      super.init();
   }

   public void containerTick() {
      super.containerTick();
      if (this.minecraft != null && this.minecraft.player != null) {
         ItemStack selectedStack = this.playerInventory.getSelected();
         if (!(selectedStack.getItem() instanceof AttachmentHost)) {
            Minecraft.getInstance().setScreen(null);
         } else {
            AttachmentHighlightEvent event = this.attachmentEventQueue.peek();
            if (event != null && event.isExpired()) {
               this.attachmentEventQueue.poll();
            }
         }
      }

   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      PoseStack poseStack = guiGraphics.pose();
      poseStack.pushPose();
      this.renderBackground(guiGraphics);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 1, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
      RenderSystem.disableBlend();
      super.render(guiGraphics, mouseX, mouseY, partialTicks);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
      poseStack.popPose();
   }

   protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      ItemStack selectedItem = this.minecraft.player.getMainHandItem();
      if (selectedItem != null && selectedItem.getItem() instanceof AttachmentHost) {
         Component label = selectedItem.getItem().getName(selectedItem);
         guiGraphics.drawCenteredString(this.font, label, 180, 15, 16776960);
      }

   }

   private void renderItemInHand(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      Minecraft minecraft = Minecraft.getInstance();
      PoseStack poseStack = guiGraphics.pose();
      poseStack.pushPose();
      poseStack.translate((float)(this.width / 2 + 5), (float)(this.height / 2 - 30), 180.0F);
      this.applyMouseInteractionTransforms(poseStack, mouseX, mouseY);
      float zoom = this.mouseInteractionHandler.getZoom();
      poseStack.scale(zoom, zoom, zoom);
      poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
      poseStack.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
      poseStack.scale(100.0F, 100.0F, 100.0F);
      PoseStack modelStack = RenderSystem.getModelViewStack();
      modelStack.pushPose();
      modelStack.mulPoseMatrix(poseStack.last().pose());
      RenderSystem.applyModelViewMatrix();
      MultiBufferSource.BufferSource buffer = this.minecraft.renderBuffers().bufferSource();
      ItemStack itemStack = minecraft.player.getMainHandItem();
      BakedModel model = minecraft.getItemRenderer().getModel(itemStack, MiscUtil.getLevel(minecraft.player), minecraft.player, minecraft.player.getId() + ItemDisplayContext.GROUND.ordinal());
      minecraft.getItemRenderer().render(itemStack, ItemDisplayContext.GROUND, false, new PoseStack(), buffer, 15728880, OverlayTexture.NO_OVERLAY, model);
      buffer.endBatch();
      modelStack.popPose();
      poseStack.popPose();
      RenderSystem.applyModelViewMatrix();
   }

   private void applyMouseInteractionTransforms(PoseStack poseStack, int mouseX, int mouseY) {
      float interactionOffsetX = (float)this.mouseInteractionHandler.getX() + (this.mouseInteractionHandler.isInteracting() && this.mouseInteractionHandler.isTranslating() ? (float)mouseX - this.mouseInteractionHandler.getMouseClickedX() : 0.0F);
      float interactionOffsetY = (float)this.mouseInteractionHandler.getY() + (this.mouseInteractionHandler.isInteracting() && this.mouseInteractionHandler.isTranslating() ? (float)mouseY - this.mouseInteractionHandler.getMouseClickedY() : 0.0F);
      poseStack.translate(interactionOffsetX, interactionOffsetY, 0.0F);
      float interactionPitch = this.mouseInteractionHandler.getRotationPitch() - (this.mouseInteractionHandler.isInteracting() && this.mouseInteractionHandler.isRotating() ? (float)mouseY - this.mouseInteractionHandler.getMouseClickedY() : 0.0F) - 30.0F;
      float interactionYaw = this.mouseInteractionHandler.getRotationYaw() + (this.mouseInteractionHandler.isInteracting() && this.mouseInteractionHandler.isRotating() ? (float)mouseX - this.mouseInteractionHandler.getMouseClickedX() : 0.0F) + 150.0F;
      poseStack.mulPose((new Quaternionf()).rotationXYZ(interactionPitch * ((float)Math.PI / 180F), interactionYaw * ((float)Math.PI / 180F), 0.0F));
   }

   public String getSelectedAttachmentPath() {
      return this.selectedAttachmentPath;
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
      PoseStack poseStack = guiGraphics.pose();
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, 250.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int left = this.getLeft();
      int top = this.getTop();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      guiGraphics.blit(GUI_TEXTURES, left, top, 1000, 0.0F, 0.0F, 176, 90, 256, 256);
      int attLeft = this.leftPos + 5;
      int attTop = this.topPos + 8;
      int slotTextureLeftOffset = 176;
      int unavailableSlotTextureTopOffset = 36;
      SimpleAttachmentContainer childContainer = null;
      Slot i = this.hoveredSlot;
      if (i instanceof HierarchicalSlot hSlot) {
         this.selectedAttachmentPath = hSlot.getPath();
      }

      i = this.hoveredSlot;
      if (i instanceof AttachmentSlot attachmentSlot) {
         childContainer = attachmentSlot.getChildContainer();
         if (childContainer != null) {
            int childContainerIndex = childContainer.getContainerIndex();
            poseStack.pushPose();
            int elementsCount = childContainer.getVirtualInventory().getElements().size();
            guiGraphics.renderOutline(attLeft + childContainerIndex * 22 - 2, attTop - 2, 22, 24 + 18 * elementsCount, -1061093377);
            poseStack.popPose();
         }
      }

      SimpleAttachmentContainer[] attachmentContainers = this.menu.getAttachmentContainers();

      for(int j = 0; j < attachmentContainers.length; ++j) {
         SimpleAttachmentContainer attachmentContainer = attachmentContainers[j];
         int availableSlotTextureTopOffset = j == 0 ? 0 : 18;
         int containerStartIndex = SimpleAttachmentContainer.getContainerStartIndex(attachmentContainers, j);
         if (attachmentContainer.getVirtualInventory() != null && !attachmentContainer.getVirtualInventory().getElements().isEmpty()) {
            if (this.menu.getSlot(containerStartIndex).isActive()) {
               guiGraphics.blit(GUI_TEXTURES, attLeft + j * 22, attTop, 1000, (float)slotTextureLeftOffset, 54.0F, 18, 18, 256, 256);
            }

            for(int k = 1; k < attachmentContainer.getContainerSize(); ++k) {
               int adjustedSlotIndex = containerStartIndex + k;
               AttachmentSlot slot = (AttachmentSlot)this.menu.getSlot(adjustedSlotIndex);
               if (slot.isActive()) {
                  int textureTopOffset = this.menu.getCarried() != null && !this.menu.getCarried().isEmpty() && !slot.mayPlace(this.menu.getCarried()) ? unavailableSlotTextureTopOffset : availableSlotTextureTopOffset;
                  guiGraphics.blit(GUI_TEXTURES, attLeft + j * 22, attTop + this.headerBottomPadding + k * 18, 1000, (float)slotTextureLeftOffset, (float)textureTopOffset, 18, 18, 256, 256);
               }
            }
         }
      }

      poseStack.popPose();
      poseStack.pushPose();
      this.renderItemInHand(guiGraphics, mouseX, mouseY);
      this.selectedAttachmentPath = null;
      poseStack.popPose();
      RenderSystem.disableBlend();
      poseStack.translate(0.0F, 0.0F, 2000.0F);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
      return this.mouseInteractionHandler.onMouseScrolled(mouseX, mouseY, scroll);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.mouseInteractionHandler.onMouseButtonClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return this.mouseInteractionHandler.onMouseButtonReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
   }

   private int getLeft() {
      return (this.width - 176) / 2;
   }

   private int getTop() {
      return (this.height - 90) / 2 + 74;
   }

   private boolean isMouseInScreen(double mouseX, double mouseY) {
      int width = 176;
      int left = this.getLeft();
      int top = this.getTop();
      boolean isMouseInInventory = mouseX >= (double)left && mouseX <= (double)(left + width) && mouseY >= (double)top && mouseY <= (double)(top + 90);
      if (isMouseInInventory) {
         return false;
      } else {
         int attLeft = this.leftPos + 5;
         int attTop = this.topPos + 8;
         boolean isMouseInAttachmentContainer = false;
         SimpleAttachmentContainer[] attachmentContainers = this.menu.getAttachmentContainers();

         for(int i = 0; i < attachmentContainers.length; ++i) {
            SimpleAttachmentContainer attachmentContainer = attachmentContainers[i];
            int containerStartIndex = SimpleAttachmentContainer.getContainerStartIndex(attachmentContainers, i);
            if (attachmentContainer.getVirtualInventory() != null && !attachmentContainer.getVirtualInventory().getElements().isEmpty()) {
               int slotLeft = attLeft + i * 22;
               int slotRight = slotLeft + 18 + 4;
               if (!(mouseX < (double)slotLeft) && !(mouseX > (double)slotRight) && !(mouseY < (double)attTop)) {
                  int yBottom = 0;

                  for(int j = 1; j < attachmentContainer.getContainerSize(); ++j) {
                     int adjustedSlotIndex = containerStartIndex + j;
                     AttachmentSlot slot = (AttachmentSlot)this.menu.getSlot(adjustedSlotIndex);
                     if (!slot.isActive()) {
                        yBottom = attTop + this.headerBottomPadding + j * 18;
                        break;
                     }
                  }

                  if (mouseY < (double)yBottom) {
                     isMouseInAttachmentContainer = true;
                     break;
                  }
               }
            }
         }

         return !isMouseInAttachmentContainer;
      }
   }

   public void onClose() {
      this.selectedAttachmentPath = null;
      MinecraftForge.EVENT_BUS.unregister(this);
      super.onClose();
   }

   public void beforeRenderingSlot(GuiGraphics guiGraphics, Slot slot) {
      if (!(slot instanceof AttachmentSlot)) {
         if (this.menu.getPlayerInventory().selected == 9 - (this.menu.slots.size() - slot.index)) {
            guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 1348235500);
         }

         if (slot.index >= this.menu.getTotalAttachmentSlots() && this.mayPlaceAttachment(slot.getItem())) {
            guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, -1605328816);
         }

      }
   }

   private boolean mayPlaceAttachment(ItemStack attachmentStack) {
      if (!attachmentStack.isEmpty() && attachmentStack.getItem() instanceof Attachment) {
         SimpleAttachmentContainer[] attachmentContainers = this.menu.getAttachmentContainers();

         for(int i = 0; i < attachmentContainers.length; ++i) {
            SimpleAttachmentContainer attachmentContainer = attachmentContainers[i];
            int containerStartIndex = SimpleAttachmentContainer.getContainerStartIndex(attachmentContainers, i);
            if (attachmentContainer.getVirtualInventory() != null && !attachmentContainer.getVirtualInventory().getElements().isEmpty()) {
               for(int j = 1; j < attachmentContainer.getContainerSize(); ++j) {
                  int adjustedSlotIndex = containerStartIndex + j;
                  AttachmentSlot slot = (AttachmentSlot)this.menu.getSlot(adjustedSlotIndex);
                  if (slot.isActive() && slot.mayPlace(attachmentStack)) {
                     return true;
                  }
               }
            }
         }

      }
       return false;
   }

   public Pair<RenderType, Vector4f> getRenderTypeOverride(ItemStack baseItemStack, ItemStack attachmentItemStack, String attachmentName, String attachmentPath) {
      ResourceLocation texture = new ResourceLocation("pointblank", "textures/item/" + attachmentName + ".png");
      AttachmentHighlightEvent attachmentHighlight = this.attachmentEventQueue.peek();
      if (attachmentHighlight != null && ItemStack.isSameItem(attachmentItemStack, attachmentHighlight.attachmentStack)) {
         if (attachmentHighlight.isHighlighted()) {
            RenderType renderType = RenderType.eyes(texture);
            return Pair.of(renderType, COLOR_GREEN);
         } else {
            return null;
         }
      } else {
         String selectedAttachmentPath = this.getSelectedAttachmentPath();
         if (Objects.equals(attachmentPath, selectedAttachmentPath)) {
            RenderType renderType = RenderType.eyes(texture);
            return Pair.of(renderType, COLOR_GREEN);
         } else if (selectedAttachmentPath != null && selectedAttachmentPath.endsWith("/") && attachmentPath.startsWith(selectedAttachmentPath.substring(0, selectedAttachmentPath.length() - 1))) {
            RenderType renderType = RenderType.eyes(texture);
            return Pair.of(renderType, COLOR_GREEN);
         } else {
            return null;
         }
      }
   }

   @SubscribeEvent
   public void onAttachmentAdded(AttachmentAddedEvent event) {
      if (EffectiveSide.get() == LogicalSide.CLIENT) {
         if (this.minecraft.screen != this) {
            return;
         }
         ClientUtils.getClientPlayer().playSound(SoundRegistry.ATTACHMENT_ADDED.get(), 1.0F, 1.0F);
         this.attachmentEventQueue.add(new AttachmentHighlightEvent(System.currentTimeMillis(), 750L, event.getParentStack(), event.getAttachmentStack()));
      }

   }

   @SubscribeEvent
   public void onAttachmentRemoved(AttachmentRemovedEvent event) {
      if (EffectiveSide.get() == LogicalSide.CLIENT) {
         if (this.minecraft.screen != this) {
            return;
         }

         ClientUtils.getClientPlayer().playSound(SoundRegistry.ATTACHMENT_REMOVED.get(), 1.0F, 1.0F);
         this.attachmentEventQueue.add(new AttachmentHighlightEvent(System.currentTimeMillis(), 750L, event.getParentStack(), event.getAttachmentStack()));
      }

   }

   private static class AttachmentHighlightEvent {
      private final long startTime;
      private final long duration;
      private final ItemStack parentStack;
      private final ItemStack attachmentStack;
      private final long blinkInterval;

      public AttachmentHighlightEvent(long startTime, long duration, ItemStack parentStack, ItemStack attachmentStack) {
         this.startTime = startTime;
         this.duration = duration;
         this.parentStack = parentStack;
         this.attachmentStack = attachmentStack;
         this.blinkInterval = 150L;
      }

      public boolean isHighlighted() {
         long elapsedTime = System.currentTimeMillis() - this.startTime;
         if (elapsedTime >= this.duration) {
            return false;
         } else {
            long k = elapsedTime / this.blinkInterval;
            return k % 2L == 0L;
         }
      }

      public boolean isExpired() {
         return System.currentTimeMillis() >= this.startTime + this.duration;
      }
   }
}
