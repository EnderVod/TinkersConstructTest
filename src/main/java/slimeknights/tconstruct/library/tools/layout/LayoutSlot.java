package slimeknights.tconstruct.library.tools.layout;

import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

import javax.annotation.Nullable;
import java.util.Objects;

/** A single slot in a slot layout */
@RequiredArgsConstructor
public class LayoutSlot {
  public static final LayoutSlot EMPTY = new LayoutSlot(null, "", -1, -1, null);

  @Nullable @Getter
  private final Pattern icon;
  @Nullable
  private final String translation_key;
  @Getter
  private final int x;
  @Getter
  private final int y;
  @Nullable @Getter(AccessLevel.PROTECTED) @VisibleForTesting
  private final Ingredient filter;

  public boolean isEmpty() {
    return getTranslationKey().isEmpty();
  }

  public boolean isHidden() {
    return x == -1 && y == -1;
  }

  public String getTranslationKey() {
    return Objects.requireNonNullElse(translation_key, "");
  }

  public boolean isValid(ItemStack stack) {
    return !stack.isEmpty() && (filter == null || filter.test(stack));
  }

  public static LayoutSlot read(FriendlyByteBuf buffer) {
    Pattern pattern = null;
    if (buffer.readBoolean()) {
      pattern = new Pattern(buffer.readResourceLocation());
    }
    String name = buffer.readUtf(Short.MAX_VALUE);
    int x = buffer.readVarInt();
    int y = buffer.readVarInt();
    Ingredient ingredient = null;
    if (buffer.readBoolean()) {
      ingredient = IngredientLoadable.DISALLOW_EMPTY.decode(buffer, TypedMap.EMPTY);
    }
    return new LayoutSlot(pattern, name, x, y, ingredient);
  }

  public void write(FriendlyByteBuf buffer) {
    if (icon != null) {
      buffer.writeBoolean(true);
      buffer.writeResourceLocation(icon);
    } else {
      buffer.writeBoolean(false);
    }
    buffer.writeUtf(getTranslationKey());
    buffer.writeVarInt(x);
    buffer.writeVarInt(y);
    if (filter != null) {
      buffer.writeBoolean(true);
      IngredientLoadable.DISALLOW_EMPTY.encode(buffer, filter);
    } else {
      buffer.writeBoolean(false);
    }
  }
}
