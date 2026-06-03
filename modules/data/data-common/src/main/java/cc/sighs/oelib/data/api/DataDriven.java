package cc.sighs.oelib.data.api;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking data-driven types.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @DataDriven(
 *     modid = "mymod",
 *     folder = "replacements",
 *     syncToClient = true,
 *     validator = ReplacementValidator.class,
 *     supportArray = true
 * )
 * public record Replacement(List<String> matchItems, String resultItem) {
 *     public static final Codec<Replacement> CODEC = RecordCodecBuilder.create(instance ->
 *         instance.group(
 *             Codec.STRING.listOf().fieldOf("matchItems").forGetter(Replacement::matchItems),
 *             Codec.STRING.fieldOf("resultItem").forGetter(Replacement::resultItem)
 *         ).apply(instance, Replacement::new)
 *     );
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataDriven {

    /**
     * The mod ID.
     * <p>
     * Specifies the mod ID to which the data pack belongs.
     * Data files will be loaded from {@code data/<modid>/<folder>/}.
     * If left as an empty string, the default mod ID provided at registration time will be used.
     * </p>
     *
     * @return the mod ID, defaulting to an empty string
     */
    String modid() default "";

    /**
     * The data pack folder name.
     * <p>
     * Data files will be loaded from {@code data/<modid>/<folder>/}.
     * </p>
     *
     * @return the folder name
     */
    String folder();

    /**
     * Whether to synchronize data to clients.
     * <p>
     * If true, data will be automatically synced to all clients after being loaded on the server.
     * </p>
     *
     * @return whether to sync to clients, defaulting to false
     */
    boolean syncToClient() default false;

    /**
     * The data validator class.
     * <p>
     * Specifies a class implementing {@link DataValidator} to validate data validity.
     * </p>
     *
     * @return the validator class, defaulting to no validator
     */
    Class<? extends DataValidator<?>> validator() default DataValidator.NoValidator.class;

    /**
     * Whether caching is enabled.
     * <p>
     * Enabling caching improves data lookup performance at the cost of increased memory usage.
     * </p>
     *
     * @return whether caching is enabled, defaulting to true
     */
    boolean enableCache() default true;

    /**
     * Data processing priority.
     * <p>
     * Lower values indicate higher priority. Controls the loading order of multiple data types.
     * </p>
     *
     * @return the priority, defaulting to 1000
     */
    int priority() default 1000;

    /**
     * Whether array format is supported.
     * <p>
     * If true, JSON files may contain an array of objects, each treated as a separate data entry.
     * If false, JSON files must contain a single object.
     * </p>
     *
     * @return whether array format is supported, defaulting to false
     */
    boolean supportArray() default false;

    /**
     * Binds specific validators to different namespaces.
     * Useful when the same data type is shared across multiple namespaces but requires different validation logic per namespace.
     */
    @ApiStatus.Internal
    ValidatorBinding[] namespaceValidators() default {};

    /**
     * Namespace-validator binding annotation.
     */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @ApiStatus.Internal
    @interface ValidatorBinding {
        String namespace();
        Class<? extends DataValidator<?>> validator();
    }
}