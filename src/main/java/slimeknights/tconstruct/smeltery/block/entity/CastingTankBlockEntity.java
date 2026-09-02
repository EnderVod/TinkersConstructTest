package slimeknights.tconstruct.smeltery.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.CastingTankBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity.ITankBlock;
import slimeknights.tconstruct.smeltery.item.TankItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CastingTankBlockEntity extends TableBlockEntity implements ITankBlockEntity.ITankInventoryBlockEntity, WorldlyContainer {
  /** Max capacity for the tank */
  public static final int DEFAULT_CAPACITY = FluidType.BUCKET_VOLUME * 4;
  public static final int INPUT = 0;
  public static final int OUTPUT = 1;
  private static final Component NAME = TConstruct.makeTranslation("gui", "casting");

  @Getter
  protected final FluidTankAnimated tank;
  private boolean lastRedstone = false;
  @Getter @Setter
  private int lastStrength = -1;

  public static int getCapacity(Block block) {
    return DEFAULT_CAPACITY;
  }

  public static int getCapacity(Item item) {
    return DEFAULT_CAPACITY;
  }

  public CastingTankBlockEntity(BlockPos pos, BlockState state) {
    this(pos, state, state.getBlock() instanceof ITankBlock tankBlock
                     ? tankBlock
                     : TinkerSmeltery.searedCastingTank.get());
  }

  public CastingTankBlockEntity(BlockPos pos, BlockState state, ITankBlock block) {
    this(TinkerSmeltery.castingTank.get(), pos, state, block);
  }

  @SuppressWarnings("WeakerAccess")
  protected CastingTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ITankBlock block) {
    super(type, pos, state, NAME, 2, 1);
    tank = new FluidTankAnimated(block.getCapacity(), this);
    itemHandler = new SidedInvWrapper(this, Direction.DOWN);
  }

  public void interact(Player player, InteractionHand hand, boolean clickedTank) {
    if (level == null || level.isClientSide) {
      return;
    }
    if (clickedTank) {
      if (!FluidTransferHelper.interactWithContainer(level, worldPosition, tank, player, hand).didTransfer()) {
        FluidTransferHelper.interactWithFilledBucket(level, worldPosition, tank, player, hand, getBlockState().getValue(CastingTankBlock.FACING));
      }
    } else {
      ItemStack input = getItem(INPUT);
      ItemStack output = getItem(OUTPUT);
      ItemStack held = player.getItemInHand(hand);
      if (!output.isEmpty()) {
        setItem(OUTPUT, ItemStack.EMPTY);
        ItemHandlerHelper.giveItemToPlayer(player, output, player.getInventory().selected);
      } else if (!input.isEmpty()) {
        setItem(INPUT, ItemStack.EMPTY);
        ItemHandlerHelper.giveItemToPlayer(player, input, player.getInventory().selected);
      } else if (!held.isEmpty() && canPlaceItem(INPUT, held)) {
        setItem(INPUT, held.split(1));
      }
    }
  }

  @Override
  public void setItem(int slot, ItemStack newStack) {
    ItemStack oldStack = getItem(INPUT);
    super.setItem(slot, newStack);
    if (slot == INPUT && !newStack.isEmpty() && !ItemStack.matches(oldStack, newStack)) {
      tryToProcessItem();
    }
  }

  private void setInputItem(ItemStack stack) {
    super.setItem(INPUT, stack);
  }

  @Override
  public boolean canPlaceItem(int index, ItemStack stack) {
    if (index == INPUT) {
      return getItem(INPUT).isEmpty() && getItem(OUTPUT).isEmpty() && !stack.isEmpty() && (
        FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack)
          || stack.getCapability(Capabilities.FluidHandler.ITEM) != null
      );
    }
    return false;
  }

  public void handleRedstone(boolean hasSignal) {
    if (lastRedstone != hasSignal) {
      if (hasSignal && level != null) {
        level.scheduleTick(worldPosition, this.getBlockState().getBlock(), 2);
      }
      lastRedstone = hasSignal;
    }
  }

  public void swap() {
    ItemStack output = getItem(OUTPUT);
    setItem(OUTPUT, getItem(INPUT));
    setItem(INPUT, output);
    if (level != null) {
      level.playSound(null, getBlockPos(), Sounds.CASTING_CLICKS.getSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
    }
  }

  protected void tryToProcessItem() {
    ItemStack input = getItem(INPUT);
    if (input.isEmpty() || !getItem(OUTPUT).isEmpty()) {
      return;
    }
    setInputItem(ItemStack.EMPTY);
    TransferResult result = FluidTransferHelper.interactWithStack(tank, input, IFluidContainerTransfer.TransferDirection.AUTO);
    if (result == null) {
      setInputItem(input);
    } else {
      setItem(OUTPUT, result.stack());
      if (level != null) {
        level.playSound(null, getBlockPos(), result.getSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
      }
    }
  }

  @Override
  @Nonnull
  public int[] getSlotsForFace(Direction side) {
    return new int[]{INPUT, OUTPUT};
  }

  @Override
  public boolean canPlaceItemThroughFace(int index, ItemStack itemStackIn, @Nullable Direction direction) {
    return index == INPUT && !isStackInSlot(OUTPUT);
  }

  @Override
  public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
    return index == OUTPUT;
  }

  @Nonnull
  @Override
  public ModelData getModelData() {
    return ModelData.builder()
      .with(ModelProperties.FLUID_STACK, tank.getFluid())
      .with(ModelProperties.TANK_CAPACITY, tank.getCapacity()).build();
  }

  @Override
  public void onTankContentsChanged() {
    ITankInventoryBlockEntity.super.onTankContentsChanged();
    tryToProcessItem();
    if (this.level != null) {
      TankBlockEntity.updateLight(this, tank);
      this.requestModelDataUpdate();
    }
  }

  private static final String TAG_REDSTONE = "redstone";

  public void setTankTag(ItemStack stack) {
    TankItem.setTank(stack, tank);
  }

  public void updateTank(CompoundTag nbt) {
    updateTank(nbt, level == null ? TagUtil.BUILTIN_LOOKUP : level.registryAccess());
  }

  private void updateTank(CompoundTag nbt, HolderLookup.Provider registries) {
    if (nbt.isEmpty()) {
      tank.setFluid(FluidStack.EMPTY);
    } else if (nbt.contains("FluidName", Tag.TAG_STRING)) {
      tank.setFluid(TankItem.readFluid(nbt));
      TankBlockEntity.updateLight(this, tank);
    } else {
      tank.readFromNBT(registries, nbt);
      TankBlockEntity.updateLight(this, tank);
    }
  }

  @Override
  public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    tank.setCapacity(getCapacity(getBlockState().getBlock()));
    updateTank(tag.getCompound(NBTTags.TANK), registries);
    lastRedstone = tag.getBoolean(TAG_REDSTONE);
    super.loadAdditional(tag, registries);
  }

  @Override
  public void saveAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.saveAdditional(tags, registries);
    tags.putBoolean(TAG_REDSTONE, lastRedstone);
  }

  @Override
  public void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveSynced(tag, registries);
    if (!tank.isEmpty()) {
      tag.put(NBTTags.TANK, tank.writeToNBT(registries, new CompoundTag()));
    }
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
    return null;
  }
}
