package cc.sighs.oelib.bless.util;

import cc.sighs.oelib.bless.FestivalToastConfig;
import cc.sighs.oelib.bless.OverlayRegistry;
import cc.sighs.oelib.bless.render.AbstractShaderOverlay;
import cc.sighs.oelib.bless.render.ChongYangOverlay;
import cc.sighs.oelib.bless.render.NewYearOverlay;
import cc.sighs.oelib.bless.render.ValentineOverlay;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import net.minecraft.client.Minecraft;

import java.time.LocalDate;

public class BlessUtil {
    public static void checkFestival(AbstractShaderOverlay overlay, LocalDate today, Solar solar, Lunar lunar, boolean chineseLanguage, FestivalToastConfig config) {
        var id = overlay.festivalId();
        var preferences = config.festival(id);
        if (!preferences.enabled) {
            return;
        }
        if (overlay.isChineseFestival() && config.chineseFestivalsOnlyForChineseLanguage && !chineseLanguage) {
            return;
        }
        boolean match = isFestivalMatch(overlay, solar, lunar);
        if (!match) {
            return;
        }
        if (config.hasShownToday(id, today)) {
            return;
        }
        config.markShownToday(id, today);
        overlay.show();
    }

    public static boolean isFestivalMatch(AbstractShaderOverlay overlay, Solar solar, Lunar lunar) {
        var id = overlay.festivalId();
        var name = overlay.festivalName();
        return switch (id) {
            case NewYearOverlay.FESTIVAL_ID, ChongYangOverlay.FESTIVAL_ID -> lunar.getFestivals().contains(name);
            case ValentineOverlay.FESTIVAL_ID -> solar.getFestivals().contains(name);
            default -> false;
        };
    }

    public static boolean isChineseLanguage(Minecraft minecraft, FestivalToastConfig config) {
        var code = minecraft.options.languageCode;
        if (code.isEmpty()) {
            return false;
        }
        return code.startsWith("zh");
    }

    public static void showFestivalToast() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        var config = FestivalToastConfig.get();
        if (!config.enabled) return;

        var today = LocalDate.now();
        boolean chineseLanguage = isChineseLanguage(minecraft, config);

        var solar = Solar.fromYmd(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth()
        );
        var lunar = solar.getLunar();

        for (AbstractShaderOverlay overlay : OverlayRegistry.REGISTERED_OVERLAYS) {
            checkFestival(
                    overlay, today, solar, lunar, chineseLanguage, config
            );
        }

        FestivalToastConfig.save();
    }
}
