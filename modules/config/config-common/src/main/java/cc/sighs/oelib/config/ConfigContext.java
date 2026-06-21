package cc.sighs.oelib.config;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Scoped state carrier active during configuration schema definition.
 *
 * <p>A {@code ConfigContext} is bound as a {@link ThreadLocal} while a
 * {@link ConfigSchema} or {@link ConfigRecordCodecBuilder} call tree is
 * executing. It carries the current configuration id, the current
 * record class, an accumulator for {@link ConfigValueMeta} entries, and a
 * dotted path prefix used to qualify keys for nested records.
 *
 * <p>Callers outside the schema-definition flow may query whether a context
 * is currently active via {@link #isActive()}, but mutation methods such as
 * {@link #recordMeta(ConfigValueMeta)} require an active context and throw
 * {@link IllegalStateException} otherwise.
 */
public final class ConfigContext {
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    private ConfigContext() {
    }

    /**
     * Executes the given supplier with a root-level configuration context
     * bound to the current thread.
     *
     * @param configId the id of the configuration being defined
     * @param fields   a mutable list that accumulates {@link ConfigValueMeta} entries
     * @param supplier the code block to run under this context
     * @param <T>      the type returned by the supplier
     * @return the value returned by the supplier
     */
    public static <T> T withRoot(ResourceLocation configId, List<ConfigValueMeta> fields, Supplier<T> supplier) {
        return withRoot(configId, null, fields, supplier);
    }

    /**
     * Executes the given supplier with a root-level configuration context,
     * optionally recording the root record class.
     *
     * @param configId       the id of the configuration being defined
     * @param rootRecordClass the root record class, or {@code null}
     * @param fields         a mutable list that accumulates {@link ConfigValueMeta} entries
     * @param supplier       the code block to run under this context
     * @param <T>            the type returned by the supplier
     * @return the value returned by the supplier
     */
    public static <T> T withRoot(ResourceLocation configId, @Nullable Class<?> rootRecordClass, List<ConfigValueMeta> fields, Supplier<T> supplier) {
        Objects.requireNonNull(configId);
        Objects.requireNonNull(fields);
        Objects.requireNonNull(supplier);
        var frame = new Frame(configId, rootRecordClass, fields, "", 0);
        Frame old = CURRENT.get();
        CURRENT.set(frame);
        try {
            return supplier.get();
        } finally {
            CURRENT.set(old);
        }
    }

    /**
     * Executes the given supplier with a nested record context, extending
     * the current path prefix with the given key.
     *
     * @param key         the key name for this nested record within the parent
     * @param recordClass the class of the nested record
     * @param supplier    the code block to run under the nested context
     * @param <T>         the type returned by the supplier
     * @return the value returned by the supplier
     * @throws IllegalStateException if no context is currently active
     */
    public static <T> T withRecord(String key, Class<?> recordClass, Supplier<T> supplier) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(supplier);
        Frame parent = requireCurrent("record(...) can only be used during ConfigSchema/ConfigRecordCodecBuilder definition");
        String prefix = qualify(parent.pathPrefix(), key);
        var frame = new Frame(parent.configId(), recordClass, parent.fields(), prefix, parent.depth() + 1);
        Frame old = CURRENT.get();
        CURRENT.set(frame);
        try {
            return supplier.get();
        } finally {
            CURRENT.set(old);
        }
    }

    /**
     * Returns {@code true} if a configuration definition context is active
     * on the current thread.
     *
     * @return {@code true} if a context is bound
     */
    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    /**
     * Returns the id of the configuration currently being defined,
     * or {@code null} if no context is active.
     *
     * @return the current configuration id, or {@code null}
     */
    @Nullable
    public static ResourceLocation currentConfigId() {
        return isActive() ? CURRENT.get().configId() : null;
    }

    /**
     * Returns the record class active in the current context, or {@code null}
     * if no context is active.
     *
     * @return the current record class, or {@code null}
     */
    @Nullable
    public static Class<?> currentRecordClass() {
        return isActive() ? CURRENT.get().recordClass() : null;
    }

    /**
     * Returns the dotted path prefix accumulated from nesting levels,
     * or an empty string if no context is active.
     *
     * @return the current path prefix, never {@code null}
     */
    public static String currentPathPrefix() {
        return isActive() ? CURRENT.get().pathPrefix() : "";
    }

    /**
     * Qualifies the given key with the current path prefix.
     *
     * @param key the local field key
     * @return the fully qualified key (for example {@code "parent.child"})
     */
    public static String qualifyKey(String key) {
        Objects.requireNonNull(key);
        return qualify(currentPathPrefix(), key);
    }

    /**
     * Records a {@link ConfigValueMeta} entry into the active context's field list.
     *
     * <p>This is a no-op if no context is active.
     *
     * @param meta the field metadata to record
     */
    public static void recordMeta(ConfigValueMeta meta) {
        Objects.requireNonNull(meta);
        if (isActive()) {
            CURRENT.get().fields().add(meta);
        }
    }

    private static String qualify(String prefix, String key) {
        return prefix == null || prefix.isBlank() ? key : prefix + "." + key;
    }

    private static Frame requireCurrent(String message) {
        if (!isActive()) {
            throw new IllegalStateException(message);
        }
        return CURRENT.get();
    }

    private record Frame(ResourceLocation configId, @Nullable Class<?> recordClass, List<ConfigValueMeta> fields,
                         String pathPrefix, int depth) {
    }
}
