package mod.pbj.client.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class BaseBlockModel<T extends GeoAnimatable> extends DefaultedBlockGeoModel<T> {
	private final Map<String, Predicate<BlockEntity>> glowingParts;

	private BaseBlockModel(ResourceLocation resource, Map<String, Predicate<BlockEntity>> glowingParts) {
		super(resource);
		this.glowingParts = Collections.unmodifiableMap(glowingParts);
	}

	public RenderType getRenderType(T animatable, ResourceLocation texture) {
		return RenderType.entityTranslucent(this.getTextureResource(animatable));
	}

	public Map<String, Predicate<BlockEntity>> getGlowingParts() {
		return this.glowingParts;
	}

	public static class Builder<T extends GeoAnimatable> {
		private ResourceLocation resource;
		private final Map<String, Predicate<BlockEntity>> glowingParts = new HashMap<>();

		public Builder<T> withResource(ResourceLocation resource) {
			this.resource = resource;
			return this;
		}

		public Builder<T> withGlow(String glowingPartName) {
			return this.withGlow(glowingPartName, (x) -> true);
		}

		public Builder<T> withGlow(String glowingPartName, Predicate<BlockEntity> predicate) {
			this.glowingParts.put(glowingPartName, predicate);
			return this;
		}

		public BaseBlockModel<T> build() {
			if (this.resource == null) {
				throw new IllegalStateException("Model resource not set");
			} else {
				return new BaseBlockModel<>(this.resource, this.glowingParts);
			}
		}
	}

	public record GlowingPart(String partName, Predicate<?> predicate) {
		public String partName() {
			return this.partName;
		}

		public Predicate<?> predicate() {
			return this.predicate;
		}
	}
}
