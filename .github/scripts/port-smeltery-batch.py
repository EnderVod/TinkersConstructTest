from pathlib import Path

ROOT = Path('src/main/java/slimeknights/tconstruct')

def read(rel):
    p = ROOT / rel
    return p, p.read_text()

def once(rel, old, new):
    p, s = read(rel)
    n = s.count(old)
    if n != 1:
        raise SystemExit(f'{rel}: expected 1 occurrence, found {n}: {old[:100]!r}')
    p.write_text(s.replace(old, new))

# Shared registry/block compatibility.
p, s = read('shared/TinkerCommons.java')
if s.count('import net.minecraft.advancements.CriteriaTriggers;\n') != 1:
    raise SystemExit('TinkerCommons: CriteriaTriggers import mismatch')
s = s.replace('import net.minecraft.advancements.CriteriaTriggers;\n', '')
old = 'glassBuilder(MapColor.COLOR_GRAY).noOcclusion().isValidSpawn(Blocks::never).isRedstoneConductor(Blocks::never).isSuffocating(Blocks::never).isViewBlocking(Blocks::never)'
new = 'glassBuilder(MapColor.COLOR_GRAY).noOcclusion().isValidSpawn((state, level, pos, entityType) -> false).isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)'
if s.count(old) != 1:
    raise SystemExit('TinkerCommons: clear tinted glass predicate mismatch')
s = s.replace(old, new)
anchor = '''  void registerRecipeSerializers(RegisterEvent event) {
    if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER) {
      CriteriaTriggers.register(CONTAINER_OPENED_TRIGGER);

'''
replacement = '''  void registerRecipeSerializers(RegisterEvent event) {
    if (event.getRegistryKey() == Registries.TRIGGER_TYPE) {
      event.register(Registries.TRIGGER_TYPE, getResource("block_container_opened"), () -> CONTAINER_OPENED_TRIGGER);
    }
    if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER) {

'''
if s.count(anchor) != 1:
    raise SystemExit('TinkerCommons: trigger registration block mismatch')
p.write_text(s.replace(anchor, replacement))

# Beacon tint API now uses packed RGB.
once('shared/block/ClearStainedGlassPaneBlock.java',
'''  @Nullable
  @Override
  public float[] getBeaconColorMultiplier(BlockState state, LevelReader world, BlockPos pos, BlockPos beaconPos) {
    return this.glassColor.getRgb();
  }
''',
'''  @Nullable
  @Override
  public Integer getBeaconColorMultiplier(BlockState state, LevelReader world, BlockPos pos, BlockPos beaconPos) {
    return this.glassColor.getColor();
  }
''')

# BlockSource became record-like.
once('shared/block/PlaceBlockDispenserBehavior.java', 'Level level = source.getLevel();', 'Level level = source.level();')
once('shared/block/PlaceBlockDispenserBehavior.java', 'BlockPos target = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));', 'BlockPos target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));')

# Namespaced geometry loader ID.
once('fluids/FluidClientEvents.java', 'event.register("fluid_container", FluidContainerModel.LOADER);', 'event.register(TConstruct.getResource("fluid_container"), FluidContainerModel.LOADER);')

# CauldronInteraction now returns ItemInteractionResult.
for rel in ['fluids/util/EmptyBottleIntoEmpty.java', 'fluids/util/EmptyBottleIntoWater.java', 'fluids/util/FillBottle.java']:
    p, s = read(rel)
    if s.count('import net.minecraft.world.InteractionResult;\n') != 1:
        raise SystemExit(f'{rel}: InteractionResult import mismatch')
    s = s.replace('import net.minecraft.world.InteractionResult;\n', 'import net.minecraft.world.ItemInteractionResult;\n')
    if s.count('public InteractionResult interact(') != 1:
        raise SystemExit(f'{rel}: cauldron signature mismatch')
    s = s.replace('public InteractionResult interact(', 'public ItemInteractionResult interact(')
    if s.count('return InteractionResult.sidedSuccess(level.isClientSide);') != 1:
        raise SystemExit(f'{rel}: sided success mismatch')
    s = s.replace('return InteractionResult.sidedSuccess(level.isClientSide);', 'return ItemInteractionResult.sidedSuccess(level.isClientSide);')
    p.write_text(s)

# Fluid registrations: holders, InteractionMap accessors, and BlockSource accessors.
p, s = read('fluids/TinkerFluids.java')
for effect in ['bouncy', 'ricochet', 'enderference']:
    old = f'new MobEffectInstance(TinkerEffects.{effect}.get(),'
    new = f'new MobEffectInstance(TinkerEffects.holder(TinkerEffects.{effect}),'
    expected = 2 if effect == 'enderference' else 1
    if s.count(old) != expected:
        raise SystemExit(f'TinkerFluids: expected {expected} {effect} effect constructors, got {s.count(old)}')
    s = s.replace(old, new)
s = s.replace('CauldronInteraction.WATER.put(', 'CauldronInteraction.WATER.map().put(')
s = s.replace('CauldronInteraction.EMPTY.put(', 'CauldronInteraction.EMPTY.map().put(')
s = s.replace('CauldronInteraction.WATER.get(', 'CauldronInteraction.WATER.map().get(')
s = s.replace('CauldronInteraction.EMPTY.get(', 'CauldronInteraction.EMPTY.map().get(')
if s.count('source.getPos()') != 1 or s.count('source.getBlockState()') != 1 or s.count('source.getLevel()') != 1:
    raise SystemExit('TinkerFluids: dispenser BlockSource accessor mismatch')
s = s.replace('source.getPos()', 'source.pos()').replace('source.getBlockState()', 'source.state()').replace('source.getLevel()', 'source.level()')
p.write_text(s)

# Cheese uses NeoForge cure sets and holder-based effects.
p, s = read('shared/item/CheeseItem.java')
if s.count('import net.minecraft.world.effect.MobEffect;\n') != 1 or s.count('import net.minecraft.world.item.Items;\n') != 1:
    raise SystemExit('CheeseItem: old imports mismatch')
s = s.replace('import net.minecraft.world.effect.MobEffect;\n', 'import net.minecraft.core.Holder;\nimport net.minecraft.world.effect.MobEffect;\n')
s = s.replace('import net.minecraft.world.item.Items;\n', '')
anchor = 'import net.minecraft.world.level.Level;\n'
if s.count(anchor) != 1:
    raise SystemExit('CheeseItem: Level import anchor mismatch')
s = s.replace(anchor, anchor + 'import net.neoforged.neoforge.common.EffectCures;\n')
old = 'List<MobEffect> removable = effects.stream().filter(effect -> effect.getCurativeItems().stream().anyMatch(item -> item.is(Items.MILK_BUCKET))).map(MobEffectInstance::getEffect).toList();'
new = 'List<Holder<MobEffect>> removable = effects.stream().filter(effect -> effect.getCures().contains(EffectCures.MILK)).map(MobEffectInstance::getEffect).toList();'
if s.count(old) != 1:
    raise SystemExit('CheeseItem: cure selection mismatch')
s = s.replace(old, new)
old = 'public void appendHoverText(ItemStack stack, @Nullable Level pLevel, List<Component> tooltip, TooltipFlag pIsAdvanced)'
new = 'public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag pIsAdvanced)'
if s.count(old) != 1:
    raise SystemExit('CheeseItem: tooltip signature mismatch')
s = s.replace(old, new)
p.write_text(s)

once('shared/item/CheeseBlockItem.java',
     'public void appendHoverText(ItemStack stack, @Nullable Level pLevel, List<Component> tooltip, TooltipFlag pIsAdvanced)',
     'public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag pIsAdvanced)')

# LiquidBlock now receives the actual FlowingFluid; its fluid field is public in 1.21.
p, s = read('fluids/block/MobEffectLiquidBlock.java')
if s.count('import java.util.ArrayList;\n') != 1:
    raise SystemExit('MobEffectLiquidBlock: ArrayList import mismatch')
s = s.replace('import java.util.ArrayList;\n', '')
if s.count('super(supplier, properties);') != 1:
    raise SystemExit('MobEffectLiquidBlock: constructor mismatch')
s = s.replace('super(supplier, properties);', 'super(supplier.get(), properties);')
s = s.replace('entity.getFluidTypeHeight(getFluid().getFluidType())', 'entity.getFluidTypeHeight(fluid.getFluidType())')
if s.count('effect.setCurativeItems(new ArrayList<>());') != 1:
    raise SystemExit('MobEffectLiquidBlock: old cure mutation mismatch')
s = s.replace('effect.setCurativeItems(new ArrayList<>());', 'effect.getCures().clear();')
p.write_text(s)

p, s = read('fluids/block/BurningLiquidBlock.java')
if s.count('super(supplier, properties);') != 1:
    raise SystemExit('BurningLiquidBlock: constructor mismatch')
s = s.replace('super(supplier, properties);', 'super(supplier.get(), properties);')
s = s.replace('entity.getFluidTypeHeight(getFluid().getFluidType())', 'entity.getFluidTypeHeight(fluid.getFluidType())')
if s.count('entity.setSecondsOnFire(burnTime);') != 1:
    raise SystemExit('BurningLiquidBlock: old fire setter mismatch')
s = s.replace('entity.setSecondsOnFire(burnTime);', 'entity.igniteForSeconds(burnTime);')
p.write_text(s)

# Food tooltips use PossibleEffect records, holder values, TooltipContext tick rate, and the new duration signature.
p, s = read('fluids/item/ContainerFoodItem.java')
if s.count('import com.mojang.datafixers.util.Pair;\n') != 1:
    raise SystemExit('ContainerFoodItem: Pair import mismatch')
s = s.replace('import com.mojang.datafixers.util.Pair;\n', '')
if s.count('public int getUseDuration(ItemStack pStack)') != 1:
    raise SystemExit('ContainerFoodItem: use duration signature mismatch')
s = s.replace('public int getUseDuration(ItemStack pStack)', 'public int getUseDuration(ItemStack pStack, LivingEntity entity)')
old = '''    for (Pair<MobEffectInstance, Float> pair : food.getEffects()) {
      MobEffectInstance effect = pair.getFirst();
'''
new = '''    for (FoodProperties.PossibleEffect possible : food.effects()) {
      MobEffectInstance effect = possible.effect();
'''
if s.count(old) != 1:
    raise SystemExit('ContainerFoodItem: food effects loop mismatch')
s = s.replace(old, new)
if s.count('MobEffectUtil.formatDuration(effect, 1.0f)') != 1:
    raise SystemExit('ContainerFoodItem: old duration formatter mismatch')
s = s.replace('MobEffectUtil.formatDuration(effect, 1.0f)', 'MobEffectUtil.formatDuration(effect, 1.0f, tickRate)')
s = s.replace('effect.getEffect().getCategory()', 'effect.getEffect().value().getCategory()')
old = '''  public static void addEffectTooltip(FoodProperties food, List<Component> tooltip) {
'''
new = '''  public static void addEffectTooltip(FoodProperties food, List<Component> tooltip, float tickRate) {
'''
if s.count(old) != 1:
    raise SystemExit('ContainerFoodItem: tooltip helper signature mismatch')
s = s.replace(old, new)
old = '''  public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
    FoodProperties food = stack.getFoodProperties(null);
    if (food != null) {
      addEffectTooltip(food, tooltip);
    }
  }
'''
new = '''  public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
    FoodProperties food = stack.getFoodProperties(null);
    if (food != null) {
      addEffectTooltip(food, tooltip, context.tickRate());
    }
  }
'''
if s.count(old) != 1:
    raise SystemExit('ContainerFoodItem: item tooltip override mismatch')
s = s.replace(old, new)
p.write_text(s)
