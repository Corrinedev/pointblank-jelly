package mod.pbj.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import mod.pbj.Enableable;
import mod.pbj.client.render.RenderUtil;
import mod.pbj.client.uv.PlayOnceSpriteUVProvider;
import mod.pbj.client.uv.SpriteUVProvider;
import mod.pbj.crafting.Craftable;
import mod.pbj.crafting.PointBlankRecipe;
import mod.pbj.inventory.CraftingContainerMenu;
import mod.pbj.inventory.IngredientSlot;
import mod.pbj.inventory.SearchSlot;
import mod.pbj.network.CraftingRequestPacket;
import mod.pbj.network.CraftingRequestPacket.RequestType;
import mod.pbj.network.Network;
import mod.pbj.registry.SoundRegistry;
import mod.pbj.util.CancellableSound;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.StateMachine;
import mod.pbj.util.StateMachine.TransitionMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TooltipFlag.Default;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.util.ClientUtils;

@OnlyIn(Dist.CLIENT)
public class CraftingScreen extends EffectRenderingInventoryScreen<CraftingContainerMenu> {
	public static final int CELL_WIDTH = 18;
	public static final int CELL_HEIGHT = 18;
	private static final ResourceLocation BACKGROUND_TEXTURE =
		new ResourceLocation("pointblank", "textures/gui/craftnew.png");
	private static final ResourceLocation GLOW_OUTLINE =
		new ResourceLocation("pointblank", "textures/gui/glow_outline.png");
	private static final ResourceLocation TABS_TEXTURE =
		new ResourceLocation("minecraft:textures/gui/container/creative_inventory/tabs.png");
	private final StateMachine<CraftingState, Context> stateMachine = this.createStateMachine();
	private float scrollOffs;
	private boolean scrolling;
	private EditBox searchBox;
	private CustomButton craftButton;
	private CreativeInventoryListener listener;
	private boolean ignoreTextInput;
	private boolean hasClickedOutside;
	private final Set<TagKey<Item>> visibleTags = new HashSet<>();
	private final int scrollbarXOffset = 121;
	private final int scrollbarYOffset = 18;
	private final int scrollbarWidth = 14;
	private final int scrollbarHeight = 126;
	private ItemStack selectedItem;
	private PointBlankRecipe selectedItemRecipe;
	private long craftingStartTime;
	private long craftingCompletedCooldownStartTime;
	private long craftingCompletedCooldownDuration = 2000L;
	private float itemRotationAngleDegrees;
	private float itemRotationAngleDegreesPerTick;
	private final float idleItemRotationsPerSecond = 0.1F;
	private final SpriteUVProvider glowOutlineSpriteUVProvider;

	public CraftingScreen(CraftingContainerMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		playerInventory.player.containerMenu = this.menu;
		this.imageWidth = 312;
		this.imageHeight = 151;
		this.itemRotationAngleDegreesPerTick = rotationsPerSecondToDegress(this.idleItemRotationsPerSecond);
		this.craftingCompletedCooldownDuration = 600L;
		this.glowOutlineSpriteUVProvider = new PlayOnceSpriteUVProvider(6, 6, 50, 600L);
	}

	private static float rotationsPerSecondToDegress(float rotationsPerSecond) {
		return 360.0F * rotationsPerSecond * 0.05F;
	}

	private StateMachine<CraftingState, Context> createStateMachine() {
		StateMachine.Builder<CraftingState, Context> builder = new StateMachine.Builder<>();
		builder.withTransition(CraftingScreen.CraftingState.IDLE, CraftingScreen.CraftingState.CRAFTING, (context) -> {
			boolean var10000;
			if (this.selectedItem != null && this.selectedItem.getItem() instanceof Craftable &&
				this.selectedItemRecipe != null) {
			label20: {
				Item patt6136$temp = this.selectedItem.getItem();
				if (patt6136$temp instanceof Enableable en) {
					if (!en.isEnabled()) {
						break label20;
					}
				}

				if (this.menu.isIdle() && this.craftButton.isPressed()) {
					var10000 = true;
					return var10000;
				}
			}
			}

			var10000 = false;
			return var10000;
		}, TransitionMode.EVENT, null, this::actionStartCrafting);
		builder.withTransition(
			CraftingScreen.CraftingState.CRAFTING,
			CraftingScreen.CraftingState.IDLE,
			(context)
				-> !this.craftButton.isPressed(),
			TransitionMode.AUTO,
			null,
			this::actionCancelCrafting);
		builder.withTransition(
			CraftingScreen.CraftingState.CRAFTING,
			CraftingScreen.CraftingState.IDLE,
			(context)
				-> true,
			TransitionMode.EVENT,
			null,
			null);
		builder.withTransition(
			CraftingScreen.CraftingState.CRAFTING,
			CraftingScreen.CraftingState.CRAFTING_COMPLETED,
			(ctx)
				-> System.currentTimeMillis() - this.craftingStartTime >=
					   ((Craftable)this.selectedItem.getItem()).getCraftingDuration(),
			TransitionMode.AUTO,
			null,
			this::actionCraftingCompleted);
		builder.withTransition(
			CraftingScreen.CraftingState.CRAFTING_COMPLETED,
			CraftingScreen.CraftingState.CRAFTING_COMPLETED_COOLDOWN,
			(context)
				-> true,
			TransitionMode.AUTO,
			null,
			(ctx, f, t) -> this.craftingCompletedCooldownStartTime = System.currentTimeMillis());
		builder.withTransition(
			CraftingScreen.CraftingState.CRAFTING_COMPLETED_COOLDOWN,
			CraftingScreen.CraftingState.IDLE,
			(ctx)
				-> System.currentTimeMillis() - this.craftingCompletedCooldownStartTime >=
					   this.craftingCompletedCooldownDuration,
			TransitionMode.AUTO,
			null,
			null);
		builder.withOnSetStateAction(CraftingScreen.CraftingState.IDLE, this::actionIdle);
		return builder.build(CraftingScreen.CraftingState.IDLE);
	}

	private void actionIdle(Context context, CraftingState fromState, CraftingState toState) {
		this.craftButton.setFocused(false);
		this.itemRotationAngleDegreesPerTick = rotationsPerSecondToDegress(this.idleItemRotationsPerSecond);
	}

	private void actionStartCrafting(Context context, CraftingState fromState, CraftingState toState) {
		this.craftingStartTime = System.currentTimeMillis();
		Player player = ClientUtils.getClientPlayer();
		this.minecraft.getSoundManager().play(new CancellableSound(
			player,
			SoundRegistry.CRAFTING_IN_PROGRESS.get(),
			player.getSoundSource(),
			player.getRandom(),
			(s)
				-> this.minecraft.screen == this &&
					   this.stateMachine.getCurrentState() == CraftingScreen.CraftingState.CRAFTING));
		Network.networkChannel.sendToServer(
			new CraftingRequestPacket(RequestType.START_CRAFTING, this.selectedItemRecipe.getId()));
		this.craftButton.setTooltip(null);
	}

	public void onClose() {
		if (this.stateMachine.getCurrentState() == CraftingScreen.CraftingState.CRAFTING &&
			this.selectedItemRecipe != null) {
			Network.networkChannel.sendToServer(
				new CraftingRequestPacket(RequestType.CANCEL_CRAFTING, this.selectedItemRecipe.getId()));
		}

		super.onClose();
	}

	private void actionCancelCrafting(Context context, CraftingState fromState, CraftingState toState) {
		Network.networkChannel.sendToServer(
			new CraftingRequestPacket(RequestType.CANCEL_CRAFTING, this.selectedItemRecipe.getId()));
	}

	private void actionCraftingCompleted(Context context, CraftingState fromState, CraftingState toState) {
		ClientUtils.getClientPlayer().playSound(SoundRegistry.CRAFTING_COMPLETED.get(), 1.0F, 1.0F);
	}

	private float getCraftingProgress() {
		return this.stateMachine.getCurrentState() != CraftingScreen.CraftingState.CRAFTING
			? 0.0F
			: Mth.clamp(
				  (float)(System.currentTimeMillis() - this.craftingStartTime) /
					  (float)((Craftable)this.selectedItem.getItem()).getCraftingDuration(),
				  0.0F,
				  1.0F);
	}

	private float getCraftingCompletedCooldownProgress() {
		return this.stateMachine.getCurrentState() != CraftingScreen.CraftingState.CRAFTING_COMPLETED_COOLDOWN
			? 0.0F
			: Mth.clamp(
				  (float)(System.currentTimeMillis() - this.craftingCompletedCooldownStartTime) /
					  (float)this.craftingCompletedCooldownDuration,
				  0.0F,
				  1.0F);
	}

	private void refreshContents() {
		this.updateIngredientSlots();
	}

	public void containerTick() {
		if (this.minecraft != null) {
			Context context = new Context();
			this.stateMachine.update(context);
			this.craftButton.active =
				this.selectedItem != null && (this.menu.isIdle() || this.menu.isCrafting()) &&
				(this.stateMachine.getCurrentState() == CraftingState.IDLE ||
				 (this.stateMachine.getCurrentState() == CraftingState.CRAFTING && this.craftButton.isPressed()));

			this.itemRotationAngleDegrees += this.itemRotationAngleDegreesPerTick;
			if (this.minecraft.player != null) {
				this.refreshContents();
			}

			this.searchBox.tick();
		}
	}

	protected void slotClicked(@Nullable Slot slot, int slotIndex, int mouseButton, ClickType clickType) {
		if (this.menu.isIdle() && this.menu.isCreativeSlot(slot)) {
			this.searchBox.moveCursorToEnd();
			this.searchBox.setHighlightPos(0);
			this.craftButton.setFocused(false);
			this.craftButton.active = false;
			this.onSelectCraftableItem(slot.getItem());
		}
	}

	private void onSelectCraftableItem(ItemStack itemStack) {
		this.craftButton.active = false;
		this.selectedItem = itemStack;
		this.menu.clearIngredientSlots();
		ClientUtils.getClientPlayer().playSound(SoundRegistry.CRAFTING_ITEM_SELECTED.get(), 1.0F, 1.0F);
		if (this.selectedItem != null && !this.selectedItem.isEmpty() &&
			this.selectedItem.getItem() instanceof Craftable) {
			this.selectedItemRecipe = PointBlankRecipe.getRecipe(ClientUtils.getLevel(), itemStack.getItem());
			if (this.selectedItemRecipe != null) {
				this.updateIngredientSlots();
			}
		} else {
			this.selectedItemRecipe = null;
		}
	}

	private void updateIngredientSlots() {
		if (this.selectedItemRecipe == null) {
			this.craftButton.active = false;
		} else {
			this.craftButton.active = this.stateMachine.getCurrentState() == CraftingScreen.CraftingState.IDLE &&
									  this.menu.updateIngredientSlots(this.selectedItemRecipe);
			this.craftButton.active = this.selectedItem != null && (this.menu.isIdle() || this.menu.isCrafting()) &&
									  (this.stateMachine.getCurrentState() == CraftingState.IDLE ||
									   this.stateMachine.getCurrentState() == CraftingState.CRAFTING) &&
									  this.menu.updateIngredientSlots(this.selectedItemRecipe);
		}
	}

	protected void init() {
		super.init();
		this.menu.clearIngredientSlots();
		int searchBoxLeftOffset = 46;
		this.searchBox = new EditBox(
			this.font,
			this.leftPos + searchBoxLeftOffset,
			this.topPos + 6,
			80,
			9,
			Component.translatable("itemGroup.search"));
		this.searchBox.setMaxLength(50);
		this.searchBox.setBordered(false);
		this.searchBox.setVisible(false);
		this.searchBox.setTextColor(16777215);
		this.addWidget(this.searchBox);
		this.clearDraggingState();
		this.searchBox.setVisible(true);
		this.searchBox.setCanLoseFocus(false);
		this.searchBox.setFocused(true);
		this.searchBox.setWidth(89);
		this.searchBox.setX(this.leftPos + searchBoxLeftOffset + 89 - this.searchBox.getWidth());
		this.craftButton =
			CustomButton
				.builder(
					Component.translatable("label.pointblank.craft"),
					(b) -> {
						Context context = new Context();
						this.stateMachine.setState(context, CraftingScreen.CraftingState.CRAFTING);
					})
				.onRelease((b) -> {})
				.bounds(this.leftPos + 256, this.topPos + 115, 46, 20)
				.progressProvider(this::getCraftingProgress)
				.tooltip(Tooltip.create(Component.translatable("message.pointblank.press_and_hold_to_craft")))
				.build();
		this.craftButton.setFocused(false);
		this.craftButton.active = false;
		this.addRenderableWidget(this.craftButton);
		this.refreshSearchResults();
		this.scrollOffs = 0.0F;
		this.menu.scrollTo(0.0F);
		this.selectedItem = null;
		this.selectedItemRecipe = null;
		this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
		this.listener = new CreativeInventoryListener(this.minecraft);
		this.minecraft.player.inventoryMenu.addSlotListener(this.listener);
	}

	public void resize(Minecraft minecraft, int width, int height) {
		int i = this.menu.getRowIndexForScroll(this.scrollOffs);
		String s = this.searchBox.getValue();
		ItemStack currentSelectedItem = this.selectedItem;
		this.init(minecraft, width, height);
		this.searchBox.setValue(s);
		if (!this.searchBox.getValue().isEmpty()) {
			this.refreshSearchResults();
		}

		this.onSelectCraftableItem(currentSelectedItem);
		this.scrollOffs = this.menu.getScrollForRowIndex(i);
		this.menu.scrollTo(this.scrollOffs);
	}

	public void removed() {
		super.removed();
		if (this.minecraft.player != null && this.minecraft.player.getInventory() != null) {
			this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
		}
	}

	public boolean charTyped(char p_98521_, int p_98522_) {
		if (this.ignoreTextInput) {
			return false;
		} else {
			String s = this.searchBox.getValue();
			if (this.searchBox.charTyped(p_98521_, p_98522_)) {
				if (!Objects.equals(s, this.searchBox.getValue())) {
					this.refreshSearchResults();
				}

				return true;
			} else {
				return false;
			}
		}
	}

	public boolean keyPressed(int p_98547_, int p_98548_, int p_98549_) {
		this.ignoreTextInput = false;
		boolean flag = !this.menu.isCreativeSlot(this.hoveredSlot) || this.hoveredSlot.hasItem();
		boolean flag1 = InputConstants.getKey(p_98547_, p_98548_).getNumericKeyValue().isPresent();
		if (flag && flag1 && this.checkHotbarKeyPressed(p_98547_, p_98548_)) {
			this.ignoreTextInput = true;
			return true;
		} else {
			String s = this.searchBox.getValue();
			if (this.searchBox.keyPressed(p_98547_, p_98548_, p_98549_)) {
				if (!Objects.equals(s, this.searchBox.getValue())) {
					this.refreshSearchResults();
				}

				return true;
			} else {
				return this.searchBox.isFocused() && this.searchBox.isVisible() && p_98547_ != 256 ||
					super.keyPressed(p_98547_, p_98548_, p_98549_);
			}
		}
	}

	public boolean keyReleased(int p_98612_, int p_98613_, int p_98614_) {
		this.ignoreTextInput = false;
		return super.keyReleased(p_98612_, p_98613_, p_98614_);
	}

	private void refreshSearchResults() {
		this.menu.refreshSearchResults(this.searchBox.getValue());
		this.scrollOffs = 0.0F;
	}

	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		Component label = this.selectedItem != null && !this.selectedItem.isEmpty()
							  ? this.selectedItem.getItem().getName(this.selectedItem)
							  : Component.translatable("label.pointblank.craft");
		guiGraphics.drawCenteredString(this.font, label, 225, 8, 16776960);
	}

	public boolean mouseClicked(double posX, double posY, int mouseButton) {
		if (mouseButton == 0 && this.insideScrollbar(posX, posY)) {
			this.scrolling = this.canScroll();
			return true;
		} else {
			return super.mouseClicked(posX, posY, mouseButton);
		}
	}

	public boolean mouseReleased(double posX, double posY, int mouseButton) {
		if (this.craftButton.isPressed()) {
			this.craftButton.release();
		}

		if (mouseButton == 0) {
			this.scrolling = false;
		}

		return super.mouseReleased(posX, posY, mouseButton);
	}

	private boolean canScroll() {
		return this.menu.canScroll();
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
		if (!this.canScroll()) {
			return false;
		} else {
			this.scrollOffs = this.menu.subtractInputFromScroll(this.scrollOffs, scroll);
			this.menu.scrollTo(this.scrollOffs);
			return true;
		}
	}

	protected boolean hasClickedOutside(double mouseX, double mouseY, int p_98543_, int p_98544_, int mouseButton) {
		this.hasClickedOutside = mouseX < (double)p_98543_ || mouseY < (double)p_98544_ ||
								 mouseX >= (double)(p_98543_ + this.imageWidth) ||
								 mouseY >= (double)(p_98544_ + this.imageHeight);
		return this.hasClickedOutside;
	}

	protected boolean insideScrollbar(double mouseX, double mouseY) {
		int i = this.leftPos;
		int j = this.topPos;
		int k = i + this.scrollbarXOffset;
		int l = j + this.scrollbarYOffset;
		int i1 = k + this.scrollbarWidth;
		int j1 = l + this.scrollbarHeight;
		boolean inside = mouseX >= (double)k && mouseY >= (double)l && mouseX < (double)i1 && mouseY < (double)j1;
		return inside;
	}

	public boolean mouseDragged(double p_98535_, double p_98536_, int p_98537_, double p_98538_, double p_98539_) {
		if (this.scrolling) {
			int i = this.topPos + 18;
			int j = i + 112;
			this.scrollOffs = ((float)p_98536_ - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
			this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
			this.menu.scrollTo(this.scrollOffs);
			return true;
		} else {
			return super.mouseDragged(p_98535_, p_98536_, p_98537_, p_98538_, p_98539_);
		}
	}

	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	private void renderItemInHand(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if (this.selectedItem != null) {
			int left = (this.width - this.imageWidth) / 2;
			int top = (this.height - this.imageHeight) / 2;
			guiGraphics.enableScissor(left + 151, top + 23, left + 300, top + 98);
			PoseStack poseStack = guiGraphics.pose();
			poseStack.pushPose();
			float itemRotationAngleDegreesWithPartial = this.itemRotationAngleDegrees;
			float yOffset = 0.0F;
			float zoom = 1.0F;
			itemRotationAngleDegreesWithPartial +=
				this.itemRotationAngleDegreesPerTick * this.minecraft.getPartialTick();
			yOffset = Mth.cos(itemRotationAngleDegreesWithPartial * (float)Math.PI * 0.02F);
			if (this.stateMachine.getCurrentState() == CraftingScreen.CraftingState.CRAFTING_COMPLETED_COOLDOWN) {
				zoom += 0.2F *
						Mth.sin(this.getCraftingCompletedCooldownProgress() * (float)Math.PI * 2.0F + (float)Math.PI);
			}

			poseStack.translate((float)(left + 230), (float)(this.height / 2 - 18) + yOffset * 2.0F, 100.0F);
			float interactionPitch = -30.0F;
			float interactionYaw = 150.0F;
			poseStack.mulPose(
				(new Quaternionf())
					.rotationXYZ(
						interactionPitch * ((float)Math.PI / 180F), interactionYaw * ((float)Math.PI / 180F), 0.0F));
			poseStack.scale(zoom, zoom, zoom);
			poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F + itemRotationAngleDegreesWithPartial));
			poseStack.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
			poseStack.scale(80.0F, 80.0F, 80.0F);
			PoseStack modelStack = RenderSystem.getModelViewStack();
			modelStack.pushPose();
			modelStack.mulPoseMatrix(poseStack.last().pose());
			RenderSystem.applyModelViewMatrix();
			MultiBufferSource.BufferSource buffer = this.minecraft.renderBuffers().bufferSource();
			BakedModel model = this.minecraft.getItemRenderer().getModel(
				this.selectedItem,
				MiscUtil.getLevel(this.minecraft.player),
				this.minecraft.player,
				this.minecraft.player.getId() + ItemDisplayContext.GROUND.ordinal());
			this.minecraft.getItemRenderer().render(
				this.selectedItem,
				ItemDisplayContext.GROUND,
				false,
				new PoseStack(),
				buffer,
				15728880,
				OverlayTexture.NO_OVERLAY,
				model);
			buffer.endBatch();
			modelStack.popPose();
			poseStack.popPose();
			RenderSystem.applyModelViewMatrix();
			guiGraphics.disableScissor();
		}
	}

	public List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
		boolean flag = this.hoveredSlot != null && this.hoveredSlot instanceof SearchSlot;
		boolean flag2 = true;
		TooltipFlag.Default tooltipflag$default =
			this.minecraft.options.advancedItemTooltips ? Default.ADVANCED : Default.NORMAL;
		TooltipFlag tooltipflag = flag ? tooltipflag$default.asCreative() : tooltipflag$default;
		List<Component> list = itemStack.getTooltipLines(this.minecraft.player, tooltipflag);
		List<Component> tooltipComponents = Lists.newArrayList(list);
		if (flag2 && flag) {
			this.visibleTags.forEach((p_205407_) -> {
				if (itemStack.is(p_205407_)) {
					tooltipComponents.add(
						1, Component.literal("#" + p_205407_.location()).withStyle(ChatFormatting.DARK_PURPLE));
				}
			});
		}

		return tooltipComponents;
	}

	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(
			BACKGROUND_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 328, 328);
		this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
		int j = this.leftPos + this.scrollbarXOffset;
		int k = this.topPos + this.scrollbarYOffset;
		int i = k + this.scrollbarHeight;
		guiGraphics.blit(
			TABS_TEXTURE,
			j,
			k + (int)((float)(i - k - 17) * this.scrollOffs),
			232 + (this.canScroll() ? 0 : 12),
			0,
			12,
			15);
		int ingredientSlotOffset = CraftingContainerMenu.SEARCH_CONTAINER.getContainerSize();

		for (int var21 = 0; var21 < CraftingContainerMenu.INGREDIENT_CONTAINER.getContainerSize(); ++var21) {
			IngredientSlot ingredientSlot = (IngredientSlot)this.menu.slots.get(ingredientSlotOffset + var21);
			if (!ingredientSlot.isIngredientAvailable()) {
				guiGraphics.blit(
					BACKGROUND_TEXTURE,
					this.leftPos + ingredientSlot.x - 1,
					this.topPos + ingredientSlot.y - 1,
					48.0F,
					151.0F,
					18,
					18,
					328,
					328);
			}
		}

		int left = (this.width - this.imageWidth) / 2;
		int top = (this.height - this.imageHeight) / 2;
		if (this.stateMachine.getCurrentState() == CraftingScreen.CraftingState.CRAFTING) {
			int topOffset = 98 - (int)(75.0F * this.getCraftingProgress());
			guiGraphics.fillGradient(left + 151, top + topOffset, left + 300, top + 98, 268500736, 1342242560);
		}

		if (this.stateMachine.getCurrentState() == CraftingScreen.CraftingState.CRAFTING_COMPLETED_COOLDOWN) {
			float progress = this.getCraftingCompletedCooldownProgress();
			int minAlpha = 32;
			int maxAlpha = 96;
			int alpha1 = (int)((float)minAlpha - (float)minAlpha * progress);
			int alpha2 = (int)((float)maxAlpha - (float)maxAlpha * progress);
			guiGraphics.fillGradient(
				left + 151, top + 23, left + 300, top + 98, '\uff00' | alpha1 << 24, '\uff00' | alpha2 << 24);
			float[] spriteUV = this.glowOutlineSpriteUVProvider.getSpriteUV(progress);
			if (spriteUV != null) {
				float minU = spriteUV[0];
				float minV = spriteUV[1];
				float maxU = spriteUV[2];
				float maxV = spriteUV[3];
				RenderUtil.blit(
					guiGraphics,
					GLOW_OUTLINE,
					left + 139,
					left + 314,
					top - 30,
					top + 145,
					0,
					minU,
					maxU,
					minV,
					maxV,
					1.0F,
					1.0F,
					1.0F,
					1.0F);
			}
		}

		RenderSystem.disableBlend();
		this.renderItemInHand(guiGraphics, mouseX, mouseY);
	}

	public void cancelCrafting() {
		this.stateMachine.setState(new Context(), CraftingScreen.CraftingState.IDLE);
	}

	public enum CraftingState {
		IDLE,
		CRAFTING,
		CRAFTING_COMPLETED,
		CRAFTING_COMPLETED_COOLDOWN;

		CraftingState() {}
	}

	private static class Context {
		private Context() {}
	}
}
