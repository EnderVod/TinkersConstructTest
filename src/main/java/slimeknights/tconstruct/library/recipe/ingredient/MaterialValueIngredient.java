package slimeknights.tconstruct.library.recipe.ingredient;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.shared.TinkerMaterials;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient matching material items with the given value. Typically matches ingots or blocks. */
@Getter
@RequiredArgsConstructor
public class MaterialValueIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = TConstruct.getResource("material_value");

  private record ValueRange(float min, float max) {}
  private static final Codec<ValueRange> RANGE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
    Codec.FLOAT.optionalFieldOf("min", 0.0F).forGetter(ValueRange::min),
    Codec.FLOAT.optionalFieldOf("max", Float.POSITIVE_INFINITY).forGetter(ValueRange::max)
  ).apply(instance, ValueRange::new));
  private static final Codec<ValueRange> VALUE_CODEC = Codec.either(Codec.FLOAT, RANGE_CODEC).xmap(
    either -> either.map(value -> new ValueRange(value, value), range -> range),
    range -> range.min() == range.max() ? Either.left(range.min()) : Either.right(range));

  public static final MapCodec<MaterialValueIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    MaterialPredicate.CODEC.optionalFieldOf("material", MaterialPredicate.ANY).forGetter(ingredient -> ingredient.material),
    VALUE_CODEC.fieldOf("value").forGetter(ingredient -> new ValueRange(ingredient.minValue, ingredient.maxValue))
  ).apply(instance, (material, range) -> new MaterialValueIngredient(material, range.min(), range.max())));

  private final IJsonPredicate<MaterialVariantId> material;
  private final float minValue;
  private final float maxValue;

  /** Creates an ingredient matching a range of values. */
  public static Ingredient of(IJsonPredicate<MaterialVariantId> materials, float minValue, float maxValue) {
    return new MaterialValueIngredient(materials, minValue, maxValue).toVanilla();
  }

  /** Creates an ingredient matching an exact value. */
  public static Ingredient of(IJsonPredicate<MaterialVariantId> materials, float value) {
    return of(materials, value, value);
  }

  /** Checks the given material recipe against our filters. */
  public boolean test(MaterialRecipe material) {
    float value = material.getValue() / (float) material.getNeeded();
    return minValue <= value && value <= maxValue && this.material.matches(material.getMaterial().getVariant());
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null) {
      return false;
    }
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe);
  }

  @Override
  public Stream<ItemStack> getItems() {
    return MaterialRecipeCache.getAllRecipes().stream()
      .filter(this::test)
      .flatMap(material -> Arrays.stream(material.getIngredient().getItems()));
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerMaterials.materialValueIngredientType.get();
  }

  /* Helpers for ShapedMaterialRecipe */

  /** Checks if this ingredient fully contains the range of the other. */
  private boolean contains(MaterialValueIngredient other) {
    return this.minValue <= other.minValue && other.maxValue <= this.maxValue;
  }

  /** Creates an ingredient that matches anything either of the two ingredients matches. */
  public MaterialValueIngredient merge(MaterialValueIngredient other) {
    if (this == other) return this;

    IJsonPredicate<MaterialVariantId> predicate = this.material;
    if (this.material.equals(other.material)) {
      if (this.contains(other)) {
        return this;
      }
      if (other.contains(this)) {
        return other;
      }
    } else {
      predicate = MaterialPredicate.or(this.material, other.material);
    }
    return new MaterialValueIngredient(predicate, Math.min(this.minValue, other.minValue), Math.max(this.maxValue, other.maxValue));
  }

  /** Gets the material matching this recipe. */
  @Nullable
  public MaterialVariantId getMaterial(ItemStack stack) {
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe) ? recipe.getMaterial().getVariant() : null;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof MaterialValueIngredient other
      && Float.compare(minValue, other.minValue) == 0
      && Float.compare(maxValue, other.maxValue) == 0
      && material.equals(other.material);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ID, material, minValue, maxValue);
  }
}
