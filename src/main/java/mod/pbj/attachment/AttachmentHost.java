package mod.pbj.attachment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import mod.pbj.Nameable;
import mod.pbj.feature.FeatureProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public interface AttachmentHost extends ItemLike, Nameable, FeatureProvider {
	int DEFAULT_MAX_ATTACHMENT_CATEGORIES = 6;

	Collection<Attachment> getCompatibleAttachments();

	default Collection<Attachment> getDefaultAttachments() {
		return Collections.emptyList();
	}

	default Collection<AttachmentCategory> getCompatibleAttachmentCategories() {
		return this.getCompatibleAttachments().stream().map(Attachment::getCategory).toList();
	}

	default int getMaxAttachmentCategories() {
		return 6;
	}

	default List<Component> getCompatibleAttachmentTooltipLines(ItemStack itemStack) {
		List<Component> tooltipLines = new ArrayList<>();
		tooltipLines.add(Component.empty());
		Collection<Attachment> compatibleAttachments = this.getCompatibleAttachments();
		if (compatibleAttachments.isEmpty()) {
			tooltipLines.add(Component.translatable("label.pointblank.noCompatibleAttachments")
								 .setStyle(Style.EMPTY.withColor(11184810).withItalic(true).withUnderlined(false)));
		} else {
			tooltipLines.add(Component.translatable("label.pointblank.compatibleAttachments")
								 .append(":")
								 .withStyle(ChatFormatting.WHITE)
								 .withStyle(ChatFormatting.ITALIC));

			for (Attachment attachment : compatibleAttachments) {
				tooltipLines.add(Component.translatable(attachment.asItem().getDescriptionId())
									 .withStyle(ChatFormatting.AQUA)
									 .withStyle(ChatFormatting.ITALIC));
			}
		}

		return tooltipLines;
	}
}
