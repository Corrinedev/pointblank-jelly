package mod.pbj.event;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.eventbus.api.Event;

public class BlockHitEvent extends Event {
	private LivingEntity player;
	private BlockHitResult blockHitResult;
	@Nullable private Entity projectile;

	public BlockHitEvent(LivingEntity player, BlockHitResult blockHitResult, Entity projectile) {
		this.player = player;
		this.blockHitResult = blockHitResult;
		this.projectile = projectile;
	}

	public boolean isCancelable() {
		return true;
	}

	public LivingEntity getPlayer() {
		return this.player;
	}

	public BlockHitResult getBlockHitResult() {
		return this.blockHitResult;
	}

	public Entity getProjectile() {
		return this.projectile;
	}
}
