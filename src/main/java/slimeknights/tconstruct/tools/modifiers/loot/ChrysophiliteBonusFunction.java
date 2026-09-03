package slimeknights.tconstruct.tools.modifiers.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.tconstruct.shared.TinkerAttributes;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.List;
import java.util.Set;

/** Loot modifier to boost drops based on chrysophilite amount. */
public class ChrysophiliteBonusFunction extends LootItemConditionalFunction {
  public static final MapCodec<ChrysophiliteBonusFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
    commonFields(instance).and(instance.group(
      BonusFormula.CODEC.forGetter(function -> function.formula),
      Codec.BOOL.optionalFieldOf("include_base", true).forGetter(function -> function.includeBase)
    )).apply(instance, ChrysophiliteBonusFunction::new));

  private final BonusFormula formula;
  private final boolean includeBase;

  protected ChrysophiliteBonusFunction(List<LootItemCondition> conditions, BonusFormula formula, boolean includeBase) {
    super(conditions);
    this.formula = formula;
    this.includeBase = includeBase;
  }

  public static Builder<?> builder(BonusFormula formula, boolean includeBase) {
    return simpleBuilder(conditions -> new ChrysophiliteBonusFunction(conditions, formula, includeBase));
  }

  public static Builder<?> binomialWithBonusCount(float probability, int extra, boolean includeBase) {
    return builder(BonusFormula.binomialBonus(extra, probability), includeBase);
  }

  public static Builder<?> oreDrops(boolean includeBase) {
    return builder(BonusFormula.oreDrops(), includeBase);
  }

  public static Builder<?> uniformBonusCount(int bonusMultiplier, boolean includeBase) {
    return builder(BonusFormula.uniformBonus(bonusMultiplier), includeBase);
  }

  @Override
  protected ItemStack run(ItemStack stack, LootContext context) {
    if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity entity) {
      int level = (int) entity.getAttributeValue(TinkerAttributes.CHRYSOPHILITE.get());
      if (!includeBase) {
        level--;
      }
      if (level > 0) {
        stack.setCount(formula.calculateNewCount(context.getRandom(), stack.getCount(), level));
      }
    }
    return stack;
  }

  @Override
  public Set<LootContextParam<?>> getReferencedContextParams() {
    return ImmutableSet.of(LootContextParams.THIS_ENTITY);
  }

  @Override
  public LootItemFunctionType getType() {
    return TinkerModifiers.chrysophiliteBonusFunction.get();
  }
}
