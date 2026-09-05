package slimeknights.tconstruct.library.tools.layout;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.serialization.JsonOps;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Loader for tinker station slot layouts. */
@Log4j2
public class StationSlotLayoutLoader extends SimpleJsonResourceReloadListener {
  public static final String FOLDER = "tinkering/station_layouts";
  public static final Gson GSON = (new GsonBuilder())
    .registerTypeHierarchyAdapter(Ingredient.class, new IngredientSerializer())
    .registerTypeHierarchyAdapter(LayoutIcon.class, LayoutIcon.SERIALIZER)
    .registerTypeAdapter(Pattern.class, Pattern.PARSER)
    .setPrettyPrinting()
    .disableHtmlEscaping()
    .create();
  private static final StationSlotLayoutLoader INSTANCE = new StationSlotLayoutLoader();

  private Map<ResourceLocation, StationSlotLayout> layoutMap = Collections.emptyMap();
  private final List<ResourceLocation> requiredLayouts = new ArrayList<>();

  @Getter
  private List<StationSlotLayout> sortedSlots = Collections.emptyList();

  private IContext conditionContext = IContext.EMPTY;

  private StationSlotLayoutLoader() {
    super(GSON, FOLDER);
  }

  public void setSlots(Collection<StationSlotLayout> slots) {
    setSlots(slots.stream().collect(Collectors.toMap(StationSlotLayout::getName, Function.identity())));
  }

  private void setSlots(Map<ResourceLocation, StationSlotLayout> map) {
    this.layoutMap = map;
    this.sortedSlots = map.values().stream()
                          .filter(layout -> !layout.isMain())
                          .sorted(Comparator.comparingInt(StationSlotLayout::getSortIndex))
                          .collect(Collectors.toList());
  }

  /** Tests NeoForge conditions while preserving the reload event's tag-aware condition context. */
  private boolean conditionsMatch(JsonObject object) {
    JsonElement conditions = object.get(ConditionalOps.DEFAULT_CONDITIONS_KEY);
    if (conditions == null) {
      return true;
    }
    return ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, conditions)
      .getOrThrow(JsonParseException::new)
      .stream().allMatch(condition -> condition.test(conditionContext));
  }

  @Override
  protected void apply(Map<ResourceLocation,JsonElement> splashList, ResourceManager resourceManager, ProfilerFiller profiler) {
    long time = System.nanoTime();
    ImmutableMap.Builder<ResourceLocation, StationSlotLayout> builder = ImmutableMap.builder();
    for (Entry<ResourceLocation,JsonElement> entry : splashList.entrySet()) {
      ResourceLocation key = entry.getKey();
      JsonElement value = entry.getValue();
      try {
        JsonObject object = GsonHelper.convertToJsonObject(value, "station_layout");
        if (!object.entrySet().isEmpty() && conditionsMatch(object)) {
          StationSlotLayout layout = GSON.fromJson(object, StationSlotLayout.class);
          int size = layout.getInputSlots().size() + (layout.getToolSlot().isHidden() ? 0 : 1);
          if (size < 2) {
            throw new JsonParseException("Too few slots for layout " + key + ", must have at least 2");
          }
          layout.setName(key);
          builder.put(key, layout);
        }
      } catch (Exception e) {
        log.error("Failed to load station slot layout for name {}", key, e);
      }
    }
    setSlots(builder.build());
    log.info("Loaded {} station slot layouts in {} ms", layoutMap.size(), (System.nanoTime() - time) / 1000000f);
    List<String> missing = requiredLayouts.stream().filter(name -> !layoutMap.containsKey(name)).map(ResourceLocation::toString).collect(Collectors.toList());
    if (!missing.isEmpty()) {
      log.error("Failed to load the following required layouts: {}", String.join(", ", missing));
    }
  }

  public StationSlotLayout get(ResourceLocation name) {
    return layoutMap.getOrDefault(name, StationSlotLayout.EMPTY);
  }

  public void registerRequiredLayout(ResourceLocation name) {
    requiredLayouts.add(name);
  }

  private void onDatapackSync(OnDatapackSyncEvent event) {
    UpdateTinkerSlotLayoutsPacket packet = new UpdateTinkerSlotLayoutsPacket(layoutMap.values());
    TinkerNetwork.getInstance().sendToPlayerList(event.getPlayer(), event.getPlayerList(), packet);
  }

  private void addDataPackListeners(final AddReloadListenerEvent event) {
    event.addListener(this);
    conditionContext = event.getConditionContext();
  }

  public static StationSlotLayoutLoader getInstance() {
    return INSTANCE;
  }

  public static void init() {
    NeoForge.EVENT_BUS.addListener(INSTANCE::addDataPackListeners);
    NeoForge.EVENT_BUS.addListener(INSTANCE::onDatapackSync);
  }

  private static class IngredientSerializer implements JsonSerializer<Ingredient>, JsonDeserializer<Ingredient> {
    @Override
    public Ingredient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      return IngredientLoadable.DISALLOW_EMPTY.convert(json, "ingredient");
    }

    @Override
    public JsonElement serialize(Ingredient ingredient, Type typeOfSrc, JsonSerializationContext context) {
      return IngredientLoadable.DISALLOW_EMPTY.serialize(ingredient);
    }
  }
}
