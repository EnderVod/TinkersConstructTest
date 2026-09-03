package slimeknights.tconstruct;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.library.TinkerItemDisplays;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionLoader;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.shared.TinkerAttributes;
import slimeknights.tconstruct.shared.TinkerClient;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerEffects;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.smeltery.item.TankItemFluidHandler;
import slimeknights.tconstruct.smeltery.item.CopperCanFluidHandler;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/**
 * TConstruct, the tool mod. Craft your tools with style, then modify until the original is gone!
 *
 * @author mDiyo
 */
@Mod(TConstruct.MOD_ID)
public class TConstruct {
  public static final String MOD_ID = "tconstruct";
  public static final Logger LOG = LogManager.getLogger(MOD_ID);
  public static final Random RANDOM = new Random();

  /** Instance of this mod, used for grabbing prototype fields. */
  public static TConstruct instance;
  private static IEventBus modBus;

  /**
   * NeoForge 1.21 injects the mod event bus into the mod constructor.
   * Keeping the bus here also lets older Tinkers modules migrate without relying on the removed
   * Forge {@code FMLJavaModLoadingContext#get()} pattern.
   */
  public TConstruct(IEventBus bus) {
    instance = this;
    modBus = bus;

    Config.init();
    TinkerItemDisplays.init();
    MaterialRegistry.init();

    bus.register(new TinkerCommons());
    bus.register(new TinkerMaterials());
    bus.register(new TinkerEffects());
    bus.register(new TinkerGadgets());
    bus.register(new TinkerAttributes());
    bus.register(new TinkerWorld());
    bus.register(new TinkerStructures());
    bus.register(new TinkerTables());
    bus.register(new TinkerModifiers());
    bus.register(new TinkerToolParts());
    bus.register(new TinkerTools());
    bus.register(new TinkerSmeltery());
    bus.register(new TinkerFluids());

    TinkerModule.initRegisters(bus);
    TinkerNetwork.setup();
    TinkerTags.init();
    bus.addListener(TConstruct::commonSetup);
    bus.addListener(TConstruct::registerCapabilities);

    if (FMLEnvironment.dist == Dist.CLIENT) {
      TinkerClient.onConstruct();
    }
  }

  public static IEventBus getModBus() {
    return modBus;
  }

  static void commonSetup(final FMLCommonSetupEvent event) {
    ToolDefinitionLoader.init();
    StationSlotLayoutLoader.init();
  }

  /** Registers native NeoForge capabilities for Tinkers blocks, block entities, and item stacks. */
  static void registerCapabilities(final RegisterCapabilitiesEvent event) {
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.tank.get(), (tank, side) -> tank.getTank());
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.melter.get(), (melter, side) -> melter.getTank());
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TinkerSmeltery.melter.get(), (melter, side) -> melter.getItemHandler());
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.alloyer.get(), (alloyer, side) -> alloyer.getTank());
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TinkerSmeltery.heater.get(), (heater, side) -> heater.getItemHandler());
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.fluidCannon.get(), (cannon, side) -> cannon.getTank());
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TinkerSmeltery.fluidCannon.get(), (cannon, side) -> cannon.getItemHandler());
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.castingTank.get(), (castingTank, side) -> castingTank.getTank());
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.channel.get(), (channel, side) -> channel.getFluidHandler(side));
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.proxyTank.get(), (proxy, side) -> proxy.getItemTank());
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TinkerSmeltery.proxyTank.get(), (proxy, side) -> proxy.getItemTank());

    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.table.get(), (casting, side) -> casting.getTank());
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TinkerSmeltery.table.get(), (casting, side) -> casting.getItemHandler());
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.basin.get(), (casting, side) -> casting.getTank());
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TinkerSmeltery.basin.get(), (casting, side) -> casting.getItemHandler());

    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TinkerSmeltery.smeltery.get(), (controller, side) -> controller.getMeltingInventory());
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.smeltery.get(),
      (controller, side) -> controller.getStructure() == null ? null : controller.getTank());
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TinkerSmeltery.foundry.get(), (controller, side) -> controller.getMeltingInventory());
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.foundry.get(),
      (controller, side) -> controller.getStructure() == null ? null : controller.getTank());

    // Drains, ducts, and chutes forward the capability exposed by their validated controller.
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.drain.get(), (io, side) -> io.getHandler(side));
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TinkerSmeltery.duct.get(), (io, side) -> io.getHandler(side));
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TinkerSmeltery.chute.get(), (io, side) -> io.getHandler(side));

    for (Item item : BuiltInRegistries.ITEM) {
      if (item instanceof TankItem tankItem) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new TankItemFluidHandler(tankItem, stack), item);
      }
    }
    event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new CopperCanFluidHandler(stack), TinkerSmeltery.copperCan.get());
  }

  public static ResourceLocation getResource(String name) {
    return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
  }

  public static <T> TinkerDataKey<T> createKey(String name) {
    return TinkerDataKey.of(getResource(name));
  }

  public static <T> ComputableDataKey<T> createKey(String name, Supplier<T> constructor) {
    return ComputableDataKey.of(getResource(name), constructor);
  }

  public static String resourceString(String res) {
    return String.format("%s:%s", MOD_ID, res);
  }

  public static String prefix(String name) {
    return MOD_ID + "." + name.toLowerCase(Locale.US);
  }

  public static String makeDescriptionId(String type, String name) {
    return type + "." + MOD_ID + "." + name;
  }

  public static String makeTranslationKey(String base, String name) {
    return Util.makeTranslationKey(base, getResource(name));
  }

  public static MutableComponent makeTranslation(String base, String name) {
    return Component.translatable(makeTranslationKey(base, name));
  }

  public static MutableComponent makeTranslation(String base, String name, Object... arguments) {
    return Component.translatable(makeTranslationKey(base, name), arguments);
  }

  public static void sealTinkersClass(Object self, String base, String solution) {
    String name = self.getClass().getName();
    if (!name.startsWith("slimeknights.tconstruct.")) {
      throw new IllegalStateException(base + " being extended from invalid package " + name + ". " + solution);
    }
  }
}
