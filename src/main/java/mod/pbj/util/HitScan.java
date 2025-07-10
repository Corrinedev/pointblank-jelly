package mod.pbj.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HitScan {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");

	public HitScan() {}

	public static List<HitResult> getObjectsInCrosshair1(
		LivingEntity player,
		float partialTicks,
		double maxDistance,
		int count,
		double inaccuracy,
		long seed,
		Predicate<Block> isBreakableBlock,
		Predicate<Block> isPassThroughBlock,
		List<BlockPos> blockPosToBreakOutput) {
		Vec3 startPos = player.getEyePosition();
		Vec3 direction = player.getViewVector(partialTicks);
		return getObjectsInCrosshair(
			player,
			startPos,
			direction,
			partialTicks,
			maxDistance,
			count,
			inaccuracy,
			seed,
			isBreakableBlock,
			isPassThroughBlock,
			blockPosToBreakOutput);
	}

	public static List<HitResult> getObjectsInCrosshair(
		LivingEntity player,
		Vec3 startPos,
		Vec3 direction,
		float partialTicks,
		double maxDistance,
		int count,
		double inaccuracy,
		long seed,
		Predicate<Block> isBreakableBlock,
		Predicate<Block> isPassThroughBlock,
		List<BlockPos> blockPosToBreakOutput) {
		List<HitResult> hitResults = new ArrayList<>();
		Random random = new Random(seed);

		for (int i = 0; i < count; ++i) {
			long startTime = System.currentTimeMillis();
			long iterationSeed = seed ^ random.nextLong();
			Vec3 deviatedLookVec =
				getDeviatedDirectionVector(player, direction, partialTicks, inaccuracy, iterationSeed);
			HitResult hitResult = getNearestObjectInCrosshair(
				player,
				startPos,
				deviatedLookVec,
				partialTicks,
				maxDistance,
				isBreakableBlock,
				isPassThroughBlock,
				blockPosToBreakOutput);
			if (hitResult != null) {
				hitResults.add(hitResult);
			}

			long endTime = System.currentTimeMillis();
			LOGGER.debug("{} - obtained hit result in {}ms", System.currentTimeMillis() % 100000L, endTime - startTime);
		}

		return hitResults;
	}

	public static HitResult ensureEntityInCrosshair(
		LivingEntity player, Entity targetEntity, float partialTicks, double maxDistance, float bbExpansion) {
		Vec3 direction = player.getViewVector(partialTicks);
		Vec3 startPos = player.getEyePosition();
		return ensureEntityInCrosshair(
			player, targetEntity, startPos, direction, partialTicks, maxDistance, bbExpansion);
	}

	public static HitResult getNearestObjectInCrosshair(
		LivingEntity player,
		float partialTicks,
		double maxDistance,
		double inaccuracy,
		long seed,
		Predicate<Block> isBreakableBlock,
		Predicate<Block> isPassThroughBlock,
		List<BlockPos> blockPosToBreakOutput) {
		Vec3 direction = player.getViewVector(partialTicks);
		Vec3 deviatedLookVec = getDeviatedDirectionVector(player, direction, partialTicks, inaccuracy, seed);
		Vec3 startPos = player.getEyePosition();
		return getNearestObjectInCrosshair(
			player,
			startPos,
			deviatedLookVec,
			partialTicks,
			maxDistance,
			isBreakableBlock,
			isPassThroughBlock,
			blockPosToBreakOutput);
	}

	public static HitResult getNearestObjectInCrosshair(
		LivingEntity player,
		float partialTicks,
		double maxDistance,
		Predicate<Block> isBreakableBlock,
		Predicate<Block> isPassThroughBlock,
		List<BlockPos> blockPosToBreakOutput) {
		Vec3 direction = player.getViewVector(partialTicks);
		Vec3 startPos = player.getEyePosition();
		return getNearestObjectInCrosshair(
			player,
			startPos,
			direction,
			partialTicks,
			maxDistance,
			isBreakableBlock,
			isPassThroughBlock,
			blockPosToBreakOutput);
	}

	private static Vec3
	getDeviatedDirectionVector(LivingEntity player, Vec3 dirVector, float partialTicks, double inaccuracy, long seed) {
		Random random = new Random(seed);
		double deviationX = (random.nextDouble() - (double)0.5F) * (double)2.0F * inaccuracy;
		double deviationY = (random.nextDouble() - (double)0.5F) * (double)2.0F * inaccuracy;
		double deviationZ = (random.nextDouble() - (double)0.5F) * (double)2.0F * inaccuracy;
		return dirVector.add(deviationX, deviationY, deviationZ).normalize();
	}

	public static HitResult getNearestObjectInCrosshair(
		LivingEntity player,
		Vec3 startPos,
		Vec3 directionVector,
		float partialTicks,
		double maxDistance,
		Predicate<Block> isBreakableBlock,
		Predicate<Block> isPassThroughBlock,
		List<BlockPos> blockPosToBreakOutput) {
		Vec3 endVec = startPos.add(
			directionVector.x * maxDistance, directionVector.y * maxDistance, directionVector.z * maxDistance);
		AABB playerBox = player.getBoundingBox();
		AABB expandedBox = playerBox.expandTowards(
			directionVector.x * maxDistance, directionVector.y * maxDistance, directionVector.z * maxDistance);
		Entity closestEntity = null;
		double closestEntityDistance = maxDistance;
		Vec3 closestEntityHitVec = null;

		for (Entity entity : MiscUtil.getLevel(player).getEntities(player, expandedBox)) {
			if (!entity.isSpectator() && entity.isPickable() && entity.isAlive()) {
				AABB entityBox = entity.getBoundingBox().inflate(0.3);
				Optional<Vec3> hitVec = entityBox.clip(startPos, endVec);
				if (hitVec.isPresent()) {
					double distanceToEntity = startPos.distanceTo(hitVec.get());
					if (distanceToEntity < closestEntityDistance) {
						closestEntity = entity;
						closestEntityDistance = distanceToEntity;
						closestEntityHitVec = hitVec.get();
					}
				}
			}
		}

		BlockHitResult blockHit = MiscUtil.getLevel(player).clip(new ClipContext(
			startPos, endVec, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, player));

		while (blockHit.getType() != Type.MISS) {
			BlockPos blockPos = blockHit.getBlockPos();
			BlockState blockState = MiscUtil.getLevel(player).getBlockState(blockPos);
			Block block = blockState.getBlock();
			if (!isPassThroughBlock.test(block)) {
				if (!isBreakableBlock.test(block)) {
					double blockDistance = startPos.distanceTo(blockHit.getLocation());
					if (blockDistance < closestEntityDistance) {
						return blockHit;
					}
					break;
				}

				blockPosToBreakOutput.add(blockPos);
			}
			blockHit = MiscUtil.getLevel(player).clip(
				new ClipContext(blockHit.getLocation(), endVec, ClipContext.Block.COLLIDER, Fluid.NONE, player));
		}

		return closestEntity != null ? new EntityHitResult(closestEntity, closestEntityHitVec) : blockHit;
	}

	public static boolean isHeadshot(LivingEntity entity, Vec3 hitVec) {
		AABB entityBox = entity.getBoundingBox();
		double headHeightStart = entityBox.minY + (double)entity.getEyeHeight() - (double)entity.getBbHeight() * 0.12;
		double hOffset = 0.301;
		double babyVOffset = entity.isBaby() ? (double)0.5F : (double)0.0F;
		AABB headBox = new AABB(
			entityBox.minX - hOffset,
			headHeightStart,
			entityBox.minZ - hOffset,
			entityBox.maxX + hOffset,
			entityBox.maxY + (double)entity.getBbHeight() * 0.1 + babyVOffset,
			entityBox.maxZ + hOffset);
		return headBox.contains(hitVec);
	}

	protected static HitResult ensureEntityInCrosshair(
		LivingEntity player,
		Entity targetEntity,
		Vec3 startPos,
		Vec3 directionVector,
		float partialTicks,
		double maxDistance,
		float bbExpansion) {
		Vec3 endVec = startPos.add(
			directionVector.x * maxDistance, directionVector.y * maxDistance, directionVector.z * maxDistance);
		AABB playerBox = player.getBoundingBox();
		AABB expandedBox = playerBox.expandTowards(
			directionVector.x * maxDistance, directionVector.y * maxDistance, directionVector.z * maxDistance);
		Vec3 closestEntityHitVec = null;

		for (Entity entity : MiscUtil.getLevel(player).getEntities(player, expandedBox)) {
			if (entity == targetEntity && !entity.isSpectator() && entity.isPickable() && entity.isAlive()) {
				AABB entityBox = entity.getBoundingBox().inflate(bbExpansion);
				Optional<Vec3> hitVec = entityBox.clip(startPos, endVec);
				if (hitVec.isPresent()) {
					closestEntityHitVec = hitVec.get();
				}
			}
		}

		return closestEntityHitVec != null ? new EntityHitResult(targetEntity, closestEntityHitVec) : null;
	}
}
