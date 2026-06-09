package cc.sighs.oelib.bless;

import cc.sighs.oelib.bless.render.ChongYangOverlay;
import cc.sighs.oelib.bless.render.NewYearOverlay;
import cc.sighs.oelib.bless.render.ValentineOverlay;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigSchema;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public record FestivalToastConfig(
        boolean enabled,
        boolean chineseFestivalsOnlyForChineseLanguage,
        Map<String, FestivalEntry> festivals
) {
    private static final String FILE_NAME = "oelib_festivals";

    public static final ConfigSchema.Definition<FestivalToastConfig> DEFINITION = ConfigSchema.defineClient(
            MethodHandles.lookup(),
            ResourceLocation.fromNamespaceAndPath("oelib", "festival_toast"),
            FestivalToastConfig.class,
            meta -> meta
                    .directory("oelib")
                    .fileName(FILE_NAME)
                    .format(ConfigStorageFormat.TOML),
            schema -> schema.group(
                    ConfigField.bool("enabled")
                            .defaultValue(true)
                            .tooltip()
                            .comment("是否启用节日祝福")
                            .forGetter(FestivalToastConfig::enabled),
                    ConfigField.bool("chineseFestivalsOnlyForChineseLanguage")
                            .defaultValue(true)
                            .tooltip()
                            .comment("仅在中文语言环境显示中文节日")
                            .forGetter(FestivalToastConfig::chineseFestivalsOnlyForChineseLanguage),
                    ConfigField.map("festivals", Codec.STRING, FestivalEntry.CODEC)
                            .defaultValue(defaultFestivals())
                            .forGetter(FestivalToastConfig::festivals)
            ).apply(schema, FestivalToastConfig::new)
    );
    public static final ConfigUnit<FestivalToastConfig> UNIT = DEFINITION.unit();

    public static FestivalToastConfig get() {
        return UNIT.get();
    }

    public static void register() {
        ConfigManager.registerClient(UNIT);
    }

    private static Map<String, FestivalEntry> defaultFestivals() {
        Map<String, FestivalEntry> m = new HashMap<>();
        m.put(NewYearOverlay.FESTIVAL_ID, new FestivalEntry(true, ""));
        m.put(ChongYangOverlay.FESTIVAL_ID, new FestivalEntry(true, ""));
        m.put(ValentineOverlay.FESTIVAL_ID, new FestivalEntry(true, ""));
        return m;
    }

    public record FestivalEntry(boolean enabled, String lastShownDate) {
        public static final Codec<FestivalEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.fieldOf("enabled").orElse(true).forGetter(FestivalEntry::enabled),
                        Codec.STRING.fieldOf("lastShownDate").orElse("").forGetter(FestivalEntry::lastShownDate)
                ).apply(instance, FestivalEntry::new)
        );
    }
}
