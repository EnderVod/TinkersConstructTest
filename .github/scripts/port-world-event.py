from pathlib import Path

root = Path('src/main/java')
changed = []


def replace(rel, pairs):
    path = root / rel
    original = path.read_text(encoding='utf-8')
    text = original
    for old, new in pairs:
        if old not in text:
            raise SystemExit(f'Missing expected source in {rel}: {old!r}')
        text = text.replace(old, new)
    if text != original:
        path.write_text(text, encoding='utf-8')
        changed.append(str(path))


slime_tree = root / 'slimeknights/tconstruct/world/worldgen/trees/SlimeTree.java'
slime_tree.write_text('''package slimeknights.tconstruct.world.worldgen.trees;

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
''', encoding='utf-8')
changed.append(str(slime_tree))

replace('slimeknights/tconstruct/world/block/SlimeSaplingBlock.java', [
    ('import net.minecraft.world.level.block.grower.AbstractTreeGrower;', 'import net.minecraft.world.level.block.grower.TreeGrower;'),
    ('import net.minecraftforge.common.PlantType;\n', ''),
    ('  public SlimeSaplingBlock(AbstractTreeGrower treeIn, FoliageType foliageType, Properties properties) {',
     '  public SlimeSaplingBlock(TreeGrower treeIn, FoliageType foliageType, Properties properties) {'),
    ('\n  @Nonnull\n  @Override\n  public PlantType getPlantType(BlockGetter world, BlockPos pos) {\n    return TinkerWorld.SLIME_PLANT_TYPE;\n  }\n', '')
])

propagule = root / 'slimeknights/tconstruct/world/block/SlimePropaguleBlock.java'
propagule.write_text('''package slimeknights.tconstruct.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.world.TinkerWorld;

/** Slime propagule using vanilla 1.21 mangrove behavior with Tinkers growth and soil rules. */
public class SlimePropaguleBlock extends MangrovePropaguleBlock {
  public SlimePropaguleBlock(TreeGrower treeIn, FoliageType foliageType, Properties properties) {
    super(treeIn, properties);
  }

  @Override
  protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
    Block block = state.getBlock();
    return TinkerWorld.slimeDirt.contains(block)
      || TinkerWorld.vanillaSlimeGrass.contains(block)
      || TinkerWorld.earthSlimeGrass.contains(block)
      || TinkerWorld.skySlimeGrass.contains(block)
      || TinkerWorld.enderSlimeGrass.contains(block)
      || TinkerWorld.ichorSlimeGrass.contains(block);
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    return state.getValue(HANGING)
      ? level.getBlockState(pos.above()).is(TinkerTags.Blocks.SLIMY_LEAVES)
      : super.canSurvive(state, level, pos);
  }

  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
    return false;
  }
}
''', encoding='utf-8')
changed.append(str(propagule))

replace('slimeknights/tconstruct/world/block/SlimeTallGrassBlock.java', [
    ('import com.google.common.collect.Lists;\n', ''),
    ('import net.minecraftforge.common.IForgeShearable;\nimport net.minecraftforge.common.PlantType;',
     'import net.neoforged.neoforge.common.IShearable;'),
    ('public class SlimeTallGrassBlock extends BushBlock implements IForgeShearable {',
     'public class SlimeTallGrassBlock extends BushBlock implements IShearable {'),
    ('\n  /* Forge/MC callbacks */\n  @Nonnull\n  @Override\n  public PlantType getPlantType(BlockGetter world, BlockPos pos) {\n    return TinkerWorld.SLIME_PLANT_TYPE;\n  }\n', '\n  /* NeoForge shearing callback */\n'),
    ('  public List<ItemStack> onSheared(@Nullable Player player, ItemStack item, Level world, BlockPos pos, int fortune) {\n    return Lists.newArrayList(new ItemStack(this, 1));\n  }',
     '  public List<ItemStack> onSheared(@Nullable Player player, ItemStack item, Level world, BlockPos pos) {\n    return List.of(new ItemStack(this, 1));\n  }')
])

replace('slimeknights/tconstruct/world/block/SlimeDirtBlock.java', [
    ('import net.minecraft.world.level.block.state.BlockState;\nimport net.minecraft.core.Direction;\nimport net.minecraft.core.BlockPos;\nimport net.minecraft.world.level.BlockGetter;\nimport net.minecraftforge.common.PlantType;\nimport slimeknights.tconstruct.world.TinkerWorld;\n\n', ''),
    ('\n  @Override\n  public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos pos, Direction facing, net.minecraftforge.common.IPlantable plantable) {\n    // can sustain both slimeplants and normal plants\n    return plantable.getPlantType(world, pos) == TinkerWorld.SLIME_PLANT_TYPE || plantable.getPlantType(world, pos) == PlantType.PLAINS;\n  }\n', '\n')
])

replace('slimeknights/tconstruct/world/TinkerWorld.java', [
    ('import net.minecraftforge.common.PlantType;\n', ''),
    ('  public static final PlantType SLIME_PLANT_TYPE = PlantType.get("slime");\n\n', ''),
    ('new SlimeSaplingBlock(new SlimeTree(type), type, props.apply(type).randomTicks())',
     'new SlimeSaplingBlock(SlimeTree.get(type), type, props.apply(type).randomTicks())'),
    ('new SlimePropaguleBlock(new SlimeTree(FoliageType.ENDER), FoliageType.ENDER, props.apply(FoliageType.ENDER).randomTicks())',
     'new SlimePropaguleBlock(SlimeTree.get(FoliageType.ENDER), FoliageType.ENDER, props.apply(FoliageType.ENDER).randomTicks())')
])

replace('slimeknights/tconstruct/library/events/TinkerToolEvent.java', [
    ('import lombok.Getter;', 'import lombok.Getter;\nimport lombok.Setter;'),
    ('  private final IToolStackView tool;\n',
     '  private final IToolStackView tool;\n\n  /** Replacement for the removed Forge event result API. */\n  public enum Result { DEFAULT, ALLOW, DENY }\n'),
    ('  @HasResult\n  @Getter\n  public static class ToolHarvestEvent',
     '  @Getter\n  public static class ToolHarvestEvent'),
    ('    private final InteractionSource source;\n',
     '    private final InteractionSource source;\n    @Getter @Setter\n    private Result result = Result.DEFAULT;\n'),
    ('  @HasResult\n  @Getter\n  public static class ToolShearEvent',
     '  @Getter\n  public static class ToolShearEvent'),
    ('    private final int fortune;\n',
     '    private final int fortune;\n    @Getter @Setter\n    private Result result = Result.DEFAULT;\n')
])

for rel in [
    'slimeknights/tconstruct/tools/logic/ToolEvents.java',
    'slimeknights/tconstruct/tools/modules/interaction/HarvestModule.java',
    'slimeknights/tconstruct/tools/modules/interaction/ShearsModule.java']:
    replace(rel, [
        ('import net.neoforged.bus.api.Event.Result;', 'import slimeknights.tconstruct.library.events.TinkerToolEvent.Result;')
    ])

replace('slimeknights/tconstruct/tools/modules/interaction/ShearsModule.java', [
    ('import net.minecraftforge.common.IForgeShearable;', 'import net.neoforged.neoforge.common.IShearable;'),
    ('if (entity instanceof IForgeShearable target && target.isShearable(itemStack, world, entity.blockPosition())) {',
     'if (entity instanceof IShearable target && target.isShearable(player, itemStack, world, entity.blockPosition())) {'),
    ('target.onSheared(player, itemStack, world, entity.blockPosition(), fortune)',
     'target.onSheared(player, itemStack, world, entity.blockPosition())')
])

print(f'Updated {len(changed)} Java files')
for path in changed:
    print(path)
