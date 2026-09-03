package slimeknights.tconstruct.library.json.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.tconstruct.TConstruct;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** @deprecated use {@link slimeknights.mantle.recipe.condition.TagCombinationCondition#difference(TagKey, TagKey)} */
@Deprecated(forRemoval = true)
public class TagDifferencePresentCondition<T> implements ICondition {
  public static final ResourceLocation ID = TConstruct.getResource("tag_difference_present");
  private static final Codec<List<ResourceLocation>> SUBTRACTED_CODEC = ResourceLocation.CODEC.listOf()
    .validate(tags -> tags.isEmpty() ? DataResult.error(() -> "Cannot create a condition with no subtracted tags") : DataResult.success(tags));
  public static final MapCodec<TagDifferencePresentCondition<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    ResourceLocation.CODEC.fieldOf("registry").forGetter(condition -> condition.base.registry().location()),
    ResourceLocation.CODEC.fieldOf("base").forGetter(condition -> condition.base.location()),
    SUBTRACTED_CODEC.fieldOf("subtracted").forGetter(condition -> condition.subtracted.stream().map(TagKey::location).toList())
  ).apply(instance, TagDifferencePresentCondition::fromIds));

  private final TagKey<T> base;
  private final List<TagKey<T>> subtracted;

  public TagDifferencePresentCondition(TagKey<T> base, List<TagKey<T>> subtracted) {
    if (subtracted.isEmpty()) {
      throw new IllegalArgumentException("Cannot create a condition with no subtracted");
    }
    this.base = base;
    this.subtracted = subtracted;
  }

  private static TagDifferencePresentCondition<?> fromIds(ResourceLocation registryName, ResourceLocation baseName, List<ResourceLocation> subtracted) {
    ResourceKey<Registry<Object>> registry = ResourceKey.createRegistryKey(registryName);
    return new TagDifferencePresentCondition<>(
      TagKey.create(registry, baseName),
      subtracted.stream().map(name -> TagKey.create(registry, name)).toList());
  }

  @SafeVarargs
  public static <T> TagDifferencePresentCondition<T> ofKeys(TagKey<T> base, TagKey<T>... subtracted) {
    return new TagDifferencePresentCondition<>(base, Arrays.asList(subtracted));
  }

  public static <T> TagDifferencePresentCondition<T> ofNames(ResourceKey<? extends Registry<T>> registry, ResourceLocation base, ResourceLocation... subtracted) {
    TagKey<T> baseKey = TagKey.create(registry, base);
    return new TagDifferencePresentCondition<>(baseKey, Arrays.stream(subtracted).map(name -> TagKey.create(registry, name)).toList());
  }

  public ResourceLocation getID() {
    return ID;
  }

  @Override
  public MapCodec<TagDifferencePresentCondition<?>> codec() {
    return CODEC;
  }

  @Override
  public boolean test(IContext context) {
    Collection<Holder<T>> base = context.getTag(this.base);
    if (base.isEmpty()) {
      return false;
    }

    itemLoop:
    for (Holder<T> entry : base) {
      for (TagKey<T> tag : subtracted) {
        if (context.getTag(tag).contains(entry)) {
          continue itemLoop;
        }
      }
      return true;
    }
    return false;
  }
}
