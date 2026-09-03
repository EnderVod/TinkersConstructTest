package slimeknights.tconstruct.library.json.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.materials.RandomMaterial;

/** Bridges Tinkers legacy Mantle JSON loaders into Minecraft 1.21 codecs. */
public final class LootCodecs {
  private LootCodecs() {}

  public static final Codec<RandomMaterial> RANDOM_MATERIAL = Codec.PASSTHROUGH.xmap(
    dynamic -> RandomMaterial.LOADER.convert(dynamic.convert(JsonOps.INSTANCE).getValue(), "material", TypedMap.empty()),
    material -> new Dynamic<>(JsonOps.INSTANCE, RandomMaterial.LOADER.serialize(material))
  );
}
