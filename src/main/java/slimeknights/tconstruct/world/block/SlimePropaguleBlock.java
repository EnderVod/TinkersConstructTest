package slimeknights.tconstruct.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.world.TinkerWorld;

/** Slime propagule using vanilla 1.21 mangrove behavior with Tinkers growth and soil rules. */
public class SlimePropaguleBlock extends MangrovePropaguleBlock {
  public SlimePropaguleBlock(TreeGrower treeIn, FoliageType foliageType, Properties properties) {
    super(treeIn, properties);
  }

  @Override
  protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
    Block block = state.getBlock();
    return TinkerWorld.slimeDirt.contains(block)
      || TinkerWorld.vanillaSlimeGrass.contains(block)
      || TinkerWorld.earthSlimeGrass.contains(block)
      || TinkerWorld.skySlimeGrass.contains(block)
      || TinkerWorld.enderSlimeGrass.contains(block)
      || TinkerWorld.ichorSlimeGrass.contains(block);
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    return state.getValue(HANGING)
      ? level.getBlockState(pos.above()).is(TinkerTags.Blocks.SLIMY_LEAVES)
      : super.canSurvive(state, level, pos);
  }

  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
    return false;
  }
}
