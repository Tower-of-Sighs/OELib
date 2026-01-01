package com.sighs.oelib.fabric.bless;

import com.nlf.calendar.Solar;
import com.sighs.oelib.bless.FestivalToastConfig;
import com.sighs.oelib.bless.render.ChongYangOverlay;
import com.sighs.oelib.bless.render.NewYearOverlay;
import com.sighs.oelib.bless.util.FestivalUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.time.LocalDate;

@Environment(EnvType.CLIENT)
public class FestivalToastManager {
    public static void init() {
        ClientPlayConnectionEvents.JOIN.register(FestivalToastManager::onClientLoggingIn);
    }

    public static void onClientLoggingIn(ClientPacketListener handler, PacketSender sender, Minecraft client) {
        if (client.player == null) return;

        var config = FestivalToastConfig.get();
        if (!config.enabled) return;

        var today = LocalDate.now();
        boolean chineseLanguage = FestivalUtil.isChineseLanguage(client, config);

        var solar = Solar.fromYmd(today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        var lunar = solar.getLunar();

        FestivalUtil.checkFestival(NewYearOverlay.INSTANCE, today, solar, lunar, chineseLanguage, config);
        FestivalUtil.checkFestival(ChongYangOverlay.INSTANCE, today, solar, lunar, chineseLanguage, config);

        FestivalToastConfig.save();
    }
}