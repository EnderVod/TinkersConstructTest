package slimeknights.tconstruct.common;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Effect extension with a few helpers */
public class TinkerEffect extends MobEffect {
  /** If true, effect is visible, false for hidden */
  private final boolean show;
  public TinkerEffect(MobEffectCategory typeIn, boolean show) {
    this(typeIn, 0xffffff, show);
  }

  public TinkerEffect(MobEffectCategory typeIn, int color, boolean show) {
    super(typeIn, color);
    this.show = show;
  }

  // keep old call sites compact while targeting the holder-based 1.21 API
  public TinkerEffect addAttributeModifier(Attribute attribute, String id, double amount, Operation operation) {
    super.addAttributeModifier(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), ResourceLocation.fromNamespaceAndPath("tconstruct", id), amount, operation);
    return this;
  }

  public TinkerEffect addAttributeModifier(Holder<Attribute> attribute, String id, double amount, Operation operation) {
    super.addAttributeModifier(attribute, ResourceLocation.fromNamespaceAndPath("tconstruct", id), amount, operation);
    return this;
  }

  @Override
  public TinkerEffect addAttributeModifier(Holder<Attribute> attribute, ResourceLocation id, double amount, Operation operation) {
    super.addAttributeModifier(attribute, id, amount, operation);
    return this;
  }

  /* Visibility */

  @Override
  public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
    consumer.accept(new IClientMobEffectExtensions() {
      @Override
      public boolean isVisibleInInventory(MobEffectInstance effect) {
        return show;
      }

      @Override
      public boolean isVisibleInGui(MobEffectInstance effect) {
        return show;
      }
    });
  }

  /* Helpers */

  @Deprecated
  public MobEffectInstance apply(LivingEntity entity, int duration) {
    return this.apply(entity, duration, 0);
  }

  @Deprecated
  public MobEffectInstance apply(LivingEntity entity, int duration, int level) {
    return this.apply(entity, duration, level, false);
  }

  @Deprecated
  public MobEffectInstance apply(LivingEntity entity, int duration, int amplifier, boolean showIcon) {
    MobEffectInstance effect = new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this), duration, amplifier, false, false, showIcon);
    entity.addEffect(effect);
    return effect;
  }

  public static int getLevel(LivingEntity entity, MobEffect effect) {
    return getAmplifier(entity, effect) + 1;
  }

  public static int getLevel(LivingEntity entity, Holder<MobEffect> effect) {
    return getAmplifier(entity, effect) + 1;
  }

  public static int getLevel(LivingEntity entity, Supplier<? extends MobEffect> effect) {
    return getAmplifier(entity, effect.get()) + 1;
  }

  public static int getAmplifier(LivingEntity entity, MobEffect effect) {
    return getAmplifier(entity, BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
  }

  public static int getAmplifier(LivingEntity entity, Holder<MobEffect> effect) {
    MobEffectInstance instance = entity.getEffect(effect);
    if (instance != null) {
      return instance.getAmplifier();
    }
    return -1;
  }

  /** @deprecated use {@link #getAmplifier(LivingEntity, MobEffect)} or {@link #getLevel(LivingEntity, MobEffect)} */
  @Deprecated(forRemoval = true)
  public int getLevel(LivingEntity entity) {
    return getAmplifier(entity, this);
  }
}
