package slimeknights.tconstruct.smeltery.block.entity.module;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Fuel module that supports multiple tanks, selecting just one for the fuel result */
public class MultitankFuelModule extends FuelModule implements IFluidHandler {
  /** Block position that will never be valid in world, used for sync */
  private static final BlockPos NULL_POS = new BlockPos(0, Short.MIN_VALUE, 0);

  /** Supplier for the list of valid tank positions */
  private final Supplier<List<BlockPos>> tankSupplier;
  /** Position of the last fluid handler */
  private BlockPos lastPos = NULL_POS;

  public MultitankFuelModule(MantleBlockEntity parent, Supplier<List<BlockPos>> tankSupplier) {
    super(parent);
    this.tankSupplier = tankSupplier;
  }

  /** Clears the selected display handler. Native capabilities are resolved again on demand. */
  private void clearLastHandler() {
    fluidHandler = null;
  }

  /** Called on structure rebuild. There are no capability listeners to unregister on NeoForge. */
  public void clearFluidListeners() {
    clearLastHandler();
  }

  /** Called on servant load to refresh the selected tank if this servant was the last fuel source. */
  public void ensureTankPresent(BlockEntity be) {
    if (be.getBlockPos().equals(lastPos)) {
      fluidHandler = getTankHandler(lastPos);
    }
  }

  /** Gets the current fluid handler at a tank position. */
  @Nullable
  private IFluidHandler getTankHandler(BlockPos pos) {
    Level level = getLevel();
    return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
  }

  /** Gets the current handlers in tank order. We retain positions, not stale capability wrapper objects. */
  private Map<BlockPos,IFluidHandler> getTankHandlers() {
    Map<BlockPos,IFluidHandler> handlers = new LinkedHashMap<>();
    for (BlockPos pos : tankSupplier.get()) {
      IFluidHandler handler = getTankHandler(pos);
      if (handler != null) {
        handlers.put(pos, handler);
      }
    }
    return handlers;
  }

  /* Fuel finding */

  private int tryFuelPosition(BlockPos pos, boolean consume) {
    IFluidHandler handler = getTankHandler(pos);
    if (handler != null) {
      int temperature = tryLiquidFuel(handler, consume);
      if (temperature > 0) {
        fluidHandler = handler;
        lastPos = pos;
        return temperature;
      }
    }
    return 0;
  }

  @Override
  public int findFuel(boolean consume) {
    if (lastPos != NULL_POS) {
      int posTemp = tryFuelPosition(lastPos, consume);
      if (posTemp > 0) {
        return posTemp;
      }
    }

    for (BlockPos pos : tankSupplier.get()) {
      if (!pos.equals(lastPos)) {
        int posTemp = tryFuelPosition(pos, consume);
        if (posTemp > 0) {
          return posTemp;
        }
      }
    }

    fluidHandler = null;
    if (consume) {
      temperature = 0;
      rate = 0;
    }
    return 0;
  }

  /* NBT */
  private static final String TAG_LAST_FUEL = "last_fuel";

  @Override
  public void readFromTag(CompoundTag nbt) {
    super.readFromTag(nbt);
    NbtUtils.readBlockPos(nbt, TAG_LAST_FUEL)
            .ifPresent(pos -> lastPos = pos.offset(parent.getBlockPos()));
  }

  @Override
  public CompoundTag writeToTag(CompoundTag nbt) {
    nbt = super.writeToTag(nbt);
    if (lastPos != NULL_POS) {
      nbt.put(TAG_LAST_FUEL, NbtUtils.writeBlockPos(lastPos.subtract(parent.getBlockPos())));
    }
    return nbt;
  }

  /* UI syncing */
  private static final int LAST_X = 4;
  private static final int LAST_Y = 5;
  private static final int LAST_Z = 6;

  @Override
  public int getCount() {
    return 7;
  }

  @Override
  public int get(int index) {
    return switch (index) {
      case LAST_X -> lastPos.getX();
      case LAST_Y -> lastPos.getY();
      case LAST_Z -> lastPos.getZ();
      default -> super.get(index);
    };
  }

  @Override
  public void set(int index, int value) {
    if (LAST_X <= index && index <= LAST_Z) {
      switch (index) {
        case LAST_X -> lastPos = new BlockPos(value, lastPos.getY(), lastPos.getZ());
        case LAST_Y -> lastPos = new BlockPos(lastPos.getX(), value, lastPos.getZ());
        case LAST_Z -> lastPos = new BlockPos(lastPos.getX(), lastPos.getY(), value);
      }
      clearLastHandler();
    } else {
      super.set(index, value);
    }
  }

  @Override
  public FuelInfo getFuelInfo() {
    Map<BlockPos,IFluidHandler> handlers = getTankHandlers();
    IFluidHandler selected = lastPos.getY() != NULL_POS.getY() ? handlers.get(lastPos) : null;

    if (selected == null || selected.getFluidInTank(0).isEmpty()) {
      selected = null;
      for (IFluidHandler handler : handlers.values()) {
        if (!handler.getFluidInTank(0).isEmpty()) {
          selected = handler;
          break;
        }
      }
    }
    fluidHandler = selected;

    FuelInfo info = super.getFuelInfo();
    if (!info.isEmpty()) {
      FluidStack currentFuel = info.getFluid();
      for (IFluidHandler handler : handlers.values()) {
        if (handler != fluidHandler) {
          FluidStack fluid = handler.getFluidInTank(0);
          if (fluid.isEmpty()) {
            info.add(0, handler.getTankCapacity(0));
          } else if (FluidStack.isSameFluidSameComponents(currentFuel, fluid)) {
            info.add(fluid.getAmount(), handler.getTankCapacity(0));
          }
        }
      }
    }

    return info;
  }

  /* Fluid handler */

  public FluidStack getLastFluid() {
    if (lastPos.getY() != NULL_POS.getY()) {
      IFluidHandler handler = getTankHandler(lastPos);
      if (handler != null) {
        fluidHandler = handler;
        return handler.getFluidInTank(0);
      }
    }
    List<BlockPos> positions = tankSupplier.get();
    if (!positions.isEmpty()) {
      IFluidHandler handler = getTankHandler(positions.get(0));
      if (handler != null) {
        fluidHandler = handler;
        return handler.getFluidInTank(0);
      }
    }
    fluidHandler = null;
    return FluidStack.EMPTY;
  }

  @Override
  public int getTanks() {
    return tankSupplier.get().size();
  }

  private IFluidHandler getTank(int tank) {
    if (tank >= 0) {
      List<BlockPos> positions = tankSupplier.get();
      if (tank < positions.size()) {
        IFluidHandler handler = getTankHandler(positions.get(tank));
        if (handler != null) {
          return handler;
        }
      }
    }
    return EmptyFluidHandler.INSTANCE;
  }

  @Nonnull
  @Override
  public FluidStack getFluidInTank(int tank) {
    return getTank(tank).getFluidInTank(tank);
  }

  @Override
  public int getTankCapacity(int tank) {
    return getTank(tank).getTankCapacity(tank);
  }

  @Override
  public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
    return getTank(tank).isFluidValid(0, stack);
  }

  @Override
  public int fill(FluidStack resource, FluidAction action) {
    int totalFilled = 0;
    resource = resource.copy();
    for (IFluidHandler handler : getTankHandlers().values()) {
      int filled = handler.fill(resource, action);
      if (filled > 0) {
        totalFilled += filled;
        if (filled >= resource.getAmount()) {
          break;
        }
        if (totalFilled == filled) {
          resource = resource.copyWithAmount(resource.getAmount() - filled);
        } else {
          resource.shrink(filled);
        }
      }
    }
    return totalFilled;
  }

  @Nonnull
  @Override
  public FluidStack drain(FluidStack resource, FluidAction action) {
    FluidStack drainedSoFar = FluidStack.EMPTY;
    for (IFluidHandler handler : getTankHandlers().values()) {
      FluidStack drained = handler.drain(resource, action);
      if (!drained.isEmpty()) {
        if (drainedSoFar.isEmpty()) {
          drainedSoFar = drained;
          if (drained.getAmount() >= resource.getAmount()) {
            break;
          }
          resource = resource.copyWithAmount(resource.getAmount() - drained.getAmount());
        } else {
          drainedSoFar.grow(drained.getAmount());
          resource.shrink(drained.getAmount());
          if (resource.isEmpty()) {
            break;
          }
        }
      }
    }
    return drainedSoFar;
  }

  @Nonnull
  @Override
  public FluidStack drain(int maxDrain, FluidAction action) {
    FluidStack drainedSoFar = FluidStack.EMPTY;
    FluidStack toDrain = FluidStack.EMPTY;
    for (IFluidHandler handler : getTankHandlers().values()) {
      if (toDrain.isEmpty()) {
        FluidStack drained = handler.drain(maxDrain, action);
        if (!drained.isEmpty()) {
          drainedSoFar = drained;
          if (drained.getAmount() >= maxDrain) {
            break;
          }
          toDrain = drained.copyWithAmount(maxDrain - drained.getAmount());
        }
      } else {
        FluidStack drained = handler.drain(toDrain, action);
        if (!drained.isEmpty()) {
          drainedSoFar.grow(drained.getAmount());
          toDrain.shrink(drained.getAmount());
          if (toDrain.isEmpty()) {
            break;
          }
        }
      }
    }
    return drainedSoFar;
  }
}
