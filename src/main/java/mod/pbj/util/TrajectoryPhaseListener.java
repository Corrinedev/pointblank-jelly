package mod.pbj.util;

import net.minecraft.world.phys.Vec3;

public interface TrajectoryPhaseListener<T extends Enum<T>> {
	void onStartPhase(T var1, Vec3 var2);
}
