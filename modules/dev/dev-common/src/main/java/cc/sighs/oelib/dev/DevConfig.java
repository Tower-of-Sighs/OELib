package cc.sighs.oelib.dev;

import cc.sighs.oelib.config.*;
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

    public static final ConfigSchema.Definition<DevConfig> DEFINITION = ConfigSchema.defineServer(
            MethodHandles.lookup(),
            ResourceLocation.fromNamespaceAndPath("oelib", "dev_features"),
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
    
    public static final ConfigUnit<DevConfig> UNIT = DEFINITION.unit();

    public static void register() {
        ConfigManager.registerServer(UNIT, player -> player.hasPermissions(4));
    }

    /**
     * ConfigPath API 使用示例，仅作演示无实际用途。
     */
    public static void exampleUsages() {
        var enabledPath = DEFINITION.path(DevConfig::enableExampleContent);
        var testStringPath = DEFINITION.path(DevConfig::testString);
        var testDoublePath = DEFINITION.path(DevConfig::testDouble);
        var levelModePath = DEFINITION.pathSubtype(DevConfig::demoMode, LevelMode.class);
        var levelModeLevelPath = levelModePath.then(LevelMode::level);
        var nestedLevelPath = DEFINITION.path(DevConfig::nestedDemo).then(NestedDemo::level);
        var optionalChildPath = DEFINITION.pathOptional(DevConfig::optionalChild);
        var optionalChildEnabledPath = optionalChildPath.then(NestedDemo::enabled);
        var optionalChildLevelPath = optionalChildPath.then(NestedDemo::level);
        var featurePath = DEFINITION.pathEach(DevConfig::featureConfigs);
        var resettableFeaturePath = featurePath.where(feature -> feature.weight() > 10);
        var featureWeightPath = featurePath.then(FeatureConfig::weight);
        var featureExtraLevelPath = featurePath.thenOptional(FeatureConfig::extraConfig).then(NestedDemo::level);
        var firstFeatureWeightPath = featureWeightPath.at(0);
        var heavyFeatureWeightPath = featureWeightPath.where(weight -> weight > 10);
        var wholeMapValuePath = DEFINITION.pathValues(DevConfig::featureConfigMap);
        var mapValuePath = DEFINITION.pathValue(DevConfig::featureConfigMap, "beta");
        var mapBetaWeightPath = mapValuePath.then(FeatureConfig::weight);
        var mapWeightPath = DEFINITION.pathValues(DevConfig::featureConfigMap).then(FeatureConfig::weight);
        var mapKeysPath = DEFINITION.pathKeys(DevConfig::featureConfigMap);
        var mapExtraEnabledPath = mapValuePath.thenOptional(FeatureConfig::extraConfig).then(NestedDemo::enabled);

        // getter API：第一层字段，以及“替换整个元素值”的场景。
        UNIT.update(DevConfig::enableExampleContent, value -> !value);
        UNIT.update(DevConfig::testInt, value -> value + 10);
        UNIT.update(DevConfig::testDouble, value -> value * 1.5);
        UNIT.update(DevConfig::testString, value -> value + "_modified");
        UNIT.update(DevConfig::testEnum, ignored -> TestEnum.TEST2);
        UNIT.ifPresent(DevConfig::optionalChild, child -> new NestedDemo(!child.enabled(), child.level() + 1));
        UNIT.whenSubtype(DevConfig::demoMode, LevelMode.class, mode -> new LevelMode(mode.level() + 1));
        UNIT.updateElements(DevConfig::testList, value -> value + "_item");
        UNIT.updateWhere(
                DevConfig::featureConfigs,
                feature -> feature.weight() > 10,
                feature -> new FeatureConfig(feature.id(), 0, feature.extraConfig())
        );
        UNIT.updateValues(
                DevConfig::featureConfigMap,
                feature -> new FeatureConfig(feature.id(), feature.weight() * 2, feature.extraConfig())
        );

        List<FeatureConfig> getterFeatureList = UNIT.getAll(DevConfig::featureConfigs);
        List<FeatureConfig> getterFilteredFeatures = UNIT.getAllWhere(DevConfig::featureConfigs, feature -> feature.weight() > 10);
        long getterFeatureCount = UNIT.count(DevConfig::featureConfigs);
        boolean getterAnyFeature = UNIT.anyMatch(DevConfig::featureConfigs, feature -> feature.weight() > 10);
        boolean getterAllFeatures = UNIT.allMatch(DevConfig::featureConfigs, feature -> feature.weight() >= 0);
        List<FeatureConfig> getterFeatureValues = UNIT.getValues(DevConfig::featureConfigMap);
        List<String> getterFeatureKeys = UNIT.getKeys(DevConfig::featureConfigMap);

        // path API：适合局部字段更新，也能表达子类型、集合筛选和按 key 直达。
        boolean enabled = UNIT.view(enabledPath);
        Optional<LevelMode> levelMode = UNIT.preview(levelModePath);
        int nestedLevel = UNIT.view(nestedLevelPath);
        Optional<NestedDemo> optionalChild = UNIT.preview(optionalChildPath);
        Optional<Boolean> optionalChildEnabled = UNIT.preview(optionalChildEnabledPath);

        UNIT.update(enabledPath, value -> !value);
        UNIT.update(testStringPath, value -> value + "_path");
        UNIT.update(testDoublePath, value -> value + 0.25);
        UNIT.ifPresent(levelModeLevelPath, value -> value + 1);
        UNIT.whenSubtype(levelModePath, value -> new LevelMode(value.level() + 1));
        UNIT.update(nestedLevelPath, value -> value + 1);
        UNIT.ifPresent(optionalChildEnabledPath, value -> !value);
        UNIT.ifPresent(optionalChildLevelPath, value -> value + 2);

        // List 路径既能替换整元素，也能只改元素内部的某个字段。
        UNIT.updateEach(
                resettableFeaturePath,
                feature -> new FeatureConfig(feature.id(), 1, feature.extraConfig())
        );
        UNIT.updateEach(featureWeightPath, weight -> weight + 5);
        UNIT.ifPresent(firstFeatureWeightPath, weight -> weight * 2);
        UNIT.updateEach(featureExtraLevelPath, level -> level + 1);
        UNIT.updateEach(heavyFeatureWeightPath, weight -> weight / 2);

        long featureCount = UNIT.count(featureWeightPath);
        boolean anyHeavyFeature = UNIT.anyMatch(featureWeightPath, weight -> weight > 50);
        boolean allWeightsNonNegative = UNIT.allMatch(featureWeightPath, weight -> weight >= 0);
        Optional<Integer> firstHeavyWeight = UNIT.findFirst(featureWeightPath, weight -> weight > 10);
        List<Integer> allFeatureWeights = UNIT.getAll(featureWeightPath);

        // Map 路径同理：wholeMapValuePath 是 updateValues 的 path 等价物，
        // mapWeightPath / mapBetaWeightPath 则是局部字段版本。
        UNIT.updateEach(
                wholeMapValuePath,
                feature -> new FeatureConfig(feature.id(), feature.weight() + 1, feature.extraConfig())
        );
        UNIT.updateEach(mapWeightPath, weight -> weight + 10);
        UNIT.ifPresent(mapBetaWeightPath, weight -> weight + 99);
        UNIT.ifPresent(mapExtraEnabledPath, value -> !value);

        List<Integer> allMappedWeights = UNIT.getAll(mapWeightPath);
        List<String> allFeatureKeys = UNIT.getAll(mapKeysPath);
        Optional<FeatureConfig> betaFeature = UNIT.preview(mapValuePath);

        // ConfigUnitOps 提供 no-save 和批量修改入口。
        DevConfig draftValue = ConfigUnitOps.updateNoSave(UNIT, testStringPath, value -> value + "_draft");
        String draftString = ConfigUnitOps.setAndGetNoSave(UNIT, testStringPath, "draft");
        ConfigUnitOps.whenSubtypeNoSave(UNIT, levelModePath, value -> new LevelMode(value.level() + 1));
        ConfigUnitOps.ifPresentNoSave(UNIT, optionalChildEnabledPath, value -> !value);
        ConfigUnitOps.updateEachNoSave(UNIT, featureWeightPath, weight -> weight + 3);
        ConfigUnitOps.withBatchNoSave(UNIT, batch -> {
            batch.update(DevConfig::testInt, value -> value + 1);
            batch.updateInt(DevConfig::testInt, value -> value + 1);
            batch.updateDouble(DevConfig::testDouble, value -> value + 1.0);
            batch.updateBoolean(DevConfig::enableExampleContent, value -> !value);
            batch.update(enabledPath, value -> !value);
            batch.setAndGet(DevConfig::testString, "getter-batched");
            batch.setAndGet(testStringPath, "batched");
            batch.ifPresent(levelModePath, value -> new LevelMode(value.level() + 1));
            batch.ifPresent(mapExtraEnabledPath, value -> !value);
            batch.updateEach(featureWeightPath, weight -> weight + 1);
        });
        ConfigUnitOps.withBatch(UNIT, batch -> batch.update(testStringPath, value -> value + "_saved"));

        // ConfigMutation 同时支持 getter 风格和 path 风格。
        UNIT.updateAll(
                ConfigMutation.map(DevConfig::testString, value -> value + "_getter"),
                ConfigMutation.ifPresent(DevConfig::optionalChild, child -> new NestedDemo(child.enabled(), child.level() + 1)),
                ConfigMutation.ifPresent(DevConfig::demoMode, LevelMode.class, mode -> new LevelMode(mode.level() + 1)),
                ConfigMutation.updateWhere(
                        DevConfig::featureConfigs,
                        feature -> feature.weight() > 10,
                        feature -> new FeatureConfig(feature.id(), feature.weight() - 1, feature.extraConfig())
                ),
                ConfigMutation.updateValues(
                        DevConfig::featureConfigMap,
                        feature -> new FeatureConfig(feature.id(), feature.weight() + 1, feature.extraConfig())
                ),
                ConfigMutation.set(testStringPath, "frozen"),
                ConfigMutation.map(testDoublePath, value -> value * 1.1),
                ConfigMutation.ifPresent(optionalChildLevelPath, value -> value + 1),
                ConfigMutation.updateEach(featureWeightPath, ignored -> 0)
        );
    }
}
