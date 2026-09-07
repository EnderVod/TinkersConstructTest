package slimeknights.tconstruct.library.tools.layout;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.nbt.ToolDataComponents;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;

/** Data holder for a button icon, currently supports item stack icons and pattern icons */
public abstract class LayoutIcon {
  public static final Serializer SERIALIZER = new Serializer();

  public static final LayoutIcon EMPTY = new LayoutIcon() {
    @Nullable
    @Override
    public <T> T getValue(Class<T> clazz) {
      return null;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
      buffer.writeEnum(Type.EMPTY);
    }

    @Override
    public JsonObject toJson() {
      return new JsonObject();
    }
  };

  public static LayoutIcon ofItem(ItemStack stack) {
    return new ItemStackIcon(stack);
  }

  public static LayoutIcon ofPattern(Pattern pattern) {
    return new PatternIcon(pattern);
  }

  @Nullable
  public abstract <T> T getValue(Class<T> clazz);

  public static LayoutIcon read(FriendlyByteBuf buffer) {
    Type type = buffer.readEnum(Type.class);
    switch (type) {
      case EMPTY: return EMPTY;
      case ITEM: {
        ItemStack stack = ItemStackLoadable.REQUIRED_STACK_NBT.decode(buffer, TypedMap.EMPTY);
        return new ItemStackIcon(stack);
      }
      case PATTERN: {
        Pattern pattern = new Pattern(buffer.readResourceLocation());
        return new PatternIcon(pattern);
      }
    }
    throw new DecoderException("Invalid LayoutButtonIcon " + type);
  }

  public abstract void write(FriendlyByteBuf buffer);

  public abstract JsonObject toJson();

  @RequiredArgsConstructor @VisibleForTesting
  protected static class ItemStackIcon extends LayoutIcon {
    private final ItemStack stack;

    /**
     * Station layout JSON predates Minecraft's data component system and stores Tinkers preview
     * data in the generic NBT field. Mantle decodes that field into CUSTOM_DATA, while normal
     * 1.21 Tinkers tools read their serialized state from ToolDataComponents.TOOL_DATA.
     * Promote legacy preview NBT on demand so the custom tool model can see its materials.
     */
    private void migrateLegacyToolData() {
      if (ToolDataComponents.getTag(stack) != null) {
        return;
      }
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData != null) {
        CompoundTag tag = customData.copyTag();
        if (tag.contains(ToolStack.TAG_MATERIALS)) {
          ToolDataComponents.setTag(stack, tag);
        }
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(Class<T> clazz) {
      if (clazz == ItemStack.class) {
        migrateLegacyToolData();
        return (T) stack;
      }
      return null;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
      buffer.writeEnum(Type.ITEM);
      ItemStackLoadable.REQUIRED_STACK_NBT.encode(buffer, stack);
    }

    @Override
    public JsonObject toJson() {
      return ItemStackLoadable.REQUIRED_STACK_NBT.serialize(stack).getAsJsonObject();
    }
  }

  @RequiredArgsConstructor @VisibleForTesting
  protected static class PatternIcon extends LayoutIcon {
    private final Pattern pattern;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(Class<T> clazz) {
      if (clazz == Pattern.class) {
        return (T) pattern;
      }
      return null;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
      buffer.writeEnum(Type.PATTERN);
      buffer.writeResourceLocation(pattern);
    }

    @Override
    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.addProperty("pattern", pattern.toString());
      return json;
    }
  }

  private enum Type {
    EMPTY,
    ITEM,
    PATTERN
  }

  protected static class Serializer implements JsonSerializer<LayoutIcon>, JsonDeserializer<LayoutIcon> {
    @Override
    public LayoutIcon deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject object = GsonHelper.convertToJsonObject(json, "button_icon");
      if (object.has("pattern")) {
        Pattern pattern = new Pattern(JsonHelper.getResourceLocation(object, "pattern"));
        return new PatternIcon(pattern);
      }
      if (object.has("item")) {
        ItemStack stack = ItemStackLoadable.REQUIRED_STACK_NBT.convert(object, "button_icon");
        return new ItemStackIcon(stack);
      }
      if (object.entrySet().isEmpty()) {
        return EMPTY;
      }
      throw new JsonSyntaxException("LayoutButtonIcon must have either pattern or item");
    }

    @Override
    public JsonElement serialize(LayoutIcon icon, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
      return icon.toJson();
    }
  }
}
