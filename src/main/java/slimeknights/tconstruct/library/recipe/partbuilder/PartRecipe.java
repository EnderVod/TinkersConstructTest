package slimeknights.tconstruct.library.recipe.partbuilder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.json.field.MergingField;
import slimeknights.tconstruct.library.json.field.MergingField.MissingMode;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.IMaterialValue;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Recipe to make a tool part from a material item in the part builder
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PartRecipe implements IPartBuilderRecipe, IMultiRecipe<IDisplayPartBuilderRecipe> {
  public static final RecordLoadable<PartRecipe> LOADER = RecordLoadable.create(
    ContextKey.ID.requiredField(),
    LoadableRecipeSerializer.RECIPE_GROUP,
    Pattern.PARSER.requiredField("pattern", PartRecipe::getPattern),
    IngredientLoadable.DISALLOW_EMPTY.defaultField("pattern_item", DEFAULT_PATTERNS, r -> r.patternItem),
    IntLoadable.FROM_ONE.requiredField("cost", PartRecipe::getCost),
    BooleanLoadable.INSTANCE.defaultField("allow_uncraftable", false, false, r -> r.allowUncraftable),
    new MergingField<>(TinkerLoadables.MATERIAL_ITEM.requiredField("item", r -> r.output), "result", MissingMode.DISALLOWED),
    new MergingField<>(IntLoadable.FROM_ONE.defaultField("count", 1, r -> r.outputCount), "result", MissingMode.CREATE),
    PartRecipe::new);

  @Getter
  protected final ResourceLocation id;
  @Getter
  protected final String group;
  @Getter
  protected final Pattern pattern;
  protected final Ingredient patternItem;
  /** Recipe material cost */
  @Getter
  protected final int cost;
  /** If true, this recipe can craft items normally not craftable in the part builder */
  @Getter
  @Accessors(fluent = true)
  protected final boolean allowUncraftable;
  /** Recipe result, used to fetch a material */
  protected final IMaterialItem output;
  /** Count for the recipe output */
  protected final int outputCount;

  /** @deprecated use {@link #PartRecipe(ResourceLocation, String, Pattern, Ingredient, int, boolean, IMaterialItem, int)} */
  @Deprecated(forRemoval = true)
  public PartRecipe(ResourceLocation id, String group, Pattern pattern, Ingredient patternItem, int cost, IMaterialItem output, int outputCount) {
    this(id, group, pattern, patternItem, cost, false, output, outputCount);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.partRecipeSerializer.get();
  }

  @Override
  public boolean partialMatch(IPartBuilderContainer inv) {
    if (!patternItem.test(inv.getPatternStack())) {
      return false;
    }
    ItemStack stack = inv.getStack();
    if (!stack.isEmpty()) {
      if (stack.getItem() == output) {
        return false;
      }
      IMaterialValue materialRecipe = inv.getMaterial();
      if (materialRecipe == null) {
        return false;
      }
      MaterialVariant material = materialRecipe.getMaterial();
      return (allowUncraftable || material.get().isCraftable()) && output.canUseMaterial(material.getId());
    }
    return true;
  }

  @Override
  public boolean matches(IPartBuilderContainer inv, Level world) {
    IMaterialValue materialRecipe = inv.getMaterial();
    if (materialRecipe != null) {
      MaterialVariant material = materialRecipe.getMaterial();
      return (allowUncraftable || material.get().isCraftable()) && output.canUseMaterial(material.getId())
             && inv.getStack().getCount() >= materialRecipe.getItemsUsed(cost);
    }
    return false;
  }

  /** @deprecated use {@link #getRecipeOutput(MaterialVariantId)} */
  @Deprecated
  @Override
  public ItemStack getResultItem(HolderLookup.Provider access) {
    return new ItemStack(output);
  }

  @SuppressWarnings("WeakerAccess")
  public ItemStack getRecipeOutput(MaterialVariantId material, int count) {
    ItemStack stack = output.withMaterial(material);
    stack.setCount(count);
    return stack;
  }

  @Deprecated(forRemoval = true)
  public ItemStack getRecipeOutput(MaterialVariantId material) {
    return getRecipeOutput(material, outputCount);
  }

  @Override
  public ItemStack assemble(IPartBuilderContainer inv, HolderLookup.Provider access) {
    MaterialVariant material = MaterialVariant.UNKNOWN;
    int count = outputCount;
    IMaterialValue materialRecipe = inv.getMaterial();
    if (materialRecipe != null) {
      material = materialRecipe.getMaterial();
      int value = materialRecipe.getValue();
      if (!materialRecipe.hasLeftover() && value > cost) {
        count = outputCount * value / cost;
      }
    }
    return this.getRecipeOutput(material.getVariant(), count);
  }

  @Nullable
  private List<IDisplayPartBuilderRecipe> multiRecipes;

  @Override
  public List<IDisplayPartBuilderRecipe> getRecipes(RegistryAccess access) {
    if (multiRecipes == null) {
      multiRecipes = MaterialRegistry
        .getMaterials().stream()
        .filter(mat -> (allowUncraftable || mat.isCraftable()) && output.canUseMaterial(mat))
        .flatMap(mat -> {
          List<MaterialVariantId> variants = MaterialRecipeCache.getVariants(mat.getIdentifier()).stream()
            .filter(variant -> !MaterialRecipeCache.getRecipes(variant).isEmpty()).toList();
          if (variants.isEmpty()) {
            return Stream.empty();
          }

          MaterialVariant materialTitle;
          List<ItemStack> materialItems;
          List<ItemStack> resultItems;
          if (variants.size() == 1) {
            MaterialVariantId variant = variants.get(0);
            materialTitle = MaterialVariant.of(variant);
            materialItems = MaterialRecipeCache.getItems(variant);
            resultItems = List.of(output.withMaterial(variant));
          } else {
            materialTitle = MaterialVariant.of(mat);
            materialItems = new ArrayList<>();
            resultItems = new ArrayList<>();
            for (MaterialVariantId variant : variants) {
              ItemStack result = output.withMaterial(variant);
              List<ItemStack> variantItems = MaterialRecipeCache.getItems(variant);
              materialItems.addAll(variantItems);
              for (int i = 0; i < variantItems.size(); i++) {
                resultItems.add(result);
              }
            }
            materialItems = List.copyOf(materialItems);
            resultItems = List.copyOf(resultItems);
          }
          return Stream.of(new DisplayPartRecipe(id, materialTitle, pattern, List.of(patternItem.getItems()), getCost(), materialItems, resultItems));
        })
        .collect(Collectors.toUnmodifiableList());
    }
    return multiRecipes;
  }
}
