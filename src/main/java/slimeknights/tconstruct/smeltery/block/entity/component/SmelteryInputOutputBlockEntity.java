package slimeknights.tconstruct.smeltery.block.entity.component;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

import static slimeknights.mantle.util.RetexturedHelper.TAG_TEXTURE;

/**
 * Shared logic between drains, ducts, and chutes.
 *
 * NeoForge 1.21 exposes capabilities through registered providers instead of BlockEntity#getCapability.
 * These IO blocks therefore resolve the current master capability directly when their own provider is queried.
 */
public abstract class SmelteryInputOutputBlockEntity<T> extends SmelteryComponentBlockEntity implements IRetexturedBlockEntity {
  /** Capability this block forwards from its master. */
  private final BlockCapability<T,Direction> capability;

  /* Retexturing */
  @Nonnull
  @Getter
  private Block texture = Blocks.AIR;

  protected SmelteryInputOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, BlockCapability<T,Direction> capability) {
    super(type, pos, state);
    this.capability = capability;
  }

  /**
   * Invalidates this block's externally visible capability after its master relationship changes.
   */
  private void invalidateHandler() {
    if (level != null) {
      level.invalidateCapabilities(worldPosition);
    }
  }

  @Override
  public void onMasterLoad(IMasterLogic master) {
    invalidateHandler();
  }

  @Override
  protected void setMaster(@Nullable BlockPos master, @Nullable Block block) {
    assert level != null;

    boolean masterChanged = !Objects.equals(getMasterPos(), master);
    super.setMaster(master, block);
    if (masterChanged) {
      invalidateHandler();
      // notify neighbors of the change (state change skips the notify flag)
      level.blockUpdated(worldPosition, getBlockState().getBlock());
    }
  }

  /**
   * Resolves the currently exposed handler from the validated master.
   * Returning null tells NeoForge that this IO block has no capability while its structure is invalid.
   */
  @Nullable
  public T getHandler(@Nullable Direction facing) {
    if (validateMaster()) {
      BlockPos master = getMasterPos();
      if (master != null && level != null) {
        return level.getCapability(capability, master, facing);
      }
    }
    return null;
  }


  /* Retexturing */

  @Override
  @Nonnull
  public ModelData getModelData() {
    return RetexturedHelper.getModelData(getTexture());
  }

  @Override
  public String getTextureName() {
    return RetexturedHelper.getTextureName(texture);
  }

  @Override
  public void updateTexture(String name) {
    Block oldTexture = texture;
    texture = RetexturedHelper.getBlock(name);
    if (oldTexture != texture) {
      setChangedFast();
      RetexturedHelper.onTextureUpdated(this);
    }
  }


  /* NBT */

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(CompoundTag tags) {
    super.saveSynced(tags);
    if (texture != Blocks.AIR) {
      tags.putString(TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  public void load(CompoundTag tags) {
    super.load(tags);
    if (tags.contains(TAG_TEXTURE, Tag.TAG_STRING)) {
      texture = RetexturedHelper.getBlock(tags.getString(TAG_TEXTURE));
      RetexturedHelper.onTextureUpdated(this);
    }
  }


  /** Fluid implementation of smeltery IO. */
  public static abstract class SmelteryFluidIO extends SmelteryInputOutputBlockEntity<IFluidHandler> {
    protected SmelteryFluidIO(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state, Capabilities.FluidHandler.BLOCK);
    }
  }

  /** Item implementation of smeltery IO. */
  public static class ChuteBlockEntity extends SmelteryInputOutputBlockEntity<IItemHandler> {
    public ChuteBlockEntity(BlockPos pos, BlockState state) {
      this(TinkerSmeltery.chute.get(), pos, state);
    }

    protected ChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state, Capabilities.ItemHandler.BLOCK);
    }
  }
}
