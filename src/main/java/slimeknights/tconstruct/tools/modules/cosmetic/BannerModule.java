package slimeknights.tconstruct.tools.modules.cosmetic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.display.DisplayNameModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.utils.TinkerTooltipFlags;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.List;

/** Module for banner pattern tooltips */
public enum BannerModule implements ModifierModule, DisplayNameModifierHook, TooltipModifierHook {
  INSTANCE;

  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<BannerModule>defaultHooks(ModifierHooks.DISPLAY_NAME, ModifierHooks.TOOLTIP);
  public static final RecordLoadable<BannerModule> LOADER = new SingletonLoader<>(INSTANCE);
  /** Key for a dye color, stored as its ID */
  public static final String KEY_DYE = "dye";
  /** Key for a pattern color, as a 24 bit integer */
  public static final String KEY_COLOR = "color";
  /** Key for the banner pattern registry ID. */
  public static final String KEY_PATTERN = "pattern";
  /** Key for the banner pattern asset ID used by shield/banner textures. */
  public static final String KEY_ASSET = "asset";
  /** Key for the banner pattern translation key. */
  public static final String KEY_TRANSLATION = "translation";
  /** Tooltip key saying hold shift for patterns */
  private static final Component HOLD_SHIFT = TConstruct.makeTranslation("modifier", "banner.hold_shift").withStyle(ChatFormatting.GRAY);

  @Override
  public RecordLoadable<? extends ModifierModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public Component getDisplayName(IToolStackView tool, ModifierEntry entry, Component name, @Nullable RegistryAccess access) {
    // color the tooltip the color of the first pattern
    ListTag patterns = tool.getPersistentData().getList(patternKey(entry.getId()), ListTag.TAG_COMPOUND);
    if (!patterns.isEmpty()) {
      return name.copy().withStyle(name.getStyle().withColor(DyeColor.byId(patterns.getCompound(0).getInt(KEY_DYE)).getTextColor()));
    }
    return name;
  }

  @Override
  public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
    // add all patterns in a tinker station when holding
    if (tooltipFlag == TinkerTooltipFlags.TINKER_STATION) {
      if (tooltipKey == TooltipKey.SHIFT) {
        ListTag patterns = tool.getPersistentData().getList(patternKey(modifier.getId()), ListTag.TAG_COMPOUND);
        for (int i = 0; i < patterns.size(); i++) {
          CompoundTag tag = patterns.getCompound(i);
          DyeColor dye = DyeColor.byId(tag.getInt(KEY_DYE));
          String translation = tag.getString(KEY_TRANSLATION);
          if (!translation.isEmpty()) {
            tooltip.add(Component.translatable(translation + '.' + dye.getName()).withStyle(ChatFormatting.GRAY));
          } else if (access != null) {
            ResourceLocation patternId = ResourceLocation.tryParse(tag.getString(KEY_PATTERN));
            if (patternId != null) {
              access.lookupOrThrow(Registries.BANNER_PATTERN).get(ResourceKey.create(Registries.BANNER_PATTERN, patternId))
                .ifPresent(holder -> tooltip.add(Component.translatable(holder.value().translationKey() + '.' + dye.getName()).withStyle(ChatFormatting.GRAY)));
            }
          }
        }
      } else {
        tooltip.add(HOLD_SHIFT);
      }
    }
  }

  /** Gets the key for the cache used in the model */
  public static ResourceLocation cacheKey(ModifierId modifier) {
    return modifier.withSuffix("_cache");
  }

  /** Gets the key for the pattern list in NBT */
  public static ResourceLocation patternKey(ModifierId modifier) {
    return modifier.withSuffix("_patterns");
  }

  /** Adds one banner layer to persistent tool data. */
  private static void addPattern(ListTag patterns, Holder<BannerPattern> holder, DyeColor dye) {
    BannerPattern pattern = holder.value();
    CompoundTag tag = new CompoundTag();
    holder.unwrapKey().ifPresent(key -> tag.putString(KEY_PATTERN, key.location().toString()));
    tag.putString(KEY_ASSET, pattern.assetId().toString());
    tag.putString(KEY_TRANSLATION, pattern.translationKey());
    tag.putInt(KEY_DYE, dye.getId());
    tag.putInt(KEY_COLOR, Util.getColor(dye));
    patterns.add(tag);
  }

  /** Copies the given banner pattern component to the tool's persistent data. */
  public static void copyPatterns(ModDataNBT data, ModifierId id, DyeColor dye, RegistryAccess access, BannerPatternLayers banner) {
    int baseColor = Util.getColor(dye);
    ListTag patterns = new ListTag();

    // tools render the banner base as a pattern layer, matching shield rendering behavior
    Holder<BannerPattern> base = access.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.BASE);
    addPattern(patterns, base, dye);

    int hashCode = baseColor;
    for (BannerPatternLayers.Layer layer : banner.layers()) {
      addPattern(patterns, layer.pattern(), layer.color());
      int color = Util.getColor(layer.color());
      hashCode = 31 * (31 * hashCode + color) + layer.pattern().value().assetId().hashCode();
    }

    data.put(patternKey(id), patterns);
    data.putInt(cacheKey(id), hashCode);
  }

}
