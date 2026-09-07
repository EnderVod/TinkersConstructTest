package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;

/** Common subtype logic for the fluid and item forms of potion fluid. */
public interface PotionSubtypeInterpreter<T> extends IIngredientSubtypeInterpreter<T> {
  PotionContents getPotion(T ingredient);

  @Override
  default String apply(T ingredient, UidContext context) {
    PotionContents contents = getPotion(ingredient);
    if (contents.equals(PotionContents.EMPTY)) {
      return IIngredientSubtypeInterpreter.NONE;
    }
    String potionId = contents.potion()
      .flatMap(holder -> holder.unwrapKey())
      .map(key -> key.location().toString())
      .orElse("");
    StringBuilder builder = new StringBuilder(potionId);
    for (MobEffectInstance effect : contents.getAllEffects()) {
      builder.append(';').append(effect);
    }
    return builder.toString();
  }
}
