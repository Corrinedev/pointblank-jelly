package mod.pbj.mixin;

import mod.pbj.entity.EntityExt;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = {Entity.class})
public class EntityMixin implements EntityExt {
	@Unique private long lastHitSoundTimestamp;

	public long getLastHitSoundTimestamp() {
		return this.lastHitSoundTimestamp;
	}

	public void setLastHitSoundTimestamp(long lastHitSoundTimestamp) {
		this.lastHitSoundTimestamp = lastHitSoundTimestamp;
	}
}
