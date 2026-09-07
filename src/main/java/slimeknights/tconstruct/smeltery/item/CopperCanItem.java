package slimeknights.tconstruct.smeltery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluid container holding 1 ingot of fluid
 */
public class CopperCanItem extends Item {
  private static final String TAG_FLUID = "fluid";
  private static final String TAG_FLUID_TAG = "fluid_tag";

  public CopperCanItem(Properties properties) {
    super(properties);
  }

  @Override
  public boolean hasCraftingRemainingItem(ItemStack stack) {
    return getFluid(stack) != Fluids.EMPTY;
  }

  @Override
  public ItemStack getCraftingRemainingItem(ItemStack stack) {
    if (hasCraftingRemainingItem(stack)) {
      return new ItemStack(this);
    }
    return ItemStack.EMPTY;
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    Fluid fluid = getFluid(stack);
    if (fluid != Fluids.EMPTY) {
      MutableComponent text = makeFluidStack(stack, fluid, FluidValues.INGOT).getHoverName().plainCopy();
      tooltip.add(Component.translatable(this.getDescriptionId() + ".contents", text).withStyle(ChatFormatting.GRAY));
      if (flag.isAdvanced()) {
        tooltip.add(Component.translatable(TankItem.FLUID_ID, Loadables.FLUID.getKey(fluid)).withStyle(ChatFormatting.DARK_GRAY));
      }
    } else {
      tooltip.add(Component.translatable(this.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
  }

  /** Removes the fluid from the given stack while preserving unrelated custom data. */
  public static void removeFluid(ItemStack stack) {
    CompoundTag nbt = getCustomData(stack);
    nbt.remove(TAG_FLUID);
    nbt.remove(TAG_FLUID_TAG);
    if (nbt.isEmpty()) {
      stack.remove(DataComponents.CUSTOM_DATA);
    } else {
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }
  }

  /** Writes the fluid ID and its serialized 1.21 component patch into the can's custom data. */
  private static void setFluidInternal(ItemStack stack, ResourceLocation fluid, @Nullable Tag encodedComponents) {
    CompoundTag nbt = getCustomData(stack);
    nbt.putString(TAG_FLUID, fluid.toString());
    if (encodedComponents != null) {
      nbt.put(TAG_FLUID_TAG, encodedComponents.copy());
    } else {
      nbt.remove(TAG_FLUID_TAG);
    }
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
  }

  /** Compatibility overload for old callers that supplied arbitrary FluidStack NBT. */
  @SuppressWarnings("deprecation")
  public static ItemStack setFluid(ItemStack stack, ResourceLocation fluid, @Nullable CompoundTag fluidTag) {
    if (fluid.equals(BuiltInRegistries.FLUID.getDefaultKey())) {
      removeFluid(stack);
      return stack;
    }
    Tag encoded = null;
    if (fluidTag != null && !fluidTag.isEmpty()) {
      DataComponentPatch legacy = DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(fluidTag.copy())).build();
      encoded = serializeFluidComponents(legacy);
      if (encoded == null) {
        return stack;
      }
    }
    setFluidInternal(stack, fluid, encoded);
    return stack;
  }

  /** Compatibility overload for old callers that supplied arbitrary FluidStack NBT. */
  @SuppressWarnings("deprecation")
  public static ItemStack setFluid(ItemStack stack, Fluid fluid, @Nullable CompoundTag fluidTag) {
    if (fluid == Fluids.EMPTY) {
      removeFluid(stack);
      return stack;
    }
    return setFluid(stack, BuiltInRegistries.FLUID.getKey(fluid), fluidTag);
  }

  /** Sets the fluid on the given stack, preserving all 1.21 fluid data components. */
  public static ItemStack setFluid(ItemStack stack, FluidStack fluid) {
    if (fluid.isEmpty()) {
      removeFluid(stack);
      return stack;
    }
    Tag encoded = fluid.isComponentsPatchEmpty() ? null : serializeFluidComponents(fluid.getComponentsPatch());
    if (!fluid.isComponentsPatchEmpty() && encoded == null) {
      return stack;
    }
    setFluidInternal(stack, BuiltInRegistries.FLUID.getKey(fluid.getFluid()), encoded);
    return stack;
  }

  /** Gets the fluid from the given stack */
  public static Fluid getFluid(ItemStack stack) {
    CompoundTag nbt = getCustomData(stack);
    if (nbt.contains(TAG_FLUID, Tag.TAG_STRING)) {
      ResourceLocation location = ResourceLocation.tryParse(nbt.getString(TAG_FLUID));
      if (location != null && BuiltInRegistries.FLUID.containsKey(location)) {
        Fluid fluid = BuiltInRegistries.FLUID.get(location);
        if (fluid != null) {
          return fluid;
        }
      }
    }
    return Fluids.EMPTY;
  }

  /** Adds filled variants of the copper can to the given consumer */
  @SuppressWarnings("deprecation")
  public static void addFilledVariants(Consumer<ItemStack> output) {
    BuiltInRegistries.FLUID.holders().filter(holder -> {
      Fluid fluid = holder.value();
      return fluid.isSource(fluid.defaultFluidState()) && !holder.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS);
    }).forEachOrdered(holder -> {
      output.accept(CopperCanItem.setFluid(new ItemStack(TinkerSmeltery.copperCan), holder.key().location(), null));
    });
  }

  /** Gets a mutable copy of this item's custom-data component. */
  private static CompoundTag getCustomData(ItemStack stack) {
    return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
  }

  /** Reads the stored 1.21 fluid component patch. Legacy fluid NBT becomes CUSTOM_DATA. */
  static DataComponentPatch getFluidComponents(ItemStack stack) {
    CompoundTag nbt = getCustomData(stack);
    if (!nbt.contains(TAG_FLUID_TAG)) {
      return DataComponentPatch.EMPTY;
    }
    Tag encoded = nbt.get(TAG_FLUID_TAG);
    if (encoded == null) {
      return DataComponentPatch.EMPTY;
    }
    var parsed = DataComponentPatch.CODEC.parse(NbtOps.INSTANCE, encoded);
    var result = parsed.result();
    if (result.isPresent()) {
      return result.get();
    }
    if (encoded instanceof CompoundTag legacy && !legacy.isEmpty()) {
      return DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(legacy.copy())).build();
    }
    parsed.error().ifPresent(error -> TConstruct.LOG.warn("Unable to decode Copper Can fluid components: {}", error.message()));
    return DataComponentPatch.EMPTY;
  }

  /** Serializes a fluid component patch into the can's persistent custom data. */
  @Nullable
  private static Tag serializeFluidComponents(DataComponentPatch components) {
    return DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, components)
      .resultOrPartial(error -> TConstruct.LOG.warn("Unable to encode Copper Can fluid components: {}", error))
      .orElse(null);
  }

  /** Reconstructs a component-aware fluid stack from this Copper Can. */
  static FluidStack makeFluidStack(ItemStack stack, Fluid fluid, int amount) {
    FluidStack result = new FluidStack(fluid, amount);
    result.applyComponents(getFluidComponents(stack));
    return result;
  }

  /**
   * Gets a string variant name for the given stack
   * @param stack  Stack instance to check
   * @return  String variant name
   */
  public static String getSubtype(ItemStack stack) {
    return getCustomData(stack).getString(TAG_FLUID);
  }
}
