package slimeknights.tconstruct.library.json.loot;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import slimeknights.tconstruct.shared.TinkerCommons;

import java.util.Objects;

/** Loot condition that only runs if all required values in the given loot context set are present. */
public record HasLootContextSetCondition(LootContextParamSet set) implements LootItemCondition {
  public static final MapCodec<HasLootContextSetCondition> CODEC = ResourceLocation.CODEC
    .comapFlatMap(HasLootContextSetCondition::decode, condition ->
      Objects.requireNonNull(LootContextParamSets.getKey(condition.set), "Unregistered LootContextParamSet"))
    .fieldOf("set");

  private static DataResult<HasLootContextSetCondition> decode(ResourceLocation key) {
    LootContextParamSet set = LootContextParamSets.get(key);
    if (set == null) {
      return DataResult.error(() -> "Unknown LootContextParamSet " + key);
    }
    return DataResult.success(new HasLootContextSetCondition(set));
  }

  public static Builder builder(LootContextParamSet set) {
    return new Builder(set);
  }

  @Override
  public LootItemConditionType getType() {
    return TinkerCommons.hasLootContextSet.get();
  }

  @Override
  public boolean test(LootContext context) {
    for (LootContextParam<?> param : set.getRequired()) {
      if (!context.hasParam(param)) {
        return false;
      }
    }
    return true;
  }

  public record Builder(LootContextParamSet set) implements LootItemCondition.Builder {
    @Override
    public LootItemCondition build() {
      return new HasLootContextSetCondition(set);
    }
  }
}
