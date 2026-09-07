package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Adapts Tinkers' legacy per-recipe crafting extensions to JEI 19.42's singleton extension API.
 * The factory may return null to let JEI fall back when a recipe cannot be displayed safely.
 */
@SuppressWarnings({"removal", "deprecation"})
public record LegacyCraftingExtension<R extends CraftingRecipe>(Function<R, ? extends ICraftingCategoryExtension<R>> factory)
  implements ICraftingCategoryExtension<R> {

  @Nullable
  private ICraftingCategoryExtension<R> extension(RecipeHolder<R> holder) {
    return factory.apply(holder.value());
  }

  @Override
  public boolean isHandled(RecipeHolder<R> holder) {
    return extension(holder) != null;
  }

  @Override
  public int getWidth(RecipeHolder<R> holder) {
    ICraftingCategoryExtension<R> extension = extension(holder);
    return extension == null ? 0 : extension.getWidth();
  }

  @Override
  public int getHeight(RecipeHolder<R> holder) {
    ICraftingCategoryExtension<R> extension = extension(holder);
    return extension == null ? 0 : extension.getHeight();
  }

  @Override
  public void setRecipe(RecipeHolder<R> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    ICraftingCategoryExtension<R> extension = extension(holder);
    if (extension != null) {
      extension.setRecipe(builder, craftingGridHelper, focuses);
    }
  }
}
