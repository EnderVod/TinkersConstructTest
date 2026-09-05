package slimeknights.tconstruct.library.modifiers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;

import javax.annotation.Nullable;
import java.util.List;

/** Compatibility helpers for the 1.21 NeoForge effect-cure system. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EffectCureHelper {
  /** Gets the cure selector corresponding to the given item. */
  public static EffectCure fromItem(Item item) {
    if (item == Items.MILK_BUCKET) {
      return EffectCures.MILK;
    }
    if (item == Items.HONEY_BOTTLE) {
      return EffectCures.HONEY;
    }
    if (item == Items.TOTEM_OF_UNDYING) {
      return EffectCures.PROTECTED_BY_TOTEM;
    }
    return EffectCure.get(BuiltInRegistries.ITEM.getKey(item).toString());
  }

  /** Gets the cure selector corresponding to the given stack's item. */
  public static EffectCure fromStack(ItemStack stack) {
    return fromItem(stack.getItem());
  }

  /** Replaces the instance cure set with the cures represented by the legacy curative item list. */
  public static void replaceCurativeItems(MobEffectInstance instance, @Nullable List<Item> items) {
    if (items != null) {
      var cures = instance.getCures();
      cures.clear();
      for (Item item : items) {
        cures.add(fromItem(item));
      }
    }
  }
}
