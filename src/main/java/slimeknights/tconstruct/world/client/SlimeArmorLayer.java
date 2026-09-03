package slimeknights.tconstruct.world.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SkullBlock.Type;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.Map;

/** Generics do not match to use the vanilla armor layer, so this reimplements the head-only portion of {@link HumanoidArmorLayer}. */
public class SlimeArmorLayer<T extends Slime, M extends HierarchicalModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T,M> {
  private final A armorModel;
  public final Map<Type,SkullModelBase> skullModels;
  private final boolean lavaSlime;

  public SlimeArmorLayer(RenderLayerParent<T,M> renderer, A armorModel, EntityModelSet modelSet, boolean lavaSlime) {
    super(renderer);
    this.armorModel = armorModel;
    this.skullModels = SkullBlockRenderer.createSkullRenderers(modelSet);
    this.lavaSlime = lavaSlime;
  }

  @Override
  public void render(PoseStack matrices, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float swing, float partialTicks, float age, float headYaw, float headPitch) {
    ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
    if (helmet.isEmpty()) {
      return;
    }

    matrices.pushPose();
    if (lavaSlime) {
      float squish = Mth.lerp(partialTicks, entity.oSquish, entity.squish);
      if (squish < 0) {
        squish = 0;
      }
      matrices.translate(0, 1.5 - 0.425 * squish, 0);
    } else {
      matrices.translate(0, 1.5, 0);
    }
    matrices.scale(0.9f, 0.9f, 0.9f);

    Item item = helmet.getItem();
    if (item instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.HEAD) {
      this.getParentModel().copyPropertiesTo(armorModel);
      armorModel.setAllVisible(false);
      armorModel.head.visible = true;
      armorModel.hat.visible = true;

      Model model = ClientHooks.getArmorModel(entity, helmet, EquipmentSlot.HEAD, armorModel);
      IClientItemExtensions extensions = IClientItemExtensions.of(helmet);
      extensions.setupModelAnimations(entity, helmet, EquipmentSlot.HEAD, model, limbSwing, swing, partialTicks, age, headYaw, headPitch);

      ArmorMaterial material = armor.getMaterial().value();
      int fallbackColor = extensions.getDefaultDyeColor(helmet);
      for (int layerIndex = 0; layerIndex < material.layers().size(); layerIndex++) {
        ArmorMaterial.Layer layer = material.layers().get(layerIndex);
        int color = extensions.getArmorLayerTintColor(helmet, entity, layer, layerIndex, fallbackColor);
        if (color != 0) {
          ResourceLocation texture = ClientHooks.getArmorTexture(entity, helmet, layer, false, EquipmentSlot.HEAD);
          renderModel(matrices, buffer, packedLight, model, color, texture);
        }
      }
      if (helmet.hasFoil()) {
        renderGlint(matrices, buffer, packedLight, model);
      }
    } else if (item instanceof BlockItem block && block.getBlock() instanceof AbstractSkullBlock skullBlock) {
      matrices.scale(1.1875F, -1.1875F, -1.1875F);
      ResolvableProfile profile = helmet.get(DataComponents.PROFILE);
      matrices.translate(-0.5, 0.0, -0.5);
      SkullBlock.Type type = skullBlock.getType();
      SkullModelBase skullModel = this.skullModels.get(type);
      RenderType renderType = SkullBlockRenderer.getRenderType(type, profile);
      SkullBlockRenderer.renderSkull(null, 180.0F, limbSwing, matrices, buffer, packedLight, skullModel, renderType);
    } else {
      CustomHeadLayer.translateToHead(matrices, false);
      Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
        entity, helmet, ItemDisplayContext.HEAD, false, matrices, buffer, packedLight);
    }
    matrices.popPose();
  }

  private static void renderModel(PoseStack matrices, MultiBufferSource buffer, int packedLight, Model model, int color, ResourceLocation texture) {
    VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.armorCutoutNoCull(texture));
    model.renderToBuffer(matrices, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, color);
  }

  private static void renderGlint(PoseStack matrices, MultiBufferSource buffer, int packedLight, Model model) {
    model.renderToBuffer(matrices, buffer.getBuffer(RenderType.armorEntityGlint()), packedLight, OverlayTexture.NO_OVERLAY);
  }
}
