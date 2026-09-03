package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.network.SyncPersistentDataPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Persistent per-entity Tinkers modifier data, stored through NeoForge 1.21 data attachments. */
public final class PersistentDataCapability {
  private PersistentDataCapability() {}

  private static final ResourceLocation ID = TConstruct.getResource("persistent_data");
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
    DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TConstruct.MOD_ID);

  /** Persistent attachment replacing the old Forge entity capability. */
  public static final Supplier<AttachmentType<ModDataNBT>> ATTACHMENT = ATTACHMENT_TYPES.register(ID.getPath(), () ->
    AttachmentType.builder(ModDataNBT::new)
      .serialize(new IAttachmentSerializer<CompoundTag, ModDataNBT>() {
        @Override
        public ModDataNBT read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
          return ModDataNBT.readFromNBT(tag.copy());
        }

        @Override
        public CompoundTag write(ModDataNBT attachment, HolderLookup.Provider provider) {
          return attachment.getCopy();
        }
      })
      .copyOnDeath()
      .build());

  /** Gets the data, creating the attachment on first use. */
  public static ModDataNBT getOrWarn(Entity entity) {
    if (!(entity instanceof LivingEntity) && !EntityModifierCapability.supportCapability(entity)) {
      TConstruct.LOG.warn("Creating Tinkers persistent data on unsupported entity {}", entity.getType());
    }
    return entity.getData(ATTACHMENT);
  }

  /** Gets existing data or an empty detached value when none has been created. */
  public static ModDataNBT getOrEmpty(Entity entity) {
    ModDataNBT data = entity.getExistingDataOrNull(ATTACHMENT);
    return data == null ? new ModDataNBT() : data;
  }

  /** Runs the consumer only if data is already present. */
  public static void getIfPresent(Entity entity, Consumer<ModDataNBT> consumer) {
    ModDataNBT data = entity.getExistingDataOrNull(ATTACHMENT);
    if (data != null) {
      consumer.accept(data);
    }
  }

  /** Registers the attachment and player sync listeners during mod construction. */
  public static void register() {
    ATTACHMENT_TYPES.register(TConstruct.getModBus());
    NeoForge.EVENT_BUS.addListener(PersistentDataCapability::playerRespawn);
    NeoForge.EVENT_BUS.addListener(PersistentDataCapability::playerChangeDimension);
    NeoForge.EVENT_BUS.addListener(PersistentDataCapability::playerLoggedIn);
  }

  private static void sync(Player player) {
    ModDataNBT data = player.getData(ATTACHMENT);
    TinkerNetwork.getInstance().sendTo(new SyncPersistentDataPacket(data.getCopy()), player);
  }

  private static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    sync(event.getEntity());
  }

  private static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    sync(event.getEntity());
  }

  private static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    sync(event.getEntity());
  }
}
