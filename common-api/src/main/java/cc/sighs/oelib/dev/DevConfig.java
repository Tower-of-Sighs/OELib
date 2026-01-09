package cc.sighs.oelib.dev;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigRecordCodecBuilder;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import net.minecraft.resources.ResourceLocation;

public record DevConfig(
        boolean enableExampleContent
) {
    private static final String FILE_NAME = "dev_features";

    public static final ConfigUnit<DevConfig> UNIT = ConfigRecordCodecBuilder.create(
            ResourceLocation.fromNamespaceAndPath(OELib.MODID, "dev_features"),
            instance -> instance.group(
                    ConfigField.bool("enableExampleContent")
                            .comment("是否启用示例/测试内容")
                            .defaultValue(false)
                            .forGetter(DevConfig::enableExampleContent)
            ).apply(instance, DevConfig::new),
            meta -> meta
                    .directory(OELib.MODID)
                    .fileName(FILE_NAME)
                    .format(ConfigStorageFormat.TOML)
    );

    public static void register() {
        ConfigManager.registerServer(UNIT, player -> player.hasPermissions(4));
    }
}
