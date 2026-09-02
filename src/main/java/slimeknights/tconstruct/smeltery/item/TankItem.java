package slimeknights.tconstruct.smeltery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TankItem extends BlockTooltipItem {
  public static final String FLUID_ID = TConstruct.makeTranslationKey("item", "tank.fluid_id");
  private static final Predicate<FluidStack> NO_FILL = FluidStack::isEmpty;
  private final boolean limitStackSize;

  public TankItem(Block blockIn, Item.Properties builder, boolean limitStackSize) {
    super(blockIn, builder);
    this.limitStackSize = limitStackSize;
  }

  private static boolean isFilled(ItemStack stack) {
    CompoundTag nbt = TagUtil.getTag(stack);
    return nbt != null && nbt.contains(NBTTags.TANK, Tag.TAG_COMPOUND);
  }

  @Override
  public boolean hasCraftingRemainingItem(ItemStack stack) {
    return isFilled(stack);
  }

  @Override
  public ItemStack getCraftingRemainingItem(ItemStack stack) {
    return isFilled(stack) ? new ItemStack(this) : ItemStack.EMPTY;
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    if (!limitStackSize) {
      return super.getMaxStackSize(stack);
    }
    return isFilled(stack) ? 16 : 64;
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipIPayloadContext context, List<Component> tooltip, TooltipFlag flag) {
    if (TagUtil.hasTag(stack)) {
      FluidTank tank = getTank(stack, 1);
      if (tank.getFluidAmount() > 0) {
        FluidStack fluid = tank.getFluid();
        tooltip.add(fluid.getDisplayName().plainCopy().withStyle(ChatFormatting.GRAY));
        if (flag.isAdvanced()) {
          tooltip.add(Component.translatable(FLUID_ID, Loadables.FLUID.getKey(fluid.getFluid())).withStyle(ChatFormatting.DARK_GRAY));
        }
        FluidTooltipHandler.appendMaterial(fluid, tooltip);
      }
    } else {
      super.appendHoverText(stack, context, tooltip, flag);
    }
  }

  /** Checks if the given stack has fluid transfer support. */
  public static boolean mayHaveFluid(ItemStack stack) {
    return FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack) || stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
  }

  @Override
  public boolean overrideStackedOnOther(ItemStack held, Slot slot, ClickAction action, Player player) {
    if (action == ClickAction.SECONDARY && slot.allowModification(player)) {
      ItemStack slotStack = slot.getItem();
      if (!slotStack.isEmpty() && held.getItem() != slotStack.getItem() && mayHaveFluid(slotStack)) {
        if (slotStack.getCount() == 1) {
          FluidTank tank = getTank(held, 1);
          TransferResult result = FluidTransferHelper.interactWithStack(tank, slotStack, TransferDirection.REVERSE);
          if (result != null) {
            if (player.level().isClientSide) {
              player.playSound(result.getSound());
            }
            slot.set(FluidTransferHelper.getOrTransferFilled(player, slotStack, result.stack()));
            if (held.getCount() == 1) {
              setTank(held, tank);
            } else {
              ItemStack split = held.split(1);
              setTank(split, tank);
              if (!player.getInventory().add(split)) {
                player.drop(split, false);
              }
            }
          }
        } else if (slotStack.isItemEnabled(player.level().enabledFeatures())) {
          AbstractContainerMenu menu = player.containerMenu;
          slotStack.overrideOtherStackedOnMe(held, slot, action, player, new SlotAccess() {
            @Override
            public ItemStack get() {
              return menu.getCarried();
            }

            @Override
            public boolean set(ItemStack stack) {
              menu.setCarried(stack);
              return true;
            }
          });
        }
        return true;
      }
    }
    return false;
  }

  public static void updateHeldItem(Player player, ItemStack held, ItemStack result) {
    if (player.containerMenu.getCarried() == held) {
      player.containerMenu.setCarried(FluidTransferHelper.getOrTransferFilled(player, held, result));
    } else if (!player.getInventory().add(result)) {
      player.drop(result, false);
    }
  }

  @Override
  public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack held, Slot slot, ClickAction action, Player player, SlotAccess access) {
    if (action == ClickAction.SECONDARY && slot.allowModification(player) && !held.isEmpty() && mayHaveFluid(held)) {
      if (stack.getCount() == 1 || held.getItem() instanceof TankItem) {
        FluidTank tank = getTank(stack);
        if (tank.isEmpty() && ItemStack.isSameItemSameComponents(stack, held)) {
          return false;
        }
        TransferResult result = FluidTransferHelper.interactWithStack(tank, held, TransferDirection.AUTO);
        if (result != null) {
          if (player.level().isClientSide) {
            player.playSound(result.getSound());
          }
          setTank(stack, tank);
          updateHeldItem(player, held, result.stack());
        }
      }
      return true;
    }
    return false;
  }

  private static void removeTank(ItemStack stack) {
    CompoundTag nbt = TagUtil.getTag(stack);
    if (nbt != null) {
      nbt.remove(NBTTags.TANK);
      TagUtil.setTag(stack, nbt);
    }
  }

  /** Writes a fluid stack to Tinkers' legacy tank item NBT shape for world compatibility. */
  public static CompoundTag writeFluid(FluidStack fluid) {
    CompoundTag tag = new CompoundTag();
    if (!fluid.isEmpty()) {
      tag.putString("FluidName", BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
      tag.putInt("Amount", fluid.getAmount());
    }
    return tag;
  }

  /** Reads a fluid stack from Tinkers' legacy tank item NBT shape. */
  public static FluidStack readFluid(CompoundTag tag) {
    ResourceLocation fluidName = ResourceLocation.tryParse(tag.getString("FluidName"));
    int amount = tag.getInt("Amount");
    if (fluidName == null || amount <= 0) {
      return FluidStack.EMPTY;
    }
    return BuiltInRegistries.FLUID.getOptional(fluidName)
      .map(fluid -> new FluidStack(fluid, amount))
      .orElse(FluidStack.EMPTY);
  }

  public static CompoundTag writeTank(FluidTank tank) {
    return writeFluid(tank.getFluid());
  }

  public static void readTank(FluidTank tank, CompoundTag tag) {
    tank.setFluid(readFluid(tag));
  }

  public static ItemStack setTank(ItemStack stack, FluidTank tank) {
    if (tank.isEmpty()) {
      removeTank(stack);
    } else {
      CompoundTag nbt = TagUtil.getOrCreateTag(stack);
      nbt.put(NBTTags.TANK, writeTank(tank));
      TagUtil.setTag(stack, nbt);
    }
    return stack;
  }

  public static ItemStack setTank(ItemStack stack, FluidStack fluid) {
    if (fluid.isEmpty()) {
      removeTank(stack);
    } else {
      CompoundTag nbt = TagUtil.getOrCreateTag(stack);
      nbt.put(NBTTags.TANK, writeFluid(fluid));
      TagUtil.setTag(stack, nbt);
    }
    return stack;
  }

  private static ItemStack setTank(ItemLike item, ResourceLocation fluid, int amount) {
    CompoundTag tag = new CompoundTag();
    tag.putString("FluidName", fluid.toString());
    tag.putInt("Amount", amount);
    ItemStack stack = new ItemStack(item);
    CompoundTag nbt = new CompoundTag();
    nbt.put(NBTTags.TANK, tag);
    TagUtil.setTag(stack, nbt);
    return stack;
  }

  public FluidTank getTank(ItemStack stack) {
    int count = stack.getCount();
    FluidTank tank = getTank(stack, count);
    if (limitStackSize && count > 16) {
      tank.setValidator(NO_FILL);
    }
    return tank;
  }

  public static FluidTank getTank(ItemStack stack, int scale) {
    FluidTank tank = ScaledFluidTank.create(TankBlockEntity.getCapacity(stack.getItem()), scale);
    CompoundTag nbt = TagUtil.getTag(stack);
    if (nbt != null) {
      readTank(tank, nbt.getCompound(NBTTags.TANK));
    }
    return tank;
  }

  public static String getSubtype(ItemStack stack) {
    CompoundTag nbt = TagUtil.getTag(stack);
    if (nbt != null && nbt.contains(NBTTags.TANK, Tag.TAG_COMPOUND)) {
      return nbt.getCompound(NBTTags.TANK).getString("FluidName");
    }
    return "";
  }

  @SuppressWarnings("deprecation")
  public static void addFilledVariants(Consumer<ItemStack> output) {
    BuiltInRegistries.FLUID.holders().filter(holder -> {
      Fluid fluid = holder.value();
      return fluid.isSource(fluid.defaultFluidState()) && !holder.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS);
    }).forEachOrdered(holder -> {
      TankType tank, gauge;
      if (holder.is(TinkerTags.Fluids.METAL_TOOLTIPS)) {
        tank = TankType.INGOT_TANK;
        gauge = TankType.INGOT_GAUGE;
      } else {
        tank = TankType.FUEL_TANK;
        gauge = TankType.FUEL_GAUGE;
      }
      ResourceLocation fluidName = holder.key().location();
      output.accept(setTank(TinkerSmeltery.searedLantern, fluidName, FluidValues.LANTERN_CAPACITY));
      output.accept(fillTank(TinkerSmeltery.searedTank, tank, fluidName));
      output.accept(fillTank(TinkerSmeltery.searedTank, gauge, fluidName));
      output.accept(setTank(TinkerSmeltery.scorchedLantern, fluidName, FluidValues.LANTERN_CAPACITY));
      output.accept(fillTank(TinkerSmeltery.scorchedTank, tank, fluidName));
      output.accept(fillTank(TinkerSmeltery.scorchedTank, gauge, fluidName));
    });
  }

  public static ItemStack fillTank(EnumObject<TankType,? extends ItemLike> tank, TankType type, Fluid fluid) {
    return setTank(new ItemStack(tank.get(type)), new FluidStack(fluid, type.getCapacity()));
  }

  public static ItemStack fillTank(EnumObject<TankType,? extends ItemLike> tank, TankType type, ResourceLocation fluid) {
    return setTank(tank.get(type), fluid, type.getCapacity());
  }
}
