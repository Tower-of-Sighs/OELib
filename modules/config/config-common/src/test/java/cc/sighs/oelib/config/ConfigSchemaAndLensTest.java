package cc.sighs.oelib.config;

import cc.sighs.oelib.config.codecs.ConfigMetaCodec;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConfigSchemaAndLensTest {

    @Test
    void defineCollectsNestedMetaAndDerivesDefault() {
        var definition = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "schema_meta"),
                RootConfig.class,
                meta -> meta.fileName("schema_meta").directory("unit-tests"),
                schema -> schema.group(
                        ConfigSchema.record("general", General.class, g -> g.group(
                                ConfigField.bool("enabled").defaultValue(true).forGetter(General::enabled),
                                ConfigField.intRange("retries", 0, 10).defaultValue(3).forGetter(General::retries)
                        ).apply(g, General::new), RootConfig::general),
                        ConfigField.optional("threshold", com.mojang.serialization.Codec.INT).forGetter(RootConfig::threshold),
                        ConfigField.string("name").defaultValue("demo").forGetter(RootConfig::name)
                ).apply(schema, RootConfig::new)
        );

        RootConfig defaults = definition.getDefaultValue();
        assertEquals(new General(true, 3), defaults.general());
        assertEquals(Optional.empty(), defaults.threshold());
        assertEquals("demo", defaults.name());

        Set<String> keys = definition.codec().fields().stream()
                .map(ConfigValueMeta::key)
                .collect(Collectors.toSet());
        assertEquals(Set.of("general.enabled", "general.retries", "threshold", "name"), keys);

        ConfigValueMeta retries = definition.codec().fields().stream()
                .filter(field -> field.key().equals("general.retries"))
                .findFirst()
                .orElseThrow();
        assertEquals(java.util.List.of("general", "retries"), retries.pathSegments());
        assertTrue(retries.valueCodec().isDefined());
        assertEquals(3, retries.defaultValue().get());
        assertEquals(3, retries.read(defaults));
    }

    @Test
    void lensComposeUpdatesNestedRecord() {
        var innerLens = RecordLensBuilder.lens(RootConfig.class, RootConfig::general);
        var enabledLens = RecordLensBuilder.lens(General.class, General::enabled);
        var retriesLens = RecordLensBuilder.lens(General.class, General::retries);

        RootConfig source = new RootConfig(new General(true, 1), Optional.empty(), "x");
        RootConfig toggled = innerLens.andThen(enabledLens).modify(value -> !value, source);
        RootConfig retried = innerLens.andThen(retriesLens).modify(value -> value + 2, source);

        assertFalse(toggled.general().enabled());
        assertEquals(3, retried.general().retries());
    }

    @Test
    void generatedAccessorsUpdatePrimitiveComponents() {
        var innerLens = RecordLensBuilder.lens(RootConfig.class, RootConfig::general);
        var enabledLens = RecordLensBuilder.lens(General.class, General::enabled);
        var retriesLens = RecordLensBuilder.lens(General.class, General::retries);
        var deepRetriesLens = innerLens.andThen(retriesLens);
        var deepEnabledLens = innerLens.andThen(enabledLens);

        RootConfig source = new RootConfig(new General(true, 2), Optional.empty(), "x");
        RootConfig retried = deepRetriesLens.modify(value -> value + 5, source);
        RootConfig toggled = deepEnabledLens.modify(value -> !value, source);

        assertEquals(7, retried.general().retries());
        assertFalse(toggled.general().enabled());
    }

    @Test
    void lookupOverloadBuildsAndComposesLenses() {
        var definition = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "lookup_lens"),
                RootConfig.class,
                meta -> meta.fileName("lookup_lens").directory("unit-tests"),
                schema -> schema.group(
                        ConfigSchema.record("general", General.class, g -> g.group(
                                ConfigField.bool("enabled").defaultValue(true).forGetter(General::enabled),
                                ConfigField.intRange("retries", 0, 10).defaultValue(3).forGetter(General::retries)
                        ).apply(g, General::new), RootConfig::general),
                        ConfigField.optional("threshold", Codec.INT).forGetter(RootConfig::threshold),
                        ConfigField.string("name").defaultValue("demo").forGetter(RootConfig::name)
                ).apply(schema, RootConfig::new)
        );

        var rootLens = RecordLensBuilder.lens(RootConfig.class, RootConfig::general);
        var retriesLens = RecordLensBuilder.lens(General.class, General::retries);
        var composed = rootLens.andThen(retriesLens);
        RootConfig source = new RootConfig(new General(true, 2), Optional.empty(), "x");

        RootConfig updated = composed.modify(value -> value + 4, source);
        assertEquals(6, updated.general().retries());
    }

    @Test
    void recordOverloadAcceptsPrebuiltCodec() {
        Codec<NestedCodecConfig> nestedCodec = RecordCodecBuilder.create(builder -> builder.group(
                Codec.BOOL.fieldOf("enabled").orElse(true).forGetter(NestedCodecConfig::enabled),
                Codec.INT.fieldOf("count").orElse(2).forGetter(NestedCodecConfig::count)
        ).apply(builder, NestedCodecConfig::new));

        var definition = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "schema_prebuilt_codec"),
                RootWithPrebuilt.class,
                meta -> meta.fileName("schema_prebuilt_codec").directory("unit-tests"),
                schema -> schema.group(
                        ConfigSchema.record("nested", nestedCodec, new NestedCodecConfig(true, 2), RootWithPrebuilt::nested),
                        ConfigField.string("name").defaultValue("ok").forGetter(RootWithPrebuilt::name)
                ).apply(schema, RootWithPrebuilt::new)
        );

        RootWithPrebuilt defaults = definition.getDefaultValue();
        assertEquals(new NestedCodecConfig(true, 2), defaults.nested());
        assertEquals("ok", defaults.name());
    }

    @Test
    void metaCodecRecordOverloadCollectsNestedMetaAndTranslationKeys() {
        var definition = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "schema_meta_codec"),
                RootWithMetaCodec.class,
                meta -> meta.fileName("schema_meta_codec").directory("unit-tests"),
                schema -> schema.group(
                        ConfigSchema.record("nested", NestedMeta.class, NestedMeta.META_CODEC, RootWithMetaCodec::nested),
                        ConfigField.bool("enabled").defaultValue(true).forGetter(RootWithMetaCodec::enabled)
                ).apply(schema, RootWithMetaCodec::new)
        );

        Set<String> keys = definition.codec().fields().stream()
                .map(ConfigValueMeta::key)
                .collect(Collectors.toSet());
        assertEquals(Set.of("nested.value", "nested.depth", "enabled"), keys);

        var byKey = definition.codec().fields().stream()
                .collect(Collectors.toMap(ConfigValueMeta::key, meta -> meta));
        assertEquals(
                "config.oelibtest.schema_meta_codec.nested.value",
                byKey.get("nested.value").translationKey().get()
        );
        assertEquals(
                "config.oelibtest.schema_meta_codec.nested.depth",
                byKey.get("nested.depth").translationKey().get()
        );
    }

    @Test
    void optionalFieldRejectsPresentDefault() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConfigField.optional("threshold", Codec.INT).defaultValue(Optional.of(3))
        );
    }

    private record RootConfig(General general, Optional<Integer> threshold, String name) {
    }

    private record General(boolean enabled, int retries) {
    }

    private record RootWithPrebuilt(NestedCodecConfig nested, String name) {
    }

    private record NestedCodecConfig(boolean enabled, int count) {
    }

    private record RootWithMetaCodec(NestedMeta nested, boolean enabled) {
    }

    private record NestedMeta(String value, int depth) {
        private static final ConfigMetaCodec<NestedMeta> META_CODEC = ConfigSchema.metaCodec(
                NestedMeta.class,
                nested -> nested.group(
                        ConfigField.string("value").defaultValue("v").forGetter(NestedMeta::value),
                        ConfigField.intRange("depth", 0, 5).defaultValue(1).forGetter(NestedMeta::depth)
                ).apply(nested, NestedMeta::new)
        );
    }
}
