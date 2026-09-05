package slimeknights.tconstruct.library.recipe.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shaped recipe with material value ingredients that set the material of the result.
 * @deprecated use {@link ShapedMaterialsRecipe}, which requires specifying the ingredients for each part.
 */
@Deprecated
public class ShapedMaterialRecipe implements CraftingRecipe {
  private final ResourceLocation id;
  private final ShapedRecipe recipe;
  private MaterialValueIngredient material;
  private final List<MaterialVariantId> extraMaterials;

  public ShapedMaterialRecipe(ResourceLocation id, ShapedRecipe recipe, List<MaterialVariantId> extraMaterials) {
    this.id = id;
    this.recipe = recipe;
    this.extraMaterials = extraMaterials;
  }

  /** Legacy helper used by diagnostics/JEI until all callers migrate to RecipeHolder IDs. */
  public ResourceLocation getId() {
    return id;
  }

  public int getWidth() {
    return recipe.getWidth();
  }

  public int getHeight() {
    return recipe.getHeight();
  }

  @Override
  public NonNullList<Ingredient> getIngredients() {
    return recipe.getIngredients();
  }

  @Override
  public String getGroup() {
    return recipe.getGroup();
  }

  @Override
  public CraftingBookCategory category() {
    return recipe.category();
  }

  @Override
  public boolean showNotification() {
    return recipe.showNotification();
  }

  @Override
  public boolean canCraftInDimensions(int width, int height) {
    return recipe.canCraftInDimensions(width, height);
  }

  @Override
  public boolean isIncomplete() {
    return recipe.isIncomplete();
  }

  /** Gets the material to match */
  @Nullable
  public MaterialValueIngredient getMaterial() {
    if (material == null) {
      for (Ingredient ingredient : recipe.getIngredients()) {
        if (ingredient.getCustomIngredient() instanceof MaterialValueIngredient materialValue) {
          if (material == null) {
            material = materialValue;
          } else {
            material = material.merge(materialValue);
          }
        }
      }
      if (material == null) {
        TConstruct.LOG.error("No material ingredient found for material shaped recipe {}, this indicates a broken recipe", id);
      }
    }
    return material;
  }

  @Nullable
  private MaterialVariantId findMaterial(CraftingInput inventory) {
    MaterialValueIngredient material = getMaterial();
    if (material == null) {
      return null;
    }
    MaterialVariantId firstMaterial = null;
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        MaterialVariantId matchedMaterial = material.getMaterial(stack);
        if (matchedMaterial != null) {
          if (firstMaterial == null) {
            firstMaterial = matchedMaterial;
          } else if (!firstMaterial.matchesVariant(matchedMaterial)) {
            if (firstMaterial.getId().equals(matchedMaterial.getId())) {
              firstMaterial = firstMaterial.getId();
            } else {
              return null;
            }
          }
        }
      }
    }
    return firstMaterial;
  }

  @Override
  public boolean matches(CraftingInput inventory, Level level) {
    return recipe.matches(inventory, level) && findMaterial(inventory) != null;
  }

  /** Sets the material for the given stack */
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    ShapedMaterialsRecipe.setMaterial(stack, material, extraMaterials);
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registries) {
    ItemStack stack = recipe.assemble(inventory, registries);
    MaterialVariantId material = findMaterial(inventory);
    if (material != null) {
      setMaterial(stack, material);
    }
    return stack;
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider registries) {
    return recipe.getResultItem(registries);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapedMaterialRecipeSerializer.get();
  }

  public static class Serializer implements LoggingRecipeSerializer<ShapedMaterialRecipe> {
    static final Loadable<List<MaterialVariantId>> EXTRA_MATERIALS = ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS;
    static final LoadableField<List<MaterialVariantId>,ShapedMaterialRecipe> MATERIAL_FIELD = EXTRA_MATERIALS.defaultField("extra_materials", List.of(), r -> r.extraMaterials);

    @Override
    public ShapedMaterialRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
      ShapedRecipe vanilla = SHAPED_RECIPE.fromJson(recipeId, json);
      ShapedMaterialRecipe recipe = new ShapedMaterialRecipe(recipeId, vanilla, MATERIAL_FIELD.get(json));
      if (recipe.getMaterial() == null) {
        throw new JsonSyntaxException("Invalid material ingredients for shaped material recipe " + recipeId);
      }
      return recipe;
    }

    @Override
    @Nullable
    public ShapedMaterialRecipe fromNetworkSafe(ResourceLocation recipeId, FriendlyByteBuf buffer) {
      ShapedRecipe recipe = SHAPED_RECIPE.fromNetwork(recipeId, buffer);
      List<MaterialVariantId> extraMaterials = MATERIAL_FIELD.decode(buffer);
      return recipe == null ? null : new ShapedMaterialRecipe(recipeId, recipe, extraMaterials);
    }

    @Override
    public void toNetworkSafe(FriendlyByteBuf buffer, ShapedMaterialRecipe recipe) {
      SHAPED_RECIPE.toNetwork(buffer, recipe.recipe);
      MATERIAL_FIELD.encode(buffer, recipe);
    }
  }
}
