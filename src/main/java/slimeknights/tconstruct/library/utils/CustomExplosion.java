package slimeknights.tconstruct.library.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/** Helper class for more control over explosions */
public class CustomExplosion extends Explosion {
  /** Size of the hollowed out cube determining the number of rays to cast */
  private static final int RAY_COUNT = 16;
  private static final int MAX_RAY = RAY_COUNT - 1;
  /** Default predicate for which entities to match */
  public static final Predicate<Entity> DEFAULT_ENTITY_PREDICATE = entity -> entity != null && entity.isAlive() && !entity.isSpectator();

  /*
   * Minecraft 1.21 made Explosion's state private. Mirror the state required by Tinkers'
   * custom calculation while aliasing the superclass block/player collections so that
   * inherited finalizeExplosion() still operates on our calculated results.
   */
  protected final Level level;
  @Nullable protected final Entity source;
  protected final boolean fire;
  protected final double x;
  protected final double y;
  protected final double z;
  protected final float radius;
  protected final DamageSource damageSource;
  protected final ExplosionDamageCalculator damageCalculator;
  protected final List<BlockPos> toBlow;
  protected final Map<Player,Vec3> hitPlayers;

  /** Maximum damage to deal; setting to 7*2*radius will match the vanilla explosion. */
  protected final float damage;
  /** Entity knockback scale. May be 0 to prevent knockback or negative to reverse knockback */
  protected final float knockback;
  /** Determines which entities are affected by this explosion */
  protected final Predicate<Entity> entityPredicate;
  /** If true, explosion damage bypasses the invulnerability time */
  protected final boolean bypassInvulnerableTime;

  public CustomExplosion(Level level, Vec3 location, float radius, @Nullable Entity sourceEntity, @Nullable Predicate<Entity> entityPredicate, float damage, @Nullable DamageSource damageSource, float knockback, @Nullable ExplosionDamageCalculator damageCalculator, boolean placeFire, BlockInteraction blockInteraction, boolean bypassInvulnerableTime) {
    super(level, sourceEntity, location.x, location.y, location.z, radius, placeFire, blockInteraction);
    this.level = level;
    this.source = sourceEntity;
    this.fire = placeFire;
    this.x = location.x;
    this.y = location.y;
    this.z = location.z;
    this.radius = radius;
    this.damageSource = damageSource != null ? damageSource : Explosion.getDefaultDamageSource(level, sourceEntity);
    this.damageCalculator = damageCalculator != null ? damageCalculator : new ExplosionDamageCalculator();
    this.toBlow = super.getToBlow();
    this.hitPlayers = super.getHitPlayers();
    this.entityPredicate = Objects.requireNonNullElse(entityPredicate, DEFAULT_ENTITY_PREDICATE);
    this.damage = damage;
    this.knockback = knockback;
    this.bypassInvulnerableTime = bypassInvulnerableTime;
  }

  public CustomExplosion(Level level, Vec3 location, float radius, @Nullable Entity sourceEntity, @Nullable Predicate<Entity> entityPredicate, float damage, @Nullable DamageSource damageSource, float knockback, @Nullable ExplosionDamageCalculator damageCalculator, boolean placeFire, BlockInteraction blockInteraction) {
    this(level, location, radius, sourceEntity, entityPredicate, damage, damageSource, knockback, damageCalculator, placeFire, blockInteraction, false);
  }

  @Override
  public void explode() {
    this.level.gameEvent(this.source, GameEvent.EXPLODE, center());
    calculateHitBlocks();
    damageAndPushEntities();
  }

  /** Calculates the list of blocks to hit; the actual block damage won't happen until {@link #finalizeExplosion(boolean)} */
  protected void calculateHitBlocks() {
    if (!interactsWithBlocks() && !fire) {
      return;
    }

    Set<BlockPos> set = new HashSet<>();
    for (int rayX = 0; rayX < RAY_COUNT; rayX++) {
      for (int rayY = 0; rayY < RAY_COUNT; rayY++) {
        for (int rayZ = 0; rayZ < RAY_COUNT; rayZ++) {
          if (rayX == 0 || rayX == MAX_RAY || rayY == 0 || rayY == MAX_RAY || rayZ == 0 || rayZ == MAX_RAY) {
            double stepX = rayX * 2.0 / MAX_RAY - 1;
            double stepY = rayY * 2.0 / MAX_RAY - 1;
            double stepZ = rayZ * 2.0 / MAX_RAY - 1;
            double stepScale = 0.3f / Math.sqrt(stepX * stepX + stepY * stepY + stepZ * stepZ);
            stepX *= stepScale;
            stepY *= stepScale;
            stepZ *= stepScale;

            double targetX = this.x;
            double targetY = this.y;
            double targetZ = this.z;
            for (float power = this.radius * (0.7f + level.random.nextFloat() * 0.6f); power > 0; power -= 0.225f) {
              BlockPos target = BlockPos.containing(targetX, targetY, targetZ);
              BlockState block = level.getBlockState(target);
              FluidState fluid = level.getFluidState(target);
              if (!level.isInWorldBounds(target)) {
                break;
              }

              Optional<Float> resistance = damageCalculator.getBlockExplosionResistance(this, level, target, block, fluid);
              if (resistance.isPresent()) {
                power -= (resistance.get() + 0.3f) * 0.3f;
              }

              if ((fire || !block.isAir()) && power > 0 && damageCalculator.shouldBlockExplode(this, level, target, block, power)) {
                set.add(target);
              }

              targetX += stepX;
              targetY += stepY;
              targetZ += stepZ;
            }
          }
        }
      }
    }
    toBlow.addAll(set);
  }

  /** Called to run the logic for damaging and blasting back entities in range */
  protected void damageAndPushEntities() {
    if (damage <= 0 && knockback == 0) {
      return;
    }

    float diameter = this.radius * 2;
    List<Entity> list = this.level.getEntities(
      this.source,
      new AABB(Math.floor(this.x - diameter - 1),
               Math.floor(this.y - diameter - 1),
               Math.floor(this.z - diameter - 1),
               Math.floor(this.x + diameter + 1),
               Math.floor(this.y + diameter + 1),
               Math.floor(this.z + diameter + 1)),
      entityPredicate);
    EventHooks.onExplosionDetonate(this.level, this, list, diameter);

    Vec3 center = center();
    for (Entity entity : list) {
      if (entity.ignoreExplosion(this)) {
        continue;
      }
      Vec3 dir = entity.position().subtract(center);
      double length = dir.length();
      double distance = length / diameter;
      if (distance <= 1) {
        if (!(entity instanceof PrimedTnt)) {
          dir = dir.add(0, entity.getEyeY() - entity.getY(), 0);
          length = dir.length();
        }
        if (length > 1.0E-4D) {
          double strength = (1 - distance) * getSeenPercent(center, entity);
          if (damage > 0) {
            int toDeal = (int) ((strength * strength + strength) / 2 * damage + 1);
            if (bypassInvulnerableTime) {
              ToolAttackUtil.hurtNoInvulnerableTime(entity, damageSource, toDeal);
            } else {
              entity.hurt(damageSource, toDeal);
            }
          }

          if (knockback != 0) {
            double adjustedStrength = strength * knockback;
            if (entity instanceof LivingEntity living) {
              adjustedStrength = ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, adjustedStrength);
            }
            Vec3 velocity = dir.scale(adjustedStrength / length);
            entity.setDeltaMovement(entity.getDeltaMovement().add(velocity));
            if (entity instanceof Player player) {
              if (!player.isCreative() || !player.getAbilities().flying) {
                hitPlayers.put(player, velocity);
              }
            }
          }
        }
      }
    }
  }

  /** Runs the logic on the server, syncing to the client. Based on {@link ServerLevel#explode(Entity, DamageSource, ExplosionDamageCalculator, double, double, double, float, boolean, ExplosionInteraction)}*/
  public void handleServer() {
    if (!level.isClientSide) {
      if (!EventHooks.onExplosionStart(level, this)) {
        explode();
        finalizeExplosion(false);
        syncToClient();
      }
    }
  }

  /** Runs the logic on both sides */
  public void doDualSide(Level level, boolean spawnParticles) {
    if (!EventHooks.onExplosionStart(level, this)) {
      explode();
      finalizeExplosion(spawnParticles);
    }
  }

  /** Syncs this explosion to the client */
  public void syncToClient() {
    if (!level.isClientSide && level instanceof ServerLevel server) {
      List<BlockPos> affectedBlocks = interactsWithBlocks() ? getToBlow() : List.of();
      Vec3 position = center();
      for (ServerPlayer player : server.players()) {
        if (player.distanceToSqr(position) < 4096.0D) {
          player.connection.send(new ClientboundExplodePacket(
            x, y, z, radius, affectedBlocks, hitPlayers.get(player), getBlockInteraction(),
            getSmallExplosionParticles(), getLargeExplosionParticles(), getExplosionSound()));
        }
      }
    }
  }
}
