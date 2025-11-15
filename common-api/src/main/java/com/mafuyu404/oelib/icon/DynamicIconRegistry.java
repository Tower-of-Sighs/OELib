package com.mafuyu404.oelib.icon;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态图标注册表，用于为模组在不同平台上注册多个可随机选择的图标变体。
 * <p>
 * 该系统允许模组作者为 Fabric 和 NeoForge 分别指定不同的图标资源，并支持基于权重的随机选择。
 * 图标路径可以自动推导（推荐用于标准资源位置），也可以手动指定绝对路径。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * DynamicIconRegistry.forMod(MODID)
 *         .addNameFabric("icon1.png", 1)
 *         .addNameFabric("icon2.png", 1)
 *         .addNameNeoForge("icon3.png", 1)
 *         .addNameNeoForge("icon4.png", 1)
 *         .register();
 * }</pre>
 *
 * <h3>路径解析规则</h3>
 * <ul>
 *   <li>{@link Builder#addNameFabric(String, int)}：自动拼接为 {@code assets/<modid>/<filename>}，
 *       对应资源包中的 {@code resources/assets/<modid>/<filename>}。</li>
 *   <li>{@link Builder#addNameNeoForge(String, int)}：直接使用文件名作为路径，
 *       对应资源包根目录下的 {@code resources/<filename>}。</li>
 *   <li>若图标不在上述默认位置，请使用 {@link Builder#addPathFabric(String, int)} 或
 *       {@link Builder#addPathNeoForge(String, int)} 指定完整相对路径（相对于 {@code resources/}）。</li>
 * </ul>
 *
 * <h4>注意</h4>
 *  Fabric 需安装 ModMenu 方可有效。
 */
public final class DynamicIconRegistry {
    private static final Map<String, List<IconVariant>> VARIANTS = new ConcurrentHashMap<>();
    private static final Map<String, String> SELECTED_FABRIC_PATH = new ConcurrentHashMap<>();
    private static final Map<String, String> SELECTED_NeoForge_PATH = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private DynamicIconRegistry() {
    }

    public static Builder forMod(String modId) {
        return new Builder(modId);
    }

    public static boolean hasConfig(String modId) {
        return VARIANTS.containsKey(modId);
    }

    public static void register(String modId, List<IconVariant> variants) {
        if (variants == null || variants.isEmpty()) return;
        VARIANTS.put(modId, List.copyOf(variants));
    }

    public static Optional<String> getSelectedFabricPath(String modId) {
        String cached = SELECTED_FABRIC_PATH.get(modId);
        if (cached != null) return Optional.of(cached);

        List<IconVariant> variants = VARIANTS.get(modId);
        if (variants == null || variants.isEmpty()) return Optional.empty();

        List<IconVariant> effective = filterForFabric(variants);
        if (effective.isEmpty()) return Optional.empty();

        IconVariant picked = pickWeighted(effective);
        String path = resolveFabricPath(modId, picked);
        SELECTED_FABRIC_PATH.put(modId, path);
        return Optional.of(path);
    }

    public static Optional<String> getSelectedNeoForgePath(String modId) {
        String cached = SELECTED_NeoForge_PATH.get(modId);
        if (cached != null) return Optional.of(cached);

        List<IconVariant> variants = VARIANTS.get(modId);
        if (variants == null || variants.isEmpty()) return Optional.empty();

        List<IconVariant> effective = filterForNeoForge(variants);
        if (effective.isEmpty()) return Optional.empty();

        IconVariant picked = pickWeighted(effective);
        String path = resolveNeoForgePath(picked);
        SELECTED_NeoForge_PATH.put(modId, path);
        return Optional.of(path);
    }

    private static IconVariant pickWeighted(List<IconVariant> variants) {
        int total = 0;
        for (IconVariant v : variants) {
            total += Math.max(1, v.getWeight());
        }
        int r = RANDOM.nextInt(total);
        int acc = 0;
        for (IconVariant v : variants) {
            acc += Math.max(1, v.getWeight());
            if (r < acc) {
                return v;
            }
        }
        return variants.get(variants.size() - 1);
    }

    private static List<IconVariant> filterForFabric(List<IconVariant> variants) {
        List<IconVariant> out = new ArrayList<>();
        for (IconVariant v : variants) {
            if (v.appliesToFabric()) out.add(v);
        }
        return out;
    }

    private static List<IconVariant> filterForNeoForge(List<IconVariant> variants) {
        List<IconVariant> out = new ArrayList<>();
        for (IconVariant v : variants) {
            if (v.appliesToNeoForge()) out.add(v);
        }
        return out;
    }

    private static String resolveFabricPath(String modId, IconVariant variant) {
        if (variant.isPath()) return variant.getPath();
        return "assets/" + modId + "/" + variant.getName();
    }

    private static String resolveNeoForgePath(IconVariant variant) {
        if (variant.isPath()) return variant.getPath();
        return variant.getName();
    }

    public static final class Builder {
        private final String modId;
        private final List<IconVariant> localVariants = new ArrayList<>();

        private Builder(String modId) {
            this.modId = modId;
        }

        public Builder addName(String fileName, int weight) {
            localVariants.add(IconVariant.name(fileName, weight));
            return this;
        }

        public Builder addPath(String path, int weight) {
            localVariants.add(IconVariant.path(path, weight));
            return this;
        }

        public Builder addNameFabric(String fileName, int weight) {
            localVariants.add(IconVariant.nameFabric(fileName, weight));
            return this;
        }

        public Builder addPathFabric(String path, int weight) {
            localVariants.add(IconVariant.pathFabric(path, weight));
            return this;
        }

        public Builder addNameNeoForge(String fileName, int weight) {
            localVariants.add(IconVariant.nameNeoForge(fileName, weight));
            return this;
        }

        public Builder addPathNeoForge(String path, int weight) {
            localVariants.add(IconVariant.pathNeoForge(path, weight));
            return this;
        }

        public void register() {
            DynamicIconRegistry.register(modId, localVariants);
        }
    }
}