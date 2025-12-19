package com.sighs.oelib.icon;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry for dynamic mod icons, allowing the registration of multiple weighted icon variants
 * for different modding platforms.
 * * <p>This utility enables modders to define platform-specific icons (Fabric vs. NeoForge)
 * and supports weighted random selection for dynamic icon rotation.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DynamicIconRegistry.forMod(MODID)
 * .addNameFabric("icon1.png", 1)
 * .addNameFabric("icon2.png", 1)
 * .addNameNeoForge("icon3.png", 1)
 * .addNameNeoForge("icon4.png", 1)
 * .register();
 * }</pre>
 *
 * <h3>Path Resolution Rules</h3>
 * <ul>
 * <li><b>Fabric:</b> {@link Builder#addNameFabric(String, int)} resolves to
 * {@code assets/<modid>/<filename>}, mapping to {@code src/main/resources/assets/<modid>/<filename>}.</li>
 * <li><b>NeoForge:</b> {@link Builder#addNameNeoForge(String, int)} uses the filename directly,
 * mapping to the resource root: {@code src/main/resources/<filename>}.</li>
 * <li><b>Custom Paths:</b> For icons outside standard locations, use
 * {@link Builder#addPathFabric(String, int)} or {@link Builder#addPathNeoForge(String, int)}
 * to specify a relative path starting from the {@code resources/} directory.</li>
 * </ul>
 *
 * <h4>Note</h4>
 * For Fabric environments, <b>ModMenu</b> must be installed for dynamic icons to be displayed.
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
        return variants.getLast();
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