package slimeknights.tconstruct.library.recipe.melting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import slimeknights.mantle.recipe.ICustomOutputRecipe;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

/**
 * Common interface for all melting recipes
 */
public interface IMeltingRecipe extends ICustomOutputRecipe<IMeltingContainer> {
  FluidStack getOutput(IMeltingContainer inv);

  int getTemperature(IMeltingContainer inv);

  int getTime(IMeltingContainer inv);

  default void handleByproducts(IMeltingContainer inv, IFluidHandler handler) {}

  @Override
  default RecipeType<?> getType() {
    return TinkerRecipeTypes.MELTING.get();
  }

  @Override
  default ItemStack getToastSymbol() {
    return new ItemStack(TinkerSmeltery.searedMelter);
  }

  double LOG9_2 = 0.31546487678;

  static int getTemperature(Fluid fluid) {
    return fluid.getFluidType().getTemperature() - 300;
  }

  static int getTemperature(FluidStack fluid) {
    return fluid.getFluid().getFluidType().getTemperature(fluid) - 300;
  }

  static int getTemperature(FluidObject<?> fluid) {
    return fluid.getType().getTemperature() - 300;
  }

  static int calcTime(int temperature, float factor) {
    return (int)Math.round((Math.pow(temperature + 300, 0.585f) * factor));
  }

  static float calcTimeFactor(int amount) {
    return (float)Math.sqrt(amount / (float)FluidValues.INGOT);
  }

  static int calcTimeForAmount(int temperature, int amount) {
    return calcTime(temperature, calcTimeFactor(amount));
  }
}
