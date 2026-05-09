package cc.sighs.oelib.config;

import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.mojang.serialization.Codec;

import java.util.List;

/**
 * A codec paired with its metadata and per-field descriptors.
 *
 * <p>This record bundles the Mojang {@link Codec}, the configuration
 * {@link ConfigMeta}, and the list of {@link ConfigValueMeta} entries
 * collected during schema definition. It is the canonical representation
 * of a config's serialization contract.
 *
 * @param codec  the Mojang serialization codec
 * @param meta   configuration metadata (side, format, filename)
 * @param fields per-field metadata in declaration order
 * @param <T>    the type of the configuration value
 */
public record ConfigCodec<T>(Codec<T> codec, ConfigMeta meta, List<ConfigValueMeta> fields) {
}