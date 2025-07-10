package mod.pbj.util;

import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SimpleHitResult extends HitResult {
	private final HitResult.Type type;
	private final Direction direction;
	private final int entityId;

	public SimpleHitResult(Vec3 location, HitResult.Type type, Direction direction, int entityId) {
		super(location);
		this.type = type;
		this.direction = direction;
		this.entityId = entityId;
	}

	public HitResult.Type getType() {
		return this.type;
	}

	public Direction getDirection() {
		return this.direction;
	}

	public int getEntityId() {
		return this.entityId;
	}

	public static SimpleHitResult fromHitResult(HitResult hitResult) {
		if (hitResult == null) {
			return null;
		} else if (hitResult instanceof SimpleHitResult simpleHitResult) {
			return simpleHitResult;
		} else {
			SimpleHitResult simpleHitResult = null;
			switch (hitResult.getType()) {
				case BLOCK ->
					simpleHitResult = new SimpleHitResult(
						hitResult.getLocation(), Type.BLOCK, ((BlockHitResult)hitResult).getDirection(), -1);
				case ENTITY ->
					simpleHitResult = new SimpleHitResult(
						hitResult.getLocation(), Type.ENTITY, null, ((EntityHitResult)hitResult).getEntity().getId());
				case MISS -> simpleHitResult = new SimpleHitResult(hitResult.getLocation(), Type.MISS, null, -1);
			}

			return simpleHitResult;
		}
	}

	public static FriendlyByteBuf.Writer<SimpleHitResult> writer() {
		return (buf, hitResult) -> {
			buf.writeEnum(hitResult.type);
			buf.writeDouble(hitResult.location.x);
			buf.writeDouble(hitResult.location.y);
			buf.writeDouble(hitResult.location.z);
			buf.writeOptional(Optional.ofNullable(hitResult.direction), FriendlyByteBuf::writeEnum);
			buf.writeInt(hitResult.entityId);
		};
	}

	public static FriendlyByteBuf.Reader<SimpleHitResult> reader() {
		return (buf) -> {
			HitResult.Type type = buf.readEnum(Type.class);
			double x = buf.readDouble();
			double y = buf.readDouble();
			double z = buf.readDouble();
			Optional<Direction> direction = buf.readOptional((b) -> b.readEnum(Direction.class));
			int entityId = buf.readInt();
			return new SimpleHitResult(new Vec3(x, y, z), type, direction.orElse(null), entityId);
		};
	}
}
