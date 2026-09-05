package slimeknights.tconstruct.tools.modules.ranged.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.entity.ProjectileWithKnockback;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import javax.annotation.Nullable;
import java.util.List;

/** Module implementing the punch modifier */
public record PunchModule(LevelingValue amount, ModifierCondition<IToolStackView> condition) implements ModifierModule, ProjectileLaunchModifierHook.NoShooter, ConditionalModule<IToolStackView> {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<PunchModule>defaultHooks(ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.PROJECTILE_SHOT);
  private static final ResourceLocation ARROW_KNOCKBACK = TConstruct.getResource("punch_knockback");
  public static final RecordLoadable<PunchModule> LOADER = RecordLoadable.create(LevelingValue.LOADABLE.directField(PunchModule::amount), ModifierCondition.TOOL_FIELD, PunchModule::new);

  @Override
  public RecordLoadable<PunchModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public void onProjectileShoot(IToolStackView tool, ModifierEntry modifier, @Nullable LivingEntity shooter, ItemStack ammo, Projectile projectile, @Nullable AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
    if (condition.matches(tool, modifier)) {
      float amount = this.amount.compute(modifier.getEffectiveLevel());
      if (amount > 0) {
        if (projectile instanceof ProjectileWithKnockback withKnockback) {
          withKnockback.addKnockback(amount);
        } else if (arrow != null) {
          persistentData.putFloat(ARROW_KNOCKBACK, persistentData.getFloat(ARROW_KNOCKBACK) + amount);
        }
      }
    }
  }

  /** Applies stored Tinkers punch after vanilla has accepted the arrow damage. */
  public static void applyArrowKnockback(AbstractArrow arrow, LivingEntity target) {
    ModDataNBT persistentData = PersistentDataCapability.getOrWarn(arrow);
    float knockback = persistentData.getFloat(ARROW_KNOCKBACK);
    if (knockback > 0) {
      double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
      net.minecraft.world.phys.Vec3 velocity = arrow.getDeltaMovement().multiply(1, 0, 1).normalize().scale(knockback * 0.6 * resistance);
      if (velocity.lengthSqr() > 0) {
        target.push(velocity.x, 0.1, velocity.z);
      }
    }
  }
}
