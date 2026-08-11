package cc.sighs.oelib.data.forge;

import cc.sighs.oelib.data.OELibData;
import cc.sighs.oelib.data.api.DataDriven;
import cc.sighs.oelib.data.api.DataValidator;
import cc.sighs.oelib.data.net.DataSyncPacket;
import cc.sighs.oelib.event.forge.DataReloadEvent;
import cc.sighs.oelib.data.util.CodecUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = OELibData.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataManager<T> extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Map<Class<?>, DataManager<?>> managers = new ConcurrentHashMap<>();
    private static boolean serverStarted = false;
    private static final Map<Class<?>, Set<String>> runtimeRegisteredNamespaces = new ConcurrentHashMap<>();
    private final Class<T> dataClass;
    private final DataDriven annotation;
    private final Codec<T> codec;
    private final Map<ResourceLocation, T> loadedData = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, T> deferredData = new ConcurrentHashMap<>();
    private final Map<String, Set<T>> cache = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends DataValidator<?>>> namespaceValidatorClasses = new ConcurrentHashMap<>();
    private final Map<String, DataValidator<T>> namespaceValidators = new ConcurrentHashMap<>();
    private final DataValidator<T> defaultValidator;

    private DataManager(Class<T> dataClass) {
        super(GSON, getFolder(dataClass));
        this.dataClass = dataClass;
        this.annotation = dataClass.getAnnotation(DataDriven.class);
        this.codec = CodecUtils.getCodec(dataClass);
        this.defaultValidator = createValidator(annotation.validator());

        for (DataDriven.ValidatorBinding binding : annotation.namespaceValidators()) {
            if (binding != null && binding.namespace() != null && !binding.namespace().isBlank() && binding.validator() != null) {
                namespaceValidatorClasses.put(binding.namespace(), binding.validator());
            }
        }
    }

    public static <T> void registerNamespaceValidator(Class<T> dataClass, String namespace, Class<? extends DataValidator<?>> validatorClass) {
        DataManager<T> mgr = get(dataClass);
        if (mgr != null && namespace != null && !namespace.isBlank() && validatorClass != null) {
            mgr.namespaceValidatorClasses.put(namespace, validatorClass);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> DataManager<T> register(Class<T> dataClass) {
        if (!dataClass.isAnnotationPresent(DataDriven.class)) {
            throw new IllegalArgumentException("Class " + dataClass.getSimpleName() + " must be annotated with @DataDriven");
        }

        return (DataManager<T>) managers.computeIfAbsent(dataClass, DataManager::new);
    }

    public static <T> void registerNamespace(Class<T> dataClass, String namespace) {
        runtimeRegisteredNamespaces
                .computeIfAbsent(dataClass, k -> ConcurrentHashMap.newKeySet())
                .add(namespace);
    }

    @SuppressWarnings("unchecked")
    public static <T> DataManager<T> get(Class<T> dataClass) {
        return (DataManager<T>) managers.get(dataClass);
    }

    private static String getFolder(Class<?> dataClass) {
        DataDriven annotation = dataClass.getAnnotation(DataDriven.class);
        return annotation.folder();
    }

    public Map<ResourceLocation, T> getAllData() {
        return new HashMap<>(loadedData);
    }

    public T getData(ResourceLocation location) {
        return loadedData.get(location);
    }

    public List<T> getDataList() {
        List<T> result = new ArrayList<>(loadedData.values());
        result.addAll(deferredData.values());
        return result;
    }

    public Set<T> getCachedData(String cacheKey) {
        if (!annotation.enableCache()) {
            throw new UnsupportedOperationException("Cache is disabled for " + dataClass.getSimpleName());
        }
        return cache.getOrDefault(cacheKey, Collections.emptySet());
    }

    public void addToCache(String cacheKey, T data) {
        if (annotation.enableCache()) {
            cache.computeIfAbsent(cacheKey, k -> ConcurrentHashMap.newKeySet()).add(data);
        }
    }

    public void clearCache() {
        cache.clear();
    }

    public void updateClientData(Map<ResourceLocation, T> data) {
        loadedData.clear();
        loadedData.putAll(data);
        clearCache();

        if (annotation.enableCache()) {
            for (Map.Entry<ResourceLocation, T> entry : data.entrySet()) {
                buildCache(entry.getValue());
            }
        }

        OELibData.LOGGER.debug("Updated client data for {}: {} entries", dataClass.getSimpleName(), data.size());

        MinecraftForge.EVENT_BUS.post(new DataReloadEvent(dataClass, data.size(), 0));
    }

    public void syncToPlayer(ServerPlayer player) {
        if (annotation.syncToClient() && (!loadedData.isEmpty() || !deferredData.isEmpty())) {
            try {
                Map<ResourceLocation, T> allData = new HashMap<>(loadedData);
                allData.putAll(deferredData);
                DataSyncPacket<T> packet = new DataSyncPacket<>(dataClass, allData);
                packet.sendTo(player);
                OELibData.LOGGER.debug("Synced {} data to player: {}", dataClass.getSimpleName(), player.getName().getString());
            } catch (Exception e) {
                OELibData.LOGGER.error("Failed to sync {} data to player {}", dataClass.getSimpleName(), player.getName().getString(), e);
            }
        }
    }


    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        loadedData.clear();
        deferredData.clear();
        clearCache();
        Codec<T> currentCodec = CodecUtils.getCodec(dataClass);

        Set<String> allowNamespaces = new HashSet<>();
        if (!annotation.modid().isEmpty()) allowNamespaces.add(annotation.modid());
        Set<String> runtimeSet = runtimeRegisteredNamespaces.getOrDefault(dataClass, Collections.emptySet());
        allowNamespaces.addAll(runtimeSet);

        Map<ResourceLocation, JsonElement> filteredObject = object.entrySet().stream()
                .filter(entry -> allowNamespaces.isEmpty() || allowNamespaces.contains(entry.getKey().getNamespace()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        OELibData.LOGGER.info("Loading {} data from {} files (namespaces: {})",
                dataClass.getSimpleName(), filteredObject.size(), allowNamespaces);

        int validCount = 0;
        int deferredCount = 0;
        int invalidCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : filteredObject.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                if (annotation.supportArray() && json.isJsonArray()) {
                    var jsonArray = json.getAsJsonArray();
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonElement element = jsonArray.get(i);
                        ResourceLocation elementLocation = new ResourceLocation(
                                location.getNamespace(),
                                location.getPath() + "_" + i
                        );

                        var result = currentCodec.parse(JsonOps.INSTANCE, element);
                        if (result.result().isPresent()) {
                            T data = result.result().get();
                            var v = getValidatorForNamespace(location.getNamespace());
                            var vr = v.validate(data, elementLocation);
                            if (vr.valid()) {
                                if (vr.deferrable()) {
                                    deferredData.put(elementLocation, data);
                                    deferredCount++;
                                } else {
                                    loadedData.put(elementLocation, data);
                                    if (annotation.enableCache()) buildCache(data);
                                    validCount++;
                                }
                            } else {
                                invalidCount++;
                                OELibData.LOGGER.warn("Invalid {} data in array[{}] of {}: {}", dataClass.getSimpleName(), i, location, vr.message());
                            }
                        } else {
                            invalidCount++;
                            OELibData.LOGGER.error("Failed to parse {} data from array[{}] of {}: {}", dataClass.getSimpleName(), i, location, result.error().orElse(null));
                        }
                    }
                } else {
                    var result = currentCodec.parse(JsonOps.INSTANCE, json);
                    if (result.result().isPresent()) {
                        T data = result.result().get();
                        var v = getValidatorForNamespace(location.getNamespace());
                        var vr = v.validate(data, location);
                        if (vr.valid()) {
                            if (vr.deferrable()) {
                                deferredData.put(location, data);
                                deferredCount++;
                            } else {
                                loadedData.put(location, data);
                                if (annotation.enableCache()) buildCache(data);
                                validCount++;
                            }
                        } else {
                            invalidCount++;
                            OELibData.LOGGER.warn("Invalid {} data in {}: {}", dataClass.getSimpleName(), location, vr.message());
                        }
                    } else {
                        invalidCount++;
                        OELibData.LOGGER.error("Failed to parse {} data from {}: {}", dataClass.getSimpleName(), location, result.error().orElse(null));
                    }
                }
            } catch (Exception e) {
                invalidCount++;
                OELibData.LOGGER.error("Error loading {} data from {}", dataClass.getSimpleName(), location, e);
            }
        }

        OELibData.LOGGER.info("Loaded {} valid {} entries, {} deferred entries, {} invalid entries were skipped",
                validCount, dataClass.getSimpleName(), deferredCount, invalidCount);

        if (annotation.syncToClient() && serverStarted) {
            syncToAllPlayers();
        }

        MinecraftForge.EVENT_BUS.post(new DataReloadEvent(dataClass, validCount + deferredCount, invalidCount));
    }

    protected void buildCache(T data) {
        addToCache("all", data);
    }

    public static MinecraftServer getCurrentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @SuppressWarnings("unchecked")
    private DataValidator<T> getValidatorForNamespace(String namespace) {
        if (namespace == null) return defaultValidator;
        return namespaceValidators.computeIfAbsent(namespace, ns -> {
            Class<? extends DataValidator<?>> cls = namespaceValidatorClasses.get(ns);
            if (cls == null) return defaultValidator;
            try {
                return (DataValidator<T>) cls.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                OELibData.LOGGER.warn("Failed to instantiate validator for namespace '{}', fallback to default", ns, e);
                return defaultValidator;
            }
        });
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        serverStarted = true;
    }

    @SuppressWarnings("unchecked")
    private DataValidator<T> createValidator(Class<? extends DataValidator<?>> validatorClass) {
        if (validatorClass == DataValidator.NoValidator.class) {
            return (DataValidator<T>) new DataValidator.NoValidator();
        }

        try {
            return (DataValidator<T>) validatorClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            OELibData.LOGGER.warn("Failed to create validator {}, using no validator", validatorClass.getSimpleName(), e);
            return (DataValidator<T>) new DataValidator.NoValidator();
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (DataManager<?> manager : managers.values()) {
                if (manager.annotation.syncToClient()) {
                    manager.syncToPlayer(player);
                }
            }
        }
    }

    private void syncToAllPlayers() {
        try {
            var server = getCurrentServer();
            if (server != null && (!loadedData.isEmpty() || !deferredData.isEmpty())) {
                Map<ResourceLocation, T> allData = new HashMap<>(loadedData);
                allData.putAll(deferredData);
                DataSyncPacket<T> packet = new DataSyncPacket<>(dataClass, allData);
                packet.sendToAll();
                OELibData.LOGGER.debug("Synced {} data to all players", dataClass.getSimpleName());
            }
        } catch (Exception e) {
            OELibData.LOGGER.error("Failed to sync {} data to all players", dataClass.getSimpleName(), e);
        }
    }
}