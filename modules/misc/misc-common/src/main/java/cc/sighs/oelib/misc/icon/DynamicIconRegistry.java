package cc.sighs.oelib.misc.icon;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.business.control.MaybePath;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.util.OptionalOps;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A registry for dynamic mod icons, allowing the registration of multiple weighted icon variants
 * for different modding platforms.
 * * <p>This utility enables modders to define platform-specific icons (Fabric vs. FORGE)
 * and supports weighted random selection for dynamic icon rotation.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DynamicIconRegistry.forMod(MODID)
 * .addNameFabric("icon1.png", 1)
 * .addNameFabric("icon2.png", 1)
 * .addNameForge("icon3.png", 1)
 * .addNameForge("icon4.png", 1)
 * .register();
 * }</pre>
 *
 * <h3>Path Resolution Rules</h3>
 * <ul>
 * <li><b>Fabric:</b> {@link Builder#addNameFabric(String, int)} resolves to
 * {@code assets/<modid>/<filename>}, mapping to {@code src/main/resources/assets/<modid>/<filename>}.</li>
 * <li><b>FORGE:</b> {@link Builder#addNameForge(String, int)} uses the filename directly,
 * mapping to the resource root: {@code src/main/resources/<filename>}.</li>
 * <li><b>Custom Paths:</b> For icons outside standard locations, use
 * {@link Builder#addPathFabric(String, int)} or {@link Builder#addPathForge(String, int)}
 * to specify a relative path starting from the {@code resources/} directory.</li>
 * </ul>
 *
 * <h4>Note</h4>
 * For Fabric environments, <b>ModMenu</b> must be installed for dynamic icons to be displayed.
 */
public final class DynamicIconRegistry {
    private static final Map<String, List<IconVariant>> VARIANTS = new ConcurrentHashMap<>();
    private static final Map<String, String> SELECTED_FABRIC_PATH = new ConcurrentHashMap<>();
    private static final Map<String, String> SELECTED_FORGE_PATH = new ConcurrentHashMap<>();
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
        return OptionalOps.fromMaybe(findSelectedFabricPath(modId));
    }

    public static Optional<String> getSelectedForgePath(String modId) {
        return OptionalOps.fromMaybe(findSelectedForgePath(modId));
    }

    public static Maybe<String> findSelectedFabricPath(String modId) {
        return selectPath(
                modId,
                SELECTED_FABRIC_PATH,
                DynamicIconRegistry::filterForFabric,
                variant -> resolveFabricPath(modId, variant)
        ).run();
    }

    public static Maybe<String> findSelectedForgePath(String modId) {
        return selectPath(
                modId,
                SELECTED_FORGE_PATH,
                DynamicIconRegistry::filterForForge,
                DynamicIconRegistry::resolveForgePath
        ).run();
    }

    private static MaybePath<String> selectPath(
            String modId,
            Map<String, String> cache,
            Function<List<IconVariant>, List<IconVariant>> platformFilter,
            Function<IconVariant, String> pathResolver
    ) {
        return Pathway.nullable(cache.get(modId)).orElse(() ->
                Pathway.nullable(VARIANTS.get(modId))
                        .filter(variants -> !variants.isEmpty())
                        .map(platformFilter)
                        .filter(variants -> !variants.isEmpty())
                        .map(DynamicIconRegistry::pickWeighted)
                        .map(pathResolver)
                        .peek(path -> cache.put(modId, path))
        );
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
        return variants.stream().filter(IconVariant::appliesToFabric).toList();
    }

    private static List<IconVariant> filterForForge(List<IconVariant> variants) {
        return variants.stream().filter(IconVariant::appliesToForge).toList();
    }

    private static String resolveFabricPath(String modId, IconVariant variant) {
        if (variant.isPath()) return variant.getPath();
        return "assets/" + modId + "/" + variant.getName();
    }

    private static String resolveForgePath(IconVariant variant) {
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

        public Builder addNameForge(String fileName, int weight) {
            localVariants.add(IconVariant.nameForge(fileName, weight));
            return this;
        }

        public Builder addPathForge(String path, int weight) {
            localVariants.add(IconVariant.pathForge(path, weight));
            return this;
        }

        public void register() {
            DynamicIconRegistry.register(modId, localVariants);
        }
    }
}