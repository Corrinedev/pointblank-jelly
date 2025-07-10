package mod.pbj.util;

import java.util.Optional;
import java.util.UUID;
import mod.pbj.item.GunItem;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.joml.Quaternionf;

public class MiscUtil {
	private static final double EPSILON = 1.0E-8;
	public static final FriendlyByteBuf.Writer<Vec3> VEC3_WRITER = (buf, vec3) -> {
		buf.writeDouble(vec3.x);
		buf.writeDouble(vec3.y);
		buf.writeDouble(vec3.z);
	};
	public static final FriendlyByteBuf.Reader<Vec3> VEC3_READER =
		(buf) -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());

	public MiscUtil() {}

	public static boolean isNearlyZero(double value) {
		return Math.abs(value) < 1.0E-8;
	}

	public static boolean isGreaterThanZero(double value) {
		return value > 1.0E-8;
	}

	public static Level getLevel(Entity entity) {
		return entity.level();
	}

	public static boolean isClientSide(Entity entity) {
		return entity.level().isClientSide;
	}

	public LivingEntity asLivingEntity(Entity entity) {
		if (entity instanceof LivingEntity livingEntity) {
			return livingEntity;
		} else {
			if (entity instanceof PartEntity entityPart) {
				Entity var5 = entityPart.getParent();
				if (var5 instanceof LivingEntity livingEntity) {
					return livingEntity;
				}
			}

			return null;
		}
	}

	public static boolean isProtected(Entity entity) {
		return entity instanceof Cat || entity instanceof Ocelot;
	}

	public static Optional<GunItem> getMainHeldGun(LivingEntity entity) {
		ItemStack itemStack = entity.getMainHandItem();
		return itemStack != null && itemStack.getItem() instanceof GunItem ? Optional.of((GunItem)itemStack.getItem())
																		   : Optional.empty();
	}

	public static Quaternionf getRotation(Direction face) {
		Quaternionf quaternionf = null;
		switch (face) {
			case DOWN -> quaternionf = (new Quaternionf()).rotationXYZ(((float)Math.PI / 2F), 0.0F, 0.0F);
			case UP -> {
			}
			case NORTH, SOUTH -> quaternionf = (new Quaternionf()).rotationXYZ(0.0F, 0.0F, ((float)Math.PI / 2F));
			case WEST, EAST -> quaternionf = (new Quaternionf()).rotationXYZ(0.0F, ((float)Math.PI / 2F), 0.0F);
			default -> throw new IncompatibleClassChangeError();
		}

		return quaternionf;
	}

	public static double timeToTravel(double initialSpeed, double acceleration, double distance) {
		if (acceleration == (double)0.0F) {
			return distance / initialSpeed;
		} else {
			double a = (double)0.5F * acceleration;
			double c = -distance;
			double discriminant = initialSpeed * initialSpeed - (double)4.0F * a * c;
			return discriminant < (double)0.0F ? (double)-1.0F
											   : (-initialSpeed + Math.sqrt(discriminant)) / ((double)2.0F * a);
		}
	}

	public static double adjustDivisor(double dividend, double divisor) {
		if (divisor == (double)0.0F) {
			throw new IllegalArgumentException("Divisor cannot be zero.");
		} else {
			double quotient = dividend / divisor;
			long roundedQuotient = Math.round(quotient);
			return dividend / (double)roundedQuotient;
		}
	}

	public static UUID getTagId(CompoundTag tag) {
		return tag != null ? new UUID(tag.getLong("mid"), tag.getLong("lid")) : null;
	}

	public static UUID getItemStackId(ItemStack itemStack) {
		CompoundTag idTag = itemStack.getTag();
		return getTagId(idTag);
	}
}
