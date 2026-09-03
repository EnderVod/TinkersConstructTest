package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Data attachment allowing supported entities (notably projectiles) to carry tool modifiers. */
public final class EntityModifierCapability {
  public static final EntityModifiers EMPTY = new EntityModifiers() {
    @Override
    public ModifierNBT getModifiers() {
      return ModifierNBT.EMPTY;
    }

    @Override
    public void setModifiers(ModifierNBT nbt) {}
  };

  private EntityModifierCapability() {}

  private static final List<Predicate<Entity>> ENTITY_PREDICATES = new ArrayList<>();
  private static final ResourceLocation ID = TConstruct.getResource("modifiers");
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
    DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TConstruct.MOD_ID);

  /** Serialized modifier attachment replacing the old Forge entity capability. */
  public static final Supplier<AttachmentType<ModifierNBT>> ATTACHMENT = ATTACHMENT_TYPES.register(ID.getPath(), () ->
    AttachmentType.builder(() -> ModifierNBT.EMPTY)
      .serialize(new IAttachmentSerializer<ListTag, ModifierNBT>() {
        @Override
        public ModifierNBT read(IAttachmentHolder holder, ListTag tag, HolderLookup.Provider provider) {
          return ModifierNBT.readFromNBT(tag);
        }

        @Nullable
        @Override
        public ListTag write(ModifierNBT attachment, HolderLookup.Provider provider) {
          return attachment.isEmpty() ? null : attachment.serializeToNBT();
        }
      })
      .build());

  public static EntityModifiers getCapability(Entity entity) {
    if (!supportCapability(entity)) {
      return EMPTY;
    }
    return new AttachmentEntityModifiers(entity);
  }

  public static ModifierNBT getOrEmpty(Entity entity) {
    if (!supportCapability(entity)) {
      return ModifierNBT.EMPTY;
    }
    ModifierNBT modifiers = entity.getExistingDataOrNull(ATTACHMENT);
    return modifiers == null ? ModifierNBT.EMPTY : modifiers;
  }

  public static boolean supportCapability(Entity entity) {
    for (Predicate<Entity> entityPredicate : ENTITY_PREDICATES) {
      if (entityPredicate.test(entity)) {
        return true;
      }
    }
    return false;
  }

  public static void registerEntityPredicate(Predicate<Entity> predicate) {
    ENTITY_PREDICATES.add(predicate);
  }

  /** Registers the attachment type during mod construction. */
  public static void register() {
    ATTACHMENT_TYPES.register(TConstruct.getModBus());
  }

  private record AttachmentEntityModifiers(Entity entity) implements EntityModifiers {
    @Override
    public ModifierNBT getModifiers() {
      return entity.getData(ATTACHMENT);
    }

    @Override
    public void setModifiers(ModifierNBT nbt) {
      entity.setData(ATTACHMENT, nbt);
    }
  }

  public interface EntityModifiers {
    ModifierNBT getModifiers();

    void setModifiers(ModifierNBT nbt);

    default void addModifiers(ModifierNBT nbt) {
      ModifierNBT existing = getModifiers();
      if (existing.isEmpty()) {
        setModifiers(nbt);
      } else {
        setModifiers(ModifierNBT.builder().add(existing).add(nbt).build());
      }
    }
  }
}
