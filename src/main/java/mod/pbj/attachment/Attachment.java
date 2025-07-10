package mod.pbj.attachment;

import java.util.Set;
import mod.pbj.Nameable;
import mod.pbj.feature.Feature;
import mod.pbj.feature.FeatureProvider;
import net.minecraft.world.level.ItemLike;

public interface Attachment extends Nameable, ItemLike, FeatureProvider {
	AttachmentCategory getCategory();

	Set<String> getGroups();

	default void applyFeature(Class<Feature> featureType, Object context) {}
}
