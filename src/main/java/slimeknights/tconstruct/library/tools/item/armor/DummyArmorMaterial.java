package slimeknights.tconstruct.library.tools.item.armor;

import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.registration.object.IdAwareObject;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight armor material wrapper used by modifiable armor.
 * Tinkers supplies its real protection and durability through tool stats, while vanilla 1.21 requires
 * armor items to receive a {@link Holder} containing an {@link ArmorMaterial} record.
 */
@Getter
public class DummyArmorMaterial implements IdAwareObject {
  private final ResourceLocation id;
  private final SoundEvent equipSound;
  private final Holder<ArmorMaterial> material;

  public DummyArmorMaterial(ResourceLocation id, SoundEvent equipSound) {
    this.id = id;
    this.equipSound = equipSound;

    Map<Type,Integer> defense = new EnumMap<>(Type.class);
    for (Type type : Type.values()) {
      defense.put(type, 0);
    }
    this.material = Holder.direct(new ArmorMaterial(
      defense,
      0,
      BuiltInRegistries.SOUND_EVENT.wrapAsHolder(equipSound),
      () -> Ingredient.EMPTY,
      List.of(new ArmorMaterial.Layer(id)),
      0.0F,
      0.0F));
  }
}
