//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.util;

import java.util.LinkedList;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

///*
/// Credit to TACZ for this utility, borrowed with permission from MaydayMemory.
///

public final class HitboxHelper {
	private static final WeakHashMap<Player, LinkedList<Vec3>> PLAYER_POSITION = new WeakHashMap<>();
	private static final WeakHashMap<Player, LinkedList<AABB>> PLAYER_HITBOXES = new WeakHashMap<>();
	private static final WeakHashMap<Player, LinkedList<Vec3>> PLAYER_VELOCITY = new WeakHashMap<>();
	private static final int SAVE_TICK;

	public HitboxHelper() {}

	public static void onPlayerTick(Player player) {
		if (player.isSpectator()) {
			PLAYER_POSITION.remove(player);
			PLAYER_HITBOXES.remove(player);
			PLAYER_VELOCITY.remove(player);
		} else {
			LinkedList<Vec3> positions = PLAYER_POSITION.computeIfAbsent(player, (p) -> new LinkedList<>());
			LinkedList<AABB> boxes = PLAYER_HITBOXES.computeIfAbsent(player, (p) -> new LinkedList<>());
			LinkedList<Vec3> velocities = PLAYER_VELOCITY.computeIfAbsent(player, (p) -> new LinkedList<>());
			positions.addFirst(player.position());
			boxes.addFirst(player.getBoundingBox());
			velocities.addFirst(getPlayerVelocity(player));
			if (positions.size() > 2) {
				positions.removeLast();
			}

			if (boxes.size() > SAVE_TICK) {
				boxes.removeLast();
				velocities.removeLast();
			}
		}
	}

	public static void onPlayerLoggedOut(Player player) {
		PLAYER_POSITION.remove(player);
		PLAYER_HITBOXES.remove(player);
		PLAYER_VELOCITY.remove(player);
	}

	public static Vec3 getPlayerVelocity(Player entity) {
		LinkedList<Vec3> positions = PLAYER_POSITION.computeIfAbsent(entity, (player) -> new LinkedList<>());
		if (positions.size() > 1) {
			Vec3 currPos = positions.getFirst();
			Vec3 prevPos = positions.getLast();
			return new Vec3(currPos.x - prevPos.x, currPos.y - prevPos.y, currPos.z - prevPos.z);
		} else {
			return new Vec3(0.0, 0.0, 0.0);
		}
	}

	public static AABB getBoundingBox(Player entity, int ping) {
		if (PLAYER_HITBOXES.containsKey(entity)) {
			LinkedList<AABB> boxes = PLAYER_HITBOXES.get(entity);
			int index = Mth.clamp(ping, 0, boxes.size() - 1);
			return boxes.get(index);
		} else {
			return entity.getBoundingBox();
		}
	}

	public static Vec3 getVelocity(Player entity, int ping) {
		if (PLAYER_VELOCITY.containsKey(entity)) {
			LinkedList<Vec3> velocities = PLAYER_VELOCITY.get(entity);
			int index = Mth.clamp(ping, 0, velocities.size() - 1);
			return velocities.get(index);
		} else {
			return getPlayerVelocity(entity);
		}
	}

	public static AABB getFixedBoundingBox(Entity entity, Entity owner) {
		AABB boundingBox = entity.getBoundingBox();
		Vec3 velocity = new Vec3(entity.getX() - entity.xOld, entity.getY() - entity.yOld, entity.getZ() - entity.zOld);
		if (true && entity instanceof ServerPlayer player) {
			if (owner instanceof ServerPlayer serverPlayerOwner) {
				int ping = Mth.floor((double)serverPlayerOwner.latency / 1000.0 * 20.0 + 0.5);
				boundingBox = getBoundingBox(player, ping);
				velocity = getVelocity(player, ping);
			}
		}

		double expandHeight = entity instanceof Player && !entity.isCrouching() ? 0.0625 : 0.0;
		boundingBox = boundingBox.expandTowards(0.0, expandHeight, 0.0);
		boundingBox = boundingBox.expandTowards(velocity.x, velocity.y, velocity.z);
		double playerHitboxOffset = 3.0;
		if (entity instanceof ServerPlayer) {
			if (entity.getVehicle() != null) {
				boundingBox = boundingBox.move(
					velocity.multiply(playerHitboxOffset / 2.0, playerHitboxOffset / 2.0, playerHitboxOffset / 2.0));
			}

			boundingBox =
				boundingBox.move(velocity.multiply(playerHitboxOffset, playerHitboxOffset, playerHitboxOffset));
		}

		if (entity.getVehicle() != null) {
			boundingBox = boundingBox.move(velocity.multiply(-2.5, -2.5, -2.5));
		}

		boundingBox = boundingBox.move(velocity.multiply(-5.0, -5.0, -5.0));
		return boundingBox;
	}

	static {
		SAVE_TICK = Mth.floor(20.0 + 0.5);
	}
}
