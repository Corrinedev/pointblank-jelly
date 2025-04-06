package com.vicmatskiv.pointblank.item;

import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.Level.ExplosionInteraction;

public record ExplosionDescriptor(float power, boolean fire, ExplosionInteraction interaction, String soundName, float soundVolume, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> effects) {
   public ExplosionDescriptor(float power, boolean fire, ExplosionInteraction interaction, String soundName, float soundVolume, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> effects) {
      this.power = power;
      this.fire = fire;
      this.interaction = interaction;
      this.soundName = soundName;
      this.soundVolume = soundVolume;
      this.effects = effects;
   }

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
