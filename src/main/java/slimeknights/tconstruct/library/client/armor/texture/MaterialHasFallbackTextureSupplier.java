package slimeknights.tconstruct.library.client.armor.texture;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Texture supplier that runs the nested supplier if the material at the given index has the given fallback in its render info. */
@RequiredArgsConstructor
public class MaterialHasFallbackTextureSupplier implements ArmorTextureSupplier, Function<String, Boolean> {
  public static final RecordLoadable<MaterialHasFallbackTextureSupplier> LOADER = RecordLoadable.create(
    IntLoadable.FROM_ZERO.requiredField("index", m -> m.index),
    StringLoadable.DEFAULT.set(ArrayLoadable.COMPACT).requiredField("fallback", m -> m.fallback),
    ArmorTextureSupplier.LOADER.requiredField("apply", m -> m.apply),
    MaterialHasFallbackTextureSupplier::new);

  private final int index;
  private final Set<String> fallback;
  private final ArmorTextureSupplier apply;
  private final Map<String,Boolean> cache = new HashMap<>();

  public MaterialHasFallbackTextureSupplier(int index, ArmorTextureSupplier apply, String... fallback) {
    this(index, ImmutableSet.copyOf(fallback), apply);
  }

  @Override
  public RecordLoadable<? extends MaterialHasFallbackTextureSupplier> getLoader() {
    return LOADER;
  }

  @Override
  public Boolean apply(String string) {
    MaterialVariantId material = MaterialVariantId.tryParse(string);
    if (material != null) {
      return MaterialRenderInfoLoader.INSTANCE.hasFallback(material, fallback);
    }
    return false;
  }

  @Override
  public ArmorTexture getArmorTexture(ItemStack stack, TextureType type, RegistryAccess access) {
    CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    if (tag.contains(ToolStack.TAG_MATERIALS, Tag.TAG_LIST)) {
      String material = tag.getList(ToolStack.TAG_MATERIALS, Tag.TAG_STRING).getString(index);
      if (!material.isEmpty() && cache.computeIfAbsent(material, this)) {
        return apply.getArmorTexture(stack, type, access);
      }
    }
    return ArmorTexture.EMPTY;
  }
}
