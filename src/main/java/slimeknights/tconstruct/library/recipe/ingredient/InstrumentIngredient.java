package slimeknights.tconstruct.library.recipe.ingredient;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerCommons;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Ingredient matching an InstrumentItem with a particular instrument, or excluding an instrument tag. */
public class InstrumentIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = TConstruct.getResource("instrument");
  public static final MapCodec<InstrumentIngredient> CODEC = RecordCodecBuilder.<InstrumentIngredient>mapCodec(instance -> instance.group(
    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ingredient -> ingredient.item),
    ResourceLocation.CODEC.optionalFieldOf("instrument").forGetter(ingredient -> Optional.ofNullable(ingredient.instrument).map(ResourceKey::location)),
    ResourceLocation.CODEC.optionalFieldOf("ignore").forGetter(ingredient -> Optional.ofNullable(ingredient.ignore).map(TagKey::location))
  ).apply(instance, InstrumentIngredient::decodeUnchecked)).validate(InstrumentIngredient::validate);

  private final Item item;
  @Nullable
  private final ResourceKey<Instrument> instrument;
  @Nullable
  private final TagKey<Instrument> ignore;

  protected InstrumentIngredient(Item item, @Nullable ResourceKey<Instrument> instrument, @Nullable TagKey<Instrument> ignore) {
    this.item = item.asItem();
    this.instrument = instrument;
    this.ignore = ignore;
  }

  private static InstrumentIngredient decodeUnchecked(Item item, Optional<ResourceLocation> instrument, Optional<ResourceLocation> ignore) {
    return new InstrumentIngredient(
      item,
      instrument.map(id -> ResourceKey.create(Registries.INSTRUMENT, id)).orElse(null),
      ignore.map(id -> TagKey.create(Registries.INSTRUMENT, id)).orElse(null));
  }

  private static DataResult<InstrumentIngredient> validate(InstrumentIngredient ingredient) {
    if ((ingredient.instrument == null) == (ingredient.ignore == null)) {
      return DataResult.error(() -> "Instrument ingredient must set exactly one of 'instrument' or 'ignore'");
    }
    return DataResult.success(ingredient);
  }

  /** Creates a new instance matching the given instrument. */
  public static Ingredient of(ItemLike item, ResourceKey<Instrument> instrument) {
    return new InstrumentIngredient(item.asItem(), instrument, null).toVanilla();
  }

  /** Creates a new instance ignoring the given tag. */
  public static Ingredient of(ItemLike item, TagKey<Instrument> ignore) {
    return new InstrumentIngredient(item.asItem(), null, ignore).toVanilla();
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || !stack.is(item)) {
      return false;
    }
    Holder<Instrument> selected = stack.get(DataComponents.INSTRUMENT);
    if (instrument != null) {
      return selected != null && selected.is(instrument);
    }
    return selected == null || !selected.is(Objects.requireNonNull(ignore));
  }

  @Override
  public Stream<ItemStack> getItems() {
    ItemStack stack = new ItemStack(item);
    if (instrument != null) {
      BuiltInRegistries.INSTRUMENT.getHolder(instrument).ifPresent(holder -> stack.set(DataComponents.INSTRUMENT, holder));
    }
    return Stream.of(stack);
  }

  @Override
  public IngredientType<?> getType() {
    return TinkerCommons.instrumentIngredientType.get();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof InstrumentIngredient other
      && item.equals(other.item)
      && Objects.equals(instrument, other.instrument)
      && Objects.equals(ignore, other.ignore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(item, instrument, ignore);
  }
}
