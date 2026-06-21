package cc.sighs.oelib.config;

import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Legacy entry point for defining record-based configurations.
 *
 * <p>This class delegates directly to {@link ConfigSchema#defineLegacy}. New code should use {@link ConfigSchema}
 * instead, which accepts an explicit root record class and returns a
 * {@link ConfigSchema.Definition} that provides lens helpers.
 *
 * @deprecated Use {@link ConfigSchema#defineClient(MethodHandles.Lookup, ResourceLocation, Class, Consumer, Function)} or
 *             {@link ConfigSchema#defineServer(MethodHandles.Lookup, ResourceLocation, Class, Consumer, Function)} instead.
 */
@Deprecated(since = "0.2.4", forRemoval = true)
public class ConfigRecordCodecBuilder {
    private ConfigRecordCodecBuilder() {
    }

    /**
     * Creates a client-side {@link ConfigUnit} from a {@link RecordCodecBuilder} function.
     *
     * @param configId       the configuration id in the form {@code "namespace:path"}
     * @param builder        the record codec builder function
     * @param metaCustomizer optional customizer for {@link ConfigMeta} (filename, format, directory),
     *                       or {@code null} to accept defaults
     * @param <T>            the type of the configuration record
     * @return the constructed configuration unit
     * @deprecated Use {@link ConfigSchema#defineClient(MethodHandles.Lookup, ResourceLocation, Class, Consumer, Function)} instead.
     */
    @Deprecated(since = "0.2.4", forRemoval = true)
    public static <T> ConfigUnit<T> createClient(
            ResourceLocation configId,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            Consumer<ConfigMeta.Builder> metaCustomizer
    ) {
        return ConfigSchema.defineLegacy(configId, builder, metaCustomizer, ConfigSide.CLIENT);
    }

    /**
     * Creates a server-side {@link ConfigUnit} from a {@link RecordCodecBuilder} function.
     *
     * @param configId       the configuration id in the form {@code "namespace:path"}
     * @param builder        the record codec builder function
     * @param metaCustomizer optional customizer for {@link ConfigMeta} (filename, format, directory),
     *                       or {@code null} to accept defaults
     * @param <T>            the type of the configuration record
     * @return the constructed configuration unit
     * @deprecated Use {@link ConfigSchema#defineServer(MethodHandles.Lookup, ResourceLocation, Class, Consumer, Function)} instead.
     */
    @Deprecated(since = "0.2.4", forRemoval = true)
    public static <T> ConfigUnit<T> create(
            ResourceLocation configId,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            Consumer<ConfigMeta.Builder> metaCustomizer
    ) {
        return ConfigSchema.defineLegacy(configId, builder, metaCustomizer, ConfigSide.SERVER);
    }
}
