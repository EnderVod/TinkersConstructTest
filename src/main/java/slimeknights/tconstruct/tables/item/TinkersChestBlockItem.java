package slimeknights.tconstruct.tables.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;

import javax.annotation.Nullable;

/** Dyeable chest block */
public class TinkersChestBlockItem extends BlockItem {
  public TinkersChestBlockItem(Block blockIn, Properties builder) {
    super(blockIn, builder);
  }

  public int getColor(ItemStack stack) {
    DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
    return color != null ? color.rgb() : TinkersChestBlockEntity.DEFAULT_COLOR;
  }

  @Override
  protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, @Nullable Player player, ItemStack stack, BlockState state) {
    boolean result = super.updateCustomBlockEntityTag(pos, world, player, stack, state);
    if (stack.get(DataComponents.DYED_COLOR) != null && world.getBlockEntity(pos) instanceof TinkersChestBlockEntity te) {
      te.setColor(getColor(stack));
    }
    return result;
  }
}
