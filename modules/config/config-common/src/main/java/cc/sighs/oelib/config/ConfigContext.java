package cc.sighs.oelib.config;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.flechazo.optics.LensGetter;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides the active state of a configuration schema definition.
 *
 * <p>Schema builders use this context to qualify nested field paths, collect field descriptors,
 * and associate record accessors with the complete configuration value. Context queries return
 * their documented empty value when no definition is active.
 */
@ApiStatus.Internal
public final class ConfigContext {
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    private ConfigContext() {
    }

    /**
     * Evaluates an action as the root of a configuration schema definition.
 *
     * <p>Field descriptors registered while the action is running are appended to {@code fields}.
     * Any previously active definition is restored before this method returns or propagates an
     * exception.
     *
     * @param configId the identifier of the configuration being defined
     * @param rootRecordClass the root record type
     * @param fields the list that receives field descriptors in declaration order
     * @param supplier the action evaluated as the root definition
     * @param <T> the action result type
     * @return the value returned by {@code supplier}
     */
    public static <T> T withRoot(
            ResourceLocation configId,
            Class<?> rootRecordClass,
            List<ConfigValueMeta> fields,
            Supplier<T> supplier) {
        Objects.requireNonNull(configId);
        Objects.requireNonNull(rootRecordClass);
        Objects.requireNonNull(fields);
        Objects.requireNonNull(supplier);
        var frame = new Frame(configId, rootRecordClass, fields, "", 0, Function.identity());
        Frame old = CURRENT.get();
        CURRENT.set(frame);
        try {
            return supplier.get();
        } finally {
            CURRENT.set(old);
        }
    }

    /**
     * Evaluates an action as a nested record in the active schema definition.
     *
     * <p>Field paths registered by the action are prefixed with {@code key}. Field readers are
     * composed with {@code getter} so they accept the complete configuration value. The enclosing
     * definition is restored before this method returns or propagates an exception.
     *
     * @param key the serialized name of the nested record
     * @param recordClass the nested record type
     * @param getter the parent-record accessor for the nested record
     * @param supplier the action evaluated within the nested record definition
     * @param <P> the parent record type
     * @param <C> the nested record type
     * @param <T> the action result type
     * @return the value returned by {@code supplier}
     * @throws IllegalStateException if no schema definition is active
     */
    public static <P, C, T> T withRecord(
            String key, Class<C> recordClass, LensGetter<P, C> getter, Supplier<T> supplier) {
        Objects.requireNonNull(getter, "getter");
        Frame parent = requireCurrent("record(...) can only be used during ConfigSchema definition");
        Function<Object, Object> childAccessor = compileAccessor(getter);
        String prefix = qualify(parent.pathPrefix(), key);
        var frame = new Frame(parent.configId(), recordClass, parent.fields(), prefix,
                parent.depth() + 1, childAccessor);
        Frame old = CURRENT.get();
        CURRENT.set(frame);
        try {
            return supplier.get();
        } finally {
            CURRENT.set(old);
        }
    }

    /**
     * Creates a reader from the complete configuration value to a record component.
     *
     * @param getter the component accessor for the record active in the current definition
     * @param <P> the record type containing the component
     * @param <A> the component type
     * @return a reader that accepts the complete configuration value
     * @throws IllegalArgumentException if {@code getter} does not identify a record component
     * @throws IllegalStateException if no schema definition or record type is active
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <P, A> Function<Object, Object> compileAccessor(LensGetter<P, A> getter) {
        Frame frame = requireCurrent("Field getters can only be compiled during schema definition");
        if (frame.recordClass() == null) {
            throw new IllegalStateException("The active schema context has no record class");
        }
        var lens = RecordLensBuilder.lens((Class) frame.recordClass(), (LensGetter) getter);
        return root -> lens.get(frame.rootAccessor().apply(root));
    }

    /**
     * Adapts a nested-record reader to accept the complete configuration value.
     *
     * @param localAccessor the reader whose input is the record active in the current definition
     * @return a reader whose input is the complete configuration value
     * @throws IllegalStateException if no schema definition is active
     */
    public static Function<Object, Object> rebaseAccessor(Function<Object, Object> localAccessor) {
        Objects.requireNonNull(localAccessor, "localAccessor");
        Frame frame = requireCurrent("Accessors can only be re-based during schema definition");
        return root -> localAccessor.apply(frame.rootAccessor().apply(root));
    }

    /**
     * Determines whether a configuration schema definition is active.
     *
     * @return {@code true} when a definition is active; otherwise {@code false}
     */
    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    /**
     * Returns the identifier of the active configuration definition.
     *
     * @return the configuration identifier, or {@code null} when no definition is active
     */
    @Nullable
    public static ResourceLocation currentConfigId() {
        return isActive() ? CURRENT.get().configId() : null;
    }

    /**
     * Returns the record type active in the current configuration definition.
     *
     * @return the active record type, or {@code null} when no definition is active
     */
    @Nullable
    public static Class<?> currentRecordClass() {
        return isActive() ? CURRENT.get().recordClass() : null;
    }

    /**
     * Returns the path prefix for fields in the active record definition.
     *
     * @return the dotted path prefix, or an empty string at the root level or when no definition
     *         is active
     */
    public static String currentPathPrefix() {
        return isActive() ? CURRENT.get().pathPrefix() : "";
    }

    /**
     * Qualifies a local field name with the active record path.
     *
     * @param key the local serialized field name
     * @return the fully qualified dotted path, or {@code key} when no nested record is active
     */
    public static String qualifyKey(String key) {
        Objects.requireNonNull(key);
        return qualify(currentPathPrefix(), key);
    }

    /**
     * Appends a field descriptor to the active schema definition.
     *
     * <p>This method has no effect when no definition is active.
     *
     * @param meta the field descriptor to append
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

    private record Frame(ResourceLocation configId, Class<?> recordClass, List<ConfigValueMeta> fields,
                         String pathPrefix, int depth, Function<Object, Object> rootAccessor) {
    }
}
