package slimeknights.tconstruct.tools.modifiers.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** 1.21-compatible copy of the vanilla bonus-count formula data used by Tinkers loot functions. */
public record BonusFormula(ResourceLocation id, Parameters parameters) {
  private static final ResourceLocation ORE_DROPS = ResourceLocation.withDefaultNamespace("ore_drops");
  private static final ResourceLocation UNIFORM_BONUS = ResourceLocation.withDefaultNamespace("uniform_bonus_count");
  private static final ResourceLocation BINOMIAL_BONUS = ResourceLocation.withDefaultNamespace("binomial_with_bonus_count");

  public static final MapCodec<BonusFormula> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    ResourceLocation.CODEC.fieldOf("formula").forGetter(BonusFormula::id),
    Parameters.CODEC.optionalFieldOf("parameters", Parameters.DEFAULT).forGetter(BonusFormula::parameters)
  ).apply(instance, BonusFormula::new));

  public static BonusFormula oreDrops() {
    return new BonusFormula(ORE_DROPS, Parameters.DEFAULT);
  }

  public static BonusFormula uniformBonus(int multiplier) {
    return new BonusFormula(UNIFORM_BONUS, new Parameters(multiplier, 0, 0));
  }

  public static BonusFormula binomialBonus(int extra, float probability) {
    return new BonusFormula(BINOMIAL_BONUS, new Parameters(1, extra, probability));
  }

  public int calculateNewCount(RandomSource random, int count, int level) {
    if (id.equals(ORE_DROPS)) {
      if (level > 0) {
        int bonus = random.nextInt(level + 2) - 1;
        if (bonus < 0) {
          bonus = 0;
        }
        return count * (bonus + 1);
      }
      return count;
    }
    if (id.equals(UNIFORM_BONUS)) {
      return count + random.nextInt(parameters.bonusMultiplier() * level + 1);
    }
    if (id.equals(BINOMIAL_BONUS)) {
      int result = count;
      for (int i = 0; i < level + parameters.extra(); i++) {
        if (random.nextFloat() < parameters.probability()) {
          result++;
        }
      }
      return result;
    }
    return count;
  }

  public record Parameters(int bonusMultiplier, int extra, float probability) {
    private static final Parameters DEFAULT = new Parameters(1, 0, 0);
    private static final Codec<Parameters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.optionalFieldOf("bonusMultiplier", 1).forGetter(Parameters::bonusMultiplier),
      Codec.INT.optionalFieldOf("extra", 0).forGetter(Parameters::extra),
      Codec.FLOAT.optionalFieldOf("probability", 0f).forGetter(Parameters::probability)
    ).apply(instance, Parameters::new));
  }
}
