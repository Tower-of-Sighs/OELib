package cc.sighs.oelib.bless;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.bless.render.ChongYangOverlay;
import cc.sighs.oelib.bless.render.NewYearOverlay;
import cc.sighs.oelib.bless.render.ValentineOverlay;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigRecordCodecBuilder;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record FestivalToastConfig(
        boolean enabled,
        boolean chineseFestivalsOnlyForChineseLanguage,
        Map<String, FestivalEntry> festivals,
        List<String> testList,
        String testString,
        double testDouble,
        int testInt,
        TestEnum testEnum
) {
    private static final String FILE_NAME = "oelib_festivals";

    public static final ConfigUnit<FestivalToastConfig> UNIT = ConfigRecordCodecBuilder.createClient(
            ResourceLocation.fromNamespaceAndPath(OELib.MODID, "festival_toast"),
            instance -> instance.group(
                    ConfigField.bool("enabled")
                            .defaultValue(true)
                            .comment("是否启用节日祝福")
                            .forGetter(FestivalToastConfig::enabled),
                    ConfigField.bool("chineseFestivalsOnlyForChineseLanguage")
                            .defaultValue(true)
                            .comment("仅在中文语言环境显示中文节日")
                            .forGetter(FestivalToastConfig::chineseFestivalsOnlyForChineseLanguage),
                    ConfigField.map("festivals", Codec.STRING, FestivalEntry.CODEC)
                            .defaultValue(defaultFestivals())
                            .forGetter(FestivalToastConfig::festivals),
                    ConfigField.list("testList", Codec.STRING)
                            .defaultValue(List.of(
                                    "test1",
                                    "test2"
                            ))
                            .comment("测试 List 用")
                            .forGetter(FestivalToastConfig::testList),
                    ConfigField.string("testString")
                            .defaultValue("test")
                            .comment("测试 String 用")
                            .forGetter(FestivalToastConfig::testString),
                    ConfigField.doubleRange("testDouble", 1.0, 100.0)
                            .defaultValue(50.0)
                            .comment("测试 Double 用")
                            .forGetter(FestivalToastConfig::testDouble),
                    ConfigField.intRange("testInt", 1, 100)
                            .defaultValue(50)
                            .text()
                            .comment("测试 Int 用")
                            .forGetter(FestivalToastConfig::testInt),
                    ConfigField.enumValue("testEnum", TestEnum.class)
                            .defaultValue(TestEnum.TEST)
                            .comment("测试 Enum 用")
                            .forGetter(FestivalToastConfig::testEnum)
            ).apply(instance, FestivalToastConfig::new),
            meta -> meta
                    .directory(OELib.MODID)
                    .fileName(FILE_NAME)
                    .format(ConfigStorageFormat.TOML)
    );

    public static FestivalToastConfig get() {
        return UNIT.get();
    }

    public static void save() {
        UNIT.save();
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

    public enum TestEnum {
        TEST,
        TEST2
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
