package slimeknights.tconstruct.library.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerCommons;

import javax.annotation.Nullable;
import java.util.Objects;

/** Ingredient matching an item with no container item, used to ensure NBT/component fluid items are empty. */
public class NoContainerIngredient extends NestedIngredient {
  public static final net.minecraft.resources.ResourceLocation ID = TConstruct.getResource("no_container");
  public static final MapCodec<NoContainerIngredient> CODEC = NESTED_CODEC.xmap(NoContainerIngredient::new, ingredient -> ingredient.nested);

  protected NoContainerIngredient(Ingredient nested) {
    super(nested);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && super.test(stack) && !stack.hasCraftingRemainingItem();
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerCommons.noContainerIngredientType.get();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof NoContainerIngredient other && nested.equals(other.nested);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ID, nested);
  }

  /* Static constructors */

  /** Creates an instance from the given nested ingredient. */
  public static Ingredient of(Ingredient ingredient) {
    return new NoContainerIngredient(ingredient).toVanilla();
  }

  /** Creates an instance from the given items. */
  public static Ingredient of(ItemLike... items) {
    return of(Ingredient.of(items));
  }

  /** Creates an instance from the given stacks. */
  public static Ingredient of(ItemStack... stacks) {
    return of(Ingredient.of(stacks));
  }

  /** Creates an instance from the given tag. */
  public static Ingredient of(TagKey<Item> tag) {
    return of(Ingredient.of(tag));
  }
}
