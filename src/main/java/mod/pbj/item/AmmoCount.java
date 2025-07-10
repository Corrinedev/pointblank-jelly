package mod.pbj.item;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.level.ItemLike;

public class AmmoCount {
	private Map<ItemLike, AtomicInteger> ammoByType = new HashMap<>();

	private AtomicInteger get(FireModeInstance fireModeInstance) {
		return (AtomicInteger)this.ammoByType.computeIfAbsent(
			fireModeInstance.getAmmo(), (ati) -> { return new AtomicInteger(); });
	}

	public int incrementAmmoCount(FireModeInstance fireMode, int delta) {
		return this.get(fireMode).addAndGet(delta);
	}

	public void setAmmoCount(FireModeInstance fireMode, int mainAmmoCount) {
		this.get(fireMode).set(mainAmmoCount);
	}

	public int getAmmoCount(FireModeInstance fireMode) {
		return this.get(fireMode).get();
	}
}
