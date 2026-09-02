package slimeknights.tconstruct.gadgets.capability;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.common.network.TinkerNetwork;

import java.util.List;

/**
 * Tracks the last passenger list for a player so Tinkers can explicitly resync piggyback riders when needed.
 * This state is intentionally transient; the world already owns entity persistence.
 */
@RequiredArgsConstructor
public class PiggybackHandler {
  /** Player associated with this handler. */
  private final Player riddenPlayer;
  /** Last found list of passengers, used for change detection and syncing. */
  private List<Entity> lastPassengers;

  /** Updates the passengers on the back and synchronizes changes to the ridden player. */
  public void updatePassengers() {
    if (!this.riddenPlayer.getPassengers().equals(this.lastPassengers)) {
      if (this.riddenPlayer instanceof ServerPlayer) {
        TinkerNetwork.getInstance().sendVanillaPacket(this.riddenPlayer, new ClientboundSetPassengersPacket(this.riddenPlayer));
      }
    }
    this.lastPassengers = this.riddenPlayer.getPassengers();
  }
}
