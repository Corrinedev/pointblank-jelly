package mod.pbj.feature;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.item.GunItem;
import mod.pbj.registry.SoundRegistry;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import mod.pbj.util.MiscUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SoundFeature extends ConditionalFeature {
	private final List<Pair<SoundDescriptor, Predicate<ConditionContext>>> fireSounds;
	private @Nullable Script script;

	private SoundFeature(
		FeatureProvider owner,
		Predicate<ConditionContext> predicate,
		List<Pair<SoundDescriptor, Predicate<ConditionContext>>> fireSounds,
		Script script) {
		super(owner, predicate);
		this.fireSounds = Collections.unmodifiableList(fireSounds);
		this.script = script;
	}

	public static SoundDescriptor getFireSoundAndVolume(ItemStack itemStack) {
		for (Features.EnabledFeature enabledFeature : Features.getEnabledFeatures(itemStack, SoundFeature.class)) {
			SoundFeature soundFeature = (SoundFeature)enabledFeature.feature();
			ConditionContext context = new ConditionContext(itemStack);
			if (soundFeature.hasFunction("getSoundAndVolume")) {
				Pair<String, Float> func =
					(Pair<String, Float>)soundFeature.invokeFunction("getSoundAndVolume", itemStack, soundFeature);
				Supplier<SoundEvent> soundEventSupplier = () -> SoundRegistry.getSoundEvent(func.getFirst());
				return new SoundDescriptor(soundEventSupplier, func.getSecond());
			}
			for (Pair<SoundDescriptor, Predicate<ConditionContext>> fireSound : soundFeature.fireSounds) {
				if (fireSound.getSecond().test(context)) {
					return fireSound.getFirst();
				}
			}
		}

		return null;
	}

	public static void playFireSound(Player player, ItemStack itemStack) {
		SoundDescriptor fsv = getFireSoundAndVolume(itemStack);
		SoundEvent fireSound = null;
		float fireSoundVolume = 0.0F;
		if (fsv != null) {
			fireSound = fsv.soundSupplier.get();
			fireSoundVolume = fsv.volume;
		} else {
			Item var6 = itemStack.getItem();
			if (var6 instanceof GunItem gunItem) {
				fireSound = gunItem.getFireSound();
				fireSoundVolume = gunItem.getFireSoundVolume();
			}
		}

		if (fireSound != null && fireSoundVolume > 0.0F) {
			MiscUtil.getLevel(player).playSound(
				player,
				player.getX(),
				player.getY(),
				player.getZ(),
				fireSound,
				SoundSource.PLAYERS,
				fireSoundVolume,
				1.0F);
		}
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public record SoundDescriptor(Supplier<SoundEvent> soundSupplier, float volume) {
		public Supplier<SoundEvent> soundSupplier() {
			return this.soundSupplier;
		}

		public float volume() {
			return this.volume;
		}
	}

	public static class Builder implements FeatureBuilder<Builder, SoundFeature> {
		private static final float DEFAULT_FIRE_SOUND_VOLUME = 5.0F;
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private SoundDescriptor fireSoundDescriptor;
		private final List<Pair<SoundDescriptor, Predicate<ConditionContext>>> fireSounds = new ArrayList<>();
		private Script script;

		public Builder() {}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withFireSound(Supplier<SoundEvent> sound, double volume) {
			this.fireSoundDescriptor = new SoundDescriptor(sound, (float)volume);
			return this;
		}

		public Builder withFireSound(Supplier<SoundEvent> sound, double volume, Predicate<ConditionContext> condition) {
			this.fireSounds.add(Pair.of(new SoundDescriptor(sound, (float)volume), condition));
			return this;
		}

		public Builder withScript(Script script) {
			this.script = script;
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition")) {
				this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
			}

			this.withScript(JsonUtil.getJsonScript(obj));

			for (JsonObject fireSoundObj : JsonUtil.getJsonObjects(obj, "fireSounds")) {
				Predicate<ConditionContext> condition;
				if (fireSoundObj.has("condition")) {
					JsonObject conditionObj = fireSoundObj.getAsJsonObject("condition");
					condition = Conditions.fromJson(conditionObj);
				} else {
					condition = (ctx) -> true;
				}

				String fireSoundName = JsonUtil.getJsonString(fireSoundObj, "sound");
				float fireSoundVolume = JsonUtil.getJsonFloat(fireSoundObj, "volume", 5.0F);
				this.withFireSound(() -> SoundRegistry.getSoundEvent(fireSoundName), fireSoundVolume, condition);
			}

			if (!obj.has("fireSounds")) {
				String fireSoundName = JsonUtil.getJsonString(obj, "fireSound");
				float fireSoundVolume = JsonUtil.getJsonFloat(obj, "fireSoundVolume", 5.0F);
				this.withFireSound(() -> SoundRegistry.getSoundEvent(fireSoundName), fireSoundVolume);
			}

			return this;
		}

		public SoundFeature build(FeatureProvider featureProvider) {
			List<Pair<SoundDescriptor, Predicate<ConditionContext>>> fireSounds = new ArrayList<>(this.fireSounds);
			if (this.fireSoundDescriptor != null) {
				fireSounds.add(Pair.of(this.fireSoundDescriptor, this.condition));
			}

			return new SoundFeature(featureProvider, this.condition, fireSounds, this.script);
		}
	}
}
