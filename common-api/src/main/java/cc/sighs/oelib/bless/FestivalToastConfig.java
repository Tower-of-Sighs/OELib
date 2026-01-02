package cc.sighs.oelib.bless;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.bless.render.ChongYangOverlay;
import cc.sighs.oelib.bless.render.NewYearOverlay;
import cc.sighs.oelib.bless.render.ValentineOverlay;
import cc.sighs.oelib.config.ConfigField;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.ui.ConfigUiHint;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public final class FestivalToastConfig {
    private static final String FILE_NAME = "oelib_festivals";
    public static final ConfigUnit<FestivalToastConfig> UNIT = ConfigManager.register(
            ResourceLocation.fromNamespaceAndPath(OELib.MODID, "festival_toast"),
            instance -> instance.group(
                    ConfigField.bool("enabled")
                            .comment("是否启用节日祝福")
                            .ui(ConfigUiHint.toggle())
                            .forGetter(cfg -> cfg.enabled),
                    ConfigField.bool("chineseFestivalsOnlyForChineseLanguage")
                            .comment("仅在中文语言环境显示中文节日")
                            .ui(ConfigUiHint.toggle())
                            .forGetter(cfg -> cfg.chineseFestivalsOnlyForChineseLanguage),
                    Codec.unboundedMap(Codec.STRING, FestivalPreferences.CODEC)
                            .fieldOf("festivals")
                            .forGetter(cfg -> cfg.festivals)
            ).apply(instance, FestivalToastConfig::new),
            createDefault(),
            meta -> meta
                    .directory(OELib.MODID)
                    .fileName(FILE_NAME)
                    .format(ConfigStorageFormat.TOML)
                    .side(ConfigSide.CLIENT)
    );
    public boolean enabled;
    public boolean chineseFestivalsOnlyForChineseLanguage;
    public Map<String, FestivalPreferences> festivals;

    private FestivalToastConfig(boolean enabled, boolean chineseOnly, Map<String, FestivalPreferences> festivals) {
        this.enabled = enabled;
        this.chineseFestivalsOnlyForChineseLanguage = chineseOnly;
        this.festivals = new HashMap<>(festivals);
        ensureDefaults();
    }

    public static FestivalToastConfig createDefault() {
        return new FestivalToastConfig(true, true, new HashMap<>());
    }

    public static FestivalToastConfig get() {
        return UNIT.get();
    }

    public static void save() {
        UNIT.save();
    }

    private void ensureDefaults() {
        this.festivals.putIfAbsent(NewYearOverlay.FESTIVAL_ID, new FestivalPreferences());
        this.festivals.putIfAbsent(ChongYangOverlay.FESTIVAL_ID, new FestivalPreferences());
        this.festivals.putIfAbsent(ValentineOverlay.FESTIVAL_ID, new FestivalPreferences());
    }

    public FestivalPreferences festival(String id) {
        return festivals.computeIfAbsent(id, k -> new FestivalPreferences());
    }

    public boolean hasShownToday(String festivalId, LocalDate today) {
        var preferences = festival(festivalId);
        if (!preferences.enabled) {
            return true;
        }
        if (preferences.lastShownDate == null) {
            return false;
        }
        return today.toString().equals(preferences.lastShownDate);
    }

    public void markShownToday(String festivalId, LocalDate today) {
        var preferences = festival(festivalId);
        preferences.lastShownDate = today.toString();
        save();
    }

    public static final class FestivalPreferences {
        public static final Codec<FestivalPreferences> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("enabled").orElse(true).forGetter(p -> p.enabled),
                Codec.STRING.fieldOf("lastShownDate").orElse("").forGetter(p -> p.lastShownDate == null ? "" : p.lastShownDate)
        ).apply(instance, FestivalPreferences::new));
        public String lastShownDate;
        public boolean enabled;

        public FestivalPreferences(boolean enabled, String lastShownDate) {
            this.enabled = enabled;
            this.lastShownDate = (lastShownDate == null || lastShownDate.isEmpty()) ? null : lastShownDate;
        }

        public FestivalPreferences() {
            this(true, null);
        }
    }
}