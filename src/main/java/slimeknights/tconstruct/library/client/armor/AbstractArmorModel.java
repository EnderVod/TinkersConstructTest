package slimeknights.tconstruct.library.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier.ArmorTexture;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier.TextureType;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;

import javax.annotation.Nullable;

/** Common shared logic for material armor models */
public abstract class AbstractArmorModel extends Model {
  @Nullable
  protected HumanoidModel<?> base;
  protected boolean hasGlint = false;
  protected TextureType textureType = TextureType.ARMOR;
  protected boolean hasWings = false;

  protected AbstractArmorModel() {
    super(RenderType::entityCutoutNoCull);
  }

  protected void setup(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> base) {
    this.base = base;
    this.hasGlint = stack.hasFoil();
    this.textureType = TextureType.fromSlot(slot);
    if (slot == EquipmentSlot.CHEST) {
      this.hasWings = ModifierUtil.checkVolatileFlag(stack, ModifiableArmorItem.ELYTRA);
      if (hasWings) {
        ElytraModel<LivingEntity> wings = getWings();
        wings.setupAnim(living, 0, 0, 0, 0, 0);
        copyProperties(base, wings);
      }
    } else {
      hasWings = false;
    }
  }

  /** Packs float RGBA channels into the ARGB color expected by 1.21 model rendering. */
  public static int packColor(float red, float green, float blue, float alpha) {
    return ((int)(alpha * 255.0F) & 255) << 24
      | ((int)(red * 255.0F) & 255) << 16
      | ((int)(green * 255.0F) & 255) << 8
      | (int)(blue * 255.0F) & 255;
  }

  /** Renders a colored model */
  public static void renderColored(Model model, PoseStack matrices, VertexConsumer buffer, int packedLightIn, int packedOverlayIn, int color, float red, float green, float blue, float alpha) {
    if (color != -1) {
      alpha *= (float)(color >> 24 & 255) / 255.0F;
      red *= (float)(color >> 16 & 255) / 255.0F;
      green *= (float)(color >> 8 & 255) / 255.0F;
      blue *= (float)(color & 255) / 255.0F;
    }
    model.renderToBuffer(matrices, buffer, packedLightIn, packedOverlayIn, packColor(red, green, blue, alpha));
  }

  protected void renderWings(PoseStack matrices, int packedLightIn, int packedOverlayIn, ArmorTexture texture, float red, float green, float blue, float alpha, boolean hasGlint) {
    matrices.pushPose();
    matrices.translate(0.0D, 0.0D, 0.125D);
    assert buffer != null;
    texture.renderTexture(getWings(), matrices, buffer, packedLightIn, packedOverlayIn, red, green, blue, alpha, hasGlint);
    matrices.popPose();
  }

  @Nullable
  public static MultiBufferSource buffer;

  public static void init() {
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, RenderLivingEvent.Pre.class, event -> buffer = event.getMultiBufferSource());
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, RenderLivingEvent.Post.class, event -> buffer = null);
  }

  @Nullable
  private static ElytraModel<LivingEntity> wingsModel;

  private ElytraModel<LivingEntity> getWings() {
    if (wingsModel == null) {
      wingsModel = new ElytraModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELYTRA));
    }
    return wingsModel;
  }

  @SuppressWarnings("unchecked")
  public static <T extends LivingEntity> void copyProperties(EntityModel<T> base, EntityModel<?> other) {
    base.copyPropertiesTo((EntityModel<T>)other);
  }
}
