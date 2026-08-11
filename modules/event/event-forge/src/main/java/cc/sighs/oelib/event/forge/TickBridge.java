package cc.sighs.oelib.event.forge;

import cc.sighs.oelib.event.EventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public final class TickBridge {
    private TickBridge() {
    }

    public static void initServer() {
        MinecraftForge.EVENT_BUS.addListener(TickBridge::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(TickBridge::onLevelTick);
    }

    public static void initClient() {
        MinecraftForge.EVENT_BUS.addListener(TickBridge::onClientTick);
    }

    private static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            EventBus.post(new cc.sighs.oelib.event.events.ServerTickEvent.Pre(() -> true, e.getServer()));
        } else {
            EventBus.post(new cc.sighs.oelib.event.events.ServerTickEvent.Post(() -> true, e.getServer()));
        }
    }

    private static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            EventBus.post(new cc.sighs.oelib.event.events.LevelTickEvent.Pre(() -> true, e.level));
        } else {
            EventBus.post(new cc.sighs.oelib.event.events.LevelTickEvent.Post(() -> true, e.level));
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            EventBus.post(new cc.sighs.oelib.event.events.ClientTickEvent.Pre());
        } else {
            EventBus.post(new cc.sighs.oelib.event.events.ClientTickEvent.Post());
        }
    }
}
