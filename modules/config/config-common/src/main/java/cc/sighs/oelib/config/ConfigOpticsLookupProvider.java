package cc.sighs.oelib.config;

import com.flechazo.optics.spi.OpticsLookups;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Retains module access supplied by configuration definitions.
 */
@ApiStatus.Internal
public final class ConfigOpticsLookupProvider {
    private static final ConcurrentMap<Module, MethodHandles.Lookup> LOOKUPS = new ConcurrentHashMap<>();

    private ConfigOpticsLookupProvider() {
    }

    static void register(Class<?> rootClass, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(rootClass, "rootClass");
        Objects.requireNonNull(lookup, "lookup");

        Module targetModule = rootClass.getModule();
        Module lookupModule = lookup.lookupClass().getModule();
        if (lookupModule != targetModule) {
            throw new IllegalArgumentException(
                    "The ConfigSchema lookup belongs to " + moduleName(lookupModule)
                            + " but " + rootClass.getName() + " belongs to " + moduleName(targetModule)
            );
        }
        if (!lookup.hasFullPrivilegeAccess()) {
            throw new IllegalArgumentException(
                    "ConfigSchema requires a full-privilege MethodHandles.Lookup for " + rootClass.getName()
            );
        }

        OpticsLookups.register(lookup);
        LOOKUPS.putIfAbsent(targetModule, lookup);
    }

    static MethodHandles.Lookup registeredLookup(Class<?> targetType) {
        Objects.requireNonNull(targetType, "targetType");
        MethodHandles.Lookup lookup = LOOKUPS.get(targetType.getModule());
        if (lookup == null) {
            throw new IllegalStateException(
                    "No ConfigSchema lookup has been registered for " + targetType.getName()
            );
        }
        return lookup;
    }

    private static String moduleName(Module module) {
        return module.isNamed() ? "module " + module.getName() : "the unnamed module";
    }
}
