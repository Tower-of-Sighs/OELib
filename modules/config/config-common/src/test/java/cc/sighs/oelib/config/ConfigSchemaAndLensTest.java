package cc.sighs.oelib.config;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConfigSchemaAndLensTest {

    @Test
    void defineCollectsNestedMetaAndDerivesDefault() {
        var definition = ConfigSchema.defineClient(
                ResourceLocation.fromNamespaceAndPath("oelibtest", "schema_meta"),
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

        RootConfig defaults = definition.unit().getDefaultValue();
        assertEquals(new General(true, 3), defaults.general());
        assertEquals(Optional.empty(), defaults.threshold());
        assertEquals("demo", defaults.name());

        Set<String> keys = definition.unit().codec().fields().stream()
                .map(ConfigValueMeta::key)
                .collect(Collectors.toSet());
        assertEquals(Set.of("general.enabled", "general.retries", "threshold", "name"), keys);
    }

    @Test
    void lensComposeUpdatesNestedRecord() {
        var innerLens = RecordLensBuilder.lens(RootConfig.class, RootConfig::general);
        var enabledLens = RecordLensBuilder.lens(General.class, General::enabled);
        var retriesLens = RecordLensBuilder.lens(General.class, General::retries);

        RootConfig source = new RootConfig(new General(true, 1), Optional.empty(), "x");
        RootConfig toggled = innerLens.compose(enabledLens).update(source, value -> !value);
        RootConfig retried = innerLens.compose(retriesLens).update(source, value -> value + 2);

        assertFalse(toggled.general().enabled());
        assertEquals(3, retried.general().retries());
    }

    @Test
    void primitiveUpdateApisWorkOnTypedLenses() {
        var innerLens = RecordLensBuilder.lens(RootConfig.class, RootConfig::general);
        var enabledLens = RecordLensBuilder.lens(General.class, General::enabled);
        var retriesLens = RecordLensBuilder.lens(General.class, General::retries);
        var deepRetriesLens = innerLens.compose(retriesLens);
        var deepEnabledLens = innerLens.compose(enabledLens);
        var intLens = deepRetriesLens.asInt();
        var boolLens = deepEnabledLens.asBoolean();

        RootConfig source = new RootConfig(new General(true, 2), Optional.empty(), "x");
        RootConfig retried = intLens.update(source, value -> value + 5);
        RootConfig toggled = boolLens.update(source, value -> !value);

        assertEquals(7, retried.general().retries());
        assertFalse(toggled.general().enabled());
    }

    @Test
    void lookupOverloadBuildsAndComposesLenses() {
        var definition = ConfigSchema.defineClient(
                ResourceLocation.fromNamespaceAndPath("oelibtest", "lookup_lens"),
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

        var rootLens = definition.lens(MethodHandles.lookup(), RootConfig::general);
        var retriesLens = RecordLensBuilder.lens(MethodHandles.lookup(), General.class, General::retries);
        var composed = rootLens.compose(retriesLens);
        RootConfig source = new RootConfig(new General(true, 2), Optional.empty(), "x");

        RootConfig updated = composed.update(source, value -> value + 4);
        assertEquals(6, updated.general().retries());
    }

    @Test
    void recordOverloadAcceptsPrebuiltCodec() {
        Codec<NestedCodecConfig> nestedCodec = RecordCodecBuilder.create(builder -> builder.group(
                Codec.BOOL.fieldOf("enabled").orElse(true).forGetter(NestedCodecConfig::enabled),
                Codec.INT.fieldOf("count").orElse(2).forGetter(NestedCodecConfig::count)
        ).apply(builder, NestedCodecConfig::new));

        var definition = ConfigSchema.defineClient(
                ResourceLocation.fromNamespaceAndPath("oelibtest", "schema_prebuilt_codec"),
                RootWithPrebuilt.class,
                meta -> meta.fileName("schema_prebuilt_codec").directory("unit-tests"),
                schema -> schema.group(
                        ConfigSchema.record("nested", nestedCodec, new NestedCodecConfig(true, 2), RootWithPrebuilt::nested),
                        ConfigField.string("name").defaultValue("ok").forGetter(RootWithPrebuilt::name)
                ).apply(schema, RootWithPrebuilt::new)
        );

        RootWithPrebuilt defaults = definition.unit().getDefaultValue();
        assertEquals(new NestedCodecConfig(true, 2), defaults.nested());
        assertEquals("ok", defaults.name());
    }

    @Test
    void metaCodecRecordOverloadCollectsNestedMetaAndTranslationKeys() {
        var definition = ConfigSchema.defineClient(
                ResourceLocation.fromNamespaceAndPath("oelibtest", "schema_meta_codec"),
                RootWithMetaCodec.class,
                meta -> meta.fileName("schema_meta_codec").directory("unit-tests"),
                schema -> schema.group(
                        ConfigSchema.record("nested", NestedMeta.class, NestedMeta.META_CODEC, RootWithMetaCodec::nested),
                        ConfigField.bool("enabled").defaultValue(true).forGetter(RootWithMetaCodec::enabled)
                ).apply(schema, RootWithMetaCodec::new)
        );

        Set<String> keys = definition.unit().codec().fields().stream()
                .map(ConfigValueMeta::key)
                .collect(Collectors.toSet());
        assertEquals(Set.of("nested.value", "nested.depth", "enabled"), keys);

        var byKey = definition.unit().codec().fields().stream()
                .collect(Collectors.toMap(ConfigValueMeta::key, meta -> meta));
        assertEquals(
                "config.oelibtest.schema_meta_codec.nested.value",
                byKey.get("nested.value").translationKey().orElseThrow()
        );
        assertEquals(
                "config.oelibtest.schema_meta_codec.nested.depth",
                byKey.get("nested.depth").translationKey().orElseThrow()
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
