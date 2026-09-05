package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.container.ISingleStackContainer;

import javax.annotation.Nullable;

/**
 * Inventory containing a single item and a fluid
 */
public interface ICastingContainer extends ISingleStackContainer {
  /**
   * Gets the contained fluid in this inventory
   * @return  Contained fluid
   */
  Fluid getFluid();

  /** Gets the full contained fluid stack, including 1.21 data components. */
  FluidStack getFluidStack();

  /**
   * Gets the NBT for the contained fluid
   * @return  Fluid's NBT
   */
  @Nullable
  default CompoundTag getFluidTag() {
    return null;
  }
}
