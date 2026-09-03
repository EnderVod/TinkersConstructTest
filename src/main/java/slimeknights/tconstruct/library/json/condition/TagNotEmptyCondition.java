package slimeknights.tconstruct.library.json.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import slimeknights.mantle.recipe.condition.TagCondition;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerCommons;

/** @deprecated use {@link slimeknights.mantle.recipe.condition.TagFilledCondition} */
@Deprecated(forRemoval = true)
public class TagNotEmptyCondition<T> extends TagCondition<T> implements LootItemCondition {
  public static final ResourceLocation ID = TConstruct.getResource("tag_not_empty");
  public static final MapCodec<TagNotEmptyCondition<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    ResourceLocation.CODEC.fieldOf("registry").forGetter(condition -> condition.tag.registry().location()),
    ResourceLocation.CODEC.fieldOf("tag").forGetter(condition -> condition.tag.location())
  ).apply(instance, TagNotEmptyCondition::fromIds));

  public TagNotEmptyCondition(TagKey<T> tag) {
    super(tag);
  }

  private static TagNotEmptyCondition<?> fromIds(ResourceLocation registryName, ResourceLocation tagName) {
    ResourceKey<Registry<Object>> registry = ResourceKey.createRegistryKey(registryName);
    return new TagNotEmptyCondition<>(TagKey.create(registry, tagName));
  }

  public ResourceLocation getID() {
    return ID;
  }

  @Override
  public MapCodec<TagNotEmptyCondition<?>> codec() {
    return CODEC;
  }

  @SuppressWarnings("removal")
  @Override
  public LootItemConditionType getType() {
    return TinkerCommons.lootTagNotEmptyCondition.get();
  }

  @Override
  public boolean test(IContext context) {
    return !context.getTag(tag).isEmpty();
  }

  @Override
  public boolean test(LootContext context) {
    Registry<T> registry = registry(context);
    return registry != null && registry.getTagOrEmpty(tag).iterator().hasNext();
  }
}
