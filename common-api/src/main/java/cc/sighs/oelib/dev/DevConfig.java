package cc.sighs.oelib.dev;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigField;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import net.minecraft.resources.ResourceLocation;

public class DevConfig {
    private static final String FILE_NAME = "dev_features";

    public static final ConfigUnit<DevConfig> UNIT = ConfigManager.registerServer(
            ResourceLocation.fromNamespaceAndPath(OELib.MODID, "dev_features"),
            instance -> instance.group(
                    ConfigField.bool("enableExampleContent")
                            .comment("是否启用示例/测试内容")
                            .forGetter(cfg -> cfg.enableExampleContent)
            ).apply(instance, DevConfig::new),
            new DevConfig(false),
            meta -> meta
                    .directory(OELib.MODID)
                    .fileName(FILE_NAME)
                    .format(ConfigStorageFormat.TOML),
            player -> player.hasPermissions(4)
    );

    public boolean enableExampleContent;

    private DevConfig(boolean enableExampleContent) {
        this.enableExampleContent = enableExampleContent;
    }
}
