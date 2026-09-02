package slimeknights.tconstruct.gadgets.capability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Transient piggyback state.
 * <p>
 * The original Forge capability never serialized any data; it existed only to keep a passenger-sync helper attached
 * to players. On NeoForge 1.21 we can model that intent directly without depending on the removed Forge capability API.
 */
public final class PiggybackCapability {
  private static final Map<Player,PiggybackHandler> HANDLERS = Collections.synchronizedMap(new WeakHashMap<>());

  private PiggybackCapability() {}

  /** Kept as a bootstrap hook for compatibility with the existing gadget module. */
  public static void register() {}

  /** Gets the passenger-sync helper when the living entity is a player. */
  public static Optional<PiggybackHandler> get(LivingEntity entity) {
    if (entity instanceof Player player) {
      return Optional.of(HANDLERS.computeIfAbsent(player, PiggybackHandler::new));
    }
    return Optional.empty();
  }
}
