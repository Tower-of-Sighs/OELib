package cc.sighs.oelib.forge.event;

import cc.sighs.oelib.event.EventBus;
import cc.sighs.oelib.event.LogicalSide;
import cc.sighs.oelib.event.events.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.*;

public final class TickBridge {
    private TickBridge() {}

    public static void initServer() {
        MinecraftForge.EVENT_BUS.addListener(TickBridge::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(TickBridge::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(TickBridge::onPlayerTick);
    }

    public static void initClient() {
        MinecraftForge.EVENT_BUS.addListener(TickBridge::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(TickBridge::onRenderTick);
    }

    private static void onServerTick(ServerTickEvent e) {
        TickEvent.Phase phase = e.phase == Phase.START ? TickEvent.Phase.START : TickEvent.Phase.END;
        EventBus.post(new TickEvent.ServerTickEvent(phase, e::haveTime, e.getServer()));
    }

    private static void onLevelTick(LevelTickEvent e) {
        TickEvent.Phase phase = e.phase == Phase.START ? TickEvent.Phase.START : TickEvent.Phase.END;
        LogicalSide side = e.side.isClient() ? LogicalSide.CLIENT : LogicalSide.SERVER;
        EventBus.post(new TickEvent.LevelTickEvent(side, phase, e.level, e::haveTime));
    }

    private static void onPlayerTick(PlayerTickEvent e) {
        TickEvent.Phase phase = e.phase == Phase.START ? TickEvent.Phase.START : TickEvent.Phase.END;
        EventBus.post(new TickEvent.PlayerTickEvent(phase, e.player));
    }

    private static void onClientTick(ClientTickEvent e) {
        TickEvent.Phase phase = e.phase == Phase.START ? TickEvent.Phase.START : TickEvent.Phase.END;
        EventBus.post(new TickEvent.ClientTickEvent(phase));
    }

    private static void onRenderTick(RenderTickEvent e) {
        TickEvent.Phase phase = e.phase == Phase.START ? TickEvent.Phase.START : TickEvent.Phase.END;
        EventBus.post(new TickEvent.RenderTickEvent(phase, e.renderTickTime));
    }
}

