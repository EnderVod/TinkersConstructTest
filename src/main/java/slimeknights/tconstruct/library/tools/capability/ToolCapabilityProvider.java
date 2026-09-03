package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolFluidCapability;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Registers the standard NeoForge 1.21 item capabilities exposed by modifiable tools. */
public final class ToolCapabilityProvider {
  private ToolCapabilityProvider() {}

  /** Registers the capability registration listener early in mod construction. */
  public static void init() {
    TConstruct.getModBus().addListener(ToolCapabilityProvider::registerCapabilities);
  }

  private static void registerCapabilities(RegisterCapabilitiesEvent event) {
    Item[] tools = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof IModifiable).toArray(Item[]::new);
    if (tools.length == 0) {
      return;
    }

    event.registerItem(Capabilities.ItemHandler.ITEM, (stack, context) -> {
      IToolStackView tool = ToolStack.from(stack);
      if (tool.getVolatileData().getInt(ToolInventoryCapability.TOTAL_SLOTS) <= 0) {
        return null;
      }
      return new ToolInventoryCapability(() -> ToolStack.from(stack));
    }, tools);

    event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> {
      IToolStackView tool = ToolStack.from(stack);
      if (tool.getVolatileData().getInt(ToolFluidCapability.TOTAL_TANKS) <= 0) {
        return null;
      }
      return new ToolFluidCapability(stack, () -> ToolStack.from(stack));
    }, tools);

    event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, context) -> {
      IToolStackView tool = ToolStack.from(stack);
      if (ToolEnergyCapability.getMaxEnergy(tool) <= 0) {
        return null;
      }
      return new ToolEnergyCapability(() -> ToolStack.from(stack));
    }, tools);

    event.registerItem(BlockItemProviderCapability.CAPABILITY, (stack, context) ->
      BlockItemProviderModifierHook.CapabilityImpl.INSTANCE, tools);
  }
}
