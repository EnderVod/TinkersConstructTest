package slimeknights.tconstruct.library.tools.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Interface to implement for tools that also display in the tinker station
 */
public interface ITinkerStationDisplay extends ItemLike {
  default Component getLocalizedName() {
    return Component.translatable(asItem().getDescriptionId());
  }

  default List<Component> getStatInformation(IToolStackView tool, @Nullable Player player, List<Component> tooltips, TooltipKey key, TooltipFlag tooltipFlag) {
    tooltips = TooltipUtil.getDefaultStats(tool, player, tooltips, key, tooltipFlag);
    TooltipUtil.addAttributes(this, tool, player, tooltips, TooltipUtil.SHOW_MELEE_ATTRIBUTES, EquipmentSlot.MAINHAND);
    return tooltips;
  }

  default Multimap<Attribute,AttributeModifier> getAttributeModifiers(IToolStackView tool, EquipmentSlot slot) {
    return ImmutableMultimap.of();
  }

  /**
   * Converts Tinkers' dynamic attribute map into the 1.21 item attribute component.
   * This replaces the removed Item#getAttributeModifiers bridge used by modifiable items.
   */
  default ItemAttributeModifiers buildAttributeModifiers(ItemStack stack, EquipmentSlot... slots) {
    IToolStackView tool = ToolStack.from(stack);
    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
    for (EquipmentSlot slot : slots) {
      EquipmentSlotGroup group = switch (slot) {
        case MAINHAND -> EquipmentSlotGroup.MAINHAND;
        case OFFHAND -> EquipmentSlotGroup.OFFHAND;
        case FEET -> EquipmentSlotGroup.FEET;
        case LEGS -> EquipmentSlotGroup.LEGS;
        case CHEST -> EquipmentSlotGroup.CHEST;
        case HEAD -> EquipmentSlotGroup.HEAD;
        default -> EquipmentSlotGroup.ANY;
      };
      getAttributeModifiers(tool, slot).forEach((attribute, modifier) ->
        builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), modifier, group));
    }
    return builder.build();
  }
}
