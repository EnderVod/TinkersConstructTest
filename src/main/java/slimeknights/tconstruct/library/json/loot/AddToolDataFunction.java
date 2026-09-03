package slimeknights.tconstruct.library.json.loot;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.List;

/** Loot function to add data to a tool. */
public class AddToolDataFunction extends LootItemConditionalFunction {
  public static final ResourceLocation ID = TConstruct.getResource("add_tool_data");
  public static final MapCodec<AddToolDataFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
    commonFields(instance).and(instance.group(
      Codec.floatRange(0f, 1f).optionalFieldOf("damage_percent", 0f).forGetter(function -> function.damage),
      LootCodecs.RANDOM_MATERIAL.listOf().optionalFieldOf("materials", List.of()).forGetter(function -> function.materials)
    )).apply(instance, AddToolDataFunction::new));

  private final float damage;
  private final List<RandomMaterial> materials;

  protected AddToolDataFunction(List<LootItemCondition> conditions, float damage, List<RandomMaterial> materials) {
    super(conditions);
    this.damage = damage;
    this.materials = materials;
  }

  public static AddToolDataFunction.Builder builder() {
    return new AddToolDataFunction.Builder();
  }

  @Override
  public LootItemFunctionType getType() {
    return TinkerTools.lootAddToolData.get();
  }

  @Override
  protected ItemStack run(ItemStack stack, LootContext context) {
    if (stack.is(TinkerTags.Items.MODIFIABLE)) {
      ToolStack tool = ToolStack.from(stack);
      ToolDefinition definition = tool.getDefinition();
      if (definition.hasMaterials() && !materials.isEmpty()) {
        tool.setMaterials(RandomMaterial.build(ToolMaterialHook.stats(definition), materials, context.getRandom()));
      } else {
        tool.rebuildStats();
      }
      if (damage > 0) {
        tool.setDamage((int)(tool.getStats().get(ToolStats.DURABILITY) * damage));
      }
    }
    return stack;
  }

  @Accessors(chain = true)
  public static class Builder extends LootItemConditionalFunction.Builder<AddToolDataFunction.Builder> {
    private final ImmutableList.Builder<RandomMaterial> materials = ImmutableList.builder();
    private float damage = 0;

    protected Builder() {}

    @Override
    protected Builder getThis() {
      return this;
    }

    public void setDamage(float damage) {
      if (damage < 0 || damage > 1) {
        throw new IllegalArgumentException("Damage must be between 0 and 1, given " + damage);
      }
      this.damage = damage;
    }

    public Builder addMaterial(RandomMaterial mat) {
      materials.add(mat);
      return this;
    }

    public Builder addMaterial(MaterialId mat) {
      return addMaterial(RandomMaterial.fixed(mat));
    }

    @Override
    public LootItemFunction build() {
      return new AddToolDataFunction(getConditions(), damage, materials.build());
    }
  }
}
