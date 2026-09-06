package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import slimeknights.tconstruct.library.modifiers.hook.combat.ArmorLootingModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.LootingModifierHook;
import slimeknights.tconstruct.library.tools.context.LootingContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logic to handle the looting event for all main tinker tools
 */
public class ModifierLootingHandler {
  /** If contained in the set, they should use the offhand for looting */
  private static final Map<UUID,EquipmentSlot> LOOTING_OFFHAND = new HashMap<>();
  private static boolean init = false;

  /** Initializies this listener */
  public static void init() {
    if (init) {
      return;
    }
    init = true;
    // we overwrite looting values from vanilla in a couple cases, but mod effects that globally boost looting should still boost us
    NeoForge.EVENT_BUS.addListener(ModifierLootingHandler::onLeaveServer);
  }

  /**
   * Sets the hand used for looting, so the tool is fetched from the proper context
   * @param entity    Player to set
   * @param slotType  Slot type
   */
  public static void setLootingSlot(LivingEntity entity, EquipmentSlot slotType) {
    if (slotType == EquipmentSlot.MAINHAND) {
      LOOTING_OFFHAND.remove(entity.getUUID());
    } else {
      LOOTING_OFFHAND.put(entity.getUUID(), slotType);
    }
  }

  /** Gets the slot to use for looting */
  public static EquipmentSlot getLootingSlot(@Nullable LivingEntity entity) {
    return entity != null ? LOOTING_OFFHAND.getOrDefault(entity.getUUID(), EquipmentSlot.MAINHAND) : EquipmentSlot.MAINHAND;
  }

  /**
   * Rebuilds the looting value for a loot context on 1.21.
   * Vanilla no longer stores a mutable looting integer directly on {@link LootContext}, so start from the
   * enchantment on the active held item and then run Tinkers' weapon and armor looting hooks.
   */
  public static int getLooting(IToolStackView tool, LootContext context) {
    Entity target = context.getParamOrNull(LootContextParams.THIS_ENTITY);
    Entity attackingEntity = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
    if (target == null || !(attackingEntity instanceof LivingEntity holder)) {
      return 0;
    }

    // Projectiles intentionally have no looting slot; this matches the existing LootingContext contract.
    EquipmentSlot slot = context.getParamOrNull(LootContextParams.DIRECT_ATTACKING_ENTITY) instanceof Projectile
                         ? null : getLootingSlot(holder);

    int looting = 0;
    if (slot != null) {
      var enchantment = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
      looting = EnchantmentHelper.getItemEnchantmentLevel(enchantment, holder.getItemBySlot(slot));
    }

    LootingContext lootingContext = new LootingContext(holder, target, context.getParamOrNull(LootContextParams.DAMAGE_SOURCE), slot);
    looting = LootingModifierHook.getLooting(tool, lootingContext, looting);
    return ArmorLootingModifierHook.getLooting(slot == null ? null : tool, lootingContext, looting);
  }

  /** Called when a player leaves the server to clear the face */
  private static void onLeaveServer(PlayerLoggedOutEvent event) {
    LOOTING_OFFHAND.remove(event.getEntity().getUUID());
  }
}
