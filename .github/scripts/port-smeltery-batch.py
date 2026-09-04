from pathlib import Path
ROOT = Path('src/main/java/slimeknights/tconstruct')

def read(rel):
    p=ROOT/rel; return p,p.read_text()
def once(rel, old, new, label=None):
    p,s=read(rel); c=s.count(old)
    if c!=1: raise SystemExit(f'{label or rel}: expected 1, got {c}: {old[:80]!r}')
    p.write_text(s.replace(old,new))
def all_count(rel, old, new, count, label=None):
    p,s=read(rel); c=s.count(old)
    if c!=count: raise SystemExit(f'{label or rel}: expected {count}, got {c}: {old[:80]!r}')
    p.write_text(s.replace(old,new))

# Registry holder tag check
once('library/tools/nbt/IToolContext.java', '.builtInRegistryHolder().containsTag(tag)', '.builtInRegistryHolder().is(tag)')

# Material ingredient custom data presence
p,s=read('library/recipe/ingredient/MaterialIngredient.java')
anchor='import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;\n'
if anchor not in s or 'import slimeknights.tconstruct.library.utils.TagUtil;\n' in s: raise SystemExit('MaterialIngredient import state')
s=s.replace(anchor, anchor+'import slimeknights.tconstruct.library.utils.TagUtil;\n')
if s.count('.filter(ItemStack::hasTag)')!=1: raise SystemExit('MaterialIngredient filter state')
p.write_text(s.replace('.filter(ItemStack::hasTag)', '.filter(TagUtil::hasTag)'))

# Block tag registry iterable -> stream
p,s=read('library/recipe/ingredient/BlockTagIngredient.java')
if 'import java.util.stream.StreamSupport;\n' in s: raise SystemExit('BlockTagIngredient already migrated')
anchor='import java.util.stream.Stream;\n'
if s.count(anchor)!=1 or s.count('BuiltInRegistries.BLOCK.getTagOrEmpty(tag).stream()')!=1: raise SystemExit('BlockTagIngredient state')
s=s.replace(anchor, anchor+'import java.util.stream.StreamSupport;\n')
s=s.replace('BuiltInRegistries.BLOCK.getTagOrEmpty(tag).stream()', 'StreamSupport.stream(BuiltInRegistries.BLOCK.getTagOrEmpty(tag).spliterator(), false)')
p.write_text(s)

# Material recipe IDs and stack sizing
p,s=read('library/recipe/material/MaterialRecipe.java')
if s.count('new ResourceLocation("missingno")')!=1 or s.count('ItemHandlerHelper.copyStackWithSize(stack, needed)')!=1: raise SystemExit('MaterialRecipe state')
s=s.replace('new ResourceLocation("missingno")','ResourceLocation.withDefaultNamespace("missingno")')
s=s.replace('ItemHandlerHelper.copyStackWithSize(stack, needed)','stack.copyWithCount(needed)')
s=s.replace('import net.neoforged.neoforge.items.ItemHandlerHelper;\n','')
p.write_text(s)

# Tool inventory stack copies and network stack codec
p,s=read('library/tools/capability/inventory/ToolInventoryCapability.java')
for old,new,count in [
 ('ItemHandlerHelper.copyStackWithSize(stack, canInsert)','stack.copyWithCount(canInsert)',1),
 ('ItemHandlerHelper.copyStackWithSize(stack, leftover)','stack.copyWithCount(leftover)',1),
 ('ItemHandlerHelper.copyStackWithSize(current, amount)','current.copyWithCount(amount)',1),
 ('buf.writeItem(stack);','ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);',1),
 ('IntStream.range(0, handler.getSlots()).mapToObj(handler::getStackInSlot).filter(stack -> !stack.isEmpty())','IntStream.range(0, handler.getSlots()).mapToObj(handler::getStackInSlot).filter(stack -> !stack.isEmpty()).toList()',1),
]:
    if s.count(old)!=count: raise SystemExit(f'ToolInventoryCapability {old}: {s.count(old)}')
    s=s.replace(old,new)
p.write_text(s)

# Persistent tool inventory item serialization bridge
p,s=read('library/tools/capability/inventory/InventoryModule.java')
anchor='import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;\n'
if s.count(anchor)!=1 or 'import slimeknights.tconstruct.library.utils.TagUtil;\n' in s: raise SystemExit('InventoryModule import state')
s=s.replace(anchor,anchor+'import slimeknights.tconstruct.library.utils.TagUtil;\n')
if s.count('ItemStack.of(compound)')!=3: raise SystemExit(f'InventoryModule ItemStack.of count {s.count("ItemStack.of(compound)")}')
s=s.replace('ItemStack.of(compound)','TagUtil.readItem(compound)')
if s.count('stack.save(compound);')!=1: raise SystemExit('InventoryModule save state')
s=s.replace('stack.save(compound);','TagUtil.saveItem(stack, compound);')
p.write_text(s)

# Component-preserving fluid resizing
fluid_files = {
 'library/tools/capability/fluid/FluidModifierHookIterator.java': [
   ('new FluidStack(resource, resource.getAmount() - drained.getAmount())','resource.copyWithAmount(resource.getAmount() - drained.getAmount())'),
   ('new FluidStack(drained, maxDrain - drained.getAmount())','drained.copyWithAmount(maxDrain - drained.getAmount())'),
 ],
 'library/tools/capability/fluid/ToolFluidCapability.java': [
   ('new FluidStack(resource, resource.getAmount() / size)','resource.copyWithAmount(resource.getAmount() / size)'),
 ],
 'library/tools/capability/fluid/TankModule.java': [
   ('new FluidStack(current, Math.min(current.getAmount(), resource.getAmount()))','current.copyWithAmount(Math.min(current.getAmount(), resource.getAmount()))'),
   ('new FluidStack(current, Math.min(current.getAmount(), maxDrain))','current.copyWithAmount(Math.min(current.getAmount(), maxDrain))'),
 ],
}
for rel,repls in fluid_files.items():
    p,s=read(rel)
    for old,new in repls:
        if s.count(old)!=1: raise SystemExit(f'{rel}: missing {old}')
        s=s.replace(old,new)
    p.write_text(s)

# Entity fluid data serializer: 1.21 stream codec API
p,s=read('tools/network/FluidDataSerializer.java')
new='''package slimeknights.tconstruct.tools.network;\n\nimport net.minecraft.network.RegistryFriendlyByteBuf;\nimport net.minecraft.network.codec.StreamCodec;\nimport net.minecraft.network.syncher.EntityDataSerializer;\nimport net.neoforged.neoforge.fluids.FluidStack;\n\n/** Serializer for fluid stack data in entities */\npublic class FluidDataSerializer implements EntityDataSerializer<FluidStack> {\n  @Override\n  public StreamCodec<? super RegistryFriendlyByteBuf, FluidStack> codec() {\n    return FluidStack.STREAM_CODEC;\n  }\n\n  @Override\n  public FluidStack copy(FluidStack stack) {\n    return stack.copy();\n  }\n}\n'''
if 'public void write(FriendlyByteBuf buffer, FluidStack stack)' not in s or 'public FluidStack read(FriendlyByteBuf buffer)' not in s: raise SystemExit('FluidDataSerializer state')
p.write_text(new)

# Custom data presence for mining hook
p,s=read('library/tools/definition/module/mining/MiningSpeedToolHook.java')
anchor='import slimeknights.tconstruct.library.tools.stat.ToolStats;\n'
if s.count(anchor)!=1 or 'import slimeknights.tconstruct.library.utils.TagUtil;\n' in s: raise SystemExit('MiningSpeed import state')
s=s.replace(anchor,anchor+'import slimeknights.tconstruct.library.utils.TagUtil;\n')
if s.count('if (!tool.hasTag())')!=1: raise SystemExit('MiningSpeed hasTag state')
p.write_text(s.replace('if (!tool.hasTag())','if (!TagUtil.hasTag(tool))'))

# Qualify enum constant shadowed by EquipmentSlot.Type.ARMOR
once('library/modifiers/hook/interaction/InteractionSource.java','case ARMOR -> ARMOR;','case ARMOR -> InteractionSource.ARMOR;')

# Attribute registry values are holders in 1.21; existing helper still operates on Attribute values
p,s=read('library/tools/context/ToolAttackContext.java')
for old,new in [
 ('ToolAttackUtil.getToolAttribute(tool, attacker, Attributes.ATTACK_DAMAGE, tool.getStats().get(ToolStats.ATTACK_DAMAGE))','ToolAttackUtil.getToolAttribute(tool, attacker, Attributes.ATTACK_DAMAGE.value(), tool.getStats().get(ToolStats.ATTACK_DAMAGE))'),
 ('ToolAttackUtil.getToolAttribute(tool, attacker, Attributes.ATTACK_KNOCKBACK, baseKnockback)','ToolAttackUtil.getToolAttribute(tool, attacker, Attributes.ATTACK_KNOCKBACK.value(), baseKnockback)'),
]:
    if s.count(old)!=1: raise SystemExit('ToolAttackContext attribute state')
    s=s.replace(old,new)
p.write_text(s)

print('mechanical batch validated')
