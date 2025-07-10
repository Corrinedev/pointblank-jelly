//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.util;

import java.util.function.Predicate;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public final class CancellableSound extends AbstractTickableSoundInstance {
	private Predicate<CancellableSound> predicate;
	private final Player player;

	public CancellableSound(
		Player player,
		SoundEvent soundEvent,
		SoundSource soundSource,
		RandomSource randomSource,
		Predicate<CancellableSound> predicate) {
		super(soundEvent, soundSource, randomSource);
		this.player = player;
		this.predicate = predicate;
	}

	public void tick() {
		if (!this.predicate.test(this)) {
			this.stop();
		}
	}

	public double getX() {
		return this.player.getX();
	}

	public double getY() {
		return this.player.getY();
	}

	public double getZ() {
		return this.player.getZ();
	}
}
