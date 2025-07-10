package mod.pbj.item;

import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.entity.ProjectileLike;

public record EffectBuilderInfo(
	Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier, Predicate<ProjectileLike> predicate) {
	public Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier() {
		return this.effectSupplier;
	}

	public Predicate<ProjectileLike> predicate() {
		return this.predicate;
	}
}
