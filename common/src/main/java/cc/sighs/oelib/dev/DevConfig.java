package cc.sighs.oelib.dev;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigRecordCodecBuilder;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;

public record DevConfig(
        boolean enableExampleContent,
        List<String> testList,
        String testString,
        double testDouble,
        int testInt,
        TestEnum testEnum
) {
    private static final String FILE_NAME = "dev_features";
    public static final Permission OP_LEVEL_4 = new Permission.HasCommandLevel(PermissionLevel.OWNERS);

    public static final ConfigUnit<DevConfig> UNIT = ConfigRecordCodecBuilder.create(
            Identifier.fromNamespaceAndPath(OELib.MODID, "dev_features"),
            instance -> instance.group(
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
                            .forGetter(DevConfig::testEnum)
            ).apply(instance, DevConfig::new),
            meta -> meta
                    .directory(OELib.MODID)
                    .fileName(FILE_NAME)
                    .format(ConfigStorageFormat.TOML)
    );

    public static void register() {
        ConfigManager.registerServer(UNIT, player -> player.permissions().hasPermission(OP_LEVEL_4));
    }

    public enum TestEnum {
        TEST,
        TEST2
    }
}
