package mod.pbj.block;

import javax.annotation.Nullable;
import mod.pbj.block.entity.PrinterBlockEntity;
import mod.pbj.block.entity.PrinterBlockEntity.State;
import mod.pbj.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class PrinterBlock extends BaseEntityBlock implements EntityBlock {
	public static final DirectionProperty FACING;

	public PrinterBlock() {
		super(Properties.of()
				  .mapColor(MapColor.METAL)
				  .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
				  .requiresCorrectToolForDrops()
				  .strength(1.0F, 2.0F)
				  .sound(SoundType.METAL)
				  .noOcclusion());
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
		return (BlockEntityRegistry.PRINTER_BLOCK_ENTITY.get()).create(blockPos, blockState);
	}

	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		return true;
	}

	public InteractionResult
	use(BlockState blockState,
		Level level,
		BlockPos blockPos,
		Player player,
		InteractionHand interationHand,
		BlockHitResult blockHitResult) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		} else {
			BlockEntity blockEntity = level.getBlockEntity(blockPos);
			if (blockEntity instanceof PrinterBlockEntity wbe) {
				if (wbe.getState() == State.IDLE) {
					player.openMenu(blockState.getMenuProvider(level, blockPos));
					player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
					return InteractionResult.CONSUME;
				}
			}

			return InteractionResult.FAIL;
		}
	}

	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T>
	getTicker(Level level, BlockState blockState, BlockEntityType<T> entityType) {
		return createTickerHelper(
			entityType,
			BlockEntityRegistry.PRINTER_BLOCK_ENTITY.get(),
			level.isClientSide ? PrinterBlockEntity::clientTick : PrinterBlockEntity::serverTick);
	}

	static {
		FACING = BlockStateProperties.FACING;
	}
}
