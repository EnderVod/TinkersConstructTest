from pathlib import Path
ROOT=Path('src/main/java/slimeknights/tconstruct')
def read(rel): p=ROOT/rel; return p,p.read_text()
def once(rel,old,new):
 p,s=read(rel); c=s.count(old)
 if c!=1: raise SystemExit(f'{rel}: expected 1 got {c}: {old[:90]!r}')
 p.write_text(s.replace(old,new))

# Advancement API now uses AdvancementHolder throughout.
p,s=read('shared/AchievementEvents.java')
if s.count('import net.minecraft.advancements.Advancement;\n')!=1: raise SystemExit('Achievement import')
s=s.replace('import net.minecraft.advancements.Advancement;\n','import net.minecraft.advancements.AdvancementHolder;\n')
old='Advancement advancement = server.getAdvancements().getAdvancement(ResourceLocation.parse(advancementResource));'
new='AdvancementHolder advancement = server.getAdvancements().get(ResourceLocation.parse(advancementResource));'
if s.count(old)!=1: raise SystemExit('Achievement lookup')
p.write_text(s.replace(old,new))

# WeatheringCopper renamed its random tick helper; block interactions split in 1.21.
p,s=read('shared/block/WeatheringPlatformBlock.java')
if s.count('import net.minecraft.world.InteractionResult;\n')!=1 or 'ItemInteractionResult' in s: raise SystemExit('Weathering imports')
s=s.replace('import net.minecraft.world.InteractionResult;\n','import net.minecraft.world.ItemInteractionResult;\n')
if s.count('this.onRandomTick(pState, pLevel, pPos, pRandom);')!=1: raise SystemExit('Weathering random tick')
s=s.replace('this.onRandomTick(pState, pLevel, pPos, pRandom);','this.changeOverTime(pState, pLevel, pPos, pRandom);')
old='''  @Override
  public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    ItemStack stack = player.getItemInHand(hand);
    if (stack.getItem() == Items.HONEYCOMB) {
      if (player instanceof ServerPlayer serverPlayer) {
        CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
      }
      if (!player.isCreative()) {
        stack.shrink(1);
      }
      level.setBlock(pos, TinkerCommons.waxedCopperPlatform.get(age).withPropertiesOf(state), 11);
      level.levelEvent(player, LevelEvent.PARTICLES_AND_SOUND_WAX_ON, pos, 0);
      return InteractionResult.sidedSuccess(level.isClientSide);
    }
    return InteractionResult.PASS;
  }
'''
new='''  @Override
  protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (stack.getItem() == Items.HONEYCOMB) {
      if (player instanceof ServerPlayer serverPlayer) {
        CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
      }
      if (!player.isCreative()) {
        stack.shrink(1);
      }
      level.setBlock(pos, TinkerCommons.waxedCopperPlatform.get(age).withPropertiesOf(state), 11);
      level.levelEvent(player, LevelEvent.PARTICLES_AND_SOUND_WAX_ON, pos, 0);
      return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }
'''
if s.count(old)!=1: raise SystemExit('Weathering use block')
p.write_text(s.replace(old,new))

# Magma bottle item signatures and fire API.
p,s=read('fluids/item/MagmaBottleItem.java')
if s.count('import javax.annotation.Nullable;\n')!=1: raise SystemExit('Magma nullable import')
s=s.replace('import javax.annotation.Nullable;\n','')
old='public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {'
new='public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {'
if s.count(old)!=1: raise SystemExit('Magma tooltip signature')
s=s.replace(old,new)
s=s.replace('super.appendHoverText(stack, worldIn, tooltip, flagIn);','super.appendHoverText(stack, context, tooltip, flagIn);')
if s.count('StringUtil.formatTickDuration(fireTime * 20)')!=1: raise SystemExit('Magma duration')
s=s.replace('StringUtil.formatTickDuration(fireTime * 20)','StringUtil.formatTickDuration(fireTime * 20, context.tickRate())')
if s.count('public int getUseDuration(ItemStack pStack)')!=1: raise SystemExit('Magma use duration')
s=s.replace('public int getUseDuration(ItemStack pStack)','public int getUseDuration(ItemStack pStack, LivingEntity entity)')
if s.count('living.setSecondsOnFire(fireTime);')!=1: raise SystemExit('Magma fire setter')
s=s.replace('living.setSecondsOnFire(fireTime);','living.igniteForSeconds(fireTime);')
p.write_text(s)

# Public packet listener connection accessor.
once('library/materials/MaterialRegistry.java','player.connection.connection.isMemoryConnection()','player.connection.getConnection().isMemoryConnection()')

# Rebuild BlockHitResult from public UseOnContext accessors instead of protected getHitResult().
once('library/tools/definition/module/aoe/BoxAOEIterator.java',
     'BlockHitResult hit = context.getHitResult();',
     'BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());')

# InventoryMenu is the one vanilla menu with a null MenuType; use the public type accessor for all others.
p,s=read('library/tools/capability/inventory/InventorySlotMenuModule.java')
anchor='import net.minecraft.world.inventory.AbstractContainerMenu;\n'
if s.count(anchor)!=1 or 'import net.minecraft.world.inventory.InventoryMenu;\n' in s: raise SystemExit('InventorySlot import')
s=s.replace(anchor,anchor+'import net.minecraft.world.inventory.InventoryMenu;\n')
old='''    // player inventory has a null type, which throws when used through the getter
    if (menu.menuType == null) {
      return true;
    }
'''
new='''    // player inventory has a null type, which throws when used through the getter
    if (menu instanceof InventoryMenu) {
      return true;
    }
'''
if s.count(old)!=1: raise SystemExit('InventorySlot raw menuType')
p.write_text(s.replace(old,new))

# Avoid enum constant shadowing between EquipmentSlot.Type.ARMOR and InteractionSource.ARMOR.
p,s=read('library/modifiers/hook/interaction/InteractionSource.java')
old='''  public static InteractionSource fromEquipmentSlot(EquipmentSlot slot) {
    return switch (slot.getType()) {
      case ARMOR -> InteractionSource.ARMOR;
      case HAND -> RIGHT_CLICK;
    };
  }
'''
new='''  public static InteractionSource fromEquipmentSlot(EquipmentSlot slot) {
    return slot.getType() == EquipmentSlot.Type.ARMOR ? ARMOR : RIGHT_CLICK;
  }
'''
if s.count(old)!=1: raise SystemExit('InteractionSource equipment switch')
p.write_text(s.replace(old,new))

print('small2+interaction validated')
