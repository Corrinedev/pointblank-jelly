package mod.pbj.block.entity;

import mod.pbj.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WorkstationBlockEntity extends BlockEntity implements GeoBlockEntity {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public WorkstationBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityRegistry.WORKSTATION_BLOCK_ENTITY.get(), pos, state);
	}

	public void registerControllers(ControllerRegistrar controllers) {}

	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
