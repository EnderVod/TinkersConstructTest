package slimeknights.tconstruct.library.recipe.material;

import com.google.gson.JsonArray;
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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Shaped recipe with material ingredients that set the materials of the result.
 * Wraps vanilla's 1.21 shaped recipe so the vanilla codec remains authoritative for pattern/result data.
 */
public class ShapedMaterialsRecipe implements CraftingRecipe, MaterialsCraftingTableRecipe {
  private final ResourceLocation id;
  private final ShapedRecipe recipe;
  /** List of tool parts to search for in the final recipe */
  @Getter
  private final List<Ingredient> parts;
  /** If true, repeated inputs for one part must use the same material. */
  private final boolean checkRepeats;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;

  public ShapedMaterialsRecipe(ResourceLocation id, ShapedRecipe recipe, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    this.id = id;
    this.recipe = recipe;
    this.parts = parts;
    this.checkRepeats = parts.stream().unordered().distinct().count() == parts.size();
    this.extraMaterials = extraMaterials;
  }

  /** Legacy helper used by JEI integration until it migrates to RecipeHolder IDs. */
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
  public int getPartCount() {
    return parts.size();
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

  /**
   * Finds materials for each of the parts.
   * @return array of matched materials, or null if the inputs mix incompatible materials.
   */
  @Nullable
  static MaterialVariantId[] findMaterials(CraftingInput inventory, List<Ingredient> parts, int partCount, boolean checkRepeats) {
    MaterialVariantId[] materials = new MaterialVariantId[partCount];
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        for (int p = 0; p < partCount; p++) {
          MaterialVariantId current = materials[p];
          if ((current == null || checkRepeats) && parts.get(p).test(stack)) {
            MaterialVariantId matched;
            if (stack.getItem() instanceof IMaterialItem materialItem) {
              matched = materialItem.getMaterial(stack);
            } else {
              matched = MaterialRecipeCache.findRecipe(stack).getMaterial().getVariant();
            }
            if (current == null) {
              materials[p] = matched;
              break;
            } else if (!current.matchesVariant(matched)) {
              if (current.getId().equals(matched.getId())) {
                materials[p] = current.getId();
                break;
              }
              return null;
            }
          }
        }
      }
    }
    for (int p = 0; p < partCount; p++) {
      if (materials[p] == null) {
        return null;
      }
    }
    return materials;
  }

  @Override
  public boolean matches(CraftingInput inventory, Level level) {
    return recipe.matches(inventory, level) && findMaterials(inventory, parts, parts.size(), checkRepeats) != null;
  }

  /** Common logic shared by the material crafting recipes. */
  public static void setMaterial(ItemStack stack, MaterialVariantId material, List<MaterialVariantId> extraMaterials) {
    if (extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
      materialItem.setMaterial(stack, material);
    } else {
      MaterialNBT.Builder builder = MaterialNBT.builder();
      builder.add(material);
      for (MaterialVariantId extraMaterial : extraMaterials) {
        builder.add(extraMaterial);
      }
      ToolStack.from(stack).setMaterials(builder.build());
    }
  }

  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    setMaterial(stack, material, extraMaterials);
  }

  /** Assembles the item with material information. */
  static ItemStack assemble(ItemStack stack, CraftingInput inventory, List<Ingredient> parts, int partCount, boolean checkRepeats, List<MaterialVariantId> extraMaterials) {
    MaterialVariantId[] materials = findMaterials(inventory, parts, partCount, checkRepeats);
    if (materials != null) {
      if (materials.length == 1 && extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
        return materialItem.setMaterial(stack, materials[0]);
      }
      MaterialNBT.Builder builder = MaterialNBT.builder();
      for (MaterialVariantId material : materials) {
        builder.add(material);
      }
      builder.add(extraMaterials);
      ToolStack.from(stack).setMaterials(builder.build());
    }
    return stack;
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registries) {
    return assemble(recipe.assemble(inventory, registries), inventory, parts, parts.size(), checkRepeats, extraMaterials);
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider registries) {
    return recipe.getResultItem(registries);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapedMaterialsRecipeSerializer.get();
  }

  public static class Serializer implements LoggingRecipeSerializer<ShapedMaterialsRecipe> {
    static final Loadable<List<MaterialVariantId>> EXTRA_MATERIALS = MaterialVariantId.LOADABLE.list(0);
    static final LoadableField<List<MaterialVariantId>, ShapedMaterialsRecipe> MATERIAL_FIELD = EXTRA_MATERIALS.defaultField("extra_materials", List.of(), r -> r.extraMaterials);

    /** Maps the legacy "parts" symbol list onto the Ingredient instances already decoded by vanilla. */
    private static List<Ingredient> partsFromJson(JsonObject json, ShapedRecipe vanilla) {
      JsonArray rawPattern = GsonHelper.getAsJsonArray(json, "pattern");
      List<String> rows = new ArrayList<>(rawPattern.size());
      for (int i = 0; i < rawPattern.size(); i++) {
        rows.add(rawPattern.get(i).getAsString());
      }

      int top = 0;
      while (top < rows.size() && rows.get(top).trim().isEmpty()) {
        top++;
      }
      int bottom = rows.size() - 1;
      while (bottom >= top && rows.get(bottom).trim().isEmpty()) {
        bottom--;
      }
      if (top > bottom) {
        throw new JsonSyntaxException("Invalid empty shaped recipe pattern");
      }

      int left = Integer.MAX_VALUE;
      int right = -1;
      for (int y = top; y <= bottom; y++) {
        String row = rows.get(y);
        int first = 0;
        while (first < row.length() && row.charAt(first) == ' ') {
          first++;
        }
        int last = row.length() - 1;
        while (last >= 0 && row.charAt(last) == ' ') {
          last--;
        }
        if (first <= last) {
          left = Math.min(left, first);
          right = Math.max(right, last);
        }
      }

      int width = vanilla.getWidth();
      int height = vanilla.getHeight();
      if (bottom - top + 1 != height || right - left + 1 != width) {
        throw new JsonSyntaxException("Vanilla shaped recipe dimensions do not match the decoded pattern");
      }

      List<Ingredient> inputs = vanilla.getIngredients();
      String partPattern = GsonHelper.getAsString(json, "parts");
      List<Ingredient> parts = new ArrayList<>(partPattern.length());
      for (int p = 0; p < partPattern.length(); p++) {
        char symbol = partPattern.charAt(p);
        Ingredient found = null;
        for (int y = 0; y < height && found == null; y++) {
          String row = rows.get(top + y);
          for (int x = 0; x < width; x++) {
            int rawX = left + x;
            if (rawX < row.length() && row.charAt(rawX) == symbol) {
              found = inputs.get(y * width + x);
              break;
            }
          }
        }
        if (found == null || found.isEmpty()) {
          throw new JsonSyntaxException("Parts references symbol '" + symbol + "' but it is not present in the shaped pattern");
        }
        parts.add(found);
      }
      return List.copyOf(parts);
    }

    @Override
    public ShapedMaterialsRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
      ShapedRecipe vanilla = SHAPED_RECIPE.fromJson(recipeId, json);
      return new ShapedMaterialsRecipe(recipeId, vanilla, partsFromJson(json, vanilla), MATERIAL_FIELD.get(json));
    }

    @Override
    @Nullable
    public ShapedMaterialsRecipe fromNetworkSafe(ResourceLocation recipeId, FriendlyByteBuf buffer) {
      ShapedRecipe vanilla = SHAPED_RECIPE.fromNetwork(recipeId, buffer);
      if (vanilla == null) {
        return null;
      }
      List<MaterialVariantId> extraMaterials = MATERIAL_FIELD.decode(buffer);
      List<Ingredient> inputs = vanilla.getIngredients();
      int size = buffer.readVarInt();
      List<Ingredient> parts = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        int index = buffer.readVarInt();
        if (index < 0 || index >= inputs.size()) {
          throw new IllegalArgumentException("Invalid shaped material part index " + index);
        }
        parts.add(inputs.get(index));
      }
      return new ShapedMaterialsRecipe(recipeId, vanilla, List.copyOf(parts), extraMaterials);
    }

    @Override
    public void toNetworkSafe(FriendlyByteBuf buffer, ShapedMaterialsRecipe recipe) {
      SHAPED_RECIPE.toNetwork(buffer, recipe.recipe);
      MATERIAL_FIELD.encode(buffer, recipe);
      List<Ingredient> inputs = recipe.recipe.getIngredients();
      buffer.writeVarInt(recipe.parts.size());
      for (Ingredient part : recipe.parts) {
        int index = -1;
        for (int i = 0; i < inputs.size(); i++) {
          if (inputs.get(i) == part) {
            index = i;
            break;
          }
        }
        if (index < 0) {
          throw new IllegalStateException("Material part is not part of the wrapped shaped recipe");
        }
        buffer.writeVarInt(index);
      }
    }
  }
}
