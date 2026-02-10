package cc.sighs.oelib.fabric.bless;

import cc.sighs.oelib.bless.util.BlessUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

@Environment(EnvType.CLIENT)
public class FestivalToastManager {

    private static int delayTicks = -1;

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register(FestivalToastManager::onClientJoin);
        ClientTickEvents.END_CLIENT_TICK.register(FestivalToastManager::onClientTick);
    }

    private static void onClientJoin(
            ClientPacketListener handler,
            PacketSender sender,
            Minecraft client
    ) {
        delayTicks = 40;
    }

    private static void onClientTick(Minecraft client) {
        if (delayTicks < 0) return;
        if (client.player == null) return;

        delayTicks--;
        if (delayTicks > 0) return;

        delayTicks = -1;
        BlessUtil.showFestivalToast();
    }
}