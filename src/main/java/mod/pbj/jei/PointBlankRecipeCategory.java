package mod.pbj.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mod.pbj.crafting.PointBlankIngredient;
import mod.pbj.crafting.PointBlankRecipe;
import mod.pbj.registry.BlockRegistry;
import mod.pbj.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class PointBlankRecipeCategory implements IRecipeCategory<PointBlankRecipe> {
	public static final ResourceLocation BACKGROUND_RESOURCE =
		new ResourceLocation("pointblank", "textures/gui/jei.png");
	private final IDrawableStatic backgroundDrawable;
	private final IDrawableStatic itemDrawable;
	private final IDrawableStatic inventoryDrawable;
	private final IDrawable icon;
	private final Component title;

	public PointBlankRecipeCategory(IGuiHelper gui) {
		this.backgroundDrawable = gui.createBlankDrawable(180, 124);
		this.itemDrawable = gui.createDrawable(BACKGROUND_RESOURCE, 2, 2, 180, 102);
		this.inventoryDrawable = gui.createDrawable(BACKGROUND_RESOURCE, 2, 108, 180, 19);
		this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(BlockRegistry.PRINTER.get()));
		this.title = Component.translatable("block.pointblank.printer");
	}

	public RecipeType<PointBlankRecipe> getRecipeType() {
		return PointBlankJeiPlugin.POINTBLANK_RECIPE_TYPE;
	}

	public Component getTitle() {
		return this.title;
	}

	public IDrawable getBackground() {
		return this.backgroundDrawable;
	}

	public IDrawable getIcon() {
		return this.icon;
	}

	public void setRecipe(IRecipeLayoutBuilder builder, PointBlankRecipe recipe, IFocusGroup focuses) {
		ItemStack resultStack = recipe.getResultItem(null);
		List<PointBlankIngredient> pbi = recipe.getPointBlankIngredients();

		for (int i = 0; i < pbi.size(); ++i) {
			builder.addSlot(RecipeIngredientRole.INPUT, i * 18 + 1, 106)
				.addIngredients(VanillaTypes.ITEM_STACK, pbi.get(i).getItemStacks());
		}

		builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(resultStack);
	}

	public void draw(
		PointBlankRecipe recipe,
		IRecipeSlotsView recipeSlotsView,
		GuiGraphics guiGraphics,
		double mouseX,
		double mouseY) {
		Minecraft mc = Minecraft.getInstance();
		this.itemDrawable.draw(guiGraphics, 0, 0);
		this.inventoryDrawable.draw(guiGraphics, 0, this.itemDrawable.getHeight() + 3);
		Font font = mc.font;
		ItemStack itemStack = recipe.getInitializedStack();
		MutableComponent displayName = itemStack.getHoverName().copy();
		if (itemStack.getCount() > 1) {
			displayName.append(Component.literal(" x " + itemStack.getCount()));
		}

		int offsetX = this.itemDrawable.getWidth() >> 1;
		guiGraphics.drawCenteredString(font, displayName, offsetX, 4, -256);
		PoseStack poseStack = RenderSystem.getModelViewStack();
		float fullTick = (float)mc.player.tickCount + mc.getFrameTime();
		float yOffset = Mth.cos(fullTick * (float)Math.PI * 0.02F) * 2.0F;
		poseStack.pushPose();
		poseStack.mulPoseMatrix(guiGraphics.pose().last().pose());
		poseStack.translate(90.0F, 55.0F + yOffset, 1000.0F);
		poseStack.mulPose(
			(new Quaternionf()).rotationXYZ(-0.34906584F, fullTick * ((float)Math.PI / 180F) * 2.0F, 0.0F));
		poseStack.scale(60.0F, -60.0F, 60.0F);
		RenderSystem.applyModelViewMatrix();
		BakedModel model = mc.getItemRenderer().getModel(
			itemStack,
			MiscUtil.getLevel(mc.player),
			mc.player,
			mc.player.getId() + ItemDisplayContext.GROUND.ordinal());
		Lighting.setupFor3DItems();
		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
		mc.getItemRenderer().render(
			itemStack,
			ItemDisplayContext.GROUND,
			false,
			new PoseStack(),
			buffer,
			15728880,
			OverlayTexture.NO_OVERLAY,
			model);
		buffer.endBatch();
		poseStack.popPose();
		RenderSystem.applyModelViewMatrix();
	}
}
