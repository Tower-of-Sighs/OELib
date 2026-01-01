package com.sighs.oelib.neoforge.bless;

import com.nlf.calendar.Solar;
import com.sighs.oelib.OELib;
import com.sighs.oelib.bless.FestivalToastConfig;
import com.sighs.oelib.bless.render.ChongYangOverlay;
import com.sighs.oelib.bless.render.NewYearOverlay;
import com.sighs.oelib.bless.util.FestivalUtil;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.time.LocalDate;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
public class FestivalToastManager {
    private FestivalToastManager() {
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        var config = FestivalToastConfig.get();
        if (!config.enabled) {
            return;
        }
        var today = LocalDate.now();
        boolean chineseLanguage = FestivalUtil.isChineseLanguage(minecraft, config);
        var solar = Solar.fromYmd(today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        var lunar = solar.getLunar();
        FestivalUtil.checkFestival(NewYearOverlay.INSTANCE, today, solar, lunar, chineseLanguage, config);
        FestivalUtil.checkFestival(ChongYangOverlay.INSTANCE, today, solar, lunar, chineseLanguage, config);
        FestivalToastConfig.save();
    }
}