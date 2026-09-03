package slimeknights.tconstruct.library.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.shared.TinkerMaterials;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Custom ingredient that displays material variants on material items and supports filtering by material predicates.
 */
public class MaterialIngredient extends NestedIngredient {
  public static final ResourceLocation ID = TConstruct.getResource("material");
  public static final MapCodec<MaterialIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    NESTED_CODEC.forGetter(ingredient -> ingredient.nested),
    MaterialPredicate.CODEC.optionalFieldOf("material", MaterialPredicate.ANY).forGetter(ingredient -> ingredient.material)
  ).apply(instance, MaterialIngredient::new));

  private final IJsonPredicate<MaterialVariantId> material;

  protected MaterialIngredient(Ingredient nested, IJsonPredicate<MaterialVariantId> material) {
    super(nested);
    this.material = material;
  }

  /** @deprecated use {@link #MaterialIngredient(Ingredient, IJsonPredicate)} */
  @Deprecated(forRemoval = true)
  protected MaterialIngredient(Ingredient nested, MaterialVariantId material, @Nullable TagKey<IMaterial> tag) {
    this(nested, makePredicate(material, tag));
  }

  /** Converts the legacy material and tag into a predicate. */
  private static IJsonPredicate<MaterialVariantId> makePredicate(MaterialVariantId material, @Nullable TagKey<IMaterial> tag) {
    IJsonPredicate<MaterialVariantId> predicate = material.equals(IMaterial.UNKNOWN.getIdentifier()) ? MaterialPredicate.ANY : MaterialPredicate.variant(material);
    if (tag != null) {
      IJsonPredicate<MaterialVariantId> tagPredicate = MaterialPredicate.tag(tag);
      predicate = predicate == MaterialPredicate.ANY ? tagPredicate : MaterialPredicate.and(predicate, tagPredicate);
    }
    return predicate;
  }

  /** Creates an ingredient matching the given materials. */
  public static Ingredient of(Ingredient ingredient, IJsonPredicate<MaterialVariantId> material) {
    return new MaterialIngredient(ingredient, material).toVanilla();
  }

  /** Creates an ingredient matching the given materials. */
  public static Ingredient of(ItemLike item, IJsonPredicate<MaterialVariantId> material) {
    return of(Ingredient.of(item), material);
  }

  /** Creates an ingredient matching any material. */
  public static Ingredient of(Ingredient ingredient) {
    return of(ingredient, MaterialPredicate.ANY);
  }

  /** Creates an ingredient matching a single material. */
  public static Ingredient of(Ingredient ingredient, MaterialVariantId material) {
    return of(ingredient, MaterialPredicate.variant(material));
  }

  /** Creates an ingredient matching a material tag. */
  public static Ingredient of(Ingredient ingredient, TagKey<IMaterial> tag) {
    return of(ingredient, MaterialPredicate.tag(tag));
  }

  /** Creates a new instance from an item with a fixed material. */
  public static Ingredient of(ItemLike item, MaterialVariantId material) {
    return of(Ingredient.of(item), material);
  }

  /** Creates a new instance from an item with a tagged material. */
  public static Ingredient of(ItemLike item, TagKey<IMaterial> tag) {
    return of(Ingredient.of(item), tag);
  }

  /** Creates a new ingredient matching any material from an item. */
  public static Ingredient of(ItemLike item) {
    return of(Ingredient.of(item));
  }

  /** Creates a new ingredient from an item tag with a fixed material. */
  public static Ingredient of(TagKey<Item> tag, MaterialVariantId material) {
    return of(Ingredient.of(tag), material);
  }

  /** Creates a new ingredient matching any material from an item tag. */
  public static Ingredient of(TagKey<Item> tag) {
    return of(Ingredient.of(tag));
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || stack.isEmpty() || !super.test(stack)) {
      return false;
    }
    return material == MaterialPredicate.ANY || material.matches(IMaterialItem.getMaterialFromStack(stack));
  }

  @Override
  public Stream<ItemStack> getItems() {
    if (!MaterialRegistry.isFullyLoaded()) {
      return Arrays.stream(nested.getItems());
    }
    return Arrays.stream(nested.getItems())
      .flatMap(stack -> MaterialRecipeCache.getAllVariants().stream()
        .filter(material::matches)
        .map(mat -> IMaterialItem.withMaterial(stack, mat))
        .filter(ItemStack::hasTag))
      .distinct();
  }

  @Override
  public boolean isSimple() {
    return material == MaterialPredicate.ANY && nested.isSimple();
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerMaterials.materialIngredientType.get();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof MaterialIngredient other && nested.equals(other.nested) && material.equals(other.material);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ID, nested, material);
  }
}
