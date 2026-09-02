package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;

import java.util.ArrayList;
import java.util.List;

/**
 * Alloy tank that takes inputs from neighboring blocks.
 *
 * NeoForge 1.21 capabilities are queried from the level instead of being held as Forge LazyOptionals.
 * The level internally caches capability resolution, while querying here ensures a provider invalidation
 * cannot leave the alloyer holding a stale handler.
 */
@RequiredArgsConstructor
public class MixerAlloyTank implements IMutableAlloyTank {
  /** Handler parent */
  private final MantleBlockEntity parent;
  /** Tank for outputs */
  private final IFluidHandler outputTank;

  /** Current temperature. Provided as a getter and setter as there are a few contexts with different source for temperature */
  @Getter
  @Setter
  private int temperature = 0;

  /** Finds all valid neighboring input tanks, preserving the original Direction iteration order. */
  private IFluidHandler[] findTanks() {
    Level world = parent.getLevel();
    if (world == null) {
      return new IFluidHandler[0];
    }

    List<IFluidHandler> inputs = new ArrayList<>(5);
    for (Direction direction : Direction.values()) {
      if (direction == Direction.DOWN) {
        continue;
      }
      BlockPos target = parent.getBlockPos().relative(direction);
      // limit by blocks as that gives the modpack more control, say they want to allow only scorched tanks
      if (!world.getBlockState(target).is(TinkerTags.Blocks.ALLOYER_TANKS)) {
        continue;
      }
      BlockEntity blockEntity = world.getBlockEntity(target);
      if (blockEntity != null) {
        IFluidHandler handler = world.getCapability(
          Capabilities.FluidHandler.BLOCK, target, world.getBlockState(target), blockEntity, direction.getOpposite());
        if (handler != null) {
          inputs.add(handler);
        }
      }
    }
    return inputs.toArray(IFluidHandler[]::new);
  }

  @Override
  public int getTanks() {
    return findTanks().length;
  }

  /** Gets the fluid handler for the given tank index */
  public IFluidHandler getFluidHandler(int tank) {
    IFluidHandler[] tanks = findTanks();
    if (tank < 0 || tank >= tanks.length) {
      return EmptyFluidHandler.INSTANCE;
    }
    return tanks[tank];
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    IFluidHandler handler = getFluidHandler(tank);
    return handler == EmptyFluidHandler.INSTANCE ? FluidStack.EMPTY : handler.getFluidInTank(0);
  }

  @Override
  public FluidStack drain(int tank, FluidStack fluidStack) {
    return getFluidHandler(tank).drain(fluidStack, FluidAction.EXECUTE);
  }

  @Override
  public boolean canFit(FluidStack fluid, int removed) {
    return outputTank.fill(fluid, FluidAction.SIMULATE) == fluid.getAmount();
  }

  @Override
  public int fill(FluidStack fluidStack) {
    return outputTank.fill(fluidStack, FluidAction.EXECUTE);
  }

  /**
   * Kept for callers that notify the alloyer of neighbor changes. No local capability cache remains,
   * so the next operation will naturally resolve the current handler from the level.
   */
  public void refresh(Direction direction, boolean checkInput) {}
}
