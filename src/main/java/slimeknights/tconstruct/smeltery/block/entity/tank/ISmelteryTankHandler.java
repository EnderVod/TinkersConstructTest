package slimeknights.tconstruct.smeltery.block.entity.tank;

import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Common tank access used by smeltery controllers and their client sync. */
public interface ISmelteryTankHandler {
  /** Updates the fluids in the tank with data from the packet, should only be called client side. */
  void updateFluidsFromPacket(List<FluidStack> fluids);

  /** Gets the smeltery tank. */
  SmelteryTank<?> getTank();

  /** Called when the tank adds or removes a fluid to notify listeners. */
  default void notifyFluidsChanged(FluidChange type, FluidStack fluid) {}

  enum FluidChange {
    ADDED,
    CHANGED,
    REMOVED,
    ORDER_CHANGED
  }
}
