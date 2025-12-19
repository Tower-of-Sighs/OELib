package com.sighs.oelib.fabric.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.sighs.oelib.OELib;
import com.sighs.oelib.data.api.DataDriven;
import com.sighs.oelib.data.api.DataValidator;
import com.sighs.oelib.fabric.data.net.DataSyncPacket;
import com.sighs.oelib.fabric.event.DataReloadEvent;
import com.sighs.oelib.util.CodecUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 通用数据管理器。
 * <p>
 * 负责管理所有带有 {@link DataDriven} 注解的数据类型，
 * 提供数据加载、验证、缓存、网络同步等功能。
 * </p>
 *
 * @param <T> 数据类型
 */
public class DataManager<T> implements SimpleResourceReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Map<Class<?>, DataManager<?>> managers = new ConcurrentHashMap<>();
    private static boolean serverStarted = false;
    private static final Map<Class<?>, Set<String>> runtimeRegisteredNamespaces = new ConcurrentHashMap<>();
    private static MinecraftServer currentServer = null;
    private final Class<T> dataClass;
    private final DataDriven annotation;
    private final Codec<T> codec;
    private final Map<ResourceLocation, T> loadedData = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, T> deferredData = new ConcurrentHashMap<>();
    private final Map<String, Set<T>> cache = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends DataValidator<?>>> namespaceValidatorClasses = new ConcurrentHashMap<>();
    private final Map<String, DataValidator<T>> namespaceValidators = new ConcurrentHashMap<>();
    private final DataValidator<T> defaultValidator;

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

    private DataManager(Class<T> dataClass) {
        this.dataClass = dataClass;
        this.annotation = dataClass.getAnnotation(DataDriven.class);
        this.codec = CodecUtils.getCodec(dataClass);
        this.defaultValidator = createValidator(annotation.validator());

        // 注解声明的 namespace 绑定
        for (DataDriven.ValidatorBinding binding : annotation.namespaceValidators()) {
            if (binding != null && binding.namespace() != null && !binding.namespace().isBlank() && binding.validator() != null) {
                namespaceValidatorClasses.put(binding.namespace(), binding.validator());
            }
        }
    }

    /**
     * 注册数据驱动类型。
     *
     * @param dataClass 数据类型
     * @param <T>       数据类型泛型
     * @return 数据管理器实例
     */
    @SuppressWarnings("unchecked")
    public static <T> DataManager<T> register(Class<T> dataClass) {
        if (!dataClass.isAnnotationPresent(DataDriven.class)) {
            throw new IllegalArgumentException("Class " + dataClass.getSimpleName() + " must be annotated with @DataDriven");
        }

        return (DataManager<T>) managers.computeIfAbsent(dataClass, DataManager::new);
    }

    /**
     * 获取数据管理器实例。
     *
     * @param dataClass 数据类型
     * @param <T>       数据类型泛型
     * @return 数据管理器实例，如果未注册则返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> DataManager<T> get(Class<T> dataClass) {
        return (DataManager<T>) managers.get(dataClass);
    }

    /**
     * 运行时注册命名空间（允许附属 mod 在父 mod 数据结构下增加自己的 namespace）
     */
    public static <T> void registerNamespace(Class<T> dataClass, String namespace) {
        runtimeRegisteredNamespaces
                .computeIfAbsent(dataClass, k -> ConcurrentHashMap.newKeySet())
                .add(namespace);
    }

    /**
     * 运行时注册命名空间验证器（解耦合子模组引用）。
     */
    public static <T> void registerNamespaceValidator(Class<T> dataClass, String namespace, Class<? extends DataValidator<?>> validatorClass) {
        DataManager<T> mgr = get(dataClass);
        if (mgr != null && namespace != null && !namespace.isBlank() && validatorClass != null) {
            mgr.namespaceValidatorClasses.put(namespace, validatorClass);
        }
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(OELib.MODID, "data_manager_" + dataClass.getSimpleName().toLowerCase());
    }

    @Override
    public CompletableFuture<Map<ResourceLocation, JsonElement>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, JsonElement> data = new HashMap<>();
            String folder = getFolder(dataClass);
            String expectedNamespace = getModId(dataClass);

            manager.listResources(folder, path -> path.getPath().endsWith(".json")).forEach((rl, resource) -> {
                if (!expectedNamespace.isEmpty() && !rl.getNamespace().equals(expectedNamespace)) {
                    return;
                }

                try {
                    JsonElement json = GSON.fromJson(resource.openAsReader(), JsonElement.class);
                    data.put(rl, json);
                } catch (Exception e) {
                    OELib.LOGGER.error("Failed to load JSON from {}", rl, e);
                }
            });

            return data;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            loadedData.clear();
            deferredData.clear();
            clearCache();

            Codec<T> currentCodec = CodecUtils.getCodec(dataClass);

            Set<String> allowNamespaces = new HashSet<>();
            if (!annotation.modid().isEmpty()) allowNamespaces.add(annotation.modid());
            Set<String> runtimeSet = runtimeRegisteredNamespaces.getOrDefault(dataClass, Collections.emptySet());
            allowNamespaces.addAll(runtimeSet);

            Map<ResourceLocation, JsonElement> filteredObject = data.entrySet().stream()
                    .filter(entry -> allowNamespaces.isEmpty() || allowNamespaces.contains(entry.getKey().getNamespace()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            OELib.LOGGER.info("Loading {} data from {} files (namespaces: {})",
                    dataClass.getSimpleName(), filteredObject.size(), allowNamespaces);

            int validCount = 0;
            int deferredCount = 0;
            int invalidCount = 0;

            for (Map.Entry<ResourceLocation, JsonElement> entry : filteredObject.entrySet()) {
                ResourceLocation location = entry.getKey();
                JsonElement json = entry.getValue();

                try {
                    if (annotation.supportArray() && json.isJsonArray()) {
                        // 处理数组格式
                        var jsonArray = json.getAsJsonArray();
                        OELib.LOGGER.debug("Processing array with {} elements from {}", jsonArray.size(), location);

                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonElement element = jsonArray.get(i);
                            ResourceLocation elementLocation = ResourceLocation.fromNamespaceAndPath(
                                    location.getNamespace(),
                                    location.getPath() + "_" + i
                            );

                            var result = currentCodec.parse(JsonOps.INSTANCE, element);
                            if (result.result().isPresent()) {
                                T dataObj = result.result().get();

                                // 验证数据
                                var vr = validateData(dataObj, elementLocation);
                                if (vr.valid()) {
                                    if (vr.deferrable()) {
                                        // 延迟验证的数据
                                        deferredData.put(elementLocation, dataObj);
                                        deferredCount++;
                                        OELib.LOGGER.debug("Deferred {} from array[{}]: {} ({})",
                                                dataClass.getSimpleName(), i, elementLocation, vr.message());
                                    } else {
                                        // 正常验证通过的数据
                                        loadedData.put(elementLocation, dataObj);

                                        // 构建缓存
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
                        // 处理单个对象格式
                        var result = currentCodec.parse(JsonOps.INSTANCE, json);
                        if (result.result().isPresent()) {
                            T dataObj = result.result().get();

                            // 验证数据
                            var vr = validateData(dataObj, location);
                            if (vr.valid()) {
                                if (vr.deferrable()) {
                                    // 延迟验证的数据
                                    deferredData.put(location, dataObj);
                                    deferredCount++;
                                    OELib.LOGGER.debug("Deferred {}: {} ({})",
                                            dataClass.getSimpleName(), location, vr.message());
                                } else {
                                    // 正常验证通过的数据
                                    loadedData.put(location, dataObj);

                                    // 构建缓存
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
        }, executor);
    }

    /**
     * 验证数据，如果验证器支持服务器上下文则使用上下文验证。
     */
    private DataValidator.ValidationResult validateData(T data, ResourceLocation source) {
        var validator = getValidatorForNamespace(source.getNamespace());
        if (validator instanceof DataValidator.ServerContextAware<T> contextAwareValidator) {
            return contextAwareValidator.validateWithContext(data, source, getCurrentServer());
        } else {
            return validator.validate(data, source);
        }
    }

    /**
     * 获取所有已加载的数据。
     *
     * @return 数据映射的副本
     */
    public Map<ResourceLocation, T> getAllData() {
        return new HashMap<>(loadedData);
    }

    /**
     * 根据资源位置获取数据。
     *
     * @param location 资源位置
     * @return 数据，如果不存在则返回 null
     */
    public T getData(ResourceLocation location) {
        return loadedData.get(location);
    }

    /**
     * 获取所有数据的列表。
     *
     * @return 数据列表
     */
    public List<T> getDataList() {
        List<T> result = new ArrayList<>(loadedData.values());
        result.addAll(deferredData.values());
        return result;
    }

    /**
     * 根据缓存键获取数据。
     * <p>
     * 仅在启用缓存时有效。
     * </p>
     *
     * @param cacheKey 缓存键
     * @return 数据集合
     */
    public Set<T> getCachedData(String cacheKey) {
        if (!annotation.enableCache()) {
            throw new UnsupportedOperationException("Cache is disabled for " + dataClass.getSimpleName());
        }
        return cache.getOrDefault(cacheKey, Collections.emptySet());
    }

    /**
     * 更新客户端数据。
     * <p>
     * 此方法仅在客户端调用。
     * </p>
     *
     * @param data 新数据
     */
    public void updateClientData(Map<ResourceLocation, T> data) {
        loadedData.clear();
        loadedData.putAll(data);
        clearCache();

        // 重建缓存
        if (annotation.enableCache()) {
            for (Map.Entry<ResourceLocation, T> entry : data.entrySet()) {
                buildCache(entry.getValue());
            }
        }

        OELib.LOGGER.debug("Updated client data for {}: {} entries", dataClass.getSimpleName(), data.size());

        DataReloadEvent.EVENT.invoker().onDataReload(dataClass, data.size(), 0);
    }

    /**
     * 添加数据到缓存。
     *
     * @param cacheKey 缓存键
     * @param data     数据
     */
    public void addToCache(String cacheKey, T data) {
        if (annotation.enableCache()) {
            cache.computeIfAbsent(cacheKey, k -> ConcurrentHashMap.newKeySet()).add(data);
        }
    }

    /**
     * 清空缓存。
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * 构建缓存。
     * <p>
     * 子类可以重写此方法来实现自定义的缓存逻辑。
     * </p>
     *
     * @param data 数据
     */
    protected void buildCache(T data) {
        // 默认实现，将数据添加到 "all" 缓存键
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
            if (!loadedData.isEmpty() || !deferredData.isEmpty()) {
                Map<ResourceLocation, T> allData = new HashMap<>(loadedData);
                allData.putAll(deferredData);
                DataSyncPacket<T> packet = new DataSyncPacket<>(dataClass, allData);
                packet.sendToAll();
                OELib.LOGGER.debug("Synced {} data to all players", dataClass.getSimpleName());
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to sync {} data to all players", dataClass.getSimpleName(), e);
        }
    }

    /**
     * 同步数据到指定玩家。
     *
     * @param player 玩家
     */
    public void syncToPlayer(ServerPlayer player) {
        if (player != null && annotation.syncToClient() && (!loadedData.isEmpty() || !deferredData.isEmpty())) {
            try {
                Map<ResourceLocation, T> allData = new HashMap<>(loadedData);
                allData.putAll(deferredData);
                DataSyncPacket<T> packet = new DataSyncPacket<>(dataClass, allData);
                packet.sendTo(player);
                OELib.LOGGER.debug("Synced {} data to player: {}", dataClass.getSimpleName(), player.getName().getString());
            } catch (Exception e) {
                OELib.LOGGER.error("Failed to sync {} data to player {}", dataClass.getSimpleName(), player.getName().getString(), e);
            }
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