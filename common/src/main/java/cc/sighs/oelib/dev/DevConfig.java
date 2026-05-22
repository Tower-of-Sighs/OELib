package cc.sighs.oelib.dev;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigMetaCodec;
import cc.sighs.oelib.config.ConfigSchema;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record DevConfig(
        boolean enableExampleContent,
        List<String> testList,
        String testString,
        double testDouble,
        int testInt,
        TestEnum testEnum,
        NestedDemo nestedDemo
) {
    private static final String FILE_NAME = "dev_features";

    public static final ConfigSchema.Definition<DevConfig> DEFINITION = ConfigSchema.defineServer(
            new ResourceLocation(OELib.MODID, "dev_features"),
            DevConfig.class,
            meta -> meta
                    .directory(OELib.MODID)
                    .fileName(FILE_NAME)
                    .format(ConfigStorageFormat.TOML),
            schema -> schema.group(
                    ConfigField.bool("enableExampleContent")
                            .comment("是否启用示例/测试内容")
                            .tooltip()
                            .defaultValue(false)
                            .forGetter(DevConfig::enableExampleContent),
                    ConfigField.list("testList", Codec.STRING)
                            .defaultValue(List.of(
                                    "test1",
                                    "test2"
                            ))
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
                    ConfigSchema.record("nestedDemo", NestedDemo.class, NestedDemo.META_CODEC, DevConfig::nestedDemo)
            ).apply(schema, DevConfig::new)
    );
    public static final ConfigUnit<DevConfig> UNIT = DEFINITION.unit();

    public static void register() {
        ConfigManager.registerServer(UNIT, player -> player.hasPermissions(4));
    }

    public enum TestEnum {
        TEST,
        TEST2
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
}
