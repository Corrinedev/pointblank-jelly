package mod.pbj.block;

import javax.annotation.Nullable;
import mod.pbj.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class WorkstationBlock extends BaseEntityBlock implements EntityBlock {
	public static final DirectionProperty FACING;

	public WorkstationBlock() {
		super(Properties.of().strength(2.0F).noOcclusion());
	}

	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(
			FACING, context.getHorizontalDirection().getClockWise().getClockWise());
	}

	@Nullable
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return (BlockEntityRegistry.WORKSTATION_BLOCK_ENTITY.get()).create(blockPos, blockState);
	}

	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		return true;
	}

	static {
		FACING = BlockStateProperties.FACING;
	}
}
