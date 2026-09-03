package slimeknights.tconstruct.tools.modules;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipeLookup;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningTierToolHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.HarvestTiers;

import java.util.List;

/**
 * Module that makes a tool effective based on whether it has a valid melting recipe and the result would fit in the tool's tank.
 * Note the former is possible with {@link IsEffectiveModule} using {@link slimeknights.tconstruct.library.json.predicate.TinkerPredicate#CAN_MELT_BLOCK}, but the latter requires tool context.
 */
public record MeltingFluidEffectiveModule(IJsonPredicate<BlockState> predicate, int temperature, boolean ignoreTier) implements ToolModule, IsEffectiveToolHook {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<MeltingFluidEffectiveModule>defaultHooks(ToolHooks.IS_EFFECTIVE);
  public static final RecordLoadable<MeltingFluidEffectiveModule> LOADER = RecordLoadable.create(
    BlockPredicate.LOADER.directField("predicate_type", MeltingFluidEffectiveModule::predicate),
    IntLoadable.FROM_ZERO.requiredField("temperature", MeltingFluidEffectiveModule::temperature),
    BooleanLoadable.INSTANCE.defaultField("ignore_tier", false, MeltingFluidEffectiveModule::ignoreTier),
    MeltingFluidEffectiveModule::new);

  @Override
  public RecordLoadable<MeltingFluidEffectiveModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public boolean isToolEffective(IToolStackView tool, BlockState state) {
    if (predicate.matches(state)) {
      int capacity = ToolTankHelper.TANK_HELPER.getCapacity(tool);
      if (capacity > 0) {
        FluidStack currentFluid = ToolTankHelper.TANK_HELPER.getFluid(tool);
        if (capacity > currentFluid.getAmount()) {
          FluidStack meltingResult = MeltingRecipeLookup.findResult(state.getBlock(), temperature);
          return (!meltingResult.isEmpty() && (currentFluid.isEmpty() || currentFluid.isFluidEqual(meltingResult)))
                 && (ignoreTier || HarvestTiers.isCorrectTierForDrops(MiningTierToolHook.getTier(tool), state));
        }
      }
    }
    return false;
  }
}
