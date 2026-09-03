package slimeknights.tconstruct.smeltery.block.entity.controller;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.block.entity.NameableBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.controller.MelterBlock;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.SolidFuelModule;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.MixerAlloyTank;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.SingleAlloyingModule;
import slimeknights.tconstruct.smeltery.menu.AlloyerContainerMenu;

import javax.annotation.Nullable;

/** Dedicated alloying block. */
public class AlloyerBlockEntity extends NameableBlockEntity implements ITankBlockEntity {
  private static final int TANK_CAPACITY = TankType.INGOT_TANK.getCapacity();
  private static final Component NAME = TConstruct.makeTranslation("gui", "alloyer");

  public static final BlockEntityTicker<AlloyerBlockEntity> SERVER_TICKER = (level, pos, state, self) -> self.tick(level, pos, state);

  /** Tank for this mixer. Exposed through NeoForge RegisterCapabilitiesEvent in TConstruct. */
  @Getter
  protected final FluidTankAnimated tank = new FluidTankAnimated(TANK_CAPACITY, this);

  @Getter
  private final MixerAlloyTank alloyTank = new MixerAlloyTank(this, tank);
  private final SingleAlloyingModule alloyingModule = new SingleAlloyingModule(this, alloyTank);
  @Getter
  private final SolidFuelModule fuelModule;

  @Getter @Setter
  private int lastStrength = -1;
  private int tick;

  public AlloyerBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.alloyer.get(), pos, state);
  }

  protected AlloyerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state, NAME);
    this.fuelModule = new SolidFuelModule(this, pos.below());
  }

  private boolean isFormed() {
    BlockState state = this.getBlockState();
    return state.hasProperty(MelterBlock.IN_STRUCTURE) && state.getValue(MelterBlock.IN_STRUCTURE);
  }

  private void tick(Level level, BlockPos pos, BlockState state) {
    if (isFormed()) {
      switch (tick) {
        case 0 -> {
          alloyTank.setTemperature(fuelModule.findFuel(false));
          if (!fuelModule.hasFuel() && alloyingModule.canAlloy()) {
            fuelModule.findFuel(true);
          }
        }
        case 2 -> {
          boolean hasFuel = fuelModule.hasFuel();

          if (state.getValue(ControllerBlock.ACTIVE) != hasFuel) {
            level.setBlockAndUpdate(pos, state.setValue(ControllerBlock.ACTIVE, hasFuel));
            BlockPos down = pos.below();
            BlockState downState = level.getBlockState(down);
            if (downState.is(TinkerTags.Blocks.FUEL_TANKS) && downState.hasProperty(ControllerBlock.ACTIVE) && downState.getValue(ControllerBlock.ACTIVE) != hasFuel) {
              level.setBlockAndUpdate(down, downState.setValue(ControllerBlock.ACTIVE, hasFuel));
            }
          }

          if (hasFuel) {
            alloyTank.setTemperature(fuelModule.getTemperature());
            alloyingModule.doAlloy();
            fuelModule.decreaseFuel(1);
          }
        }
      }
    } else if (tick == 2 && fuelModule.hasFuel()) {
      fuelModule.decreaseFuel(1);
    }
    tick = (tick + 1) % 4;
  }

  /** Called when a neighbor of this block is changed to update the tank cache. */
  public void neighborChanged(Direction side) {
    alloyTank.refresh(side, true);
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inv, Player playerEntity) {
    return new AlloyerContainerMenu(id, inv, this);
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  public void saveSynced(CompoundTag tag) {
    super.saveSynced(tag);
    tag.put(NBTTags.TANK, tank.writeToNBT(TagUtil.BUILTIN_LOOKUP, new CompoundTag()));
  }

  @Override
  public void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    fuelModule.writeToTag(tag);
  }

  @Override
  public void load(CompoundTag nbt) {
    super.load(nbt);
    tank.readFromNBT(TagUtil.BUILTIN_LOOKUP, nbt.getCompound(NBTTags.TANK));
    fuelModule.readFromTag(nbt);
  }
}
