package slimeknights.tconstruct.library.utils;

import com.google.common.collect.Maps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Harvest tier names and ordering for the vanilla tiers used by Tinkers. */
public class HarvestTiers {
  private HarvestTiers() {}

  /**
   * NeoForge 1.21 removed TierSortingRegistry in favor of each tier declaring its incorrect-for-drops block tag.
   * Tinkers only persists vanilla harvest tiers, so retain their stable legacy IDs and ordering locally.
   */
  private static final Map<ResourceLocation,Tier> TIERS_BY_NAME = new LinkedHashMap<>();
  private static final Map<Tier,ResourceLocation> NAMES_BY_TIER = new IdentityHashMap<>();
  private static final List<Tier> SORTED_TIERS;

  static {
    register("wood", Tiers.WOOD);
    register("gold", Tiers.GOLD);
    register("stone", Tiers.STONE);
    register("iron", Tiers.IRON);
    register("diamond", Tiers.DIAMOND);
    register("netherite", Tiers.NETHERITE);
    SORTED_TIERS = List.copyOf(TIERS_BY_NAME.values());
  }

  private static void register(String name, Tier tier) {
    ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", name);
    TIERS_BY_NAME.put(id, tier);
    NAMES_BY_TIER.put(tier, id);
  }

  /** Gets a persisted tier by its legacy ID. */
  @Nullable
  public static Tier byName(ResourceLocation id) {
    return TIERS_BY_NAME.get(id);
  }

  /** Gets the stable persisted ID for a vanilla tier. */
  @Nullable
  public static ResourceLocation getName(Tier tier) {
    return NAMES_BY_TIER.get(tier);
  }

  /** Returns the supported harvest tiers ordered from weakest to strongest. */
  public static List<Tier> getSortedTiers() {
    return SORTED_TIERS;
  }

  /** Tests whether the passed tier is strong enough for the block using the 1.21 tier contract. */
  public static boolean isCorrectTierForDrops(Tier tier, BlockState state) {
    return !state.is(tier.getIncorrectBlocksForDrops());
  }

  /** Cache of name for each tier */
  private static final Map<Tier, Component> harvestLevelNames = Maps.newHashMap();
  /** Listener to clear name cache so we get new colors */
  public static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> harvestLevelNames.clear();

  /** Makes a translation key for the given name */
  private static MutableComponent makeLevelKey(Tier tier) {
    ResourceLocation id = getName(tier);
    if (id == null) {
      id = ResourceLocation.fromNamespaceAndPath(TConstruct.MOD_ID, "unknown");
    }
    String key = Util.makeTranslationKey("harvest_tier", id);
    TextColor color = ResourceColorManager.getTextColor(key);
    return TConstruct.makeTranslation("stat", key).withStyle(style -> style.withColor(color));
  }

  /** Gets the harvest level name for the given tier. */
  public static Component getName(Tier tier) {
    return harvestLevelNames.computeIfAbsent(tier, HarvestTiers::makeLevelKey);
  }

  /** Gets the larger of two tiers. */
  public static Tier max(Tier a, Tier b) {
    if (SORTED_TIERS.indexOf(b) > SORTED_TIERS.indexOf(a)) {
      return b;
    }
    return a;
  }

  /** Gets the smaller of two tiers. */
  public static Tier min(Tier a, Tier b) {
    if (SORTED_TIERS.indexOf(b) < SORTED_TIERS.indexOf(a)) {
      return b;
    }
    return a;
  }

  /** Gets the smallest supported tier. */
  public static Tier minTier() {
    return SORTED_TIERS.get(0);
  }
}
