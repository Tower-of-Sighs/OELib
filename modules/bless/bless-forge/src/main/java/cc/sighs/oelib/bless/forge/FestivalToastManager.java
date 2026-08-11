package cc.sighs.oelib.bless.forge;

import cc.sighs.oelib.bless.OELibBless;
import cc.sighs.oelib.bless.util.BlessUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = OELibBless.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FestivalToastManager {

    private static int delayTicks = -1;

    private FestivalToastManager() {
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        delayTicks = 40;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (delayTicks < 0) return;

        delayTicks--;
        if (delayTicks > 0) return;

        delayTicks = -1;
        BlessUtil.showFestivalToast();
    }
}