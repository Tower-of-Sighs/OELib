package cc.sighs.oelib.config;

import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.mojang.serialization.Codec;

import java.util.List;

public record ConfigCodec<T>(Codec<T> codec, ConfigMeta meta, List<ConfigValueMeta> fields) {
}