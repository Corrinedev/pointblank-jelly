package mod.pbj.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface TrajectoryProvider {
	Trajectory<?> createTrajectory(Level var1, Vec3 var2, Vec3 var3);
}
