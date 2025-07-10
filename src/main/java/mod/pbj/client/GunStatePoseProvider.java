package mod.pbj.client;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public class GunStatePoseProvider {
	private static final GunStatePoseProvider INSTANCE = new GunStatePoseProvider();
	private final Map<Key, Pose> poses = new HashMap<>();
	private final Map<Key, Vec3[]> positionsAndDirections = new HashMap<>();

	public static GunStatePoseProvider getInstance() {
		return INSTANCE;
	}

	private GunStatePoseProvider() {}

	public Pose getPose(GunClientState gunClientState, PoseContext poseContext) {
		return this.poses.get(new Key(gunClientState.getId(), poseContext));
	}

	public void setPose(GunClientState gunClientState, PoseContext poseContext, Pose pose) {
		this.poses.put(new Key(gunClientState.getId(), poseContext), pose);
	}

	public void clear(UUID id) {
		var it = this.poses.entrySet().iterator();

		Entry e;
		while (it.hasNext()) {
			e = it.next();
			if (Objects.equals(((Key)e.getKey()).id, id)) {
				it.remove();
			}
		}

		var it2 = this.positionsAndDirections.entrySet().iterator();

		while (it2.hasNext()) {
			e = it2.next();
			if (Objects.equals(((Key)e.getKey()).id, id)) {
				it2.remove();
			}
		}
	}

	public void
	setPositionAndDirection(GunClientState gunClientState, PoseContext poseContext, Vec3 pos, Vec3 direction) {
		this.positionsAndDirections.put(new Key(gunClientState.getId(), poseContext), new Vec3[] {pos, direction});
	}

	public Vec3 getPosition(GunClientState gunClientState, PoseContext poseContext) {
		Vec3[] positionAndDirection = this.positionsAndDirections.get(new Key(gunClientState.getId(), poseContext));
		return positionAndDirection != null ? positionAndDirection[0] : null;
	}

	public Vec3[] getPositionAndDirection(GunClientState gunClientState, PoseContext poseContext) {
		return this.positionsAndDirections.get(new Key(gunClientState.getId(), poseContext));
	}

	private static class Key {
		UUID id;
		PoseContext poseContext;

		Key(UUID id, PoseContext poseContext) {
			this.id = id;
			this.poseContext = poseContext;
		}

		public int hashCode() {
			return Objects.hash(this.id, this.poseContext);
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj == null) {
				return false;
			} else if (this.getClass() != obj.getClass()) {
				return false;
			} else {
				Key other = (Key)obj;
				return Objects.equals(this.id, other.id) && this.poseContext == other.poseContext;
			}
		}
	}

	public enum PoseContext {
		FIRST_PERSON_MUZZLE,
		FIRST_PERSON_MUZZLE_FLASH,
		THIRD_PERSON_MUZZLE,
		THIRD_PERSON_MUZZLE_FLASH;

		// $FF: synthetic method
		private static PoseContext[] $values() {
			return new PoseContext[] {
				FIRST_PERSON_MUZZLE, FIRST_PERSON_MUZZLE_FLASH, THIRD_PERSON_MUZZLE, THIRD_PERSON_MUZZLE_FLASH};
		}
	}
}
