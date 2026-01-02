package cc.sighs.oelib.neoforge.bless;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.bless.util.BlessUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
public class FestivalToastManager {

    private static int delayTicks = -1;

    private FestivalToastManager() {
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        delayTicks = 40;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (delayTicks < 0) return;

        delayTicks--;
        if (delayTicks > 0) return;

        delayTicks = -1;
        BlessUtil.showFestivalToast();
    }
}