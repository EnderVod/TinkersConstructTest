from pathlib import Path
ROOT = Path('src/main/java/slimeknights/tconstruct')

def read(rel):
 p=ROOT/rel; return p,p.read_text()
def once(rel, old, new):
 p,s=read(rel); n=s.count(old)
 if n!=1: raise SystemExit(f'{rel}: expected 1, got {n}: {old[:80]!r}')
 p.write_text(s.replace(old,new))

# Vanilla Recipe output methods now accept HolderLookup.Provider, not RegistryAccess.
files = [
 'library/recipe/casting/ItemCastingRecipe.java',
 'library/recipe/casting/CastDuplicationRecipe.java',
 'library/recipe/casting/PotionCastingRecipe.java',
 'library/recipe/casting/TippingCastingRecipe.java',
 'library/recipe/casting/TipClearingCastingRecipe.java',
 'library/recipe/casting/RetexturedCastingRecipe.java',
 'library/recipe/casting/material/MaterialCastingRecipe.java',
 'library/recipe/casting/material/PartSwapCastingRecipe.java',
 'library/recipe/casting/material/ToolCastingRecipe.java',
]
for rel in files:
 p,s=read(rel)
 old_sigs = s.count('public ItemStack getResultItem(RegistryAccess ') + s.count('public ItemStack assemble(ICastingContainer inv, RegistryAccess ')
 if old_sigs == 0:
  raise SystemExit(f'{rel}: no legacy recipe output signature found')
 if 'import net.minecraft.core.HolderLookup;\n' not in s:
  anchor='import net.minecraft.core.RegistryAccess;\n'
  if s.count(anchor)!=1: raise SystemExit(f'{rel}: RegistryAccess import mismatch')
  s=s.replace(anchor, 'import net.minecraft.core.HolderLookup;\n'+anchor)
 s=s.replace('public ItemStack getResultItem(RegistryAccess access)', 'public ItemStack getResultItem(HolderLookup.Provider access)')
 s=s.replace('public ItemStack getResultItem(RegistryAccess registryAccess)', 'public ItemStack getResultItem(HolderLookup.Provider registryAccess)')
 s=s.replace('public ItemStack assemble(ICastingContainer inv, RegistryAccess access)', 'public ItemStack assemble(ICastingContainer inv, HolderLookup.Provider access)')
 p.write_text(s)

# FluidStack copy constructor was removed; preserve fluid components with copyWithAmount.
once('library/recipe/casting/material/AbstractMaterialCastingRecipe.java',
     '.map(fluid -> new FluidStack(fluid, fluid.getAmount() * itemCost))',
     '.map(fluid -> fluid.copyWithAmount(fluid.getAmount() * itemCost))')
once('library/recipe/melting/MeltingRecipe.java',
     'return new FluidStack(output.get(), Config.COMMON.foundryOreRate.applyOreBoost(rate, output.getAmount()));',
     'return output.get().copyWithAmount(Config.COMMON.foundryOreRate.applyOreBoost(rate, output.getAmount()));')
once('library/recipe/melting/DamageableMeltingRecipe.java',
     'return new FluidStack(fluid, amount);',
     'return fluid.copyWithAmount(amount);')
rel='library/recipe/melting/MaterialMeltingRecipe.java'
p,s=read(rel)
repls={
 'return new FluidStack(result.get(), result.getAmount() * cost);':'return result.get().copyWithAmount(result.getAmount() * cost);',
 'handler.fill(new FluidStack(byproduct.get(), byproduct.getAmount() * cost), FluidAction.EXECUTE);':'handler.fill(byproduct.get().copyWithAmount(byproduct.getAmount() * cost), FluidAction.EXECUTE);',
 'output = FluidOutput.fromStack(new FluidStack(output.get(), output.getAmount() * cost));':'output = FluidOutput.fromStack(output.get().copyWithAmount(output.getAmount() * cost));',
 'byproducts = byproducts.stream().map(fluid -> FluidOutput.fromStack(new FluidStack(fluid.get(), fluid.getAmount() * cost))).toList();':'byproducts = byproducts.stream().map(fluid -> FluidOutput.fromStack(fluid.get().copyWithAmount(fluid.getAmount() * cost))).toList();',
}
for old,new in repls.items():
 if s.count(old)!=1: raise SystemExit(f'{rel}: expected {old!r} once, got {s.count(old)}')
 s=s.replace(old,new)
p.write_text(s)

# RecipeManager now returns RecipeHolder. Keep value caches, but preserve the holder ID for active casts.
rel='smeltery/block/entity/CastingBlockEntity.java'
p,s=read(rel)
anchor='import net.minecraft.world.item.crafting.RecipeType;\n'
if s.count(anchor)!=1 or 'import net.minecraft.world.item.crafting.RecipeHolder;\n' in s:
 raise SystemExit('CastingBlockEntity: RecipeHolder import state mismatch')
s=s.replace(anchor, 'import net.minecraft.world.item.crafting.RecipeHolder;\n'+anchor)
old='''  /** Current in progress recipe */
  private ICastingRecipe currentRecipe;
  /** Name of the current recipe, fetched from Tag. Used since Tag is read before recipe manager access */
  private ResourceLocation recipeName;
  /** Cache recipe to reduce time during recipe lookups. Not saved to Tag */
  private ICastingRecipe lastCastingRecipe;
'''
new='''  /** Current in progress recipe */
  private ICastingRecipe currentRecipe;
  /** ID of the current in progress recipe, kept separately since recipes no longer own their registry ID. */
  private ResourceLocation currentRecipeId;
  /** Name of the current recipe, fetched from Tag. Used since Tag is read before recipe manager access */
  private ResourceLocation recipeName;
  /** Cache recipe to reduce time during recipe lookups. Not saved to Tag */
  private ICastingRecipe lastCastingRecipe;
  /** ID paired with {@link #lastCastingRecipe}. */
  private ResourceLocation lastCastingRecipeId;
'''
if s.count(old)!=1: raise SystemExit('CastingBlockEntity: recipe fields mismatch')
s=s.replace(old,new)
old='''    ICastingRecipe castingRecipe = level.getRecipeManager().getRecipeFor(this.castingType, castingInventory, level).orElse(null);
    if (castingRecipe != null) {
      this.lastCastingRecipe = castingRecipe;
    }
    return castingRecipe;
'''
new='''    RecipeHolder<ICastingRecipe> holder = level.getRecipeManager().getRecipeFor(this.castingType, castingInventory, level).orElse(null);
    if (holder != null) {
      this.lastCastingRecipe = holder.value();
      this.lastCastingRecipeId = holder.id();
      return holder.value();
    }
    return null;
'''
if s.count(old)!=1: raise SystemExit('CastingBlockEntity: casting lookup mismatch')
s=s.replace(old,new)
old='''    Optional<MoldingRecipe> newRecipe = level.getRecipeManager().getRecipeFor(moldingType, moldingInventory, level);
    if (newRecipe.isPresent()) {
      lastMoldingRecipe = newRecipe.get();
      return lastMoldingRecipe;
    }
'''
new='''    Optional<RecipeHolder<MoldingRecipe>> newRecipe = level.getRecipeManager().getRecipeFor(moldingType, moldingInventory, level);
    if (newRecipe.isPresent()) {
      lastMoldingRecipe = newRecipe.get().value();
      return lastMoldingRecipe;
    }
'''
if s.count(old)!=1: raise SystemExit('CastingBlockEntity: molding lookup mismatch')
s=s.replace(old,new)
old='''          currentRecipe = findCastingRecipe();
          recipeName = null;
'''
new='''          currentRecipe = findCastingRecipe();
          currentRecipeId = lastCastingRecipeId;
          recipeName = null;
'''
if s.count(old)!=1: raise SystemExit('CastingBlockEntity: refresh assignment mismatch')
s=s.replace(old,new)
old='''          this.currentRecipe = castingRecipe;
          this.recipeName = null;
          this.lastOutput = null;
'''
if s.count(old)!=2: raise SystemExit(f'CastingBlockEntity: expected two init assignment blocks, got {s.count(old)}')
s=s.replace(old, '''          this.currentRecipe = castingRecipe;
          this.currentRecipeId = lastCastingRecipeId;
          this.recipeName = null;
          this.lastOutput = null;
''')
old='''    currentRecipe = null;
    recipeName = null;
'''
new='''    currentRecipe = null;
    currentRecipeId = null;
    recipeName = null;
'''
if s.count(old)!=1: raise SystemExit('CastingBlockEntity: reset block mismatch')
s=s.replace(old,new)
old='''        this.currentRecipe = recipe;
        castingInventory.setFluid(fluid);
'''
new='''        this.currentRecipe = recipe;
        this.currentRecipeId = name;
        castingInventory.setFluid(fluid);
'''
if s.count(old)!=1: raise SystemExit('CastingBlockEntity: load assignment mismatch')
s=s.replace(old,new)
old='''    if (currentRecipe != null) {
      tags.putString(TAG_RECIPE, currentRecipe.getId().toString());
'''
new='''    if (currentRecipe != null && currentRecipeId != null) {
      tags.putString(TAG_RECIPE, currentRecipeId.toString());
'''
if s.count(old)!=1: raise SystemExit('CastingBlockEntity: save recipe ID mismatch')
s=s.replace(old,new)
old='ResourceLocation name = new ResourceLocation(tags.getString(TAG_RECIPE));'
if s.count(old)!=1: raise SystemExit('CastingBlockEntity: old ResourceLocation parse mismatch')
s=s.replace(old, 'ResourceLocation name = ResourceLocation.parse(tags.getString(TAG_RECIPE));')
p.write_text(s)

# Alloy recipe manager results are holders; unwrap into a mutable value list since this class removes entries while iterating.
rel='smeltery/block/entity/module/alloying/MultiAlloyingModule.java'
p,s=read(rel)
anchor='import java.util.Collections;\n'
if s.count(anchor)!=1 or 'import java.util.ArrayList;\n' in s: raise SystemExit('MultiAlloyingModule: import state mismatch')
s=s.replace(anchor, 'import java.util.ArrayList;\n'+anchor)
old='lastRecipes = getLevel().getRecipeManager().getRecipesFor(TinkerRecipeTypes.ALLOYING.get(), alloyTank, getLevel());'
new='lastRecipes = new ArrayList<>(getLevel().getRecipeManager().getRecipesFor(TinkerRecipeTypes.ALLOYING.get(), alloyTank, getLevel()).stream().map(holder -> holder.value()).toList());'
if s.count(old)!=1: raise SystemExit('MultiAlloyingModule: recipe list mismatch')
s=s.replace(old,new)
p.write_text(s)
