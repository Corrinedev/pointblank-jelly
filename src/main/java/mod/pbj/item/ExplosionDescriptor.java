package mod.pbj.item;

import java.util.List;
import java.util.function.Supplier;
import mod.pbj.client.effect.EffectBuilder;
import net.minecraft.world.level.Level.ExplosionInteraction;

public record ExplosionDescriptor(
	float power,
	boolean fire,
	ExplosionInteraction interaction,
	String soundName,
	float soundVolume,
	List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> effects) {
	public float power() {
		return this.power;
	}

	public boolean fire() {
		return this.fire;
	}

	public ExplosionInteraction interaction() {
		return this.interaction;
	}

	public String soundName() {
		return this.soundName;
	}

	public float soundVolume() {
		return this.soundVolume;
	}

	public List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> effects() {
		return this.effects;
	}
}
