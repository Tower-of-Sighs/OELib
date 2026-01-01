package com.sighs.oelib.bless.util;

import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import com.sighs.oelib.bless.FestivalToastConfig;
import com.sighs.oelib.bless.render.AbstractShaderOverlay;
import com.sighs.oelib.bless.render.ChongYangOverlay;
import com.sighs.oelib.bless.render.NewYearOverlay;
import net.minecraft.client.Minecraft;

import java.time.LocalDate;

public class FestivalUtil {
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
            case NewYearOverlay.FESTIVAL_ID, ChongYangOverlay.FESTIVAL_ID -> solar.getFestivals().contains(name);
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
}
