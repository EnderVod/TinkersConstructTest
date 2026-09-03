from pathlib import Path

root = Path('src/main/java/slimeknights/tconstruct')

def replace_exact(path, old, new, count=1):
    p = root / path
    s = p.read_text()
    n = s.count(old)
    if n != count:
        raise SystemExit(f'{path}: expected {count} occurrences, found {n}: {old[:80]!r}')
    p.write_text(s.replace(old, new))

# TinkerWorld: private Blocks::never helpers, removed custom client factory, private ComposterBlock.add.
p = root / 'world/TinkerWorld.java'
s = p.read_text()
for old, new, count in [
    ('.isSuffocating(Blocks::never)', '.isSuffocating((state, level, pos) -> false)', 3),
    ('.isViewBlocking(Blocks::never)', '.isViewBlocking((state, level, pos) -> false)', 3),
    ('.isValidSpawn(Blocks::never)', '.isValidSpawn((state, level, pos, entityType) -> false)', 2),
    ('.isRedstoneConductor(Blocks::never)', '.isRedstoneConductor((state, level, pos) -> false)', 2),
]:
    if s.count(old) != count:
        raise SystemExit(f'TinkerWorld: expected {count} of {old}, found {s.count(old)}')
    s = s.replace(old, new)
for entity in ['skySlimeEntity', 'enderSlimeEntity', 'terracubeEntity']:
    import re
    pattern = re.compile(r'\n\s+\.setCustomClientFactory\(\(spawnEntity, world\) -> TinkerWorld\.' + entity + r'\.get\(\)\.create\(world\)\)')
    s2, n = pattern.subn('', s)
    if n != 1:
        raise SystemExit(f'TinkerWorld: expected one custom client factory for {entity}, got {n}')
    s = s2
compost = {
    'ComposterBlock.add(type.isNether() ? 0.85f : 0.35f, block)': 'ComposterBlock.COMPOSTABLES.put(block, type.isNether() ? 0.85f : 0.35f)',
    'ComposterBlock.add(0.35f, block)': 'ComposterBlock.COMPOSTABLES.put(block, 0.35f)',
    'ComposterBlock.add(0.65f, block)': 'ComposterBlock.COMPOSTABLES.put(block, 0.65f)',
    'ComposterBlock.add(0.35F, block)': 'ComposterBlock.COMPOSTABLES.put(block, 0.35F)',
    'ComposterBlock.add(0.5f, skySlimeVine)': 'ComposterBlock.COMPOSTABLES.put(skySlimeVine, 0.5f)',
    'ComposterBlock.add(0.5f, enderSlimeVine)': 'ComposterBlock.COMPOSTABLES.put(enderSlimeVine, 0.5f)',
    'ComposterBlock.add(0.4f, enderbarkRoots)': 'ComposterBlock.COMPOSTABLES.put(enderbarkRoots, 0.4f)',
}
expected = {'ComposterBlock.add(0.35f, block)': 2}
for old, new in compost.items():
    count = expected.get(old, 1)
    if s.count(old) != count:
        raise SystemExit(f'TinkerWorld: expected {count} occurrences of {old}, found {s.count(old)}')
    s = s.replace(old, new)
p.write_text(s)

# 1.21 worldgen registries require MapCodec for decorators, root placers, and structures.
replace_exact('world/worldgen/trees/LeaveVineDecorator.java', 'import com.mojang.serialization.Codec;\n', 'import com.mojang.serialization.Codec;\nimport com.mojang.serialization.MapCodec;\n')
replace_exact('world/worldgen/trees/LeaveVineDecorator.java', 'public static final Codec<LeaveVineDecorator> CODEC = RecordCodecBuilder.create(', 'public static final MapCodec<LeaveVineDecorator> CODEC = RecordCodecBuilder.mapCodec(')

replace_exact('world/worldgen/trees/ExtraRootVariantPlacer.java', 'import com.mojang.serialization.Codec;\n', 'import com.mojang.serialization.Codec;\nimport com.mojang.serialization.MapCodec;\n')
replace_exact('world/worldgen/trees/ExtraRootVariantPlacer.java', 'public static final Codec<ExtraRootVariantPlacer> CODEC = RecordCodecBuilder.create(', 'public static final MapCodec<ExtraRootVariantPlacer> CODEC = RecordCodecBuilder.mapCodec(')
replace_exact('world/worldgen/trees/ExtraRootVariantPlacer.java', '  private final List<RootVariant> rootVariants;\n', '  private final MangroveRootPlacement mangroveRootPlacement;\n  private final List<RootVariant> rootVariants;\n')
replace_exact('world/worldgen/trees/ExtraRootVariantPlacer.java', '    super(pTrunkOffset, pRootProvider, pAboveRootPlacement, mangrovePlacement);\n    this.rootVariants = rootVariants;\n', '    super(pTrunkOffset, pRootProvider, pAboveRootPlacement, mangrovePlacement);\n    this.mangroveRootPlacement = mangrovePlacement;\n    this.rootVariants = rootVariants;\n')

replace_exact('world/worldgen/islands/IslandStructure.java', 'import com.mojang.serialization.Codec;\n', 'import com.mojang.serialization.Codec;\nimport com.mojang.serialization.MapCodec;\n')
replace_exact('world/worldgen/islands/IslandStructure.java', 'public static final Codec<IslandStructure> CODEC = RecordCodecBuilder.create(', 'public static final MapCodec<IslandStructure> CODEC = RecordCodecBuilder.mapCodec(')

# ModelLayers.ALL_MODELS is private and this helper is unused; layer definitions are registered through the event below.
p = root / 'world/WorldClientEvents.java'
s = p.read_text()
old = '''  /** Register a layer without being under the minecraft domain. TODO: is this needed? */
  private static ModelLayerLocation registerLayer(String name) {
    ModelLayerLocation location = new ModelLayerLocation(TConstruct.getResource(name), "main");
    if (!ModelLayers.ALL_MODELS.add(location)) {
      throw new IllegalStateException("Duplicate registration for " + location);
    } else {
      return location;
    }
  }

'''
if s.count(old) != 1:
    raise SystemExit('WorldClientEvents: unused private layer helper mismatch')
s = s.replace(old, '')
if s.count('import net.minecraft.client.model.geom.ModelLayerLocation;\n') != 1:
    raise SystemExit('WorldClientEvents: ModelLayerLocation import mismatch')
s = s.replace('import net.minecraft.client.model.geom.ModelLayerLocation;\n', '')
p.write_text(s)

# Model rendering now receives one packed ARGB color.
replace_exact('world/client/PiglinSkullModel.java',
'''  public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
    poseStack.pushPose();
    poseStack.scale(0.97f, 0.97f, 0.97f);
    super.renderToBuffer(poseStack, buffer, light, overlay, red, green, blue, alpha);
    poseStack.popPose();
  }
''',
'''  public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
    poseStack.pushPose();
    poseStack.scale(0.97f, 0.97f, 0.97f);
    super.renderToBuffer(poseStack, buffer, light, overlay, color);
    poseStack.popPose();
  }
''')

replace_exact('world/client/DragonSkullModel.java',
'''  public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
    // note this does not work properly with rotations due to the head origin being weird
    // however, our only usage of this is in SlimeskullArmorModel so its not an issue
    // could patch setupAnim to set two local floats and apply the rotation here via Quaternionf#rotationZYX if it becomes an issue
    poseStack.pushPose();
    poseStack.translate(0, -0.25f, 0.075f);
    poseStack.scale(0.5f, 0.5f, 0.49f);
    this.root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    poseStack.popPose();
  }
''',
'''  public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
    // note this does not work properly with rotations due to the head origin being weird
    // however, our only usage of this is in SlimeskullArmorModel so its not an issue
    // could patch setupAnim to set two local floats and apply the rotation here via Quaternionf#rotationZYX if it becomes an issue
    poseStack.pushPose();
    poseStack.translate(0, -0.25f, 0.075f);
    poseStack.scale(0.5f, 0.5f, 0.49f);
    this.root.render(poseStack, buffer, light, overlay, color);
    poseStack.popPose();
  }
''')

replace_exact('world/client/BlockModelSkullRenderer.java',
'''  public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
''',
'''  public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
''')
replace_exact('world/client/BlockModelSkullRenderer.java',
'''    // applying tint is a pain with these, sop hope we don't need it
    if (red != 1 || green != 1 || blue != 1 || alpha != 1) {
      buffer = new TintedVertexBuilder(buffer, (int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));
    }
''',
'''    // Model rendering now supplies one packed ARGB color; preserve the old per-channel tint wrapper.
    if (color != -1) {
      buffer = new TintedVertexBuilder(buffer, (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF);
    }
''')

# Resource reload no longer exposes ModLoader.isLoadingStateValid(); reload listeners run after loading is valid.
p = root / 'world/client/SlimeColorReloadListener.java'
s = p.read_text()
if s.count('import net.neoforged.fml.ModLoader;\n') != 1:
    raise SystemExit('SlimeColorReloadListener: ModLoader import mismatch')
s = s.replace('import net.neoforged.fml.ModLoader;\n', '')
old = '''    if (!ModLoader.isLoadingStateValid()) {
      return new int[0];
    }
'''
if s.count(old) != 1:
    raise SystemExit('SlimeColorReloadListener: loading-state guard mismatch')
s = s.replace(old, '')
p.write_text(s)

# MerchantOffer now wraps the input in ItemCost.
p = root / 'world/logic/AncientToolItemListing.java'
s = p.read_text()
anchor = 'import net.minecraft.world.item.trading.MerchantOffer;\n'
if s.count(anchor) != 1:
    raise SystemExit('AncientToolItemListing: MerchantOffer import mismatch')
s = s.replace(anchor, 'import net.minecraft.world.item.trading.ItemCost;\n' + anchor)
old = 'return new MerchantOffer(new ItemStack(Items.EMERALD, cost), tool.createStack(), 1, 15, 1);'
new = 'return new MerchantOffer(new ItemCost(Items.EMERALD, cost), tool.createStack(), 1, 15, 1);'
if s.count(old) != 1:
    raise SystemExit('AncientToolItemListing: old MerchantOffer constructor mismatch')
s = s.replace(old, new)
p.write_text(s)
