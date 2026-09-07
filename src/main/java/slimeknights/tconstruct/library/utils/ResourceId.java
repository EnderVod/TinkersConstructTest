package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * Helper for Tinkers' strongly typed resource IDs.
 * <p>
 * Minecraft 1.21 made {@link ResourceLocation} final, so typed Tinkers IDs can no longer subclass it. This class wraps
 * the vanilla location while preserving the namespace/path helpers and value semantics used by material, modifier,
 * stat, and pattern IDs.
 *
 * @see IdParser
 */
public abstract class ResourceId implements Comparable<ResourceId> {
  private final ResourceLocation location;

  /** Compatibility marker retained for the private validation constructors in existing typed-ID subclasses. */
  protected static final class Dummy {
    private Dummy() {}
  }

  protected ResourceId(String namespace, String path, @Nullable Dummy ignored) {
    this(ResourceLocation.fromNamespaceAndPath(namespace, path));
  }

  public ResourceId(ResourceLocation location) {
    this.location = location;
  }

  public ResourceId(String namespace, String path) {
    this(ResourceLocation.fromNamespaceAndPath(namespace, path));
  }

  public ResourceId(String location) {
    this(ResourceLocation.parse(location));
  }

  /** Gets the vanilla resource location represented by this typed ID. */
  public final ResourceLocation location() {
    return location;
  }

  public final String getNamespace() {
    return location.getNamespace();
  }

  public final String getPath() {
    return location.getPath();
  }

  /** Creates a vanilla resource location with the given prefix added to this ID's path. */
  public final ResourceLocation withPrefix(String prefix) {
    return location.withPrefix(prefix);
  }

  /** Creates a vanilla resource location with the given suffix added to this ID's path. */
  public final ResourceLocation withSuffix(String suffix) {
    return location.withSuffix(suffix);
  }

  @Override
  public final int compareTo(ResourceId other) {
    return location.compareTo(other.location);
  }

  @Override
  public final boolean equals(Object obj) {
    return this == obj || obj instanceof ResourceId other && location.equals(other.location);
  }

  @Override
  public final int hashCode() {
    return location.hashCode();
  }

  @Override
  public final String toString() {
    return location.toString();
  }

  /* Helpers for static constructors */

  /** Creates a new typed ID from the given string, or null if invalid. */
  @Nullable
  protected static <T extends ResourceId> T tryParse(String string, BiFunction<String,String,T> constructor) {
    ResourceLocation parsed = ResourceLocation.tryParse(string);
    if (parsed == null) {
      return null;
    }
    return constructor.apply(parsed.getNamespace(), parsed.getPath());
  }

  /** Creates a new typed ID from the given namespace and path, or null if invalid. */
  @Nullable
  protected static <T extends ResourceId> T tryBuild(String namespace, String path, BiFunction<String,String,T> constructor) {
    if (ResourceLocation.isValidNamespace(namespace) && ResourceLocation.isValidPath(path)) {
      return constructor.apply(namespace, path);
    }
    return null;
  }
}
