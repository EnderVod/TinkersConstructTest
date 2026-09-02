package slimeknights.tconstruct.smeltery.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

import javax.annotation.Nonnull;

/**
 * Native NeoForge fluid handler for a Tinkers tank item.
 * The handler reads the current stack state for each operation and writes changes back to the same stack.
 */
@RequiredArgsConstructor
public class TankItemFluidHandler implements IFluidHandlerItem {
  private final TankItem tankItem;
  @Getter
  private final ItemStack container;

  /** Gets the tank represented by the current stack data. */
  private FluidTank getTank() {
    return tankItem.getTank(container);
  }

  /** Updates the stack after a successful fluid operation. */
  private void updateContainer(FluidTank tank) {
    TankItem.setTank(container, tank);
  }

  @Override
  public int getTanks() {
    return 1;
  }

  @Nonnull
  @Override
  public FluidStack getFluidInTank(int tank) {
    return getTank().getFluidInTank(tank);
  }

  @Override
  public int getTankCapacity(int tank) {
    return TankBlockEntity.getCapacity(container.getItem()) * container.getCount();
  }

  @Override
  public boolean isFluidValid(int tank, FluidStack stack) {
    return true;
  }

  @Override
  public int fill(FluidStack resource, FluidAction action) {
    FluidTank tank = getTank();
    int didFill = tank.fill(resource, action);
    if (didFill > 0 && action.execute()) {
      updateContainer(tank);
    }
    return didFill;
  }

  @Nonnull
  @Override
  public FluidStack drain(FluidStack resource, FluidAction action) {
    FluidTank tank = getTank();
    FluidStack didDrain = tank.drain(resource, action);
    if (!didDrain.isEmpty() && action.execute()) {
      updateContainer(tank);
    }
    return didDrain;
  }

  @Nonnull
  @Override
  public FluidStack drain(int maxDrain, FluidAction action) {
    FluidTank tank = getTank();
    FluidStack didDrain = tank.drain(maxDrain, action);
    if (!didDrain.isEmpty() && action.execute()) {
      updateContainer(tank);
    }
    return didDrain;
  }
}
