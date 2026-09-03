package slimeknights.tconstruct.smeltery.block.entity.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.utils.TagUtil;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * This class represents a single item slot that can melt into a liquid
 */
@RequiredArgsConstructor
public class MeltingModule implements IMeltingContainer, ContainerData {
  public static final int NO_SPACE = -1;

  private static final String TAG_CURRENT_TIME = "time";
  private static final String TAG_REQUIRED_TIME = "required";
  private static final String TAG_REQUIRED_TEMP = "temp";
  private static final int CURRENT_TIME = 0;
  private static final int REQUIRED_TIME = 1;
  private static final int REQUIRED_TEMP = 2;

  private final MantleBlockEntity parent;
  private final Predicate<IMeltingRecipe> outputFunction;
  private final IOreRate oreRate;
  private final int slotIndex;

  @Getter
  private int currentTime = 0;
  @Getter
  private int requiredTime = 0;
  @Getter
  private int requiredTemp = 0;

  private IMeltingRecipe lastRecipe;

  @Getter
  private ItemStack stack = ItemStack.EMPTY;

  @Override
  public IOreRate getOreRate() {
    return oreRate;
  }

  private void resetRecipe() {
    currentTime = 0;
    requiredTime = 0;
    requiredTemp = 0;
  }

  public void setStack(ItemStack newStack) {
    Level world = parent.getLevel();
    if (slotIndex != -1 && world != null && !world.isClientSide && !ItemStack.matches(stack, newStack)) {
      TinkerNetwork.getInstance().sendToClientsAround(new InventorySlotSyncPacket(newStack, slotIndex, parent.getBlockPos()), world, parent.getBlockPos());
    }

    if (newStack.isEmpty()) {
      resetRecipe();
    } else if (this.stack.isEmpty() || !ItemStack.isSameItemSameComponents(this.stack, newStack)) {
      currentTime = 0;
    }

    this.stack = newStack;
    int newTime = 0;
    int newTemp = 0;
    if(!stack.isEmpty()) {
      IMeltingRecipe recipe = findRecipe();
      if (recipe != null) {
        newTime = recipe.getTime(this) * 10;
        newTemp = recipe.getTemperature(this);
      }
    }
    requiredTime = newTime;
    requiredTemp = newTemp;
    parent.setChangedFast();
  }

  public boolean canHeatItem(int temperature) {
    if (requiredTime > 0) {
      if (stack.isEmpty()) {
        resetRecipe();
        return false;
      }
      return currentTime != NO_SPACE && temperature >= requiredTemp;
    }
    return false;
  }

  public void heatItem(int temperature, int rate) {
    if (currentTime == NO_SPACE || canHeatItem(temperature)) {
      if (currentTime == NO_SPACE || currentTime >= requiredTime) {
        if (onItemFinishedHeating()) {
          resetRecipe();
        }
      } else {
        currentTime += rate;
      }
    }
  }

  public void coolItem() {
    if (currentTime == NO_SPACE) {
      if (onItemFinishedHeating()) {
        resetRecipe();
      }
    } else if (currentTime > 0 && requiredTime > 0) {
      currentTime -= 5;
    }
  }

  @Nullable
  private IMeltingRecipe findRecipe() {
    Level world = parent.getLevel();
    if (world == null) {
      return null;
    }

    IMeltingRecipe last = lastRecipe;
    if (last != null && last.matches(this, world)) {
      return last;
    }
    var newRecipe = world.getRecipeManager().getRecipeFor(TinkerRecipeTypes.MELTING.get(), this, world);
    if (newRecipe.isPresent()) {
      lastRecipe = newRecipe.get().value();
      return lastRecipe;
    }
    return null;
  }

  private boolean onItemFinishedHeating() {
    IMeltingRecipe recipe = findRecipe();
    if (recipe == null) {
      return true;
    }

    if (outputFunction.test(recipe)) {
      setStack(ItemStack.EMPTY);
      return true;
    }

    currentTime = NO_SPACE;
    return false;
  }

  public CompoundTag writeToTag() {
    CompoundTag nbt = new CompoundTag();
    if (!stack.isEmpty()) {
      nbt = TagUtil.saveItem(stack, nbt);
      nbt.putInt(TAG_CURRENT_TIME, currentTime);
      nbt.putInt(TAG_REQUIRED_TIME, requiredTime);
      nbt.putInt(TAG_REQUIRED_TEMP, requiredTemp);
    }
    return nbt;
  }

  public void readFromTag(CompoundTag nbt) {
    stack = TagUtil.readItem(nbt);
    if (!stack.isEmpty()) {
      currentTime = nbt.getInt(TAG_CURRENT_TIME);
      requiredTime = nbt.getInt(TAG_REQUIRED_TIME);
      requiredTemp = nbt.getInt(TAG_REQUIRED_TEMP);
    }
  }

  @Override
  public int getCount() {
    return 3;
  }

  @Override
  public int get(int index) {
    return switch (index) {
      case CURRENT_TIME -> currentTime;
      case REQUIRED_TIME -> requiredTime;
      case REQUIRED_TEMP -> requiredTemp;
      default -> 0;
    };
  }

  @Override
  public void set(int index, int value) {
    switch (index) {
      case CURRENT_TIME -> currentTime = value;
      case REQUIRED_TIME -> requiredTime = value;
      case REQUIRED_TEMP -> requiredTemp = value;
    }
  }
}
