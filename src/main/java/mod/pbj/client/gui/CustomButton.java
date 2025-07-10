package mod.pbj.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import mod.pbj.util.Interpolators;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomButton extends AbstractButton {
	public static final ResourceLocation BUTTON_RESOURCE =
		new ResourceLocation("pointblank", "textures/gui/buttons.png");
	public static final int SMALL_WIDTH = 120;
	public static final int DEFAULT_WIDTH = 150;
	public static final int DEFAULT_HEIGHT = 20;
	protected static final CreateNarration DEFAULT_NARRATION = Supplier::get;
	private final Event onPress;
	private final Event onRelease;
	protected final CreateNarration createNarration;
	private boolean isPressed;
	private final Interpolators.FloatProvider progressProvider;

	public static Builder builder(Component title, Event onPress) {
		return new Builder(title, onPress);
	}

	protected CustomButton(
		int x,
		int y,
		int width,
		int height,
		Component title,
		Event onPress,
		Event onRelease,
		CreateNarration narration,
		Interpolators.FloatProvider progressProvider) {
		super(x, y, width, height, title);
		this.onPress = onPress;
		this.onRelease = onRelease;
		this.createNarration = narration;
		this.progressProvider = progressProvider;
	}

	protected CustomButton(Builder builder) {
		this(
			builder.x,
			builder.y,
			builder.width,
			builder.height,
			builder.message,
			builder.onPress,
			builder.onRelease,
			builder.createNarration,
			builder.progressProvider);
		this.setTooltip(builder.tooltip);
	}

	public boolean isPressed() {
		return this.isPressed;
	}

	public void onPress() {
		this.onPress.handle(this);
	}

	public void onClick(double mouseX, double mouseY) {
		this.isPressed = true;
		super.onClick(mouseX, mouseY);
	}

	public void onRelease(double mouseX, double mouseY) {
		super.onRelease(mouseX, mouseY);
		this.release();
	}

	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		int textureWidth = 48;
		int textureHeight = 20;
		int vOffset = 32;
		if (!this.active) {
			vOffset += 40;
		} else if (this.isPressed) {
			vOffset += 20;
		} else if (this.isHoveredOrFocused()) {
			vOffset += 60;
		}

		guiGraphics.blitNineSliced(
			BUTTON_RESOURCE,
			this.getX(),
			this.getY(),
			this.getWidth(),
			this.getHeight(),
			20,
			4,
			textureWidth,
			textureHeight,
			0,
			vOffset);
		if (this.progressProvider != null) {
			float progress = this.progressProvider.getValue();
			int height = this.getHeight() - 2;
			guiGraphics.fill(
				this.getX() + 1,
				this.getY() + 1 + height - (int)((float)height * progress),
				this.getX() + this.getWidth() - 1,
				this.getY() + 1 + height,
				-2147434496);
		}

		guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		int i = this.getFGColor();
		renderScrollingString(
			guiGraphics,
			minecraft.font,
			this.getMessage(),
			this.getX() + 8,
			this.getY(),
			this.getX() + this.getWidth() - 8,
			this.getY() + this.getHeight(),
			i | Mth.ceil(this.alpha * 255.0F) << 24);
	}

	public void renderString(GuiGraphics guiGraphics, Font font, int color) {
		this.renderScrollingString(guiGraphics, font, 2, color);
	}

	public int getFGColor() {
		return super.getFGColor();
	}

	protected MutableComponent createNarrationMessage() {
		return this.createNarration.createNarrationMessage(super::createNarrationMessage);
	}

	public void updateWidgetNarration(NarrationElementOutput p_259196_) {
		this.defaultButtonNarrationText(p_259196_);
	}

	public void release() {
		if (this.isPressed && this.onRelease != null) {
			this.onRelease.handle(this);
		}

		this.isPressed = false;
	}

	@OnlyIn(Dist.CLIENT)
	public static class Builder {
		private final Component message;
		private final Event onPress;
		private Event onRelease;
		private Interpolators.FloatProvider progressProvider;
		@Nullable private Tooltip tooltip;
		private int x;
		private int y;
		private int width = 150;
		private int height = 20;
		private CreateNarration createNarration;

		public Builder(Component p_254097_, Event onPress) {
			this.createNarration = CustomButton.DEFAULT_NARRATION;
			this.message = p_254097_;
			this.onPress = onPress;
		}

		public Builder progressProvider(Interpolators.FloatProvider progressProvider) {
			this.progressProvider = progressProvider;
			return this;
		}

		public Builder onRelease(Event onRelease) {
			this.onRelease = onRelease;
			return this;
		}

		public Builder pos(int posX, int posY) {
			this.x = posX;
			this.y = posY;
			return this;
		}

		public Builder width(int width) {
			this.width = width;
			return this;
		}

		public Builder size(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public Builder bounds(int posX, int posY, int width, int height) {
			return this.pos(posX, posY).size(width, height);
		}

		public Builder tooltip(@Nullable Tooltip tooltip) {
			this.tooltip = tooltip;
			return this;
		}

		public Builder createNarration(CreateNarration p_253638_) {
			this.createNarration = p_253638_;
			return this;
		}

		public CustomButton build() {
			return this.build(CustomButton::new);
		}

		public CustomButton build(Function<Builder, CustomButton> builder) {
			return builder.apply(this);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public interface CreateNarration {
		MutableComponent createNarrationMessage(Supplier<MutableComponent> var1);
	}

	@OnlyIn(Dist.CLIENT)
	public interface Event {
		void handle(CustomButton var1);
	}
}
