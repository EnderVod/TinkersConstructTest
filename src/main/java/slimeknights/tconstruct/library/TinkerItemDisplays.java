package slimeknights.tconstruct.library;

import net.minecraft.world.item.ItemDisplayContext;

/** Custom transform types used for Tinkers item rendering. */
public class TinkerItemDisplays {
  private TinkerItemDisplays() {}

  /**
   * Kept as the existing bootstrap hook. NeoForge 1.21 loads the actual enum values from
   * META-INF/enumextensions.json before normal mod construction, so no runtime registry call is needed.
   */
  public static void init() {}

  /** Used by the melter and smeltery for display of items being melted. */
  public static final ItemDisplayContext MELTER = ItemDisplayContext.valueOf("TCONSTRUCT_MELTER");
  /** Used by the part builder, crafting station, tinkers station, and tinker anvil. */
  public static final ItemDisplayContext TABLE = ItemDisplayContext.valueOf("TCONSTRUCT_TABLE");
  /** Used by the casting table for item rendering. */
  public static final ItemDisplayContext CASTING_TABLE = ItemDisplayContext.valueOf("TCONSTRUCT_CASTING_TABLE");
  /** Used by the casting basin for item rendering. */
  public static final ItemDisplayContext CASTING_BASIN = ItemDisplayContext.valueOf("TCONSTRUCT_CASTING_BASIN");
  /** Used by the fluid cannon for display of the item in front. */
  public static final ItemDisplayContext FLUID_CANNON = ItemDisplayContext.valueOf("TCONSTRUCT_FLUID_CANNON");
  /** Used by throwing to allow adjusting the tool position. */
  public static final ItemDisplayContext THROWN = ItemDisplayContext.valueOf("TCONSTRUCT_THROWN");
}
