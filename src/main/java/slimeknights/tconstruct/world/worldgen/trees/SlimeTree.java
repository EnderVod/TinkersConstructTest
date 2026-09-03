package slimeknights.tconstruct.world.worldgen.trees;

import net.minecraft.world.level.block.grower.TreeGrower;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.Optional;

/** 1.21 tree grower definitions for the slime island foliage variants. */
public final class SlimeTree {
  private SlimeTree() {}

  private static final TreeGrower EARTH = new TreeGrower(
    "tconstruct_earth_slime", Optional.empty(), Optional.of(TinkerStructures.earthSlimeTree), Optional.empty());
  private static final TreeGrower SKY = new TreeGrower(
    "tconstruct_sky_slime", Optional.empty(), Optional.of(TinkerStructures.skySlimeTree), Optional.empty());
  private static final TreeGrower ENDER = new TreeGrower(
    "tconstruct_ender_slime", 0.85f,
    Optional.empty(), Optional.empty(),
    Optional.of(TinkerStructures.enderSlimeTree), Optional.of(TinkerStructures.enderSlimeTreeTall),
    Optional.empty(), Optional.empty());
  private static final TreeGrower BLOOD = new TreeGrower(
    "tconstruct_blood_slime", Optional.empty(), Optional.of(TinkerStructures.bloodSlimeFungus), Optional.empty());
  private static final TreeGrower ICHOR = new TreeGrower(
    "tconstruct_ichor_slime", Optional.empty(), Optional.of(TinkerStructures.ichorSlimeFungus), Optional.empty());

  public static TreeGrower get(FoliageType foliageType) {
    return switch (foliageType) {
      case EARTH -> EARTH;
      case SKY -> SKY;
      case ENDER -> ENDER;
      case BLOOD -> BLOOD;
      case ICHOR -> ICHOR;
    };
  }
}
