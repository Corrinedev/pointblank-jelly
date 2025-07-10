package mod.pbj.util;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public record SoundInfo(Supplier<SoundEvent> soundEvent, float volume) {
	public SoundInfo(Supplier<SoundEvent> soundEvent, float volume) {
		this.soundEvent = soundEvent;
		this.volume = volume;
	}

	public Supplier<SoundEvent> soundEvent() {
		return this.soundEvent;
	}

	public float volume() {
		return this.volume;
	}
}
