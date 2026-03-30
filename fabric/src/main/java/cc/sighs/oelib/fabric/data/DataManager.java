package cc.sighs.oelib.fabric.data;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.data.api.DataDriven;
import cc.sighs.oelib.data.api.DataValidator;
import cc.sighs.oelib.data.net.DataSyncPacket;
import cc.sighs.oelib.fabric.event.DataReloadEvent;
import cc.sighs.oelib.util.CodecUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.Strictness;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DataManager<T> extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.LENIENT).create();
    private static final Map<Class<?>, DataManager<?>> managers = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Set<String>> runtimeRegisteredNamespaces = new ConcurrentHashMap<>();
    private static boolean serverStarted = false;
    private static MinecraftServer currentServer = null;

    static {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverStarted = true;
            currentServer = server;
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            serverStarted = false;
            currentServer = null;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (server != null) {
                for (DataManager<?> manager : managers.values()) {
                    if (manager.annotation.syncToClient()) {
                        manager.syncToPlayer(player);
                    }
                }
            }
        });

    }

    private final Class<T> dataClass;
    private final DataDriven annotation;
    private final Codec<T> codec;
    private final Map<Identifier, T> loadedData = new ConcurrentHashMap<>();
    private final Map<Identifier, T> deferredData = new ConcurrentHashMap<>();
    private final Map<String, Set<T>> cache = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends DataValidator<?>>> namespaceValidatorClasses = new ConcurrentHashMap<>();
    private final Map<String, DataValidator<T>> namespaceValidators = new ConcurrentHashMap<>();
    private final DataValidator<T> defaultValidator;

    private DataManager(Class<T> dataClass) {
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

    @SuppressWarnings("unchecked")
    public static <T> DataManager<T> register(Class<T> dataClass) {
        if (!dataClass.isAnnotationPresent(DataDriven.class)) {
            throw new IllegalArgumentException("Class " + dataClass.getSimpleName() + " must be annotated with @DataDriven");
        }

        return (DataManager<T>) managers.computeIfAbsent(dataClass, DataManager::new);
    }

    @SuppressWarnings("unchecked")
    public static <T> DataManager<T> get(Class<T> dataClass) {
        return (DataManager<T>) managers.get(dataClass);
    }

    public static <T> void registerNamespace(Class<T> dataClass, String namespace) {
        runtimeRegisteredNamespaces
                .computeIfAbsent(dataClass, k -> ConcurrentHashMap.newKeySet())
                .add(namespace);
    }

    public static <T> void registerNamespaceValidator(Class<T> dataClass, String namespace, Class<? extends DataValidator<?>> validatorClass) {
        DataManager<T> mgr = get(dataClass);
        if (mgr != null && namespace != null && !namespace.isBlank() && validatorClass != null) {
            mgr.namespaceValidatorClasses.put(namespace, validatorClass);
        }
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }

    private static String getFolder(Class<?> dataClass) {
        DataDriven annotation = dataClass.getAnnotation(DataDriven.class);
        return annotation.folder();
    }

    private static String getModId(Class<?> dataClass) {
        DataDriven annotation = dataClass.getAnnotation(DataDriven.class);
        return annotation.modid();
    }

    public Identifier getReloadListenerId() {
        String classPath = dataClass.getName()
                .toLowerCase(Locale.ROOT)
                .replace('.', '/')
                .replace('$', '_');
        return Identifier.fromNamespaceAndPath(OELib.MODID, "data_manager/" + classPath);
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> data = new HashMap<>();
        String folder = getFolder(dataClass);
        String expectedNamespace = getModId(dataClass);

        manager.listResources(folder, path -> path.getPath().endsWith(".json")).forEach((rl, resource) -> {
            if (!expectedNamespace.isEmpty() && !rl.getNamespace().equals(expectedNamespace)) {
                return;
            }

            try (var reader = resource.openAsReader()) {
                JsonElement json = GSON.fromJson(reader, JsonElement.class);
                data.put(rl, json);
            } catch (Exception e) {
                OELib.LOGGER.error("Failed to load JSON from {}", rl, e);
            }
        });

        return data;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
        loadedData.clear();
        deferredData.clear();
        clearCache();

        Codec<T> currentCodec = CodecUtils.getCodec(dataClass);

        Set<String> allowNamespaces = new HashSet<>();
        if (!annotation.modid().isEmpty()) allowNamespaces.add(annotation.modid());
        Set<String> runtimeSet = runtimeRegisteredNamespaces.getOrDefault(dataClass, Collections.emptySet());
        allowNamespaces.addAll(runtimeSet);

        Map<Identifier, JsonElement> filteredObject = data.entrySet().stream()
                .filter(entry -> allowNamespaces.isEmpty() || allowNamespaces.contains(entry.getKey().getNamespace()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        OELib.LOGGER.info("Loading {} data from {} files (namespaces: {})",
                dataClass.getSimpleName(), filteredObject.size(), allowNamespaces);

        int validCount = 0;
        int deferredCount = 0;
        int invalidCount = 0;

        for (Map.Entry<Identifier, JsonElement> entry : filteredObject.entrySet()) {
            Identifier location = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                if (annotation.supportArray() && json.isJsonArray()) {
                    var jsonArray = json.getAsJsonArray();
                    OELib.LOGGER.debug("Processing array with {} elements from {}", jsonArray.size(), location);

                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonElement element = jsonArray.get(i);
                        Identifier elementLocation = Identifier.fromNamespaceAndPath(
                                location.getNamespace(),
                                location.getPath() + "_" + i
                        );

                        var result = currentCodec.parse(JsonOps.INSTANCE, element);
                        if (result.result().isPresent()) {
                            T dataObj = result.result().get();

                            var vr = validateData(dataObj, elementLocation);
                            if (vr.valid()) {
                                if (vr.deferrable()) {
                                    deferredData.put(elementLocation, dataObj);
                                    deferredCount++;
                                    OELib.LOGGER.debug("Deferred {} from array[{}]: {} ({})",
                                            dataClass.getSimpleName(), i, elementLocation, vr.message());
                                } else {
                                    loadedData.put(elementLocation, dataObj);

                                    if (annotation.enableCache()) {
                                        buildCache(dataObj);
                                    }

                                    validCount++;
                                    OELib.LOGGER.debug("Loaded {} from array[{}]: {}", dataClass.getSimpleName(), i, elementLocation);
                                }
                            } else {
                                invalidCount++;
                                OELib.LOGGER.warn("Invalid {} data in array[{}] of {}: {}",
                                        dataClass.getSimpleName(), i, location, vr.message());
                            }
                        } else {
                            invalidCount++;
                            OELib.LOGGER.error("Failed to parse {} data from array[{}] of {}: {}",
                                    dataClass.getSimpleName(), i, location, result.error().orElse(null));
                        }
                    }
                } else {
                    var result = currentCodec.parse(JsonOps.INSTANCE, json);
                    if (result.result().isPresent()) {
                        T dataObj = result.result().get();

                        var vr = validateData(dataObj, location);
                        if (vr.valid()) {
                            if (vr.deferrable()) {
                                deferredData.put(location, dataObj);
                                deferredCount++;
                                OELib.LOGGER.debug("Deferred {}: {} ({})",
                                        dataClass.getSimpleName(), location, vr.message());
                            } else {
                                loadedData.put(location, dataObj);

                                if (annotation.enableCache()) {
                                    buildCache(dataObj);
                                }

                                validCount++;
                                OELib.LOGGER.debug("Loaded {}: {}", dataClass.getSimpleName(), location);
                            }
                        } else {
                            invalidCount++;
                            OELib.LOGGER.warn("Invalid {} data in {}: {}", dataClass.getSimpleName(), location, vr.message());
                        }
                    } else {
                        invalidCount++;
                        OELib.LOGGER.error("Failed to parse {} data from {}: {}", dataClass.getSimpleName(), location, result.error().orElse(null));
                    }
                }
            } catch (Exception e) {
                invalidCount++;
                OELib.LOGGER.error("Error loading {} data from {}", dataClass.getSimpleName(), location, e);
            }
        }

        OELib.LOGGER.info("Loaded {} valid {} entries, {} deferred entries, {} invalid entries were skipped",
                validCount, dataClass.getSimpleName(), deferredCount, invalidCount);

        if (annotation.syncToClient() && serverStarted) {
            syncToAllPlayers();
        }

        DataReloadEvent.EVENT.invoker().onDataReload(dataClass, validCount + deferredCount, invalidCount);
    }

    private DataValidator.ValidationResult validateData(T data, Identifier source) {
        var validator = getValidatorForNamespace(source.getNamespace());
        if (validator instanceof DataValidator.ServerContextAware<T> contextAwareValidator) {
            return contextAwareValidator.validateWithContext(data, source, getCurrentServer());
        } else {
            return validator.validate(data, source);
        }
    }

    public Map<Identifier, T> getAllData() {
        return new HashMap<>(loadedData);
    }

    public T getData(Identifier location) {
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

    public void updateClientData(Map<Identifier, T> data) {
        loadedData.clear();
        loadedData.putAll(data);
        clearCache();

        if (annotation.enableCache()) {
            for (Map.Entry<Identifier, T> entry : data.entrySet()) {
                buildCache(entry.getValue());
            }
        }

        OELib.LOGGER.debug("Updated client data for {}: {} entries", dataClass.getSimpleName(), data.size());

        DataReloadEvent.EVENT.invoker().onDataReload(dataClass, data.size(), 0);
    }

    public void addToCache(String cacheKey, T data) {
        if (annotation.enableCache()) {
            cache.computeIfAbsent(cacheKey, k -> ConcurrentHashMap.newKeySet()).add(data);
        }
    }

    public void clearCache() {
        cache.clear();
    }

    protected void buildCache(T data) {
        addToCache("all", data);
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
                OELib.LOGGER.warn("Failed to instantiate validator for namespace '{}', fallback to default", ns, e);
                return defaultValidator;
            }
        });
    }

    private void syncToAllPlayers() {
        try {
            if (getCurrentServer() != null && (!loadedData.isEmpty() || !deferredData.isEmpty())) {
                Map<Identifier, T> allData = new HashMap<>(loadedData);
                allData.putAll(deferredData);
                DataSyncPacket<T> packet = new DataSyncPacket<>(dataClass, allData);
                packet.sendToAll();
                OELib.LOGGER.debug("Synced {} data to all players", dataClass.getSimpleName());
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to sync {} data to all players", dataClass.getSimpleName(), e);
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        if (player != null && annotation.syncToClient() && (!loadedData.isEmpty() || !deferredData.isEmpty())) {
            try {
                Map<Identifier, T> allData = new HashMap<>(loadedData);
                allData.putAll(deferredData);
                DataSyncPacket<T> packet = new DataSyncPacket<>(dataClass, allData);
                packet.sendTo(player);
                OELib.LOGGER.debug("Synced {} data to player: {}", dataClass.getSimpleName(), player.getName().getString());
            } catch (Exception e) {
                OELib.LOGGER.error("Failed to sync {} data to player {}", dataClass.getSimpleName(), player.getName().getString(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private DataValidator<T> createValidator(Class<? extends DataValidator<?>> validatorClass) {
        if (validatorClass == DataValidator.NoValidator.class) {
            return (DataValidator<T>) new DataValidator.NoValidator();
        }

        try {
            return (DataValidator<T>) validatorClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            OELib.LOGGER.warn("Failed to create validator {}, using no validator", validatorClass.getSimpleName(), e);
            return (DataValidator<T>) new DataValidator.NoValidator();
        }
    }
}
