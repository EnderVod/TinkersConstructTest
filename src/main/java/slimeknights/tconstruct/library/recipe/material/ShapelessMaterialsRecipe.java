package slimeknights.tconstruct.library.recipe.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shapeless recipe with a number of material ingredients to set the materials of the result.
 * Wraps vanilla's 1.21 shapeless recipe so its codec owns the vanilla recipe data.
 */
public class ShapelessMaterialsRecipe implements CraftingRecipe, MaterialsCraftingTableRecipe {
  private final ResourceLocation id;
  private final ShapelessRecipe recipe;
  /** Number of parts to match */
  @Getter
  private final int partCount;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;

  public ShapelessMaterialsRecipe(ResourceLocation id, ShapelessRecipe recipe, int partCount, List<MaterialVariantId> extraMaterials) {
    this.id = id;
    this.recipe = recipe;
    this.partCount = partCount;
    this.extraMaterials = extraMaterials;
  }

  /** Legacy helper used by JEI integration until it migrates to RecipeHolder IDs. */
  public ResourceLocation getId() {
    return id;
  }

  @Override
  public List<Ingredient> getParts() {
    return recipe.getIngredients();
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
  public boolean canCraftInDimensions(int width, int height) {
    return recipe.canCraftInDimensions(width, height);
  }

  @Override
  public boolean matches(CraftingInput inventory, Level level) {
    return recipe.matches(inventory, level);
  }

  /** Sets the material for the given stack */
  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    ShapedMaterialsRecipe.setMaterial(stack, material, extraMaterials);
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registries) {
    return ShapedMaterialsRecipe.assemble(recipe.assemble(inventory, registries), inventory, recipe.getIngredients(), partCount, false, extraMaterials);
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider registries) {
    return recipe.getResultItem(registries);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapelessMaterialsRecipeSerializer.get();
  }

  public static class Serializer implements LoggingRecipeSerializer<ShapelessMaterialsRecipe> {
    static final Loadable<List<MaterialVariantId>> EXTRA_MATERIALS = ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS;
    static final LoadableField<List<MaterialVariantId>,ShapelessMaterialsRecipe> MATERIAL_FIELD = EXTRA_MATERIALS.defaultField("extra_materials", List.of(), r -> r.extraMaterials);

    @Override
    public ShapelessMaterialsRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
      ShapelessRecipe vanilla = SHAPELESS_RECIPE.fromJson(recipeId, json);
      int parts = GsonHelper.getAsInt(json, "parts");
      if (parts < 1 || parts > vanilla.getIngredients().size()) {
        throw new JsonSyntaxException("Parts must be between 1 and the number of ingredients " + vanilla.getIngredients().size());
      }
      return new ShapelessMaterialsRecipe(recipeId, vanilla, parts, MATERIAL_FIELD.get(json));
    }

    @Override
    @Nullable
    public ShapelessMaterialsRecipe fromNetworkSafe(ResourceLocation recipeId, FriendlyByteBuf buffer) {
      ShapelessRecipe recipe = SHAPELESS_RECIPE.fromNetwork(recipeId, buffer);
      return recipe == null ? null : new ShapelessMaterialsRecipe(recipeId, recipe, buffer.readByte(), MATERIAL_FIELD.decode(buffer));
    }

    @Override
    public void toNetworkSafe(FriendlyByteBuf buffer, ShapelessMaterialsRecipe recipe) {
      SHAPELESS_RECIPE.toNetwork(buffer, recipe.recipe);
      buffer.writeByte(recipe.partCount);
      MATERIAL_FIELD.encode(buffer, recipe);
    }
  }
}
