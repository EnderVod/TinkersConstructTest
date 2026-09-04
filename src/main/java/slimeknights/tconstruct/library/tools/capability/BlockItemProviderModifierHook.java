package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;

/** A hook used to provide BlockItems through the BlockItemProviderCapability. */
public interface BlockItemProviderModifierHook {
  ItemStack getBlockItemStack(IToolStackView tool, ModifierEntry modifier, @Nullable LivingEntity entity);

  boolean consumeBlockItem(IToolStackView tool, ModifierEntry modifier, ItemStack backingStack, @Nullable LivingEntity entity);

  /** Stateless capability implementation; reads the current tool stack for every operation. */
  final class CapabilityImpl implements BlockItemProviderCapability {
    public static final CapabilityImpl INSTANCE = new CapabilityImpl();
    private CapabilityImpl() {}

    @Override
    public ItemStack getBlockItemStack(ItemStack capStack, @Nullable LivingEntity entity) {
      IToolStackView tool = ToolStack.from(capStack);
      for (ModifierEntry entry : tool.getModifiers()) {
        BlockItemProviderModifierHook hook = entry.getHook(ModifierHooks.BLOCK_ITEM_PROVIDER);
        ItemStack stack = hook.getBlockItemStack(tool, entry, entity);
        if (!stack.isEmpty()) {
          Item item = stack.getItem();
          if (item instanceof BlockItem) {
            return stack;
          }
          TConstruct.LOG.warn("ToolBlockItemProviderHook implementation tried to return a non-empty, non-blockitem stack! Hook: {}, Hook Class: {}, Provided Item: {}", hook, hook.getClass().getName(), Loadables.ITEM.getKey(item));
        }
      }
      return ItemStack.EMPTY;
    }

    @Override
    public void consume(ItemStack capStack, ItemStack backingStack, @Nullable LivingEntity entity) {
      IToolStackView tool = ToolStack.from(capStack);
      for (ModifierEntry entry : tool.getModifiers()) {
        if (entry.getHook(ModifierHooks.BLOCK_ITEM_PROVIDER).consumeBlockItem(tool, entry, backingStack, entity)) {
          return;
        }
      }
      TConstruct.LOG.warn("Could not find a modifier to consume {} from after providing it from ToolBlockItemProviderHook. This is likely causing a duplication glitch! Stack components: {}", Loadables.ITEM.getKey(backingStack.getItem()), backingStack.getComponents());
    }
  }
}
