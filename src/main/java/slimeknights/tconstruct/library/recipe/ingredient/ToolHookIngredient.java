package slimeknights.tconstruct.library.recipe.ingredient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.tools.TinkerTools;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient that only matches tools with a specific hook. */
public class ToolHookIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = TConstruct.getResource("tool_hook");
  private static final Codec<ModuleHook<?>> HOOK_CODEC = TinkerLoadables.codec(ToolHooks.LOADER, "hook");
  public static final MapCodec<ToolHookIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    TagKey.codec(Registries.ITEM).optionalFieldOf("tag", TinkerTags.Items.MODIFIABLE).forGetter(ingredient -> ingredient.tag),
    HOOK_CODEC.fieldOf("hook").forGetter(ingredient -> ingredient.hook)
  ).apply(instance, ToolHookIngredient::new));

  private final TagKey<Item> tag;
  private final ModuleHook<?> hook;

  protected ToolHookIngredient(TagKey<Item> tag, ModuleHook<?> hook) {
    this.tag = tag;
    this.hook = hook;
  }

  public static Ingredient of(TagKey<Item> tag, ModuleHook<?> hook) {
    return new ToolHookIngredient(tag, hook).toVanilla();
  }

  public static Ingredient of(ModuleHook<?> hook) {
    return of(TinkerTags.Items.MODIFIABLE, hook);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && stack.is(tag) && stack.getItem() instanceof IModifiable modifiable
      && modifiable.getToolDefinition().getData().getHooks().hasHook(hook);
  }

  @Override
  public Stream<ItemStack> getItems() {
    List<ItemStack> list = new ArrayList<>();
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
      if (holder.value() instanceof IModifiable modifiable && modifiable.getToolDefinition().getData().getHooks().hasHook(hook)) {
        list.add(new ItemStack(modifiable));
      }
    }
    if (list.isEmpty()) {
      ItemStack barrier = new ItemStack(Blocks.BARRIER);
      barrier.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Tag: " + tag.location()));
      list.add(barrier);
    }
    return list.stream();
  }

  @Override
  public boolean isSimple() { return true; }

  @Override
  public IngredientType<?> getType() { return TinkerTools.toolHookIngredientType.get(); }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof ToolHookIngredient other && tag.equals(other.tag) && hook.equals(other.hook);
  }

  @Override
  public int hashCode() { return Objects.hash(ID, tag, hook); }
}
