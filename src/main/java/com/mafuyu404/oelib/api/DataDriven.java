package com.mafuyu404.oelib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记数据驱动类型的注解。
 * <p>
 * 使用此注解标记的记录类将被自动注册到数据驱动系统中，
 * 支持从数据包加载、网络同步、热重载等功能。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @DataDriven(
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
 *
 * @author Flechazo
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataDriven {

    /**
     * 数据包文件夹名称。
     * <p>
     * 数据文件将从 {@code data/<namespace>/<folder>/} 目录加载。
     * </p>
     *
     * @return 文件夹名称
     */
    String folder();

    /**
     * 是否同步到客户端。
     * <p>
     * 如果为 true，数据将在服务器加载后自动同步到所有客户端。
     * </p>
     *
     * @return 是否同步到客户端，默认为 false
     */
    boolean syncToClient() default false;

    /**
     * 数据验证器类。
     * <p>
     * 指定一个实现了 {@link DataValidator} 接口的类来验证数据的有效性。
     * </p>
     *
     * @return 验证器类，默认为无验证器
     */
    Class<? extends DataValidator<?>> validator() default DataValidator.NoValidator.class;

    /**
     * 是否启用缓存。
     * <p>
     * 启用缓存可以提高数据查询性能，但会占用更多内存。
     * </p>
     *
     * @return 是否启用缓存，默认为 true
     */
    boolean enableCache() default true;

    /**
     * 数据处理优先级。
     * <p>
     * 数值越小优先级越高，用于控制多个数据类型的加载顺序。
     * </p>
     *
     * @return 优先级，默认为 1000
     */
    int priority() default 1000;

    /**
     * 是否支持数组格式。
     * <p>
     * 如果为 true，JSON 文件可以包含对象数组，每个数组元素将作为单独的数据条目处理。
     * 如果为 false，JSON 文件必须包含单个对象。
     * </p>
     *
     * @return 是否支持数组格式，默认为 false
     */
    boolean supportArray() default false;
}