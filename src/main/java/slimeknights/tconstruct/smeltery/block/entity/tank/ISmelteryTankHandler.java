package slimeknights.tconstruct.smeltery.block.entity.tank;

import net.minecraftforge.common.util.LazyOptional;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

/** Common tank access used by smeltery controllers and their client sync. */
public interface ISmelteryTankHandler {
  /** Updates the fluids in the tank with data from the packet, should only be called client side. */
  void updateFluidsFromPacket(List<FluidStack> fluids);

  /** Gets the smeltery tank. */
  SmelteryTank<?> getTank();

  /**
   * Legacy internal holder used by drains while the multiblock capability path is migrated.
   * External capability exposure is being moved to NeoForge RegisterCapabilitiesEvent providers.
   */
  LazyOptional<IFluidHandler> getFluidCapability();

  /** Called when the tank adds or removes a fluid to notify listeners. */
  default void notifyFluidsChanged(FluidChange type, FluidStack fluid) {}

  enum FluidChange {
    ADDED,
    CHANGED,
    REMOVED,
    ORDER_CHANGED
  }
}
