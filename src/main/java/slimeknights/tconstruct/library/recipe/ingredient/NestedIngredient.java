package slimeknights.tconstruct.library.recipe.ingredient;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Stream;

/** Custom ingredient that contains another ingredient nested inside. */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class NestedIngredient implements ICustomIngredient {
  /**
   * Supports both the legacy flattened vanilla ingredient form and the explicit {@code match} form used when nesting
   * arrays or custom ingredients.
   */
  protected static final MapCodec<Ingredient> NESTED_CODEC = NeoForgeExtraCodecs.xor(
      Ingredient.Value.MAP_CODEC,
      Ingredient.CODEC_NONEMPTY.fieldOf("match"))
    .xmap(
      either -> either.map(value -> Ingredient.fromValues(Stream.of(value)), ingredient -> ingredient),
      ingredient -> {
        if (!ingredient.isCustom()) {
          Ingredient.Value[] values = ingredient.getValues();
          if (values.length == 1) {
            return Either.left(values[0]);
          }
        }
        return Either.right(ingredient);
      });

  protected final Ingredient nested;

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return nested.test(stack);
  }

  @Override
  public Stream<ItemStack> getItems() {
    return Arrays.stream(nested.getItems());
  }

  @Override
  public boolean isSimple() {
    return nested.isSimple();
  }
}
