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

/** @deprecated use {@link slimeknights.mantle.recipe.condition.TagCombinationCondition#intersection(TagKey[])} */
@Deprecated(forRemoval = true)
public class TagIntersectionPresentCondition<T> implements ICondition {
  public static final ResourceLocation ID = TConstruct.getResource("tag_intersection_present");
  private static final Codec<List<ResourceLocation>> TAGS_CODEC = ResourceLocation.CODEC.listOf()
    .validate(tags -> tags.isEmpty() ? DataResult.error(() -> "Cannot create a condition with no tags") : DataResult.success(tags));
  public static final MapCodec<TagIntersectionPresentCondition<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    ResourceLocation.CODEC.fieldOf("registry").forGetter(condition -> condition.names.get(0).registry().location()),
    TAGS_CODEC.fieldOf("tags").forGetter(condition -> condition.names.stream().map(TagKey::location).toList())
  ).apply(instance, TagIntersectionPresentCondition::fromIds));

  private final List<TagKey<T>> names;

  public TagIntersectionPresentCondition(List<TagKey<T>> names) {
    if (names.isEmpty()) {
      throw new IllegalArgumentException("Cannot create a condition with no names");
    }
    this.names = names;
  }

  private static TagIntersectionPresentCondition<?> fromIds(ResourceLocation registryName, List<ResourceLocation> names) {
    ResourceKey<Registry<Object>> registry = ResourceKey.createRegistryKey(registryName);
    return new TagIntersectionPresentCondition<>(names.stream().map(name -> TagKey.create(registry, name)).toList());
  }

  @SafeVarargs
  public static <T> TagIntersectionPresentCondition<T> ofKeys(TagKey<T>... names) {
    return new TagIntersectionPresentCondition<>(Arrays.asList(names));
  }

  public static <T> TagIntersectionPresentCondition<T> ofNames(ResourceKey<? extends Registry<T>> registry, ResourceLocation... names) {
    return new TagIntersectionPresentCondition<>(Arrays.stream(names).map(name -> TagKey.create(registry, name)).toList());
  }

  public ResourceLocation getID() {
    return ID;
  }

  @Override
  public MapCodec<TagIntersectionPresentCondition<?>> codec() {
    return CODEC;
  }

  @Override
  public boolean test(IContext context) {
    List<Collection<Holder<T>>> tags = names.stream().map(context::getTag).toList();
    if (tags.size() == 1) {
      return !tags.get(0).isEmpty();
    }
    int count = tags.size();
    for (int i = 1; i < count; i++) {
      if (tags.get(i).isEmpty()) {
        return false;
      }
    }

    itemLoop:
    for (Holder<T> entry : tags.get(0)) {
      for (int i = 1; i < count; i++) {
        if (!tags.get(i).contains(entry)) {
          continue itemLoop;
        }
      }
      return true;
    }
    return false;
  }
}
