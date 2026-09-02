package slimeknights.tconstruct.smeltery.block.entity.module;

import lombok.RequiredArgsConstructor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.common.TinkerTags.EntityTypes;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipeCache;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/** Module to handle fetching items from the bounds and interacting with entities in the structure. */
@RequiredArgsConstructor
public class EntityMeltingModule {
  private final MantleBlockEntity parent;
  private final IFluidHandler tank;
  private final BooleanSupplier canMeltEntities;
  private final Function<ItemStack, ItemStack> insertFunction;
  private final Supplier<AABB> bounds;

  @Nullable
  private EntityMeltingRecipe lastRecipe;

  private Level getLevel() {
    return Objects.requireNonNull(parent.getLevel(), "Parent tile entity has null world");
  }

  @Nullable
  private DamageSource smelteryMagic = null;
  private DamageSource smelteryMagic() {
    if (smelteryMagic == null) {
      smelteryMagic = TinkerDamageTypes.source(getLevel().registryAccess(), TinkerDamageTypes.SMELTERY_MAGIC);
    }
    return smelteryMagic;
  }

  @Nullable
  private DamageSource smelteryHeat = null;
  private DamageSource smelteryHeat() {
    if (smelteryHeat == null) {
      smelteryHeat = TinkerDamageTypes.source(getLevel().registryAccess(), TinkerDamageTypes.SMELTERY_HEAT);
    }
    return smelteryHeat;
  }

  @Nullable
  private EntityMeltingRecipe findRecipe(EntityType<?> type) {
    if (lastRecipe != null && lastRecipe.matches(type)) {
      return lastRecipe;
    }
    EntityMeltingRecipe recipe = EntityMeltingRecipeCache.findRecipe(getLevel().getRecipeManager(), type);
    if (recipe != null) {
      lastRecipe = recipe;
    }
    return recipe;
  }

  public static FluidStack getDefaultFluid() {
    return new FluidStack(TinkerFluids.liquidSoul.get(), FluidValues.GLASS_PANE / 5);
  }

  private boolean canMeltEntity(LivingEntity entity) {
    return !entity.isInvulnerableTo(entity.fireImmune() ? smelteryMagic() : smelteryHeat())
           && !(entity instanceof Player && ((Player)entity).getAbilities().invulnerable)
           && !entity.hasEffect(MobEffects.FIRE_RESISTANCE);
  }

  /** Interacts with entities in the structure. Returns true if something was melted and fuel is needed. */
  public boolean interactWithEntities() {
    AABB boundingBox = bounds.get();
    if (boundingBox == null) {
      return false;
    }

    Boolean canMelt = null;
    boolean melted = false;
    for (Entity entity : getLevel().getEntitiesOfClass(Entity.class, boundingBox)) {
      if (!entity.isAlive()) {
        continue;
      }

      EntityType<?> type = entity.getType();
      if (entity instanceof ItemEntity itemEntity) {
        ItemStack stack = insertFunction.apply(itemEntity.getItem());
        if (stack.isEmpty()) {
          entity.discard();
        } else {
          itemEntity.setItem(stack);
        }
      } else if (canMelt != Boolean.FALSE && !type.is(EntityTypes.MELTING_HIDE) && entity instanceof LivingEntity living && canMeltEntity(living)) {
        if (canMelt == null) {
          canMelt = canMeltEntities.getAsBoolean();
        }
        if (canMelt) {
          FluidStack fluid;
          int damage;
          EntityMeltingRecipe recipe = findRecipe(entity.getType());
          if (recipe != null) {
            fluid = recipe.getOutput(living);
            damage = recipe.getDamage();
          } else {
            fluid = getDefaultFluid();
            damage = 2;
          }

          if (entity.hurt(entity.fireImmune() ? smelteryMagic() : smelteryHeat(), damage)) {
            tank.fill(fluid, FluidAction.EXECUTE);
            melted = true;
          }
        }
      }
    }
    return melted;
  }
}
