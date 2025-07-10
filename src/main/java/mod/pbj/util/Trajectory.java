package mod.pbj.util;

import net.minecraft.world.phys.Vec3;

public interface Trajectory<P extends Enum<P>> {
	Vec3 getStartOfTickPosition();

	Vec3 getEndOfTickPosition();

	Vec3 getDeltaMovement();

	void setTargetPosition(Vec3 var1);

	void tick();

	boolean isCompleted();

	default void addListener(TrajectoryPhaseListener<P> listener) {}
}
