package cc.sighs.oelib.dev;

import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigSchema;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.codecs.ConfigMetaCodec;
import cc.sighs.oelib.config.codecs.ConfigSealedCodec;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record DevConfig(
        boolean enableExampleContent,
        List<String> testList,
        String testString,
        double testDouble,
        int testInt,
        TestEnum testEnum,
        DemoMode demoMode,
        NestedDemo nestedDemo,
        Optional<NestedDemo> optionalChild,
        List<FeatureConfig> featureConfigs,
        Map<String, FeatureConfig> featureConfigMap
) {
    private static final String FILE_NAME = "dev_features";

    public static final ConfigUnit<DevConfig> UNIT = ConfigSchema.defineServer(
            MethodHandles.lookup(),
            new ResourceLocation("oelib", "dev_features"),
            DevConfig.class,
            meta -> meta
                    .directory("oelib")
                    .fileName(FILE_NAME)
                    .format(ConfigStorageFormat.JSON),
            schema -> schema.group(
                    ConfigField.bool("enableExampleContent")
                            .comment("是否启用示例/测试内容")
                            .tooltip()
                            .defaultValue(false)
                            .forGetter(DevConfig::enableExampleContent),
                    ConfigField.list("testList", Codec.STRING)
                            .defaultValue(List.of("test1", "test2"))
                            .comment("测试 List 用")
                            .forGetter(DevConfig::testList),
                    ConfigField.string("testString")
                            .defaultValue("test")
                            .comment("测试 String 用")
                            .forGetter(DevConfig::testString),
                    ConfigField.doubleRange("testDouble", 1.0, 100.0)
                            .defaultValue(50.0)
                            .comment("测试 Double 用")
                            .forGetter(DevConfig::testDouble),
                    ConfigField.intRange("testInt", 1, 100)
                            .defaultValue(50)
                            .text()
                            .comment("测试 Int 用")
                            .forGetter(DevConfig::testInt),
                    ConfigField.enumValue("testEnum", TestEnum.class)
                            .defaultValue(TestEnum.TEST)
                            .comment("测试 Enum 用")
                            .forGetter(DevConfig::testEnum),
                    ConfigField.sealed("demoMode", DemoMode.CODEC).forGetter(DevConfig::demoMode),
                    ConfigSchema.record("nestedDemo", NestedDemo.class, NestedDemo.META_CODEC, DevConfig::nestedDemo),
                    ConfigField.optional("optionalChild", NestedDemo.META_CODEC)
                            .comment("可选的嵌套子配置")
                            .forGetter(DevConfig::optionalChild),
                    ConfigField.list("featureConfigs", FeatureConfig.META_CODEC)
                            .defaultValue(List.of(
                                    new FeatureConfig("feature_a", 10, Optional.of(new NestedDemo(true, 3))),
                                    new FeatureConfig("feature_b", 20, Optional.empty())
                            ))
                            .comment("功能特性配置列表")
                            .forGetter(DevConfig::featureConfigs),
                    ConfigField.map("featureConfigMap", Codec.STRING, FeatureConfig.META_CODEC)
                            .defaultValue(Map.of(
                                    "alpha", new FeatureConfig("alpha", 10, Optional.empty()),
                                    "beta", new FeatureConfig("beta", 20, Optional.of(new NestedDemo(false, 5)))
                            ))
                            .comment("功能特性配置映射")
                            .forGetter(DevConfig::featureConfigMap)
            ).apply(schema, DevConfig::new)
    );

    public sealed interface DemoMode permits LevelMode, ToggleMode {
        ConfigSealedCodec<DemoMode> CODEC = ConfigSchema.sealedCodec(
                DemoMode.class,
                "type",
                mode -> mode instanceof LevelMode ? "level" : "toggle",
                new LevelMode(2),
                ConfigSchema.sealedVariant("level", LevelMode.class, LevelMode.META_CODEC),
                ConfigSchema.sealedVariant("toggle", ToggleMode.class, ToggleMode.META_CODEC)
        );
    }

    public record LevelMode(int level) implements DemoMode {
        public static final ConfigMetaCodec<LevelMode> META_CODEC = ConfigSchema.metaCodec(
                LevelMode.class,
                levelMode -> levelMode.group(
                        ConfigField.intRange("level", 0, 10)
                                .defaultValue(1)
                                .comment("子类型示例：等级模式")
                                .forGetter(LevelMode::level)
                ).apply(levelMode, LevelMode::new)
        );
    }

    public record ToggleMode(boolean enabled) implements DemoMode {
        public static final ConfigMetaCodec<ToggleMode> META_CODEC = ConfigSchema.metaCodec(
                ToggleMode.class,
                toggleMode -> toggleMode.group(
                        ConfigField.bool("enabled")
                                .defaultValue(true)
                                .comment("子类型示例：开关模式")
                                .forGetter(ToggleMode::enabled)
                ).apply(toggleMode, ToggleMode::new)
        );
    }

    public record NestedDemo(
            boolean enabled,
            int level
    ) {
        public static final ConfigMetaCodec<NestedDemo> META_CODEC =
                ConfigSchema.metaCodec(
                        NestedDemo.class,
                        nested -> nested.group(
                                ConfigField.bool("enabled")
                                        .defaultValue(true)
                                        .comment("嵌套示例：是否启用")
                                        .forGetter(NestedDemo::enabled),
                                ConfigField.intRange("level", 1, 10)
                                        .defaultValue(3)
                                        .comment("嵌套示例：等级")
                                        .forGetter(NestedDemo::level)
                        ).apply(nested, NestedDemo::new)
                );
    }

    public record FeatureConfig(
            String id,
            int weight,
            Optional<NestedDemo> extraConfig
    ) {
        public static final ConfigMetaCodec<FeatureConfig> META_CODEC =
                ConfigSchema.metaCodec(
                        FeatureConfig.class,
                        feature -> feature.group(
                                ConfigField.string("id")
                                        .defaultValue("unnamed")
                                        .comment("功能标识")
                                        .forGetter(FeatureConfig::id),
                                ConfigField.intRange("weight", 0, 100)
                                        .defaultValue(0)
                                        .comment("功能权重")
                                        .forGetter(FeatureConfig::weight),
                                ConfigField.optional("extraConfig", NestedDemo.META_CODEC)
                                        .comment("额外嵌套配置")
                                        .forGetter(FeatureConfig::extraConfig)
                        ).apply(feature, FeatureConfig::new)
                );
    }

    public enum TestEnum {
        TEST,
        TEST2
    }
    
    public static void register() {
        ConfigManager.registerServer(UNIT, player -> player.hasPermissions(4));
    }

    // Exercises the public configuration API from a consumer module.
    public static void exampleUsages() {
        var nestedLevel = UNIT.focus(DevConfig::nestedDemo).then(NestedDemo::level);
        var optionalChildLevel = UNIT.focusOptional(DevConfig::optionalChild).then(NestedDemo::level);
        var featureWeights = UNIT.focusListElements(DevConfig::featureConfigs).then(FeatureConfig::weight);
        var heavyFeatureWeights = featureWeights.filter(weight -> weight > 10);
        var firstFeatureWeight = featureWeights.at(0);
        var mapWeights = UNIT.focusMapValues(DevConfig::featureConfigMap).then(FeatureConfig::weight);
        var betaEnabled = UNIT.focusMapValue(DevConfig::featureConfigMap, "beta")
                .thenOptional(FeatureConfig::extraConfig)
                .then(NestedDemo::enabled);

        UNIT.update(DevConfig::enableExampleContent, value -> !value);
        UNIT.ifPresent(DevConfig::optionalChild, child -> new NestedDemo(!child.enabled(), child.level() + 1));
        UNIT.whenSubtype(DevConfig::demoMode, LevelMode.class, mode -> new LevelMode(mode.level() + 1));
        UNIT.updateElements(DevConfig::testList, value -> value + "_item");
        UNIT.update(nestedLevel, value -> value + 1);
        UNIT.ifPresent(optionalChildLevel, value -> value + 1);
        UNIT.updateEach(heavyFeatureWeights, value -> value / 2);
        UNIT.ifPresent(firstFeatureWeight, value -> value * 2);
        UNIT.updateEach(mapWeights, value -> value + 1);
        UNIT.ifPresent(betaEnabled, value -> !value);

        List<Integer> weights = UNIT.getAll(featureWeights);
        Optional<Integer> firstWeight = UNIT.preview(firstFeatureWeight);

        UNIT.updateNoSave(DevConfig::testString, value -> value + "_draft");
        UNIT.ifPresentNoSave(optionalChildLevel, value -> value + 1);
        UNIT.updateEachNoSave(featureWeights, value -> value + 1);

        UNIT.applyMutation(
                UNIT.mutation()
                        .set(DevConfig::testString, "batched")
                        .map(DevConfig::testInt, value -> value + 1)
                        .ifPresent(DevConfig::optionalChild,
                                child -> new NestedDemo(child.enabled(), child.level() + 1))
                        .whenSubtype(DevConfig::demoMode, LevelMode.class,
                                mode -> new LevelMode(mode.level() + 1))
                        .updateEach(featureWeights, value -> value + 1)
                        .ifPresent(betaEnabled, value -> !value)
        );

        UNIT.applyMutationNoSave(
                UNIT.mutation()
                        .map(nestedLevel, value -> value + 1)
                        .updateWhere(featureWeights, value -> value > 50, value -> 50)
        );
    }
}
