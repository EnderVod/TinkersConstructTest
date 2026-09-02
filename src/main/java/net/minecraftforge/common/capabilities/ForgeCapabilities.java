package net.minecraftforge.common.capabilities;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;

/** Compatibility constants for legacy capability lookups while the providers are migrated to NeoForge. */
public final class ForgeCapabilities {
  private ForgeCapabilities() {}

  public static final Capability<IFluidHandler> FLUID_HANDLER = new Capability<>();
  public static final Capability<IFluidHandlerItem> FLUID_HANDLER_ITEM = new Capability<>();
  public static final Capability<IItemHandler> ITEM_HANDLER = new Capability<>();
  public static final Capability<IEnergyStorage> ENERGY = new Capability<>();
}
