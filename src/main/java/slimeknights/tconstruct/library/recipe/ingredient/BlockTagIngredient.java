package slimeknights.tconstruct.library.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerCommons;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Item ingredient matching items with a block form in the given tag. */
@RequiredArgsConstructor
public class BlockTagIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = TConstruct.getResource("block_tag");
  public static final MapCodec<BlockTagIngredient> CODEC = TagKey.codec(Registries.BLOCK)
    .xmap(BlockTagIngredient::new, ingredient -> ingredient.tag)
    .fieldOf("tag");

  private final TagKey<Block> tag;
  @Nullable
  private Set<Item> matchingItems;

  /** Gets the ordered matching items set. */
  private Set<Item> getMatchingItems() {
    if (matchingItems == null) {
      matchingItems = BuiltInRegistries.BLOCK.getTagOrEmpty(tag).stream()
        .map(Holder::value)
        .map(Block::asItem)
        .filter(item -> item != Items.AIR)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    return matchingItems;
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && getMatchingItems().contains(stack.getItem());
  }

  @Override
  public Stream<ItemStack> getItems() {
    Set<Item> matching = getMatchingItems();
    if (matching.isEmpty()) {
      ItemStack barrier = new ItemStack(Blocks.BARRIER);
      barrier.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Tag: " + tag.location()));
      return Stream.of(barrier);
    }
    return matching.stream().map(ItemStack::new);
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerCommons.blockTagIngredientType.get();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof BlockTagIngredient other && tag.equals(other.tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ID, tag);
  }

  /** Creates a vanilla Ingredient wrapper for this custom ingredient. */
  public static Ingredient of(TagKey<Block> tag) {
    return new BlockTagIngredient(tag).toVanilla();
  }
}
