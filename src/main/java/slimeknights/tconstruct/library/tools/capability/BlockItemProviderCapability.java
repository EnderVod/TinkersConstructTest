package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.ApiStatus;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;

/**
 * Provides block items to block-placement logic such as Exchanging and block-placement fluid effects.
 */
public interface BlockItemProviderCapability {
  ResourceLocation ID = TConstruct.getResource("block_provider");
  ItemCapability<BlockItemProviderCapability, Void> CAPABILITY = ItemCapability.createVoid(ID, BlockItemProviderCapability.class);

  /** Registers the default BlockItem provider through NeoForge's 1.21 item capability API. */
  @ApiStatus.Internal
  static void register() {
    TConstruct.getModBus().addListener(BlockItemProviderCapability::registerCapabilities);
  }

  private static void registerCapabilities(RegisterCapabilitiesEvent event) {
    Item[] blockItems = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof BlockItem).toArray(Item[]::new);
    if (blockItems.length > 0) {
      event.registerItem(CAPABILITY, (stack, context) -> SimpleBlockItem.INSTANCE, blockItems);
    }
  }

  /** Gets the provider for this stack, or null if the stack does not provide blocks. */
  @Nullable
  static BlockItemProviderCapability getBlockProvider(ItemStack stack) {
    return stack.getCapability(CAPABILITY);
  }

  /** Verifies that a provided stack contains a BlockItem. */
  @Nullable
  static BlockItem verifyBlockItem(ItemStack stack, BlockItemProviderCapability blockProvider) {
    if (stack.getItem() instanceof BlockItem blockItem) {
      return blockItem;
    }
    TConstruct.LOG.warn("BlockItemProviderCapability implementation tried to return a non-empty, non-blockitem stack! Cap: {}, Cap Class: {}, Provided Item: {}", blockProvider, blockProvider.getClass().getName(), Loadables.ITEM.getKey(stack.getItem()));
    return null;
  }

  ItemStack getBlockItemStack(ItemStack stack, @Nullable LivingEntity entity);

  void consume(ItemStack stack, ItemStack backingStack, @Nullable LivingEntity entity);

  /** Default provider for ordinary BlockItem stacks. */
  final class SimpleBlockItem implements BlockItemProviderCapability {
    public static final SimpleBlockItem INSTANCE = new SimpleBlockItem();
    private SimpleBlockItem() {}

    @Override
    public ItemStack getBlockItemStack(ItemStack capStack, @Nullable LivingEntity entity) {
      return capStack.isEmpty() ? ItemStack.EMPTY : capStack;
    }

    @Override
    public void consume(ItemStack capStack, ItemStack backingStack, @Nullable LivingEntity entity) {
      capStack.shrink(1);
    }
  }
}
