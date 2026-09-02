package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * Helper for Tinkers' strongly typed resource IDs.
 * <p>
 * Minecraft 1.21 made {@link ResourceLocation} final and its namespace/path constructor non-public. The port access
 * transformer restores subclassing so the existing public Tinkers ID types (materials, modifiers, stats and patterns)
 * can keep their type safety and binary shape instead of becoming unrelated wrapper objects.
 *
 * @see IdParser
 */
public abstract class ResourceId extends ResourceLocation {
  /**
   * Compatibility marker retained for the private validation constructors in existing typed-ID subclasses.
   * It is intentionally unrelated to Minecraft internals in 1.21.
   */
  protected static final class Dummy {
    private Dummy() {}
  }

  protected ResourceId(String namespace, String path, @Nullable Dummy ignored) {
    super(namespace, path);
  }

  public ResourceId(ResourceLocation location) {
    this(location.getNamespace(), location.getPath(), null);
  }

  public ResourceId(String namespace, String path) {
    super(namespace, path);
  }

  public ResourceId(String location) {
    this(ResourceLocation.parse(location));
  }

  /* Helpers for static constructors */

  /** Creates a new typed ID from the given string, or null if invalid. */
  @Nullable
  protected static <T extends ResourceLocation> T tryParse(String string, BiFunction<String,String,T> constructor) {
    ResourceLocation parsed = ResourceLocation.tryParse(string);
    if (parsed == null) {
      return null;
    }
    return constructor.apply(parsed.getNamespace(), parsed.getPath());
  }

  /** Creates a new typed ID from the given namespace and path, or null if invalid. */
  @Nullable
  protected static <T extends ResourceLocation> T tryBuild(String namespace, String path, BiFunction<String,String,T> constructor) {
    if (ResourceLocation.isValidNamespace(namespace) && ResourceLocation.isValidPath(path)) {
      return constructor.apply(namespace, path);
    }
    return null;
  }
}
