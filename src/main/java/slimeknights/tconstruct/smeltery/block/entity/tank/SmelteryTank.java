package slimeknights.tconstruct.smeltery.block.entity.tank;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.fluid.IMultitankListChange;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.library.utils.WeakListenerList;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler.FluidChange;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.smeltery.network.SmelteryTankUpdatePacket;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

/**
 * Fluid handler implementation for the smeltery
 */
public class SmelteryTank<T extends MantleBlockEntity & ISmelteryTankHandler> implements IFluidHandler, IMultitankListChange {
  private final T parent;
  /** Fluids actually contained in the tank */
  @Getter
  private final List<FluidStack> fluids;
  /** Maximum capacity of the smeltery */
  private int capacity;
  /** Current amount of fluid in the tank */
  @Getter
  private int contained;
  /** Listener for the tank list changing */
  private final WeakListenerList tankListChange = new WeakListenerList();

  public SmelteryTank(T parent) {
    fluids = Lists.newArrayList();
    capacity = 0;
    contained = 0;
    this.parent = parent;
  }

  /** Called when the fluids change to sync to client. */
  public void syncFluids() {
    Level world = parent.getLevel();
    if (world != null && !world.isClientSide) {
      BlockPos pos = parent.getBlockPos();
      TinkerNetwork.getInstance().sendToClientsAround(new SmelteryTankUpdatePacket(pos, fluids), world, pos);
    }
  }

  /** Updates the maximum tank capacity. */
  public void setCapacity(int maxCapacity) {
    this.capacity = maxCapacity;
  }

  /** Gets the maximum amount of space in the smeltery tank. */
  public int getCapacity() {
    return capacity;
  }

  /** Gets the amount of empty space in the tank. */
  public int getRemainingSpace() {
    if (contained >= capacity) {
      return 0;
    }
    return capacity - contained;
  }

  @Override
  public boolean isFluidValid(int tank, FluidStack stack) {
    return true;
  }

  @Override
  public int getTanks() {
    if (contained < capacity) {
      return fluids.size() + 1;
    }
    return fluids.size();
  }

  @Nonnull
  @Override
  public FluidStack getFluidInTank(int tank) {
    if (tank < 0 || tank >= fluids.size()) {
      return FluidStack.EMPTY;
    }
    return fluids.get(tank);
  }

  @Override
  public int getTankCapacity(int tank) {
    if (tank < 0) {
      return 0;
    }
    int remaining = capacity - contained;
    if (tank == fluids.size()) {
      return remaining;
    }
    return fluids.get(tank).getAmount() + remaining;
  }

  /** Moves the fluid with the passed index to the beginning/bottom of the fluid tank stack. */
  public void moveFluidToBottom(int index) {
    if (index < fluids.size()) {
      FluidStack fluid = fluids.get(index);
      fluids.remove(index);
      fluids.add(0, fluid);
      parent.notifyFluidsChanged(FluidChange.CHANGED, FluidStack.EMPTY);
      tankListChange.run();
    }
  }

  @Override
  public int fill(FluidStack resource, FluidAction action) {
    if (contained >= capacity || resource.isEmpty()) {
      return 0;
    }

    int usable = Math.min(capacity - contained, resource.getAmount());
    if (usable <= 0) {
      return 0;
    }

    if (action.simulate()) {
      return usable;
    }

    contained += usable;

    for (FluidStack fluid : fluids) {
      if (FluidStack.isSameFluidSameComponents(fluid, resource)) {
        fluid.grow(usable);
        parent.notifyFluidsChanged(FluidChange.CHANGED, fluid);
        if (contained >= capacity) {
          tankListChange.run();
        }
        return usable;
      }
    }

    resource = resource.copyWithAmount(usable);
    fluids.add(resource);
    parent.notifyFluidsChanged(FluidChange.ADDED, resource);
    tankListChange.run();
    return usable;
  }

  @Nonnull
  @Override
  public FluidStack drain(int maxDrain, FluidAction action) {
    if (fluids.isEmpty()) {
      return FluidStack.EMPTY;
    }
    boolean wasFull = contained >= capacity;

    FluidStack fluid = fluids.get(0);
    int drainable = Math.min(maxDrain, fluid.getAmount());
    FluidStack ret = fluid.copyWithAmount(drainable);

    if (action.execute()) {
      fluid.shrink(drainable);
      contained -= drainable;
      if (fluid.getAmount() <= 0) {
        fluids.remove(fluid);
        parent.notifyFluidsChanged(FluidChange.REMOVED, fluid);
        tankListChange.run();
      } else {
        parent.notifyFluidsChanged(FluidChange.CHANGED, fluid);
        if (wasFull && contained < capacity) {
          tankListChange.run();
        }
      }
    }

    return ret;
  }

  @Nonnull
  @Override
  public FluidStack drain(FluidStack toDrain, FluidAction action) {
    boolean wasFull = contained >= capacity;
    ListIterator<FluidStack> iter = fluids.listIterator();
    while (iter.hasNext()) {
      FluidStack fluid = iter.next();
      if (FluidStack.isSameFluidSameComponents(fluid, toDrain)) {
        int drainable = Math.min(toDrain.getAmount(), fluid.getAmount());
        FluidStack ret = fluid.copyWithAmount(drainable);

        if (action.execute()) {
          fluid.shrink(drainable);
          contained -= drainable;
          if (fluid.getAmount() <= 0) {
            iter.remove();
            parent.notifyFluidsChanged(FluidChange.REMOVED, fluid);
            tankListChange.run();
          } else {
            parent.notifyFluidsChanged(FluidChange.CHANGED, fluid);
            if (wasFull && contained < capacity) {
              tankListChange.run();
            }
          }
        }

        return ret;
      }
    }

    return FluidStack.EMPTY;
  }

  private static final String TAG_FLUIDS = "fluids";
  private static final String TAG_CAPACITY = "capacity";

  /** Updates fluids in the tank, typically from a packet. */
  public void setFluids(List<FluidStack> fluids) {
    FluidStack oldFirst = getFluidInTank(0);
    this.fluids.clear();
    this.fluids.addAll(fluids);
    contained = fluids.stream().mapToInt(FluidStack::getAmount).reduce(0, Integer::sum);
    FluidStack newFirst = getFluidInTank(0);
    if (!FluidStack.isSameFluidSameComponents(oldFirst, newFirst)) {
      parent.notifyFluidsChanged(FluidChange.ORDER_CHANGED, newFirst);
      tankListChange.run();
    }
  }

  /** Writes the tank to NBT using Minecraft 1.21 registry-aware FluidStack serialization. */
  public CompoundTag write(CompoundTag nbt) {
    ListTag list = new ListTag();
    for (FluidStack liquid : fluids) {
      list.add(liquid.save(TagUtil.BUILTIN_LOOKUP));
    }
    nbt.put(TAG_FLUIDS, list);
    nbt.putInt(TAG_CAPACITY, capacity);
    return nbt;
  }

  /** Reads the tank from NBT, retaining compatibility with Tinkers' legacy FluidName/Amount data. */
  public void read(CompoundTag tag) {
    ListTag list = tag.getList(TAG_FLUIDS, Tag.TAG_COMPOUND);
    fluids.clear();
    contained = 0;
    for (int i = 0; i < list.size(); i++) {
      CompoundTag fluidTag = list.getCompound(i);
      FluidStack fluid = fluidTag.contains("FluidName", Tag.TAG_STRING)
        ? TankItem.readFluid(fluidTag)
        : FluidStack.parseOptional(TagUtil.BUILTIN_LOOKUP, fluidTag);
      if (!fluid.isEmpty()) {
        fluids.add(fluid);
        contained += fluid.getAmount();
      }
    }
    capacity = tag.getInt(TAG_CAPACITY);
  }

  @Override
  public <TE> void addTankListListener(TE parent, Consumer<TE> listener) {
    tankListChange.addListener(parent, listener);
  }

  @Override
  public void removeTankListListeners(Object parent) {
    tankListChange.removeListeners(parent);
  }
}
